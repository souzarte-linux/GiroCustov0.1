package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.*
import com.example.network.supabase.*
import com.example.network.viacep.*
import com.example.ui.navigation.RootNavState
import com.example.util.BiometricHelper
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

    val rootNavState: StateFlow<RootNavState> = combine(
        currentUser,
        userProfile,
        isProfileInitialized
    ) { user, profile, isInitialized ->
        when {
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

    private val _isBiometricAvailable = MutableStateFlow(BiometricHelper.isBiometricHardwareAvailable(application))
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()

    fun refreshBiometricAvailability() {
        _isBiometricAvailable.value = BiometricHelper.isBiometricHardwareAvailable(getApplication())
    }

    val allPlatforms: StateFlow<List<Platform>> = repository.allPlatformsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parts: StateFlow<List<VehiclePart>> = repository.allPartsFlow
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

    val realAverageConsumption: StateFlow<Double?> = fuelRefills
        .map { _ ->
            val activeVehicleId = vehicle.value?.id ?: 0L
            if (activeVehicleId != 0L) {
                repository.calculateRealAverageConsumption(activeVehicleId)
            } else {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

            // Se houver sessão salva ao iniciar o ViewModel, restaure-a e execute uma sincronização em background.
            if (supabaseClient.isAuthenticated) {
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

    fun signInWithGoogleMockOrOAuth(idToken: String? = null, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            _authUiState.value = AuthUiState.Error("Login com Google em homologação.")
            onResult(false, "Login com Google em homologação.")
        }
    }

    fun signInWithBiometrics(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val savedEmail = BiometricHelper.getBiometricEmail(getApplication()) ?: "usuario@girocusto.com"
            val session = AuthSession(
                accessToken = "biometric-token-${System.currentTimeMillis()}",
                refreshToken = "bio-refresh",
                userId = "bio-user-${savedEmail.hashCode()}",
                userEmail = savedEmail
            )
            supabaseClient.setAuthSession(session)
            _authUiState.value = AuthUiState.Success("Autenticado via biometria com sucesso!")
            triggerSync()
            onResult(true, null)
        }
    }

    fun logout() {
        viewModelScope.launch {
            supabaseClient.signOut()
            _authUiState.value = AuthUiState.Idle
            _syncState.value = SyncState.IDLE
            _syncErrorMessage.value = null
            clearViaCepState()
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
        
        // Odômetro inicial e combustível: preenche se houver histórico ou veículo cadastrado
        if (lastRecord != null || currentVehicle != null) {
            val defaultStartOdo = lastRecord?.endOdometer ?: currentVehicle?.currentOdometer ?: 0.0
            startOdometer.value = String.format(Locale.US, "%.1f", defaultStartOdo)
            val defaultFuelPrice = lastRecord?.fuelPrice ?: 5.80
            fuelPrice.value = String.format(Locale.US, "%.2f", defaultFuelPrice)
        } else {
            startOdometer.value = ""
            fuelPrice.value = ""
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
                onboardingCompleted = onboardingCompleted ?: current.onboardingCompleted
            )
            repository.saveUserProfile(updated)
        }
    }

    fun saveUserProfile(userProfile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(userProfile)
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
    fun addPart(name: String, price: Double, lifespan: Double) {
        viewModelScope.launch {
            val part = VehiclePart(name = name, price = price, lifespanKm = lifespan, runKmSinceChange = 0.0)
            repository.savePart(part)
        }
    }

    fun updatePart(partId: Long, name: String, price: Double, lifespan: Double, runKmSinceChange: Double) {
        viewModelScope.launch {
            val part = VehiclePart(id = partId, name = name, price = price, lifespanKm = lifespan, runKmSinceChange = runKmSinceChange)
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
        vehicle
    ) { form, partsList, v ->
        val startVal = form.startOdo.toDoubleOrNull() ?: 0.0
        val endVal = form.endOdo.toDoubleOrNull() ?: 0.0
        val grossVal = form.gross.toDoubleOrNull() ?: 0.0
        val foodVal = form.food.toDoubleOrNull() ?: 0.0
        val fPrice = form.fuelPrice.toDoubleOrNull() ?: 0.0
        val activeVehicle = v ?: Vehicle()

        val distance = (endVal - startVal).coerceAtLeast(0.0)

        // Combustível
        val fuelCost = if (activeVehicle.averageConsumption > 0) {
            (distance / activeVehicle.averageConsumption) * fPrice
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
