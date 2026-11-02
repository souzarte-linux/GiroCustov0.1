package com.example.ui.screens

import com.example.R
import com.example.data.DailyRecord
import com.example.ui.Period
import com.example.ui.util.filterByPeriod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeliveryTypeFilter {
    TODOS,
    LOGISTICA,
    DELIVERY
}

typealias QuickFilterOption = DeliveryTypeFilter

enum class HistorySortOption(val labelResId: Int) {
    DATA_DECRESCENTE(R.string.history_sort_date_desc),
    DATA_CRESCENTE(R.string.history_sort_date_asc),
    LUCRO_MAIOR(R.string.history_sort_profit_desc),
    LUCRO_MENOR(R.string.history_sort_profit_asc),
    KM_MAIOR(R.string.history_sort_km_desc),
    KM_MENOR(R.string.history_sort_km_asc),
    ENTREGAS(R.string.history_sort_deliveries_desc)
}

data class HistoryFilterState(
    val period: Period = Period.SEMANA,
    val customStartDate: Long = 0L,
    val customEndDate: Long = 0L,
    val deliveryType: DeliveryTypeFilter = DeliveryTypeFilter.TODOS,
    val selectedPlatform: String? = null,
    val sortOption: HistorySortOption = HistorySortOption.DATA_DECRESCENTE
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (period != Period.SEMANA) count++
            if (deliveryType != DeliveryTypeFilter.TODOS) count++
            if (!selectedPlatform.isNullOrBlank()) count++
            if (sortOption != HistorySortOption.DATA_DECRESCENTE) count++
            return count
        }

    val hasActiveFilters: Boolean
        get() = activeFilterCount > 0
}

fun isLogisticsPlatform(platform: String): Boolean {
    val p = platform.lowercase(Locale.ROOT)
    return p.contains("mercado") || p.contains("livre") || p.contains("meli") ||
        p.contains("shopee") || p.contains("loggi") || p.contains("lalamove")
}

fun isDeliveryPlatform(platform: String): Boolean {
    val p = platform.lowercase(Locale.ROOT)
    return p.contains("ifood") || p.contains("uber") || p.contains("99") ||
        p.contains("rappi") || p.contains("aiqfome") || p.contains("ze") || p.contains("zé")
}

fun getAvailablePlatforms(allPlatforms: List<String>, deliveryType: DeliveryTypeFilter): List<String> {
    val cleanPlatforms = allPlatforms.filter {
        it.isNotBlank() &&
            !it.equals("Todas", ignoreCase = true) &&
            !it.equals("Todas as plataformas", ignoreCase = true)
    }.distinct()

    return when (deliveryType) {
        DeliveryTypeFilter.TODOS -> cleanPlatforms
        DeliveryTypeFilter.LOGISTICA -> cleanPlatforms.filter { isLogisticsPlatform(it) }
        DeliveryTypeFilter.DELIVERY -> cleanPlatforms.filter { isDeliveryPlatform(it) }
    }
}

fun HistoryFilterState.withDeliveryType(
    newDeliveryType: DeliveryTypeFilter,
    allPlatforms: List<String>
): HistoryFilterState {
    val available = getAvailablePlatforms(allPlatforms, newDeliveryType)
    val validPlatform = if (selectedPlatform != null && selectedPlatform in available) {
        selectedPlatform
    } else {
        null
    }
    return copy(deliveryType = newDeliveryType, selectedPlatform = validPlatform)
}

fun HistoryFilterState.withCustomPeriodDefaults(now: Long = System.currentTimeMillis()): HistoryFilterState {
    if (customStartDate > 0L && customEndDate > 0L) return this
    val end = now
    val start = end - 7L * 24 * 60 * 60 * 1000
    return copy(customStartDate = start, customEndDate = end)
}

fun filterAndSortHistoryRecords(
    records: List<DailyRecord>,
    filterState: HistoryFilterState,
    searchQuery: String = ""
): List<DailyRecord> {
    val dateFiltered = filterByPeriod(
        records = records,
        period = filterState.period,
        customStart = filterState.customStartDate,
        customEnd = filterState.customEndDate
    ) { it.dateTimestamp }

    val typeFiltered = when (filterState.deliveryType) {
        DeliveryTypeFilter.TODOS -> dateFiltered
        DeliveryTypeFilter.LOGISTICA -> dateFiltered.filter { isLogisticsPlatform(it.platform) }
        DeliveryTypeFilter.DELIVERY -> dateFiltered.filter { isDeliveryPlatform(it.platform) }
    }

    val platformFiltered = if (filterState.selectedPlatform.isNullOrBlank()) {
        typeFiltered
    } else {
        typeFiltered.filter { it.platform.equals(filterState.selectedPlatform, ignoreCase = true) }
    }

    val searchFiltered = if (searchQuery.isBlank()) {
        platformFiltered
    } else {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        platformFiltered.filter { rec ->
            rec.platform.lowercase(Locale.ROOT).contains(q) ||
                rec.dateString.lowercase(Locale.ROOT).contains(q) ||
                try {
                    dateFormat.format(Date(rec.dateTimestamp)).contains(q)
                } catch (e: Exception) {
                    false
                }
        }
    }

    return when (filterState.sortOption) {
        HistorySortOption.DATA_DECRESCENTE -> searchFiltered.sortedByDescending { it.dateTimestamp }
        HistorySortOption.DATA_CRESCENTE -> searchFiltered.sortedBy { it.dateTimestamp }
        HistorySortOption.LUCRO_MAIOR -> searchFiltered.sortedByDescending { it.netProfit }
        HistorySortOption.LUCRO_MENOR -> searchFiltered.sortedBy { it.netProfit }
        HistorySortOption.KM_MAIOR -> searchFiltered.sortedByDescending { it.kmRodados }
        HistorySortOption.KM_MENOR -> searchFiltered.sortedBy { it.kmRodados }
        HistorySortOption.ENTREGAS -> searchFiltered.sortedByDescending { it.deliveriesCount }
    }
}
