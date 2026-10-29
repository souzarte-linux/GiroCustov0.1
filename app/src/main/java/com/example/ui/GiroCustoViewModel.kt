package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.*
import com.example.network.fipe.*
import com.example.network.supabase.*
import com.example.network.viacep.*
import com.example.ui.delegates.*
import com.example.ui.navigation.RootNavState
import com.example.util.BiometricHelper
import com.example.util.CurrencyUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Period {
    SEMANA,
    QUINZENA,
    MENSAL,
    ULTIMOS_30_DIAS,
    PERSONALIZADO
}

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

enum class ConsumptionAlertLevel {
    GREEN,
    AMBER,
    RED,
    GRAY;

    val isEfficient: Boolean get() = this == GREEN
}

data class FuelConsumptionComparisonState(
    val theoreticalConsumption: Double = 0.0,
    val realConsumption: Double? = null,
    val differencePercentage: Double? = null,
    val alertLevel: ConsumptionAlertLevel = ConsumptionAlertLevel.GRAY,
    val message: String = "",
    val hasSufficientData: Boolean = false
)

data class FuelCostEstimationInfo(
    val vehicleModel: String = "",
    val distanceKm: Double = 0.0,
    val effectiveConsumption: Double = 0.0,
    val isUsingRealConsumption: Boolean = false,
    val estimatedFuelCost: Double = 0.0,
    val fuelPriceUsed: Double = 0.0,
    val displayText: String = ""
)

sealed class AccountConflictState {
    abstract val pendingCount: Int

    data class UnknownOwnerWithPending(
        val currentUserId: String,
        override val pendingCount: Int
    ) : AccountConflictState()

    data class KnownOwnerMismatchWithPending(
        val previousOwnerId: String,
        val currentUserId: String,
        override val pendingCount: Int
    ) : AccountConflictState()
}

data class EstimationDetail(
    val distance: Double = 0.0,
    val fuelCost: Double = 0.0,
    val wearCost: Double = 0.0,
    val fixedCost: Double = 0.0,
    val foodCost: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0
)

class GiroCustoViewModel @JvmOverloads constructor(
    application: Application,
    val dataOwnerStorage: DataOwnerStorage = SharedPreferencesDataOwnerStorage(application),
    val repository: GiroCustoRepository = GiroCustoRepository(GiroCustoDatabase.getDatabase(application), dataOwnerStorage)
) : AndroidViewModel(application) {
    private val db = GiroCustoDatabase.getDatabase(application)
    private val supabaseClient = SupabaseClient.getInstance(application)

    // Supabase Cloud Auth & Session
    val currentUser: StateFlow<AuthSession?> = supabaseClient.sessionState

    // 1. Coordenador FIPE e ViaCEP
    private val fipeViaCepCoordinator: FipeAndViaCepCoordinator = FipeAndViaCepCoordinator(viewModelScope)
    val viaCepState: StateFlow<ViaCepUiState> = fipeViaCepCoordinator.viaCepState
    val fipeUiState: StateFlow<FipeUiState> = fipeViaCepCoordinator.fipeUiState
    val fipeMarcas: StateFlow<List<FipeItemDto>> = fipeViaCepCoordinator.fipeMarcas
    val fipeModelos: StateFlow<List<FipeItemDto>> = fipeViaCepCoordinator.fipeModelos
    val fipeAnos: StateFlow<List<FipeItemDto>> = fipeViaCepCoordinator.fipeAnos
    val fipeDetail: StateFlow<FipeDetailDto?> = fipeViaCepCoordinator.fipeDetail

    // 2. Coordenador de Busca de Postos
    private val gasStationCoordinator: GasStationSearchCoordinator = GasStationSearchCoordinator(viewModelScope)
    val nearbyGasStations: StateFlow<List<GasStationResult>> = gasStationCoordinator.nearbyGasStations
    val isSearchingStations: StateFlow<Boolean> = gasStationCoordinator.isSearchingStations
    val stationSearchError: StateFlow<String?> = gasStationCoordinator.stationSearchError
    val lastSearchLocation: StateFlow<Pair<Double, Double>?> = gasStationCoordinator.lastSearchLocation

    fun clearStationSearch() = gasStationCoordinator.clearStationSearch()
    fun searchNearbyGasStations(context: Context) = gasStationCoordinator.searchNearbyGasStations(context)

    // 3. Coordenador de Sincronização e Conflitos
    private val syncCoordinator: SyncAndConflictCoordinator = SyncAndConflictCoordinator(
        scope = viewModelScope,
        application = application,
        repository = repository,
        dataOwnerStorage = dataOwnerStorage,
        supabaseClient = supabaseClient,
        onResetSpecificPreferences = {
            clearViaCepState()
            clearFipe()
        }
    )
    val syncState: StateFlow<SyncState> = syncCoordinator.syncState
    val isAccountSwitching: StateFlow<Boolean> = syncCoordinator.isAccountSwitching
    val accountConflictState: StateFlow<AccountConflictState?> = syncCoordinator.accountConflictState
    val showDiscardDoubleConfirmationDialog: StateFlow<Boolean> = syncCoordinator.showDiscardDoubleConfirmationDialog
    val showClaimDataConfirmationDialog: StateFlow<Boolean> = syncCoordinator.showClaimDataConfirmationDialog
    val lastSyncTime: StateFlow<Long?> = syncCoordinator.lastSyncTime
    val syncErrorMessage: StateFlow<String?> = syncCoordinator.syncErrorMessage

    fun openDiscardConfirmation() = syncCoordinator.openDiscardConfirmation()
    fun dismissDiscardConfirmation() = syncCoordinator.dismissDiscardConfirmation()
    fun openClaimDataConfirmation() = syncCoordinator.openClaimDataConfirmation()
    fun dismissClaimDataConfirmation() = syncCoordinator.dismissClaimDataConfirmation()
    fun resolveConflictReturnToPreviousAccount() = syncCoordinator.resolveConflictReturnToPreviousAccount()
    fun confirmDiscardData(onComplete: () -> Unit = {}) = syncCoordinator.confirmDiscardData(onComplete)
    fun confirmClaimData(onComplete: () -> Unit = {}) = syncCoordinator.confirmClaimData(onComplete)
    fun triggerSync(onComplete: ((Boolean, String?) -> Unit)? = null) = syncCoordinator.triggerSync(onComplete)
    fun syncNow(onComplete: ((Boolean, String?) -> Unit)? = null) = syncCoordinator.syncNow(onComplete)
    fun clearSyncError() = syncCoordinator.clearSyncError()
    fun formatLastSyncTime(timestamp: Long?): String = syncCoordinator.formatLastSyncTime(timestamp)

    // 4. Coordenador de Autenticação e Biometria
    private val authCoordinator: AuthAndAccountCoordinator = AuthAndAccountCoordinator(
        scope = viewModelScope,
        application = application,
        repository = repository,
        dataOwnerStorage = dataOwnerStorage,
        supabaseClient = supabaseClient,
        syncCoordinator = syncCoordinator,
        onClearCaches = {
            clearViaCepState()
            clearFipe()
        }
    )
    val isBiometricLocked: StateFlow<Boolean> = authCoordinator.isBiometricLocked
    val authUiState: StateFlow<AuthUiState> = authCoordinator.authUiState
    val authErrorResId: StateFlow<Int?> = authCoordinator.authErrorResId
    val isBiometricAvailable: StateFlow<Boolean> = authCoordinator.isBiometricAvailable

    fun refreshBiometricAvailability() = authCoordinator.refreshBiometricAvailability()
    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) =
        authCoordinator.signInWithEmail(email, pass, onResult)
    fun signUpWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) =
        authCoordinator.signUpWithEmail(email, pass, onResult)
    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit = { _, _ -> }) =
        authCoordinator.signInWithGoogle(context, onResult)
    fun unlockWithBiometrics(onResult: (Boolean, String?) -> Unit = { _, _ -> }) =
        authCoordinator.unlockWithBiometrics(onResult)
    fun handleBiometricKeyInvalidated() = authCoordinator.handleBiometricKeyInvalidated()
    fun dismissBiometricLockForFallback() = authCoordinator.dismissBiometricLockForFallback()
    fun resetAuthError() = authCoordinator.resetAuthError()
    fun signInWithBiometrics(onResult: (Boolean, String?) -> Unit = { _, _ -> }) =
        authCoordinator.signInWithBiometrics(onResult)
    fun lockAppForBiometrics() = authCoordinator.lockAppForBiometrics()
    fun logout() = authCoordinator.logout()
    fun exportUserData(onResult: (String) -> Unit) = authCoordinator.exportUserData(onResult)
    fun deleteAccountAndData(onResult: (Boolean, String?) -> Unit) = authCoordinator.deleteAccountAndData(onResult)

    fun loginSupabase(email: String, pass: String, onResult: (Boolean, String?) -> Unit) = signInWithEmail(email, pass, onResult)
    fun registerSupabase(email: String, pass: String, onResult: (Boolean, String?) -> Unit) = signUpWithEmail(email, pass, onResult)
    fun logoutSupabase() = logout()

    // 5. Coordenador de Metas e Tema
    private val goalsCoordinator: GoalsAndThemeCoordinator = GoalsAndThemeCoordinator(
        scope = viewModelScope,
        application = application,
        repository = repository,
        supabaseClient = supabaseClient,
        triggerSync = { triggerSync() }
    )
    val dailyGoalGross: MutableStateFlow<Double> get() = goalsCoordinator.dailyGoalGross
    val dailyGoalNet: MutableStateFlow<Double> get() = goalsCoordinator.dailyGoalNet
    val dailyGoalKm: MutableStateFlow<Double> get() = goalsCoordinator.dailyGoalKm
    val dailyGoalDeliveries: MutableStateFlow<Int> get() = goalsCoordinator.dailyGoalDeliveries

    val weeklyGoalGross: MutableStateFlow<Double> get() = goalsCoordinator.weeklyGoalGross
    val weeklyGoalNet: MutableStateFlow<Double> get() = goalsCoordinator.weeklyGoalNet
    val weeklyGoalKm: MutableStateFlow<Double> get() = goalsCoordinator.weeklyGoalKm
    val weeklyGoalDeliveries: MutableStateFlow<Int> get() = goalsCoordinator.weeklyGoalDeliveries

    val monthlyGoalGross: MutableStateFlow<Double> get() = goalsCoordinator.monthlyGoalGross
    val monthlyGoalNet: MutableStateFlow<Double> get() = goalsCoordinator.monthlyGoalNet
    val monthlyGoalKm: MutableStateFlow<Double> get() = goalsCoordinator.monthlyGoalKm
    val monthlyGoalDeliveries: MutableStateFlow<Int> get() = goalsCoordinator.monthlyGoalDeliveries

    val isDarkTheme: MutableStateFlow<Boolean> get() = goalsCoordinator.isDarkTheme
    fun toggleTheme() = goalsCoordinator.toggleTheme()
    fun updateGoals(
        dailyG: Double, dailyN: Double, dailyK: Double, dailyD: Int,
        weeklyG: Double, weeklyN: Double, weeklyK: Double, weeklyD: Int,
        monthlyG: Double, monthlyN: Double, monthlyK: Double, monthlyD: Int
    ) = goalsCoordinator.updateGoals(dailyG, dailyN, dailyK, dailyD, weeklyG, weeklyN, weeklyK, weeklyD, monthlyG, monthlyN, monthlyK, monthlyD)

    // Flows do Banco de Dados
    val vehicle: StateFlow<Vehicle?> = repository.vehicleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehiclesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val isProfileInitialized = MutableStateFlow(false)

    val rootNavState: StateFlow<RootNavState> = combine(
        currentUser,
        userProfile,
        isProfileInitialized,
        isBiometricLocked
    ) { user, profile, isInitialized, isLocked ->
        when {
            isLocked -> RootNavState.BiometricLocked
            user == null -> RootNavState.Unauthenticated
            !isInitialized -> RootNavState.Initializing
            profile?.onboardingCompleted == true -> RootNavState.AuthenticatedReady
            else -> RootNavState.OnboardingPending
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = RootNavState.Initializing
    )

    val allPlatforms: StateFlow<List<Platform>> = repository.allPlatformsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parts: StateFlow<List<VehiclePart>> = repository.partsForActiveVehicleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val records: StateFlow<List<DailyRecord>> = repository.allRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fuelRefills: StateFlow<List<FuelRefill>> = repository.refillsForActiveVehicleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val maintenanceRecords: StateFlow<List<MaintenanceRecord>> = vehicle
        .flatMapLatest { v ->
            if (v != null) {
                repository.maintenanceRecordsForVehicleFlow(v.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val realAverageConsumption: StateFlow<Double?> = combine(vehicle, fuelRefills) { v, _ ->
        val activeVehicleId = v?.id ?: 0L
        if (activeVehicleId != 0L) {
            repository.calculateRealAverageConsumption(activeVehicleId)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fuelComparisonState: StateFlow<FuelConsumptionComparisonState> = combine(
        vehicle,
        realAverageConsumption
    ) { currentVehicle, real ->
        val theoretical = currentVehicle?.averageConsumption ?: 0.0
        if (currentVehicle != null && real != null && real > 0.0 && theoretical > 0.0) {
            val diff = ((real - theoretical) / theoretical) * 100.0
            val alertLevel = when {
                real >= theoretical * 0.95 -> ConsumptionAlertLevel.GREEN
                real >= theoretical * 0.80 -> ConsumptionAlertLevel.AMBER
                else -> ConsumptionAlertLevel.RED
            }
            val message = when (alertLevel) {
                ConsumptionAlertLevel.GREEN -> "Excelente! Seu veículo está operando com ótima eficiência em relação à média de fábrica."
                ConsumptionAlertLevel.AMBER -> "Atenção: Consumo moderado. Ligeiramente abaixo da média cadastrada."
                ConsumptionAlertLevel.RED -> "Alerta: Consumo elevado! Vale revisar calibragem de pneus, filtros ou manutenção."
                ConsumptionAlertLevel.GRAY -> ""
            }
            FuelConsumptionComparisonState(
                theoreticalConsumption = theoretical,
                realConsumption = real,
                differencePercentage = diff,
                alertLevel = alertLevel,
                message = message,
                hasSufficientData = true
            )
        } else {
            FuelConsumptionComparisonState(
                theoreticalConsumption = theoretical,
                realConsumption = real,
                differencePercentage = null,
                alertLevel = ConsumptionAlertLevel.GRAY,
                message = "Abasteça com tanque cheio pelo menos 2 vezes para calcular e comparar seu consumo real.",
                hasSufficientData = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FuelConsumptionComparisonState())

    val averageFuelPrice: StateFlow<Double> = fuelRefills
        .map { _ ->
            val activeVehicleId = vehicle.value?.id ?: 0L
            if (activeVehicleId != 0L) {
                repository.averageFuelPrice(activeVehicleId)
            } else {
                0.0
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageLitersPerRefill: StateFlow<Double> = fuelRefills
        .map { _ ->
            val activeVehicleId = vehicle.value?.id ?: 0L
            if (activeVehicleId != 0L) {
                repository.averageLitersPerRefill(activeVehicleId)
            } else {
                0.0
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun saveFuelRefill(refill: FuelRefill) {
        viewModelScope.launch { repository.saveFuelRefill(refill) }
    }

    fun deleteFuelRefill(refill: FuelRefill) {
        viewModelScope.launch { repository.deleteFuelRefill(refill) }
    }

    fun renameGasStation(oldName: String, newName: String) {
        viewModelScope.launch { repository.renameGasStation(oldName, newName) }
    }

    fun saveMaintenanceRecord(record: MaintenanceRecord) {
        viewModelScope.launch { repository.saveMaintenanceRecord(record) }
    }

    fun deleteMaintenanceRecord(record: MaintenanceRecord) {
        viewModelScope.launch { repository.deleteMaintenanceRecord(record) }
    }

    // 6. Coordenador de Lançamento Diário
    private val launchCoordinator: DailyLaunchCoordinator = DailyLaunchCoordinator(
        scope = viewModelScope,
        repository = repository,
        vehicleFlow = vehicle,
        partsFlow = parts,
        realAverageConsumptionFlow = realAverageConsumption
    )
    val startOdometer: MutableStateFlow<String> get() = launchCoordinator.startOdometer
    val endOdometer: MutableStateFlow<String> get() = launchCoordinator.endOdometer
    val grossEarnings: MutableStateFlow<String> get() = launchCoordinator.grossEarnings
    val deliveriesCount: MutableStateFlow<String> get() = launchCoordinator.deliveriesCount
    val fuelPrice: MutableStateFlow<String> get() = launchCoordinator.fuelPrice
    val foodExpense: MutableStateFlow<String> get() = launchCoordinator.foodExpense
    val startTime: MutableStateFlow<String> get() = launchCoordinator.startTime
    val endTime: MutableStateFlow<String> get() = launchCoordinator.endTime
    val pauseDuration: MutableStateFlow<String> get() = launchCoordinator.pauseDuration
    val launchDateTimestamp: MutableStateFlow<Long> get() = launchCoordinator.launchDateTimestamp
    val platform: MutableStateFlow<String> get() = launchCoordinator.platform

    fun setLaunchDate(timestamp: Long) = launchCoordinator.setLaunchDate(timestamp)
    val launchCalculatedDistance: StateFlow<LaunchDistanceState> get() = launchCoordinator.launchCalculatedDistance
    val realTimeEstimation: Flow<EstimationDetail> get() = launchCoordinator.realTimeEstimation
    val fuelCostEstimationInfo: StateFlow<FuelCostEstimationInfo> get() = launchCoordinator.fuelCostEstimationInfo
    suspend fun prefillLaunchFields() = launchCoordinator.prefillLaunchFields()
    fun saveDailyRecord(onSuccess: () -> Unit, onError: (String) -> Unit) = launchCoordinator.saveDailyRecord(onSuccess, onError)
    fun deleteDailyRecord(record: DailyRecord) = launchCoordinator.deleteDailyRecord(record)

    fun updateDailyRecord(
        recordId: Long,
        dateString: String,
        dateTimestamp: Long,
        platform: String,
        grossEarnings: Long,
        deliveriesCount: Int,
        startOdometer: Double,
        endOdometer: Double,
        fuelPrice: Long,
        foodExpense: Long,
        startTime: String = "",
        endTime: String = "",
        pauseDuration: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = launchCoordinator.updateDailyRecord(
        recordId, dateString, dateTimestamp, platform, grossEarnings, deliveriesCount,
        startOdometer, endOdometer, fuelPrice, foodExpense, startTime, endTime, pauseDuration, onSuccess, onError
    )

    fun updateDailyRecord(
        recordId: Long,
        dateString: String,
        dateTimestamp: Long,
        platform: String,
        grossEarnings: Double,
        deliveriesCount: Int,
        startOdometer: Double,
        endOdometer: Double,
        fuelPrice: Double,
        foodExpense: Double,
        startTime: String = "",
        endTime: String = "",
        pauseDuration: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = updateDailyRecord(
        recordId = recordId,
        dateString = dateString,
        dateTimestamp = dateTimestamp,
        platform = platform,
        grossEarnings = CurrencyUtils.toCents(grossEarnings),
        deliveriesCount = deliveriesCount,
        startOdometer = startOdometer,
        endOdometer = endOdometer,
        fuelPrice = CurrencyUtils.toFuelPriceMillis(fuelPrice),
        foodExpense = CurrencyUtils.toCents(foodExpense),
        startTime = startTime,
        endTime = endTime,
        pauseDuration = pauseDuration,
        onSuccess = onSuccess,
        onError = onError
    )

    // Períodos e Relatórios
    private val _selectedPeriod = MutableStateFlow(Period.SEMANA)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    val customStartDate = MutableStateFlow(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
    val customEndDate = MutableStateFlow(System.currentTimeMillis())

    fun setPeriod(period: Period) { _selectedPeriod.value = period }
    fun setCustomPeriod(start: Long, end: Long) {
        customStartDate.value = start
        customEndDate.value = end
    }

    private val _reportsSelectedPeriod = MutableStateFlow(Period.SEMANA)
    val reportsSelectedPeriod: StateFlow<Period> = _reportsSelectedPeriod.asStateFlow()

    val reportsCustomStartDate = MutableStateFlow(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
    val reportsCustomEndDate = MutableStateFlow(System.currentTimeMillis())

    fun setReportsPeriod(period: Period) { _reportsSelectedPeriod.value = period }
    fun setReportsCustomPeriod(start: Long, end: Long) {
        reportsCustomStartDate.value = start
        reportsCustomEndDate.value = end
    }

    fun calculateDailyDistance(startOdo: String, endOdo: String): LaunchDistanceState =
        GiroCustoDomainCalculations.calculateDailyDistance(startOdo, endOdo)

    fun calculateRefillMultiDirectional(
        pricePerLiterStr: String,
        litersStr: String,
        discountStr: String,
        totalPaidStr: String,
        focusedField: String?
    ): RefillCalculationResult =
        GiroCustoDomainCalculations.calculateRefillMultiDirectional(
            pricePerLiterStr, litersStr, discountStr, totalPaidStr, focusedField
        )

    fun checkExceedsTankCapacity(litersStr: String, tankCapacity: Double): Boolean =
        GiroCustoDomainCalculations.checkExceedsTankCapacity(litersStr, tankCapacity)

    fun computeRefillTotal(pricePerLiterStr: String, litersStr: String, discountStr: String, totalPaidStr: String): Double =
        GiroCustoDomainCalculations.computeRefillTotal(pricePerLiterStr, litersStr, discountStr, totalPaidStr)

    fun isValidFuelInput(pricePerLiterStr: String, litersStr: String, odometerStr: String): Boolean =
        GiroCustoDomainCalculations.isValidFuelInput(pricePerLiterStr, litersStr, odometerStr)

    fun calculateReportMetrics(
        records: List<DailyRecord>,
        maintenanceRecords: List<MaintenanceRecord>,
        selectedPeriod: Period,
        customStart: Long,
        customEnd: Long,
        plannedWorkDays: Int,
        monthlyGoalNet: Double
    ): ReportMetricsState = GiroCustoDomainCalculations.calculateReportMetrics(
        records = records,
        maintenanceRecords = maintenanceRecords,
        selectedPeriod = selectedPeriod,
        customStart = customStart,
        customEnd = customEnd,
        plannedWorkDays = plannedWorkDays,
        monthlyGoalNet = monthlyGoalNet
    )

    private val reportsFilterStateFlow: Flow<ReportsFilterState> = combine(
        reportsSelectedPeriod,
        reportsCustomStartDate,
        reportsCustomEndDate
    ) { period, start, end ->
        ReportsFilterState(period, start, end)
    }

    val reportMetrics: StateFlow<ReportMetricsState> = combine(
        records,
        maintenanceRecords,
        reportsFilterStateFlow,
        vehicle,
        monthlyGoalNet
    ) { recs, maint, filter, v, goalNet ->
        calculateReportMetrics(
            records = recs,
            maintenanceRecords = maint,
            selectedPeriod = filter.period,
            customStart = filter.customStart,
            customEnd = filter.customEnd,
            plannedWorkDays = v?.plannedWorkDays ?: 22,
            monthlyGoalNet = goalNet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportMetricsState())

    init {
        syncCoordinator.onLogoutFallback = { logout() }

        viewModelScope.launch {
            repository.userProfileFlow.collect {
                isProfileInitialized.value = true
            }
        }

        viewModelScope.launch {
            repository.checkAndSeedData()
            prefillLaunchFields()
            if (supabaseClient.isAuthenticated && !isBiometricLocked.value) {
                triggerSync()
            }
        }
    }

    // FIPE e ViaCEP
    fun searchCep(rawCep: String) = fipeViaCepCoordinator.searchCep(rawCep)
    fun clearViaCepState() = fipeViaCepCoordinator.clearViaCepState()
    fun loadFipeMarcas(vehicleType: String) = fipeViaCepCoordinator.loadFipeMarcas(vehicleType)
    fun loadFipeModelos(vehicleType: String, brandCode: String) = fipeViaCepCoordinator.loadFipeModelos(vehicleType, brandCode)
    fun loadFipeAnos(vehicleType: String, brandCode: String, modelCode: String) = fipeViaCepCoordinator.loadFipeAnos(vehicleType, brandCode, modelCode)
    fun loadFipeDetail(vehicleType: String, brandCode: String, modelCode: String, yearCode: String) = fipeViaCepCoordinator.loadFipeDetail(vehicleType, brandCode, modelCode, yearCode)
    fun clearFipe() = fipeViaCepCoordinator.clearFipe()

    fun completeOnboarding(
        name: String,
        phone: String,
        isWhatsapp: Boolean,
        cep: String,
        street: String,
        addressNumber: String,
        neighborhood: String,
        city: String,
        stateUf: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (name.isBlank()) {
            onError("O nome completo é obrigatório.")
            return
        }
        if (phone.isBlank()) {
            onError("O telefone para contato é obrigatório.")
            return
        }
        val cleanCep = cep.replace("\\D".toRegex(), "")
        if (cleanCep.length != 8) {
            onError("Informe um CEP válido com 8 dígitos.")
            return
        }

        viewModelScope.launch {
            try {
                val current = repository.getUserProfile() ?: UserProfile()
                val userEmail = currentUser.value?.userEmail ?: current.email
                val updated = current.copy(
                    name = name.trim(),
                    phone = phone.trim(),
                    isWhatsapp = isWhatsapp,
                    cep = cleanCep,
                    street = street.trim(),
                    addressNumber = addressNumber.trim(),
                    neighborhood = neighborhood.trim(),
                    city = city.trim(),
                    stateUf = stateUf.trim().uppercase(),
                    email = if (userEmail.isNotBlank()) userEmail else current.email,
                    onboardingCompleted = true,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPDATE
                )
                repository.saveUserProfile(updated)
                triggerSync()
                onSuccess()
            } catch (e: Exception) {
                onError("Erro ao salvar cadastro: ${e.localizedMessage ?: "Erro desconhecido"}")
            }
        }
    }

    fun checkPendingChangesBeforeLogout(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val hasPending = repository.hasPendingSyncChanges()
            onResult(hasPending)
        }
    }

    // Veículo
    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch { repository.saveVehicle(vehicle) }
    }

    fun addVehicle(
        model: String,
        consumption: Double,
        fuelType: String,
        fixedCosts: Double,
        workDays: Int,
        vehicleType: String = "MOTO",
        brand: String = "",
        manufacturingYear: String = "",
        tankCapacity: Double = 16.0,
        hasTopBox: Boolean = false,
        hasDeliveryBag: Boolean = false,
        carBodyType: String = ""
    ) {
        viewModelScope.launch {
            val newVehicle = Vehicle(
                model = model,
                averageConsumption = consumption,
                fuelType = fuelType,
                monthlyFixedCosts = CurrencyUtils.toCents(fixedCosts),
                plannedWorkDays = workDays,
                vehicleType = vehicleType,
                brand = brand,
                manufacturingYear = manufacturingYear,
                tankCapacity = tankCapacity,
                hasTopBox = hasTopBox,
                hasDeliveryBag = hasDeliveryBag,
                carBodyType = carBodyType,
                active = false
            )
            repository.saveVehicle(newVehicle)
        }
    }

    fun setActiveVehicle(vehicleId: Long) {
        viewModelScope.launch { repository.setActiveVehicle(vehicleId) }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch { repository.deleteVehicle(vehicle) }
    }

    fun restoreVehicle(vehicle: Vehicle) {
        viewModelScope.launch { repository.restoreVehicle(vehicle) }
    }

    // Perfil
    fun updateUserProfile(
        name: String,
        phone: String,
        city: String,
        email: String? = null,
        isWhatsapp: Boolean? = null,
        cep: String? = null,
        street: String? = null,
        addressNumber: String? = null,
        neighborhood: String? = null,
        stateUf: String? = null,
        onboardingCompleted: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = repository.getUserProfile() ?: UserProfile()
            val updated = current.copy(
                name = name,
                phone = phone,
                city = city,
                email = email ?: current.email,
                isWhatsapp = isWhatsapp ?: current.isWhatsapp,
                cep = cep ?: current.cep,
                street = street ?: current.street,
                addressNumber = addressNumber ?: current.addressNumber,
                neighborhood = neighborhood ?: current.neighborhood,
                stateUf = stateUf ?: current.stateUf,
                onboardingCompleted = onboardingCompleted ?: current.onboardingCompleted,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE
            )
            repository.saveUserProfile(updated)
            triggerSync()
        }
    }

    fun saveUserProfile(userProfile: UserProfile) {
        viewModelScope.launch {
            val toSave = userProfile.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE
            )
            repository.saveUserProfile(toSave)
            triggerSync()
        }
    }

    // Plataforma
    fun savePlatform(platform: Platform) {
        viewModelScope.launch { repository.savePlatform(platform) }
    }

    fun deletePlatform(platform: Platform) {
        viewModelScope.launch { repository.deletePlatform(platform) }
    }

    // Peças de Desgaste
    fun addPart(name: String, price: Double, lifespan: Double, vehicleId: Long? = null) {
        viewModelScope.launch {
            val vId = vehicleId ?: vehicle.value?.id ?: 0L
            val part = VehiclePart(vehicleId = vId, name = name, price = CurrencyUtils.toCents(price), lifespanKm = lifespan, runKmSinceChange = 0.0)
            repository.savePart(part)
        }
    }

    fun updatePart(partId: Long, name: String, price: Double, lifespan: Double, runKmSinceChange: Double, vehicleId: Long? = null) {
        viewModelScope.launch {
            val vId = vehicleId ?: vehicle.value?.id ?: 0L
            val part = VehiclePart(id = partId, vehicleId = vId, name = name, price = CurrencyUtils.toCents(price), lifespanKm = lifespan, runKmSinceChange = runKmSinceChange)
            repository.updatePart(part)
        }
    }

    fun deletePart(part: VehiclePart) {
        viewModelScope.launch { repository.deletePart(part) }
    }

    fun restorePart(part: VehiclePart) {
        viewModelScope.launch { repository.restorePart(part) }
    }

    fun resetPartWear(partId: Long) {
        viewModelScope.launch { repository.resetPartWear(partId) }
    }

    // Demonstração / Reset
    fun seedJune2026Data(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.seedJune2026Data()
                prefillLaunchFields()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Erro desconhecido")
            }
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllData()
            prefillLaunchFields()
            onComplete()
        }
    }
}
