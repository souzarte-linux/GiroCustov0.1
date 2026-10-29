package com.example.ui.delegates

import com.example.data.DailyRecord
import com.example.data.GiroCustoRepository
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.ui.EstimationDetail
import com.example.ui.FuelCostEstimationInfo
import com.example.ui.GiroCustoDomainCalculations
import com.example.ui.LaunchDistanceState
import com.example.util.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LaunchFormState(
    val startOdo: String = "",
    val endOdo: String = "",
    val gross: String = "",
    val food: String = "",
    val fuelPrice: String = ""
)

class DailyLaunchCoordinator(
    private val scope: CoroutineScope,
    private val repository: GiroCustoRepository,
    private val vehicleFlow: StateFlow<Vehicle?>,
    private val partsFlow: StateFlow<List<VehiclePart>>,
    private val realAverageConsumptionFlow: StateFlow<Double?>
) {
    val startOdometer = MutableStateFlow("")
    val endOdometer = MutableStateFlow("")
    val grossEarnings = MutableStateFlow("")
    val deliveriesCount = MutableStateFlow("")
    val fuelPrice = MutableStateFlow("")
    val foodExpense = MutableStateFlow("")
    val startTime = MutableStateFlow("")
    val endTime = MutableStateFlow("")
    val pauseDuration = MutableStateFlow("")
    val launchDateTimestamp = MutableStateFlow(System.currentTimeMillis())
    val platform = MutableStateFlow("")

    fun setLaunchDate(timestamp: Long) {
        launchDateTimestamp.value = timestamp
    }

    val launchCalculatedDistance: StateFlow<LaunchDistanceState> = combine(
        startOdometer,
        endOdometer
    ) { start, end ->
        GiroCustoDomainCalculations.calculateDailyDistance(start, end)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), LaunchDistanceState.None)

    private val formStateFlow: Flow<LaunchFormState> = combine(
        startOdometer,
        endOdometer,
        grossEarnings,
        foodExpense,
        fuelPrice
    ) { start, end, gross, food, fuel ->
        LaunchFormState(start, end, gross, food, fuel)
    }

    val realTimeEstimation: Flow<EstimationDetail> = combine(
        formStateFlow,
        partsFlow,
        vehicleFlow,
        realAverageConsumptionFlow
    ) { form, partsList, v, realAvg ->
        val startVal = form.startOdo.replace(",", ".").toDoubleOrNull() ?: 0.0
        val endVal = form.endOdo.replace(",", ".").toDoubleOrNull() ?: 0.0
        val grossVal = form.gross.replace(",", ".").toDoubleOrNull() ?: 0.0
        val foodVal = form.food.replace(",", ".").toDoubleOrNull() ?: 0.0
        val fPrice = form.fuelPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
        val activeVehicle = v ?: Vehicle()

        val distance = (endVal - startVal).coerceAtLeast(0.0)

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

        val wearCostPerKm = partsList.sumOf { it.wearCostPerKm }
        val wearCost = distance * wearCostPerKm

        val proportionalFixed = if (activeVehicle.plannedWorkDays > 0) {
            CurrencyUtils.centsToDouble(activeVehicle.monthlyFixedCosts) / activeVehicle.plannedWorkDays
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
        vehicleFlow,
        realAverageConsumptionFlow,
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
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FuelCostEstimationInfo()
    )

    suspend fun prefillLaunchFields() {
        val lastRecord = repository.allRecordsFlow.first().firstOrNull()
        val currentVehicle = repository.getVehicle()
        val latestRefill = if (currentVehicle != null) {
            repository.getLatestFuelRefill(currentVehicle.id)
        } else {
            null
        }

        if (lastRecord != null || currentVehicle != null) {
            val defaultStartOdo = lastRecord?.endOdometer ?: currentVehicle?.currentOdometer ?: 0.0
            startOdometer.value = String.format(Locale.US, "%.1f", defaultStartOdo)
            val defaultFuelPrice: Double = latestRefill?.pricePerLiter?.takeIf { it > 0L }?.let { CurrencyUtils.fuelPriceMillisToDouble(it) }
                ?: lastRecord?.fuelPrice?.takeIf { it > 0L }?.let { CurrencyUtils.fuelPriceMillisToDouble(it) }
                ?: 5.80
            fuelPrice.value = String.format(Locale.US, "%.2f", defaultFuelPrice)
        } else {
            startOdometer.value = ""
            fuelPrice.value = "5.80"
        }

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

    fun saveDailyRecord(onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            val cleanStartOdo = startOdometer.value.trim().replace(',', '.')
            val startOdoVal = cleanStartOdo.toDoubleOrNull()
            val cleanEndOdo = endOdometer.value.trim().replace(',', '.')
            val endOdoVal = cleanEndOdo.toDoubleOrNull()

            val cleanGross = grossEarnings.value.trim()
            val grossCents = if (cleanGross.isBlank()) null else CurrencyUtils.parseToCents(cleanGross, -1L).takeIf { it >= 0L }

            val deliveriesVal = deliveriesCount.value.trim().toIntOrNull()

            val cleanFuelPrice = fuelPrice.value.trim()
            val fuelPriceMillis = if (cleanFuelPrice.isBlank()) null else CurrencyUtils.parseToFuelPriceMillis(cleanFuelPrice, -1L).takeIf { it > 0L }

            val foodExpenseCents = CurrencyUtils.parseToCents(foodExpense.value, 0L)

            if (startOdoVal == null || endOdoVal == null || grossCents == null || deliveriesVal == null || fuelPriceMillis == null) {
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
                    grossEarnings = grossCents,
                    deliveriesCount = deliveriesVal,
                    startOdometer = startOdoVal,
                    endOdometer = endOdoVal,
                    fuelPrice = fuelPriceMillis,
                    foodExpense = foodExpenseCents,
                    startTime = startTime.value.trim(),
                    endTime = endTime.value.trim(),
                    pauseDuration = pauseDuration.value.trim()
                )

                prefillLaunchFields()
                onSuccess()
            } catch (e: Exception) {
                onError("Erro ao salvar lançamento: ${e.localizedMessage}")
            }
        }
    }

    fun deleteDailyRecord(record: DailyRecord) {
        scope.launch {
            repository.deleteRecord(record)
        }
    }

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
    ) {
        scope.launch {
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
}
