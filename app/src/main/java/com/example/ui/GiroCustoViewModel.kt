package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.*
import com.example.network.supabase.*
import com.example.network.viacep.*
import com.example.network.fipe.*
import com.example.ui.navigation.RootNavState
import com.example.util.BiometricHelper
import com.example.util.GoogleAuthHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

class GiroCustoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GiroCustoDatabase.getDatabase(application)
    private val repository = GiroCustoRepository(db)

    // Supabase Cloud Auth & Sync
    private val supabaseClient = SupabaseClient.getInstance(application)
    val currentUser: StateFlow<AuthSession?> = supabaseClient.sessionState

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val syncPrefs = application.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE)

    private val _lastSyncTime = MutableStateFlow<Long?>(
        syncPrefs.getLong("last_sync_timestamp", 0L).takeIf { it > 0L }
    )
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    // GPS Gas Station Search States
    private val _nearbyGasStations = MutableStateFlow<List<GasStationResult>>(emptyList())
    val nearbyGasStations: StateFlow<List<GasStationResult>> = _nearbyGasStations.asStateFlow()

    private val _isSearchingStations = MutableStateFlow(false)
    val isSearchingStations: StateFlow<Boolean> = _isSearchingStations.asStateFlow()

    private val _stationSearchError = MutableStateFlow<String?>(null)
    val stationSearchError: StateFlow<String?> = _stationSearchError.asStateFlow()

    private val _lastSearchLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val lastSearchLocation: StateFlow<Pair<Double, Double>?> = _lastSearchLocation.asStateFlow()

    fun clearStationSearch() {
        _nearbyGasStations.value = emptyList()
        _stationSearchError.value = null
        _lastSearchLocation.value = null
    }

    fun searchNearbyGasStations(context: Context) {
        viewModelScope.launch {
            _isSearchingStations.value = true
            _stationSearchError.value = null
            _nearbyGasStations.value = emptyList()
            _lastSearchLocation.value = null

            val location = LocationHelper.getCurrentLocation(context)
            if (location != null) {
                _lastSearchLocation.value = Pair(location.latitude, location.longitude)
                when (val result = GasStationRepository.findNearbyGasStations(location.latitude, location.longitude)) {
                    is GasStationSearchResult.Success -> {
                        _nearbyGasStations.value = result.stations
                        if (result.stations.isEmpty()) {
                            _stationSearchError.value = "Nenhum posto de combustível encontrado nas proximidades."
                        }
                    }
                    is GasStationSearchResult.Error -> {
                        _stationSearchError.value = result.message
                    }
                }
            } else {
                _stationSearchError.value = "Não foi possível obter a localização. Verifique se o GPS está ativo e a permissão concedida."
            }
            _isSearchingStations.value = false
        }
    }

    // Flows do Banco de Dados
    val vehicle: StateFlow<Vehicle?> = repository.vehicleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehiclesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Indicador interno de inicialização do Room Flow para evitar flickering
    private val isProfileInitialized = MutableStateFlow(false)

    // Estado de Bloqueio por Biometria
    private val _isBiometricLocked = MutableStateFlow(
        supabaseClient.hasSavedSession() && BiometricHelper.canUnlockWithBiometrics(application)
    )
    val isBiometricLocked: StateFlow<Boolean> = _isBiometricLocked.asStateFlow()

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

    // Estados de UI para Login, Cadastro, Biometria e ViaCEP
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _viaCepState = MutableStateFlow<ViaCepUiState>(ViaCepUiState.Idle)
    val viaCepState: StateFlow<ViaCepUiState> = _viaCepState.asStateFlow()

    // Estados de UI e Seletores para Tabela FIPE (Parallelum)
    private val _fipeUiState = MutableStateFlow<FipeUiState>(FipeUiState.Idle)
    val fipeUiState: StateFlow<FipeUiState> = _fipeUiState.asStateFlow()

    private val _fipeMarcas = MutableStateFlow<List<FipeItemDto>>(emptyList())
    val fipeMarcas: StateFlow<List<FipeItemDto>> = _fipeMarcas.asStateFlow()

    private val _fipeModelos = MutableStateFlow<List<FipeItemDto>>(emptyList())
    val fipeModelos: StateFlow<List<FipeItemDto>> = _fipeModelos.asStateFlow()

    private val _fipeAnos = MutableStateFlow<List<FipeItemDto>>(emptyList())
    val fipeAnos: StateFlow<List<FipeItemDto>> = _fipeAnos.asStateFlow()

    private val _fipeDetail = MutableStateFlow<FipeDetailDto?>(null)
    val fipeDetail: StateFlow<FipeDetailDto?> = _fipeDetail.asStateFlow()

    private val _isBiometricAvailable = MutableStateFlow(BiometricHelper.isBiometricHardwareAvailable(application))
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()

    fun refreshBiometricAvailability() {
        _isBiometricAvailable.value = BiometricHelper.isBiometricHardwareAvailable(getApplication())
    }

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
        viewModelScope.launch {
            repository.saveFuelRefill(refill)
        }
    }

    fun deleteFuelRefill(refill: FuelRefill) {
        viewModelScope.launch {
            repository.deleteFuelRefill(refill)
        }
    }

    fun renameGasStation(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.renameGasStation(oldName, newName)
        }
    }

    fun saveMaintenanceRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            repository.saveMaintenanceRecord(record)
        }
    }

    fun deleteMaintenanceRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            repository.deleteMaintenanceRecord(record)
        }
    }

    // Estado de Período Selecionado no Dashboard
    private val _selectedPeriod = MutableStateFlow(Period.SEMANA)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    // Estados para Período Personalizado
    val customStartDate = MutableStateFlow(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
    val customEndDate = MutableStateFlow(System.currentTimeMillis())

    fun setCustomPeriod(start: Long, end: Long) {
        customStartDate.value = start
        customEndDate.value = end
    }

    // Estado de Período Selecionado nos Relatórios (Reports)
    private val _reportsSelectedPeriod = MutableStateFlow(Period.SEMANA)
    val reportsSelectedPeriod: StateFlow<Period> = _reportsSelectedPeriod.asStateFlow()

    // Estados para Período Personalizado nos Relatórios (Reports)
    val reportsCustomStartDate = MutableStateFlow(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
    val reportsCustomEndDate = MutableStateFlow(System.currentTimeMillis())

    fun setReportsPeriod(period: Period) {
        _reportsSelectedPeriod.value = period
    }

    fun setReportsCustomPeriod(start: Long, end: Long) {
        reportsCustomStartDate.value = start
        reportsCustomEndDate.value = end
    }

    // Estado do Formulário de Lançamento (Lançar Novo Dia)
    var startOdometer = MutableStateFlow("")
    var endOdometer = MutableStateFlow("")
    var grossEarnings = MutableStateFlow("")
    var deliveriesCount = MutableStateFlow("")
    var fuelPrice = MutableStateFlow("")
    var foodExpense = MutableStateFlow("")
    var startTime = MutableStateFlow("")
    var endTime = MutableStateFlow("")
    var pauseDuration = MutableStateFlow("")
    val launchDateTimestamp = MutableStateFlow(System.currentTimeMillis())
    val platform = MutableStateFlow("")

    fun setLaunchDate(timestamp: Long) {
        launchDateTimestamp.value = timestamp
    }

    // Preferências de Metas
    private val prefs = application.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE)

    // Metas Editáveis (Valores de Destino)
    val dailyGoalGross = MutableStateFlow(prefs.getFloat("daily_goal_gross", 200.0f).toDouble())
    val dailyGoalNet = MutableStateFlow(prefs.getFloat("daily_goal_net", 130.0f).toDouble())
    val dailyGoalKm = MutableStateFlow(prefs.getFloat("daily_goal_km", 100.0f).toDouble())
    val dailyGoalDeliveries = MutableStateFlow(prefs.getInt("daily_goal_deliveries", 15))

    val weeklyGoalGross = MutableStateFlow(prefs.getFloat("weekly_goal_gross", 1200.0f).toDouble())
    val weeklyGoalNet = MutableStateFlow(prefs.getFloat("weekly_goal_net", 800.0f).toDouble())
    val weeklyGoalKm = MutableStateFlow(prefs.getFloat("weekly_goal_km", 600.0f).toDouble())
    val weeklyGoalDeliveries = MutableStateFlow(prefs.getInt("weekly_goal_deliveries", 90))

    val monthlyGoalGross = MutableStateFlow(prefs.getFloat("monthly_goal_gross", 5000.0f).toDouble())
    val monthlyGoalNet = MutableStateFlow(prefs.getFloat("monthly_goal_net", 3200.0f).toDouble())
    val monthlyGoalKm = MutableStateFlow(prefs.getFloat("monthly_goal_km", 2500.0f).toDouble())
    val monthlyGoalDeliveries = MutableStateFlow(prefs.getInt("monthly_goal_deliveries", 400))

    // Tema Claro / Dark
    val isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))

    fun toggleTheme() {
        val newVal = !isDarkTheme.value
        isDarkTheme.value = newVal
        prefs.edit().putBoolean("is_dark_theme", newVal).apply()
    }

    init {
        if (supabaseClient.hasSavedSession() && BiometricHelper.canUnlockWithBiometrics(application)) {
            _isBiometricLocked.value = true
        }

        viewModelScope.launch {
            repository.userProfileFlow.collect {
                isProfileInitialized.value = true
            }
        }

        viewModelScope.launch {
            // Inicializar/Semear Banco de Dados se necessário
            repository.checkAndSeedData()
            
            // Auto-preencher odômetro inicial e combustível
            prefillLaunchFields()

            // Se houver sessão salva ao iniciar o ViewModel e não estiver bloqueado por biometria, sincroniza
            if (supabaseClient.isAuthenticated && !_isBiometricLocked.value) {
                triggerSync()
            }
        }
    }

    // =========================================================================
    // MÉTODOS DE AUTENTICAÇÃO, BIOMETRIA E ONBOARDING
    // =========================================================================

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (email.isBlank() || pass.isBlank()) {
            onResult(false, "Informe e-mail e senha.")
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            _syncState.value = SyncState.SYNCING
            _syncErrorMessage.value = null
            val result = supabaseClient.signIn(email.trim(), pass)
            if (result.isSuccess) {
                _isBiometricLocked.value = false
                _authUiState.value = AuthUiState.Success()
                _syncState.value = SyncState.IDLE
                BiometricHelper.setBiometricEnabled(getApplication(), true, email.trim())
                triggerSync()
                onResult(true, null)
            } else {
                _syncState.value = SyncState.ERROR
                val err = result.exceptionOrNull()?.message ?: "Falha ao autenticar"
                _syncErrorMessage.value = err
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (email.isBlank() || pass.length < 6) {
            onResult(false, "A senha deve conter no mínimo 6 caracteres.")
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            _syncState.value = SyncState.SYNCING
            _syncErrorMessage.value = null
            val result = supabaseClient.signUp(email.trim(), pass)
            if (result.isSuccess) {
                _isBiometricLocked.value = false
                _authUiState.value = AuthUiState.Success("Conta criada com sucesso!")
                _syncState.value = SyncState.IDLE
                BiometricHelper.setBiometricEnabled(getApplication(), true, email.trim())
                val session = result.getOrNull()
                if (session != null && session.accessToken.isNotBlank()) {
                    triggerSync()
                }
                onResult(true, null)
            } else {
                _syncState.value = SyncState.ERROR
                val err = result.exceptionOrNull()?.message ?: "Falha ao registrar conta"
                _syncErrorMessage.value = err
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun signInWithPhone(phone: String, otpCode: String = "", onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            _authUiState.value = AuthUiState.Error("Login por SMS em homologação.")
            onResult(false, "Login por SMS em homologação.")
        }
    }

    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            _syncState.value = SyncState.SYNCING
            _syncErrorMessage.value = null

            val authResult = GoogleAuthHelper.getGoogleIdToken(context)
            if (authResult.isFailure) {
                _syncState.value = SyncState.ERROR
                val err = authResult.exceptionOrNull()?.message ?: "Falha ao autenticar com o Google"
                _syncErrorMessage.value = err
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
                return@launch
            }

            val authData = authResult.getOrNull()!!
            val result = supabaseClient.signInWithGoogleIdToken(authData.idToken, authData.rawNonce)
            if (result.isSuccess) {
                _isBiometricLocked.value = false
                _authUiState.value = AuthUiState.Success()
                _syncState.value = SyncState.IDLE
                val session = result.getOrNull()
                val email = session?.userEmail?.takeIf { it.isNotBlank() } ?: "google_user"
                BiometricHelper.setBiometricEnabled(getApplication(), true, email)
                triggerSync()
                onResult(true, null)
            } else {
                _syncState.value = SyncState.ERROR
                val err = result.exceptionOrNull()?.message ?: "Falha ao autenticar com o Google"
                _syncErrorMessage.value = err
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun unlockWithBiometrics(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val restored = supabaseClient.restoreSavedSession()
            if (restored) {
                _isBiometricLocked.value = false
                _authUiState.value = AuthUiState.Success("Autenticado via biometria com sucesso!")
                onResult(true, null)
                launch {
                    supabaseClient.refreshCurrentSession()
                    triggerSync()
                }
            } else {
                val err = "Nenhuma sessão persistente encontrada para biometria."
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun signInWithBiometrics(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        unlockWithBiometrics(onResult)
    }

    fun lockAppForBiometrics() {
        if (supabaseClient.hasSavedSession() && BiometricHelper.canUnlockWithBiometrics(getApplication())) {
            _isBiometricLocked.value = true
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isBiometricLocked.value = false
            supabaseClient.signOut()
            BiometricHelper.clearBiometrics(getApplication())
            _authUiState.value = AuthUiState.Idle
            _syncState.value = SyncState.IDLE
            _syncErrorMessage.value = null
            clearViaCepState()
            clearFipe()
        }
    }

    // Compatibilidade com chamadas anteriores
    fun loginSupabase(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        signInWithEmail(email, password, onResult)
    }

    fun registerSupabase(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        signUpWithEmail(email, password, onResult)
    }

    fun logoutSupabase() {
        logout()
    }

    // =========================================================================
    // MÉTODOS VIACEP E ONBOARDING
    // =========================================================================

    fun searchCep(rawCep: String) {
        val cleanCep = rawCep.replace("\\D".toRegex(), "")
        if (cleanCep.length != 8) {
            _viaCepState.value = ViaCepUiState.Error("O CEP deve conter exatamente 8 dígitos numéricos.")
            return
        }

        viewModelScope.launch {
            _viaCepState.value = ViaCepUiState.Loading
            try {
                val response = ViaCepNetworkClient.api.getAddressByCep(cleanCep)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isErro) {
                        _viaCepState.value = ViaCepUiState.Error("CEP não encontrado na base dos Correios.")
                    } else {
                        _viaCepState.value = ViaCepUiState.Success(body)
                    }
                } else {
                    _viaCepState.value = ViaCepUiState.Error("Serviço de CEP temporariamente indisponível (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _viaCepState.value = ViaCepUiState.Error("Tempo limite excedido ao consultar o CEP.")
            } catch (e: java.io.IOException) {
                _viaCepState.value = ViaCepUiState.Error("Sem conexão de internet para buscar o CEP.")
            } catch (e: Exception) {
                _viaCepState.value = ViaCepUiState.Error("Erro inesperado: ${e.localizedMessage ?: "Falha na consulta"}")
            }
        }
    }

    fun clearViaCepState() {
        _viaCepState.value = ViaCepUiState.Idle
    }

    // =========================================================================
    // MÉTODOS TABELA FIPE (Parallelum API)
    // =========================================================================

    fun loadFipeMarcas(vehicleType: String) {
        val fipeType = FipeVehicleType.fromString(vehicleType).apiPath
        viewModelScope.launch {
            _fipeUiState.value = FipeUiState.Loading
            _fipeMarcas.value = emptyList()
            _fipeModelos.value = emptyList()
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            try {
                val response = FipeNetworkClient.api.getMarcas(fipeType)
                if (response.isSuccessful && response.body() != null) {
                    _fipeMarcas.value = response.body()!!
                    _fipeUiState.value = FipeUiState.Success
                } else {
                    _fipeUiState.value = FipeUiState.Error("Não foi possível carregar as marcas da Tabela FIPE (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fipeUiState.value = FipeUiState.Error("Tempo limite excedido ao consultar a Tabela FIPE. Verifique sua conexão.")
            } catch (e: java.io.IOException) {
                _fipeUiState.value = FipeUiState.Error("Sem conexão com a internet para consultar a Tabela FIPE.")
            } catch (e: Exception) {
                _fipeUiState.value = FipeUiState.Error("Erro inesperado ao consultar a Tabela FIPE: ${e.localizedMessage ?: "Falha na comunicação."}")
            }
        }
    }

    fun loadFipeModelos(vehicleType: String, brandCode: String) {
        if (brandCode.isBlank()) {
            _fipeModelos.value = emptyList()
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            return
        }
        val fipeType = FipeVehicleType.fromString(vehicleType).apiPath
        viewModelScope.launch {
            _fipeUiState.value = FipeUiState.Loading
            _fipeModelos.value = emptyList()
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            try {
                val response = FipeNetworkClient.api.getModelos(fipeType, brandCode)
                if (response.isSuccessful && response.body() != null) {
                    _fipeModelos.value = response.body()!!.modelos
                    _fipeUiState.value = FipeUiState.Success
                } else {
                    _fipeUiState.value = FipeUiState.Error("Não foi possível carregar os modelos da Tabela FIPE (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fipeUiState.value = FipeUiState.Error("Tempo limite excedido ao consultar os modelos. Tente novamente.")
            } catch (e: java.io.IOException) {
                _fipeUiState.value = FipeUiState.Error("Sem conexão com a internet para consultar os modelos.")
            } catch (e: Exception) {
                _fipeUiState.value = FipeUiState.Error("Erro inesperado ao consultar modelos: ${e.localizedMessage ?: "Falha na comunicação."}")
            }
        }
    }

    fun loadFipeAnos(vehicleType: String, brandCode: String, modelCode: String) {
        if (brandCode.isBlank() || modelCode.isBlank()) {
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            return
        }
        val fipeType = FipeVehicleType.fromString(vehicleType).apiPath
        viewModelScope.launch {
            _fipeUiState.value = FipeUiState.Loading
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            try {
                val response = FipeNetworkClient.api.getAnos(fipeType, brandCode, modelCode)
                if (response.isSuccessful && response.body() != null) {
                    _fipeAnos.value = response.body()!!
                    _fipeUiState.value = FipeUiState.Success
                } else {
                    _fipeUiState.value = FipeUiState.Error("Não foi possível carregar os anos do veículo (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fipeUiState.value = FipeUiState.Error("Tempo limite excedido ao consultar anos do veículo.")
            } catch (e: java.io.IOException) {
                _fipeUiState.value = FipeUiState.Error("Sem conexão com a internet para consultar anos do veículo.")
            } catch (e: Exception) {
                _fipeUiState.value = FipeUiState.Error("Erro inesperado ao consultar anos: ${e.localizedMessage ?: "Falha na comunicação."}")
            }
        }
    }

    fun loadFipeDetail(vehicleType: String, brandCode: String, modelCode: String, yearCode: String) {
        if (brandCode.isBlank() || modelCode.isBlank() || yearCode.isBlank()) {
            _fipeDetail.value = null
            return
        }
        val fipeType = FipeVehicleType.fromString(vehicleType).apiPath
        viewModelScope.launch {
            _fipeUiState.value = FipeUiState.Loading
            _fipeDetail.value = null
            try {
                val response = FipeNetworkClient.api.getDetail(fipeType, brandCode, modelCode, yearCode)
                if (response.isSuccessful && response.body() != null) {
                    _fipeDetail.value = response.body()
                    _fipeUiState.value = FipeUiState.Success
                } else {
                    _fipeUiState.value = FipeUiState.Error("Não foi possível carregar a avaliação da Tabela FIPE (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fipeUiState.value = FipeUiState.Error("Tempo limite excedido ao consultar detalhes da Tabela FIPE.")
            } catch (e: java.io.IOException) {
                _fipeUiState.value = FipeUiState.Error("Sem conexão com a internet para consultar detalhes da Tabela FIPE.")
            } catch (e: Exception) {
                _fipeUiState.value = FipeUiState.Error("Erro inesperado ao consultar detalhes: ${e.localizedMessage ?: "Falha na comunicação."}")
            }
        }
    }

    fun clearFipe() {
        _fipeUiState.value = FipeUiState.Idle
        _fipeMarcas.value = emptyList()
        _fipeModelos.value = emptyList()
        _fipeAnos.value = emptyList()
        _fipeDetail.value = null
    }

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

    fun triggerSync(onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            if (!supabaseClient.isAuthenticated) {
                _syncState.value = SyncState.IDLE
                onComplete?.invoke(false, "Usuário não autenticado")
                return@launch
            }

            _syncState.value = SyncState.SYNCING
            _syncErrorMessage.value = null

            val result = repository.syncCurrentSession()
            when (result) {
                is SyncResult.Success -> {
                    val now = System.currentTimeMillis()
                    _lastSyncTime.value = now
                    syncPrefs.edit().putLong("last_sync_timestamp", now).apply()
                    _syncState.value = SyncState.SUCCESS
                    onComplete?.invoke(true, null)
                }
                is SyncResult.Error -> {
                    val err = result.error
                    val isOffline = err is java.net.UnknownHostException ||
                            err is java.net.ConnectException ||
                            err is java.net.SocketTimeoutException
                    _syncState.value = if (isOffline) SyncState.OFFLINE else SyncState.ERROR
                    val msg = err.localizedMessage ?: err.message ?: "Erro na sincronização"
                    _syncErrorMessage.value = msg
                    onComplete?.invoke(false, msg)
                }
            }
        }
    }

    fun formatLastSyncTime(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Nunca sincronizado"
        val now = Calendar.getInstance()
        val syncCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val isToday = now.get(Calendar.YEAR) == syncCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == syncCal.get(Calendar.DAY_OF_YEAR)

        val isYesterday = now.get(Calendar.YEAR) == syncCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - syncCal.get(Calendar.DAY_OF_YEAR) == 1

        return when {
            isToday -> "Hoje às ${timeFormat.format(Date(timestamp))}"
            isYesterday -> "Ontem às ${timeFormat.format(Date(timestamp))}"
            else -> dateFormat.format(Date(timestamp))
        }
    }

    fun setPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    // Auto-preencher com base no histórico anterior
    suspend fun prefillLaunchFields() {
        val lastRecord = repository.allRecordsFlow.first().firstOrNull()
        val currentVehicle = repository.getVehicle()
        val latestRefill = if (currentVehicle != null) {
            repository.getLatestFuelRefill(currentVehicle.id)
        } else {
            null
        }

        // Odômetro inicial e combustível: preenche se houver histórico ou veículo cadastrado
        if (lastRecord != null || currentVehicle != null) {
            val defaultStartOdo = lastRecord?.endOdometer ?: currentVehicle?.currentOdometer ?: 0.0
            startOdometer.value = String.format(Locale.US, "%.1f", defaultStartOdo)
            val defaultFuelPrice = latestRefill?.pricePerLiter?.takeIf { it > 0.0 }
                ?: lastRecord?.fuelPrice?.takeIf { it > 0.0 }
                ?: 5.80
            fuelPrice.value = String.format(Locale.US, "%.2f", defaultFuelPrice)
        } else {
            startOdometer.value = ""
            fuelPrice.value = "5.80"
        }

        // Zerar outros
        endOdometer.value = ""
        grossEarnings.value = ""
        deliveriesCount.value = ""
        foodExpense.value = ""
        startTime.value = ""
        endTime.value = ""
        pauseDuration.value = ""
        launchDateTimestamp.value = System.currentTimeMillis()
        platform.value = ""
    }

    // Salvar Metas nas SharedPreferences
    fun updateGoals(
        dailyG: Double, dailyN: Double, dailyK: Double, dailyD: Int,
        weeklyG: Double, weeklyN: Double, weeklyK: Double, weeklyD: Int,
        monthlyG: Double, monthlyN: Double, monthlyK: Double, monthlyD: Int
    ) {
        viewModelScope.launch {
            prefs.edit().apply {
                putFloat("daily_goal_gross", dailyG.toFloat())
                putFloat("daily_goal_net", dailyN.toFloat())
                putFloat("daily_goal_km", dailyK.toFloat())
                putInt("daily_goal_deliveries", dailyD)

                putFloat("weekly_goal_gross", weeklyG.toFloat())
                putFloat("weekly_goal_net", weeklyN.toFloat())
                putFloat("weekly_goal_km", weeklyK.toFloat())
                putInt("weekly_goal_deliveries", weeklyD)

                putFloat("monthly_goal_gross", monthlyG.toFloat())
                putFloat("monthly_goal_net", monthlyN.toFloat())
                putFloat("monthly_goal_km", monthlyK.toFloat())
                putInt("monthly_goal_deliveries", monthlyD)
            }.apply()

            dailyGoalGross.value = dailyG
            dailyGoalNet.value = dailyN
            dailyGoalKm.value = dailyK
            dailyGoalDeliveries.value = dailyD

            weeklyGoalGross.value = weeklyG
            weeklyGoalNet.value = weeklyN
            weeklyGoalKm.value = weeklyK
            weeklyGoalDeliveries.value = weeklyD

            monthlyGoalGross.value = monthlyG
            monthlyGoalNet.value = monthlyN
            monthlyGoalKm.value = monthlyK
            monthlyGoalDeliveries.value = monthlyD
        }
    }

    // Ações do Veículo
    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.saveVehicle(vehicle)
        }
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
                monthlyFixedCosts = fixedCosts,
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
        viewModelScope.launch {
            repository.setActiveVehicle(vehicleId)
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }

    // Ações do Perfil de Usuário
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

    // Ações de Plataforma
    fun savePlatform(platform: Platform) {
        viewModelScope.launch {
            repository.savePlatform(platform)
        }
    }

    fun deletePlatform(platform: Platform) {
        viewModelScope.launch {
            repository.deletePlatform(platform)
        }
    }

    // Ações de Peças de Desgaste
    fun addPart(name: String, price: Double, lifespan: Double, vehicleId: Long? = null) {
        viewModelScope.launch {
            val vId = vehicleId ?: vehicle.value?.id ?: 0L
            val part = VehiclePart(vehicleId = vId, name = name, price = price, lifespanKm = lifespan, runKmSinceChange = 0.0)
            repository.savePart(part)
        }
    }

    fun updatePart(partId: Long, name: String, price: Double, lifespan: Double, runKmSinceChange: Double, vehicleId: Long? = null) {
        viewModelScope.launch {
            val vId = vehicleId ?: vehicle.value?.id ?: 0L
            val part = VehiclePart(id = partId, vehicleId = vId, name = name, price = price, lifespanKm = lifespan, runKmSinceChange = runKmSinceChange)
            repository.updatePart(part)
        }
    }

    fun deletePart(part: VehiclePart) {
        viewModelScope.launch {
            repository.deletePart(part)
        }
    }

    fun resetPartWear(partId: Long) {
        viewModelScope.launch {
            repository.resetPartWear(partId)
        }
    }

    // Ações de Lançamento Diário
    fun saveDailyRecord(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val startOdoVal = startOdometer.value.toDoubleOrNull()
            val endOdoVal = endOdometer.value.toDoubleOrNull()
            val grossVal = grossEarnings.value.toDoubleOrNull()
            val deliveriesVal = deliveriesCount.value.toIntOrNull()
            val fuelPriceVal = fuelPrice.value.toDoubleOrNull()
            val foodExpenseVal = foodExpense.value.toDoubleOrNull() ?: 0.0

            if (startOdoVal == null || endOdoVal == null || grossVal == null || deliveriesVal == null || fuelPriceVal == null) {
                onError("Por favor, preencha todos os campos obrigatórios corretamente.")
                return@launch
            }

            if (endOdoVal < startOdoVal) {
                onError("O hodômetro final não pode ser menor que o inicial.")
                return@launch
            }

            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateToUse = Date(launchDateTimestamp.value)
                val dateStr = dateFormat.format(dateToUse)
                val timestampToUse = launchDateTimestamp.value
                val platformVal = platform.value.trim().ifBlank { "iFood" }

                repository.insertDailyRecord(
                    dateString = dateStr,
                    dateTimestamp = timestampToUse,
                    platform = platformVal,
                    grossEarnings = grossVal,
                    deliveriesCount = deliveriesVal,
                    startOdometer = startOdoVal,
                    endOdometer = endOdoVal,
                    fuelPrice = fuelPriceVal,
                    foodExpense = foodExpenseVal,
                    startTime = startTime.value.trim(),
                    endTime = endTime.value.trim(),
                    pauseDuration = pauseDuration.value.trim()
                )
                
                // Limpar campos para o próximo dia
                prefillLaunchFields()
                onSuccess()
            } catch (e: Exception) {
                onError("Erro ao salvar lançamento: ${e.localizedMessage}")
            }
        }
    }

    fun deleteDailyRecord(record: DailyRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

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
    ) {
        viewModelScope.launch {
            if (endOdometer < startOdometer) {
                onError("O hodômetro final não pode ser menor que o inicial.")
                return@launch
            }
            try {
                repository.updateDailyRecord(
                    recordId = recordId,
                    dateString = dateString,
                    dateTimestamp = dateTimestamp,
                    platform = platform,
                    grossEarnings = grossEarnings,
                    deliveriesCount = deliveriesCount,
                    startOdometer = startOdometer,
                    endOdometer = endOdometer,
                    fuelPrice = fuelPrice,
                    foodExpense = foodExpense,
                    startTime = startTime.trim(),
                    endTime = endTime.trim(),
                    pauseDuration = pauseDuration.trim()
                )
                onSuccess()
            } catch (e: Exception) {
                onError("Erro ao atualizar lançamento: ${e.localizedMessage}")
            }
        }
    }

    // Estimativas em Tempo Real para o Lançamento Diário
    data class FormState(
        val startOdo: String = "",
        val endOdo: String = "",
        val gross: String = "",
        val food: String = "",
        val fuelPrice: String = ""
    )

    private val formStateFlow: Flow<FormState> = combine(
        startOdometer,
        endOdometer,
        grossEarnings,
        foodExpense,
        fuelPrice
    ) { start, end, gross, food, fuel ->
        FormState(start, end, gross, food, fuel)
    }

    val realTimeEstimation: Flow<EstimationDetail> = combine(
        formStateFlow,
        parts,
        vehicle,
        realAverageConsumption
    ) { form, partsList, v, realAvg ->
        val startVal = form.startOdo.replace(",", ".").toDoubleOrNull() ?: 0.0
        val endVal = form.endOdo.replace(",", ".").toDoubleOrNull() ?: 0.0
        val grossVal = form.gross.replace(",", ".").toDoubleOrNull() ?: 0.0
        val foodVal = form.food.replace(",", ".").toDoubleOrNull() ?: 0.0
        val fPrice = form.fuelPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
        val activeVehicle = v ?: Vehicle()

        val distance = (endVal - startVal).coerceAtLeast(0.0)

        // Combustível com prioridade: realAverageConsumption (se disponível e > 0), senão vehicle.averageConsumption
        val effectiveConsumption = if (realAvg != null && realAvg > 0.0) {
            realAvg
        } else {
            activeVehicle.averageConsumption
        }

        val fuelCost = if (effectiveConsumption > 0) {
            (distance / effectiveConsumption) * fPrice
        } else {
            0.0
        }

        // Desgaste oculto
        val wearCostPerKm = partsList.sumOf { it.wearCostPerKm }
        val wearCost = distance * wearCostPerKm

        // Custos fixos proporcionais
        val proportionalFixed = if (activeVehicle.plannedWorkDays > 0) {
            activeVehicle.monthlyFixedCosts / activeVehicle.plannedWorkDays
        } else {
            0.0
        }

        val totalExpenses = fuelCost + wearCost + proportionalFixed + foodVal
        val netProfit = grossVal - totalExpenses

        EstimationDetail(
            distance = distance,
            fuelCost = fuelCost,
            wearCost = wearCost,
            fixedCost = proportionalFixed,
            foodCost = foodVal,
            totalExpenses = totalExpenses,
            netProfit = netProfit
        )
    }

    val fuelCostEstimationInfo: StateFlow<FuelCostEstimationInfo> = combine(
        vehicle,
        realAverageConsumption,
        startOdometer,
        endOdometer,
        fuelPrice
    ) { currentVehicle, realAvg, startStr, endStr, priceStr ->
        val v = currentVehicle ?: Vehicle()
        val startVal = startStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val endVal = endStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val fPrice = priceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val distance = (endVal - startVal).coerceAtLeast(0.0)

        val isUsingReal = realAvg != null && realAvg > 0.0
        val effectiveConsumption = if (isUsingReal) realAvg!! else v.averageConsumption
        val estimatedFuelCost = if (effectiveConsumption > 0.0) {
            (distance / effectiveConsumption) * fPrice
        } else {
            0.0
        }

        val vehicleName = v.model.ifBlank { "Veículo Ativo" }
        val displayDist = String.format(Locale.GERMAN, "%.1f", distance)
        val displayText = "Custo de combustível estimado para $displayDist km rodados com base no consumo do $vehicleName"

        FuelCostEstimationInfo(
            vehicleModel = vehicleName,
            distanceKm = distance,
            effectiveConsumption = effectiveConsumption,
            isUsingRealConsumption = isUsingReal,
            estimatedFuelCost = estimatedFuelCost,
            fuelPriceUsed = fPrice,
            displayText = displayText
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FuelCostEstimationInfo()
    )

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

data class EstimationDetail(
    val distance: Double = 0.0,
    val fuelCost: Double = 0.0,
    val wearCost: Double = 0.0,
    val fixedCost: Double = 0.0,
    val foodCost: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0
)
