package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class Period {
    SEMANA,
    QUINZENA,
    MENSAL,
    PERSONALIZADO
}

class GiroCustoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GiroCustoDatabase.getDatabase(application)
    private val repository = GiroCustoRepository(db)

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
            // Inicializar/Semear Banco de Dados se necessário
            repository.checkAndSeedData()
            
            // Auto-preencher odômetro inicial e combustível
            prefillLaunchFields()
        }
    }

    fun setPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    // Auto-preencher com base no histórico anterior
    suspend fun prefillLaunchFields() {
        val lastRecord = repository.allRecordsFlow.first().firstOrNull()
        val currentVehicle = repository.getVehicle()
        
        // Odômetro inicial = último odômetro final ou hodômetro do veículo
        val defaultStartOdo = lastRecord?.endOdometer ?: currentVehicle?.currentOdometer ?: 15350.0
        startOdometer.value = String.format(Locale.US, "%.1f", defaultStartOdo)
        
        // Último preço de combustível
        val defaultFuelPrice = lastRecord?.fuelPrice ?: 5.80
        fuelPrice.value = String.format(Locale.US, "%.2f", defaultFuelPrice)
        
        // Zerar outros
        endOdometer.value = ""
        grossEarnings.value = ""
        deliveriesCount.value = ""
        foodExpense.value = ""
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
        workDays: Int
    ) {
        viewModelScope.launch {
            val newVehicle = Vehicle(
                model = model,
                averageConsumption = consumption,
                fuelType = fuelType,
                monthlyFixedCosts = fixedCosts,
                plannedWorkDays = workDays,
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
        city: String
    ) {
        viewModelScope.launch {
            val current = repository.getUserProfile() ?: UserProfile()
            val updated = current.copy(
                name = name,
                phone = phone,
                city = city
            )
            repository.saveUserProfile(updated)
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
                    foodExpense = foodExpenseVal
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
                    foodExpense = foodExpense
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
