package com.example.ui

import com.example.data.DailyRecord
import com.example.data.MaintenanceRecord
import com.example.ui.util.filterByPeriod
import com.example.util.CurrencyUtils
import java.util.Locale

// --- Modelos de Estado de Cálculos e Formulários (T-16 [ARQ-03]) ---

sealed interface LaunchDistanceState {
    object None : LaunchDistanceState
    data class Valid(val distanceKm: Double, val formattedKm: String) : LaunchDistanceState
    object NegativeError : LaunchDistanceState
}

data class RefillCalculationResult(
    val updatedPricePerLiter: String? = null,
    val updatedLiters: String? = null,
    val updatedTotalPaid: String? = null
)

data class ReportsFilterState(
    val period: Period = Period.SEMANA,
    val customStart: Long = 0L,
    val customEnd: Long = 0L
)

data class ReportMetricsState(
    val filteredRecords: List<DailyRecord> = emptyList(),
    val filteredMaintenanceRecords: List<MaintenanceRecord> = emptyList(),
    val chronologicalRecords: List<DailyRecord> = emptyList(),
    val totalEarnings: Double = 0.0,
    val totalFuelCost: Double = 0.0,
    val totalWearCost: Double = 0.0,
    val totalFixedCost: Double = 0.0,
    val totalFoodCost: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalMaintenanceCost: Double = 0.0,
    val realExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val realNetProfit: Double = 0.0,
    val totalDeliveries: Int = 0,
    val totalKm: Double = 0.0,
    val bestDay: DailyRecord? = null,
    val worstDay: DailyRecord? = null,
    val avgEarningsPerDay: Double = 0.0,
    val avgProfitPerDay: Double = 0.0,
    val avgKmPerDay: Double = 0.0,
    val totalWorkMinutes: Long = 0L,
    val totalWorkHours: Double = 0.0,
    val totalPauseMinutes: Long = 0L,
    val grossPerHour: Double = 0.0,
    val netPerHour: Double = 0.0,
    val avgHoursPerShift: Double = 0.0,
    val formattedTotalWork: String = "0h 00m",
    val formattedTotalPause: String = "0h 00m",
    val projectedMonthlyProfit: Double = 0.0,
    val projectedMonthlyEarnings: Double = 0.0,
    val sumAllExpenses: Double = 0.0,
    val pctFuel: Float = 0f,
    val pctFood: Float = 0f,
    val pctWear: Float = 0f,
    val pctFixed: Float = 0f,
    val pctMaintenance: Float = 0f,
    val monthlyWorkingDays: Int = 22,
    val pctOfGoal: Double = 1.0,
    val recordsCount: Int = 0
)

/**
 * Objeto com cálculos matemáticos e regras de negócio puras (sem dependência do framework Android).
 * Permite execução ultrarrápida em testes unitários puros JVM (T-17).
 */
object GiroCustoDomainCalculations {

    // 1. Hodômetro e Distância Diária (LaunchScreen)
    fun calculateDailyDistance(startOdo: String, endOdo: String): LaunchDistanceState {
        val startVal = startOdo.replace(",", ".").toDoubleOrNull()
        val endVal = endOdo.replace(",", ".").toDoubleOrNull()
        if (startVal == null || endVal == null) {
            return LaunchDistanceState.None
        }
        val diffKm = endVal - startVal
        return if (diffKm >= 0.0) {
            val formatted = String.format(Locale.GERMAN, "%.1f", diffKm)
            LaunchDistanceState.Valid(diffKm, formatted)
        } else {
            LaunchDistanceState.NegativeError
        }
    }

    // 2. Validação e Cálculo de Abastecimento (FuelRefillsScreen)
    fun isValidFuelInput(pricePerLiterStr: String, litersStr: String, odometerStr: String): Boolean {
        val price = pricePerLiterStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val lts = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val odo = odometerStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        return price > 0.0 && lts > 0.0 && odo > 0.0
    }

    fun checkExceedsTankCapacity(litersStr: String, tankCapacity: Double): Boolean {
        val currentLiters = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        return tankCapacity > 0.0 && currentLiters > tankCapacity
    }

    fun computeRefillTotal(pricePerLiterStr: String, litersStr: String, discountStr: String, totalPaidStr: String): Double {
        val totalExplicit = totalPaidStr.replace(",", ".").toDoubleOrNull()
        if (totalExplicit != null) return totalExplicit.coerceAtLeast(0.0)
        val price = pricePerLiterStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val lts = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val disc = discountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        return (price * lts - disc).coerceAtLeast(0.0)
    }

    fun calculateRefillMultiDirectional(
        pricePerLiterStr: String,
        litersStr: String,
        discountStr: String,
        totalPaidStr: String,
        focusedField: String?
    ): RefillCalculationResult {
        val price = pricePerLiterStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val lts = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val disc = discountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val total = totalPaidStr.replace(",", ".").toDoubleOrNull() ?: 0.0

        val priceEmpty = pricePerLiterStr.isBlank() || price <= 0.0
        val ltsEmpty = litersStr.isBlank() || lts <= 0.0
        val totalEmpty = totalPaidStr.isBlank() || total <= 0.0

        val filledCount = listOf(!priceEmpty, !ltsEmpty, !totalEmpty).count { it }

        var newPrice: String? = null
        var newLts: String? = null
        var newTotal: String? = null

        if (filledCount == 2) {
            if (priceEmpty && !ltsEmpty && !totalEmpty && focusedField != "price") {
                val autoPrice = ((total + disc) / lts).coerceAtLeast(0.0)
                if (autoPrice.isFinite() && autoPrice > 0.0) {
                    val autoPriceStr = String.format(Locale.US, "%.2f", autoPrice)
                    val currentPrice = pricePerLiterStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (Math.abs(currentPrice - autoPrice) > 0.009) {
                        newPrice = autoPriceStr
                    }
                }
            } else if (ltsEmpty && !priceEmpty && !totalEmpty && focusedField != "liters") {
                val autoLts = ((total + disc) / price).coerceAtLeast(0.0)
                if (autoLts.isFinite() && autoLts > 0.0) {
                    val autoLtsStr = String.format(Locale.US, "%.2f", autoLts)
                    val currentLts = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (Math.abs(currentLts - autoLts) > 0.009) {
                        newLts = autoLtsStr
                    }
                }
            } else if (totalEmpty && !priceEmpty && !ltsEmpty && focusedField != "total") {
                val autoTotal = (price * lts - disc).coerceAtLeast(0.0)
                val autoTotalStr = String.format(Locale.US, "%.2f", autoTotal)
                val currentTotal = totalPaidStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (Math.abs(currentTotal - autoTotal) > 0.009) {
                    newTotal = autoTotalStr
                }
            }
        } else if (filledCount == 3) {
            when (focusedField) {
                "price", "liters", "discount" -> {
                    val autoTotal = (price * lts - disc).coerceAtLeast(0.0)
                    val autoTotalStr = String.format(Locale.US, "%.2f", autoTotal)
                    val currentTotal = totalPaidStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (Math.abs(currentTotal - autoTotal) > 0.009) {
                        newTotal = autoTotalStr
                    }
                }
                "total" -> {
                    val autoLts = ((total + disc) / price).coerceAtLeast(0.0)
                    if (autoLts.isFinite() && autoLts > 0.0) {
                        val autoLtsStr = String.format(Locale.US, "%.2f", autoLts)
                        val currentLts = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (Math.abs(currentLts - autoLts) > 0.009) {
                            newLts = autoLtsStr
                        }
                    }
                }
            }
        }

        return RefillCalculationResult(
            updatedPricePerLiter = newPrice,
            updatedLiters = newLts,
            updatedTotalPaid = newTotal
        )
    }

    // 3. Métricas e Projeções Financeiras de Relatórios (ReportsScreen)
    fun calculateReportMetrics(
        records: List<DailyRecord>,
        maintenanceRecords: List<MaintenanceRecord>,
        selectedPeriod: Period,
        customStart: Long,
        customEnd: Long,
        plannedWorkDays: Int,
        monthlyGoalNet: Double
    ): ReportMetricsState {
        val filtered = filterByPeriod(records, selectedPeriod, customStart, customEnd) { it.dateTimestamp }
        val filteredMaint = filterByPeriod(maintenanceRecords, selectedPeriod, customStart, customEnd) { it.dateTimestamp }
        val chronological = filtered.take(7).sortedBy { it.dateTimestamp }

        val totalEarnings = CurrencyUtils.centsToDouble(filtered.sumOf { it.grossEarnings })
        val totalFuel = CurrencyUtils.centsToDouble(filtered.sumOf { it.fuelCost })
        val totalWear = CurrencyUtils.centsToDouble(filtered.sumOf { it.wearCost })
        val totalFixed = CurrencyUtils.centsToDouble(filtered.sumOf { it.proportionalFixedCost })
        val totalFood = CurrencyUtils.centsToDouble(filtered.sumOf { it.foodExpense })
        val totalExpenses = totalFuel + totalWear + totalFixed + totalFood

        val totalMaint = CurrencyUtils.centsToDouble(filteredMaint.sumOf { it.value })
        val realExpenses = totalExpenses + totalMaint
        val netProfit = totalEarnings - totalExpenses
        val realNetProfit = netProfit - totalMaint

        val totalDeliveries = filtered.sumOf { it.deliveriesCount }
        val totalKm = filtered.sumOf { it.kmRodados }

        val bestDay = filtered.maxByOrNull { it.netProfit }
        val worstDay = filtered.minByOrNull { it.netProfit }

        val count = filtered.size
        val avgEarningsPerDay = if (count > 0) totalEarnings / count else 0.0
        val avgProfitPerDay = if (count > 0) realNetProfit / count else 0.0
        val avgKmPerDay = if (count > 0) totalKm / count else 0.0

        val totalWorkMinutes = filtered.sumOf { it.totalWorkMinutes }
        val totalWorkHours = totalWorkMinutes / 60.0
        val totalPauseMinutes = filtered.sumOf { it.pauseMinutes }
        val grossPerHour = if (totalWorkHours > 0) totalEarnings / totalWorkHours else 0.0
        val netPerHour = if (totalWorkHours > 0) realNetProfit / totalWorkHours else 0.0
        val avgHoursPerShift = if (count > 0) totalWorkHours / count else 0.0

        val workHoursPart = totalWorkMinutes / 60
        val workMinPart = totalWorkMinutes % 60
        val formattedTotalWork = "${workHoursPart}h ${String.format(Locale.GERMAN, "%02d", workMinPart)}m"

        val pauseHoursPart = totalPauseMinutes / 60
        val pauseMinPart = totalPauseMinutes % 60
        val formattedTotalPause = "${pauseHoursPart}h ${String.format(Locale.GERMAN, "%02d", pauseMinPart)}m"

        val workingDays = if (plannedWorkDays > 0) plannedWorkDays else 22
        val projectedMonthlyProfit = avgProfitPerDay * workingDays
        val projectedMonthlyEarnings = avgEarningsPerDay * workingDays

        val sumAllExpenses = totalFuel + totalWear + totalFixed + totalFood + totalMaint
        val pctFuel = if (sumAllExpenses > 0) (totalFuel / sumAllExpenses).toFloat() else 0f
        val pctFood = if (sumAllExpenses > 0) (totalFood / sumAllExpenses).toFloat() else 0f
        val pctWear = if (sumAllExpenses > 0) (totalWear / sumAllExpenses).toFloat() else 0f
        val pctFixed = if (sumAllExpenses > 0) (totalFixed / sumAllExpenses).toFloat() else 0f
        val pctMaintenance = if (sumAllExpenses > 0) (totalMaint / sumAllExpenses).toFloat() else 0f

        val pctOfGoal = if (monthlyGoalNet > 0) (projectedMonthlyProfit / monthlyGoalNet).coerceIn(0.0, 1.5) else 1.0

        return ReportMetricsState(
            filteredRecords = filtered,
            filteredMaintenanceRecords = filteredMaint,
            chronologicalRecords = chronological,
            totalEarnings = totalEarnings,
            totalFuelCost = totalFuel,
            totalWearCost = totalWear,
            totalFixedCost = totalFixed,
            totalFoodCost = totalFood,
            totalExpenses = totalExpenses,
            totalMaintenanceCost = totalMaint,
            realExpenses = realExpenses,
            netProfit = netProfit,
            realNetProfit = realNetProfit,
            totalDeliveries = totalDeliveries,
            totalKm = totalKm,
            bestDay = bestDay,
            worstDay = worstDay,
            avgEarningsPerDay = avgEarningsPerDay,
            avgProfitPerDay = avgProfitPerDay,
            avgKmPerDay = avgKmPerDay,
            totalWorkMinutes = totalWorkMinutes,
            totalWorkHours = totalWorkHours,
            totalPauseMinutes = totalPauseMinutes,
            grossPerHour = grossPerHour,
            netPerHour = netPerHour,
            avgHoursPerShift = avgHoursPerShift,
            formattedTotalWork = formattedTotalWork,
            formattedTotalPause = formattedTotalPause,
            projectedMonthlyProfit = projectedMonthlyProfit,
            projectedMonthlyEarnings = projectedMonthlyEarnings,
            sumAllExpenses = sumAllExpenses,
            pctFuel = pctFuel,
            pctFood = pctFood,
            pctWear = pctWear,
            pctFixed = pctFixed,
            pctMaintenance = pctMaintenance,
            monthlyWorkingDays = workingDays,
            pctOfGoal = pctOfGoal,
            recordsCount = filtered.size
        )
    }
}
