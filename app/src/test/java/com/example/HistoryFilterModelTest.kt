package com.example

import com.example.data.DailyRecord
import com.example.ui.Period
import com.example.ui.screens.*
import com.example.ui.util.filterByPeriod
import com.example.ui.util.getCurrentYearRange
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class HistoryFilterModelTest {

    private fun createRecord(
        id: Long,
        platform: String,
        timestamp: Long,
        netProfit: Double = 100.0,
        km: Double = 50.0,
        deliveries: Int = 10
    ): DailyRecord {
        return DailyRecord(
            id = id,
            dateString = "2026-11-01",
            dateTimestamp = timestamp,
            platform = platform,
            grossEarnings = netProfit + 50.0,
            deliveriesCount = deliveries,
            startOdometer = 100.0,
            endOdometer = 100.0 + km,
            fuelPrice = 5.0,
            foodExpense = 10.0,
            fuelCost = 25.0,
            wearCost = 10.0,
            proportionalFixedCost = 5.0,
            netProfit = netProfit,
            startTime = "08:00",
            endTime = "14:00",
            pauseDuration = "00:30"
        )
    }

    @Test
    fun testGetCurrentYearRange_boundariesAreExact() {
        val (start, end) = getCurrentYearRange()
        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        assertEquals(currentYear, calStart.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, calStart.get(Calendar.MONTH))
        assertEquals(1, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(0, calStart.get(Calendar.MILLISECOND))

        assertEquals(currentYear, calEnd.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, calEnd.get(Calendar.MONTH))
        assertEquals(31, calEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
        assertEquals(59, calEnd.get(Calendar.SECOND))
        assertEquals(999, calEnd.get(Calendar.MILLISECOND))

        assertTrue(end > start)
    }

    @Test
    fun testFilterByPeriod_anualIncludesCurrentYearOnly() {
        val (startThisYear, endThisYear) = getCurrentYearRange()

        val recCurrentYearMid = createRecord(1, "iFood", (startThisYear + endThisYear) / 2)
        val recStartYear = createRecord(2, "iFood", startThisYear)
        val recEndYear = createRecord(3, "iFood", endThisYear)

        // Registro do ano anterior
        val calPast = Calendar.getInstance().apply {
            timeInMillis = startThisYear
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val recPastYear = createRecord(4, "iFood", calPast.timeInMillis)

        val list = listOf(recCurrentYearMid, recStartYear, recEndYear, recPastYear)
        val filtered = filterByPeriod(list, Period.ANUAL) { it.dateTimestamp }

        assertEquals(3, filtered.size)
        assertTrue(filtered.contains(recCurrentYearMid))
        assertTrue(filtered.contains(recStartYear))
        assertTrue(filtered.contains(recEndYear))
        assertFalse(filtered.contains(recPastYear))
    }

    @Test
    fun testPlatformClassification_logisticsAndDelivery() {
        assertTrue(isLogisticsPlatform("Mercado Livre"))
        assertTrue(isLogisticsPlatform("shopee express"))
        assertTrue(isLogisticsPlatform("Loggi"))
        assertTrue(isLogisticsPlatform("Lalamove"))
        assertFalse(isLogisticsPlatform("iFood"))
        assertFalse(isLogisticsPlatform("Uber Eats"))

        assertTrue(isDeliveryPlatform("iFood"))
        assertTrue(isDeliveryPlatform("Uber"))
        assertTrue(isDeliveryPlatform("99 Food"))
        assertTrue(isDeliveryPlatform("Rappi"))
        assertTrue(isDeliveryPlatform("Aiqfome"))
        assertTrue(isDeliveryPlatform("Zé Delivery"))
        assertFalse(isDeliveryPlatform("Mercado Livre"))
        assertFalse(isDeliveryPlatform("Shopee"))
    }

    @Test
    fun testGetAvailablePlatforms_filtersAccordingToDeliveryType() {
        val all = listOf("iFood", "Uber", "Mercado Livre", "Shopee", "Todas as plataformas", "Todas", "")

        val allAvailable = getAvailablePlatforms(all, DeliveryTypeFilter.TODOS)
        assertEquals(listOf("iFood", "Uber", "Mercado Livre", "Shopee"), allAvailable)

        val logistics = getAvailablePlatforms(all, DeliveryTypeFilter.LOGISTICA)
        assertEquals(listOf("Mercado Livre", "Shopee"), logistics)

        val delivery = getAvailablePlatforms(all, DeliveryTypeFilter.DELIVERY)
        assertEquals(listOf("iFood", "Uber"), delivery)
    }

    @Test
    fun testCascadeInvariant_withDeliveryTypeResetsIncompatiblePlatform() {
        val all = listOf("iFood", "Mercado Livre")
        val stateInitial = HistoryFilterState(
            deliveryType = DeliveryTypeFilter.DELIVERY,
            selectedPlatform = "iFood"
        )

        // Ao mudar para LOGISTICA, "iFood" não é mais compatível e deve resetar para null
        val stateUpdated = stateInitial.withDeliveryType(DeliveryTypeFilter.LOGISTICA, all)
        assertEquals(DeliveryTypeFilter.LOGISTICA, stateUpdated.deliveryType)
        assertNull(stateUpdated.selectedPlatform)

        // Ao selecionar uma plataforma compatível e mudar para TODOS, mantém a plataforma
        val stateWithLogistics = HistoryFilterState(
            deliveryType = DeliveryTypeFilter.LOGISTICA,
            selectedPlatform = "Mercado Livre"
        )
        val stateToAll = stateWithLogistics.withDeliveryType(DeliveryTypeFilter.TODOS, all)
        assertEquals(DeliveryTypeFilter.TODOS, stateToAll.deliveryType)
        assertEquals("Mercado Livre", stateToAll.selectedPlatform)
    }

    @Test
    fun testHistoryFilterState_activeFilterCountAndHasActiveFilters() {
        val defaultState = HistoryFilterState()
        assertEquals(0, defaultState.activeFilterCount)
        assertFalse(defaultState.hasActiveFilters)

        val statePeriodChanged = defaultState.copy(period = Period.MENSAL)
        assertEquals(1, statePeriodChanged.activeFilterCount)
        assertTrue(statePeriodChanged.hasActiveFilters)

        val stateTypeChanged = statePeriodChanged.copy(deliveryType = DeliveryTypeFilter.DELIVERY)
        assertEquals(2, stateTypeChanged.activeFilterCount)

        val statePlatformChanged = stateTypeChanged.copy(selectedPlatform = "iFood")
        assertEquals(3, statePlatformChanged.activeFilterCount)

        val stateSortChanged = statePlatformChanged.copy(sortOption = HistorySortOption.LUCRO_MAIOR)
        assertEquals(4, stateSortChanged.activeFilterCount)
    }

    @Test
    fun testHistoryFilterState_defaultValuesAreStableAndDeterministic() {
        val state1 = HistoryFilterState()
        Thread.sleep(10)
        val state2 = HistoryFilterState()
        assertEquals(
            "HistoryFilterState instances created with default parameters must be identical and deterministic",
            state1,
            state2
        )
        assertEquals(0L, state1.customStartDate)
        assertEquals(0L, state1.customEndDate)
    }

    @Test
    fun testHistoryFilterState_withCustomPeriodDefaults() {
        val fixedNow = 1700000000000L
        val expectedStart = fixedNow - 7L * 24 * 60 * 60 * 1000

        // Estado inicial com datas 0L
        val defaultState = HistoryFilterState()
        val populatedState = defaultState.withCustomPeriodDefaults(fixedNow)
        assertEquals(expectedStart, populatedState.customStartDate)
        assertEquals(fixedNow, populatedState.customEndDate)

        // Estado já com datas customizadas definidas não deve ser sobrescrito
        val existingStart = 1690000000000L
        val existingEnd = 1695000000000L
        val customizedState = HistoryFilterState(
            customStartDate = existingStart,
            customEndDate = existingEnd
        )
        val notOverwrittenState = customizedState.withCustomPeriodDefaults(fixedNow)
        assertEquals(existingStart, notOverwrittenState.customStartDate)
        assertEquals(existingEnd, notOverwrittenState.customEndDate)
    }

    @Test
    fun testFilterAndSortHistoryRecords_customPeriodFilteringWithExplicitDates() {
        val oneDayMs = 24L * 60 * 60 * 1000
        val baseDay = 1700000000000L // Dia base
        val day1 = baseDay
        val day2 = baseDay + oneDayMs
        val day3 = baseDay + 2 * oneDayMs

        val rec1 = createRecord(1, "iFood", day1, netProfit = 100.0)
        val rec2 = createRecord(2, "Uber", day2, netProfit = 120.0)
        val rec3 = createRecord(3, "99", day3, netProfit = 90.0)
        val records = listOf(rec1, rec2, rec3)

        // Filtro cobrindo apenas o dia 2
        val filterCustom = HistoryFilterState(
            period = Period.PERSONALIZADO,
            customStartDate = day2,
            customEndDate = day2
        )
        val result = filterAndSortHistoryRecords(records, filterCustom)
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    @Test
    fun testFilterAndSortHistoryRecords_combinedFilteringAndSorting() {
        val (startYear, endYear) = getCurrentYearRange()
        val midYear = (startYear + endYear) / 2

        val rec1 = createRecord(1, "iFood", midYear, netProfit = 200.0, km = 80.0, deliveries = 20)
        val rec2 = createRecord(2, "Mercado Livre", midYear - 1000, netProfit = 350.0, km = 120.0, deliveries = 40)
        val rec3 = createRecord(3, "Uber", midYear - 2000, netProfit = 150.0, km = 50.0, deliveries = 15)

        val records = listOf(rec1, rec2, rec3)

        // Filtro por Delivery + Ordenar por Lucro Maior
        val filterDeliveryProfit = HistoryFilterState(
            period = Period.ANUAL,
            deliveryType = DeliveryTypeFilter.DELIVERY,
            sortOption = HistorySortOption.LUCRO_MAIOR
        )
        val result1 = filterAndSortHistoryRecords(records, filterDeliveryProfit)
        assertEquals(2, result1.size)
        assertEquals(1L, result1[0].id) // iFood tem lucro 200 > Uber lucro 150
        assertEquals(3L, result1[1].id)

        // Filtro por Plataforma específica "Mercado Livre"
        val filterMeli = HistoryFilterState(
            period = Period.ANUAL,
            selectedPlatform = "Mercado Livre"
        )
        val result2 = filterAndSortHistoryRecords(records, filterMeli)
        assertEquals(1, result2.size)
        assertEquals("Mercado Livre", result2[0].platform)

        // Busca textual por "uber"
        val filterAll = HistoryFilterState(period = Period.ANUAL)
        val resultSearch = filterAndSortHistoryRecords(records, filterAll, searchQuery = "uber")
        assertEquals(1, resultSearch.size)
        assertEquals("Uber", resultSearch[0].platform)
    }
}
