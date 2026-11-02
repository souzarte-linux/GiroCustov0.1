package com.example

import com.example.data.DailyRecord
import com.example.ui.screens.HistoryListItem
import com.example.ui.screens.formatDayLabel
import com.example.ui.screens.groupRecordsHierarchically
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class HistoryGroupingTest {

    private fun createMockRecord(
        id: Long,
        year: Int,
        month: Int,
        day: Int,
        gross: Double,
        net: Double,
        startOdometer: Double = 100.0,
        endOdometer: Double = 150.0
    ): DailyRecord {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return DailyRecord(
            id = id,
            dateString = "$year-${month + 1}-$day",
            dateTimestamp = cal.timeInMillis,
            platform = "iFood",
            grossEarnings = gross,
            deliveriesCount = 5,
            startOdometer = startOdometer,
            endOdometer = endOdometer,
            fuelPrice = 5.0,
            foodExpense = 10.0,
            fuelCost = 20.0,
            wearCost = 5.0,
            proportionalFixedCost = 5.0,
            netProfit = net
        )
    }

    @Test
    fun testTwoDifferentMonths() {
        // May 10, 2026: gross = 100, net = 50
        val rec1 = createMockRecord(1, 2026, Calendar.MAY, 10, 100.0, 50.0)
        // June 15, 2026: gross = 150, net = 80
        val rec2 = createMockRecord(2, 2026, Calendar.JUNE, 15, 150.0, 80.0)

        val result = groupRecordsHierarchically(listOf(rec1, rec2), isAscending = false)

        val yearHeaders = result.filterIsInstance<HistoryListItem.YearHeader>()
        assertEquals(1, yearHeaders.size)
        assertEquals(2026, yearHeaders[0].year)
        assertEquals(250.0, yearHeaders[0].totalGross, 0.001)
        assertEquals(130.0, yearHeaders[0].totalNet, 0.001)

        val monthHeaders = result.filterIsInstance<HistoryListItem.MonthHeader>()
        assertEquals(2, monthHeaders.size)
        
        // DESC list: June (Month 5) should come first, then May (Month 4)
        assertEquals(Calendar.JUNE, monthHeaders[0].month)
        assertEquals(150.0, monthHeaders[0].totalGross, 0.001)
        assertEquals(80.0, monthHeaders[0].totalNet, 0.001)

        assertEquals(Calendar.MAY, monthHeaders[1].month)
        assertEquals(100.0, monthHeaders[1].totalGross, 0.001)
        assertEquals(50.0, monthHeaders[1].totalNet, 0.001)
    }

    @Test
    fun testSplitWeekAcrossMonths() {
        // April 30, 2026 (Thursday)
        val recApril = createMockRecord(1, 2026, Calendar.APRIL, 30, 100.0, 60.0)
        // May 1, 2026 (Friday)
        val recMay = createMockRecord(2, 2026, Calendar.MAY, 1, 200.0, 120.0)

        val result = groupRecordsHierarchically(listOf(recApril, recMay), isAscending = false)

        val monthHeaders = result.filterIsInstance<HistoryListItem.MonthHeader>()
        assertEquals(2, monthHeaders.size)

        // May should come first in DESC
        assertEquals(Calendar.MAY, monthHeaders[0].month)
        assertEquals(200.0, monthHeaders[0].totalGross, 0.001)
        assertEquals(120.0, monthHeaders[0].totalNet, 0.001)

        // April should be second
        assertEquals(Calendar.APRIL, monthHeaders[1].month)
        assertEquals(100.0, monthHeaders[1].totalGross, 0.001)
        assertEquals(60.0, monthHeaders[1].totalNet, 0.001)

        val weekHeaders = result.filterIsInstance<HistoryListItem.WeekHeader>()
        assertEquals(2, weekHeaders.size)
        
        // Week of May 1st should have bounds start=May 1st, end=May 3rd (Friday to Sunday)
        val weekMay = weekHeaders[0]
        val calStartMay = Calendar.getInstance().apply { timeInMillis = weekMay.weekStartMillis }
        val calEndMay = Calendar.getInstance().apply { timeInMillis = weekMay.weekEndMillis }
        assertEquals(1, calStartMay.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MAY, calStartMay.get(Calendar.MONTH))
        assertEquals(3, calEndMay.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MAY, calEndMay.get(Calendar.MONTH))
        assertEquals(200.0, weekMay.totalGross, 0.001)
        assertEquals(120.0, weekMay.totalNet, 0.001)

        // Week of April 30th should have bounds start=April 27th, end=April 30th (Monday to Thursday)
        val weekApril = weekHeaders[1]
        val calStartApril = Calendar.getInstance().apply { timeInMillis = weekApril.weekStartMillis }
        val calEndApril = Calendar.getInstance().apply { timeInMillis = weekApril.weekEndMillis }
        assertEquals(27, calStartApril.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.APRIL, calStartApril.get(Calendar.MONTH))
        assertEquals(30, calEndApril.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.APRIL, calEndApril.get(Calendar.MONTH))
        assertEquals(100.0, weekApril.totalGross, 0.001)
        assertEquals(60.0, weekApril.totalNet, 0.001)
    }

    @Test
    fun testAccumulationAtAllLevels() {
        val rec1 = createMockRecord(1, 2026, Calendar.JANUARY, 5, 100.0, 40.0) // Mon, Jan 5
        val rec2 = createMockRecord(2, 2026, Calendar.JANUARY, 8, 150.0, 60.0) // Thu, Jan 8
        val rec3 = createMockRecord(3, 2026, Calendar.JANUARY, 20, 200.0, 100.0) // Jan 20
        val rec4 = createMockRecord(4, 2026, Calendar.FEBRUARY, 10, 300.0, 150.0) // Feb 10

        val result = groupRecordsHierarchically(listOf(rec1, rec2, rec3, rec4), isAscending = false)

        val yearHeader = result.filterIsInstance<HistoryListItem.YearHeader>().first()
        assertEquals(2026, yearHeader.year)
        assertEquals(750.0, yearHeader.totalGross, 0.001)
        assertEquals(350.0, yearHeader.totalNet, 0.001)

        val monthHeaders = result.filterIsInstance<HistoryListItem.MonthHeader>()
        val janHeader = monthHeaders.first { it.month == Calendar.JANUARY }
        val febHeader = monthHeaders.first { it.month == Calendar.FEBRUARY }
        assertEquals(450.0, janHeader.totalGross, 0.001)
        assertEquals(200.0, janHeader.totalNet, 0.001)
        assertEquals(300.0, febHeader.totalGross, 0.001)
        assertEquals(150.0, febHeader.totalNet, 0.001)

        val janWeekHeaders = result.filterIsInstance<HistoryListItem.WeekHeader>().filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.weekStartMillis }
            cal.get(Calendar.MONTH) == Calendar.JANUARY
        }
        assertEquals(2, janWeekHeaders.size)
        assertTrue(janWeekHeaders.any { it.totalGross == 250.0 && it.totalNet == 100.0 })
        assertTrue(janWeekHeaders.any { it.totalGross == 200.0 && it.totalNet == 100.0 })
    }

    @Test
    fun testDayHeaderLabelCapitalization() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 9) // July 9, 2026 (Quinta-feira)
        }
        val label = formatDayLabel(cal.timeInMillis)
        
        assertTrue(label.isNotEmpty())
        val firstChar = label.substring(0, 1)
        assertEquals(firstChar.uppercase(Locale.forLanguageTag("pt-BR")), firstChar)
        assertTrue(label.contains("Quinta-feira") || label.contains("Quinta"))
    }

    @Test
    fun testAnnualConsolidation_growthPercentagesWithPreviousYear() {
        // 2025: Gross = 5000.0, Net = 3000.0
        val rec2025 = createMockRecord(1, 2025, Calendar.DECEMBER, 15, 5000.0, 3000.0)
        // 2026: Gross = 6000.0, Net = 3600.0
        val rec2026 = createMockRecord(2, 2026, Calendar.JUNE, 10, 6000.0, 3600.0)

        val result = groupRecordsHierarchically(listOf(rec2025, rec2026), isAscending = false)
        val yearHeaders = result.filterIsInstance<HistoryListItem.YearHeader>()

        assertEquals(2, yearHeaders.size)

        val header2026 = yearHeaders.first { it.year == 2026 }
        assertEquals(6000.0, header2026.totalGross, 0.001)
        assertEquals(3600.0, header2026.totalNet, 0.001)
        assertEquals(20.0, header2026.growthPercentage!!, 0.001)
        assertEquals(20.0, header2026.netGrowthPercentage!!, 0.001)

        val header2025 = yearHeaders.first { it.year == 2025 }
        assertEquals(5000.0, header2025.totalGross, 0.001)
        assertEquals(3000.0, header2025.totalNet, 0.001)
        org.junit.Assert.assertNull(header2025.growthPercentage)
        org.junit.Assert.assertNull(header2025.netGrowthPercentage)
    }

    @Test
    fun testAnnualConsolidation_negativeGrowthAndZeroDivisionProtection() {
        // 2025: Gross = 10000.0, Net = 5000.0
        val rec2025 = createMockRecord(1, 2025, Calendar.OCTOBER, 1, 10000.0, 5000.0)
        // 2026: Gross = 8000.0, Net = 3500.0
        val rec2026 = createMockRecord(2, 2026, Calendar.NOVEMBER, 1, 8000.0, 3500.0)

        val result = groupRecordsHierarchically(listOf(rec2025, rec2026), isAscending = false)
        val header2026 = result.filterIsInstance<HistoryListItem.YearHeader>().first { it.year == 2026 }

        // (8000 - 10000) / 10000 * 100 = -20.0%
        assertEquals(-20.0, header2026.growthPercentage!!, 0.001)
        // (3500 - 5000) / 5000 * 100 = -30.0%
        assertEquals(-30.0, header2026.netGrowthPercentage!!, 0.001)
    }

    @Test
    fun testAnnualConsolidation_singleYearHasNullGrowthPercentagesAndAccurateDailyAverage() {
        val rec1 = createMockRecord(1, 2026, Calendar.MARCH, 10, 300.0, 200.0)
        val rec2 = createMockRecord(2, 2026, Calendar.MARCH, 11, 200.0, 150.0)

        val result = groupRecordsHierarchically(listOf(rec1, rec2), isAscending = false)
        val header2026 = result.filterIsInstance<HistoryListItem.YearHeader>().first()

        assertEquals(500.0, header2026.totalGross, 0.001)
        assertEquals(350.0, header2026.totalNet, 0.001)
        assertEquals(2, header2026.activeDays)
        // Média Dia = 500 / 2 = 250.0
        assertEquals(250.0, header2026.dailyAverage, 0.001)
        org.junit.Assert.assertNull(header2026.growthPercentage)
        org.junit.Assert.assertNull(header2026.netGrowthPercentage)
    }

    @Test
    fun testWeekHeader_accumulatesTotalKm_multipleDaysInSameWeek() {
        // Semana de 12 a 18 de Outubro de 2026
        // Segunda-feira (12/10): 2 registros no mesmo dia
        val recMon1 = createMockRecord(1, 2026, Calendar.OCTOBER, 12, 120.0, 80.0, startOdometer = 100.0, endOdometer = 145.5) // 45.5 km
        val recMon2 = createMockRecord(2, 2026, Calendar.OCTOBER, 12, 80.0, 50.0, startOdometer = 145.5, endOdometer = 160.7)  // 15.2 km

        // Quarta-feira (14/10): 1 registro com casa decimal
        val recWed = createMockRecord(3, 2026, Calendar.OCTOBER, 14, 150.0, 95.0, startOdometer = 200.0, endOdometer = 232.3)   // 32.3 km

        // Sexta-feira (16/10): 1 registro com zero km e 1 registro normal
        val recFri1 = createMockRecord(4, 2026, Calendar.OCTOBER, 16, 50.0, 30.0, startOdometer = 300.0, endOdometer = 300.0)    // 0.0 km
        val recFri2 = createMockRecord(5, 2026, Calendar.OCTOBER, 16, 100.0, 65.0, startOdometer = 300.0, endOdometer = 313.0)  // 13.0 km

        // Segunda semana para verificar isolamento (19/10/2026)
        val recNextWeek = createMockRecord(6, 2026, Calendar.OCTOBER, 19, 200.0, 130.0, startOdometer = 400.0, endOdometer = 454.8) // 54.8 km

        val result = groupRecordsHierarchically(
            listOf(recMon1, recMon2, recWed, recFri1, recFri2, recNextWeek),
            isAscending = false
        )

        val weekHeaders = result.filterIsInstance<HistoryListItem.WeekHeader>()
        assertEquals(2, weekHeaders.size)

        // Semana mais recente (19/10): totalKm = 54.8
        val week2 = weekHeaders[0]
        assertEquals(54.8, week2.totalKm, 0.001)

        // Semana anterior (12 a 18/10): totalKm = 45.5 + 15.2 + 32.3 + 0.0 + 13.0 = 106.0
        val week1 = weekHeaders[1]
        assertEquals(106.0, week1.totalKm, 0.001)

        // MonthHeader deve acumular o total das duas semanas = 106.0 + 54.8 = 160.8
        val monthHeader = result.filterIsInstance<HistoryListItem.MonthHeader>().first()
        assertEquals(160.8, monthHeader.totalKm, 0.001)
    }

    @Test
    fun testWeekHeader_edgeCases_decimalsZeroKmAndInvertedOdometer() {
        // Semana com casos de borda:
        // 1. Zero km (start == end)
        val recZeroKm = createMockRecord(1, 2026, Calendar.OCTOBER, 12, 50.0, 30.0, startOdometer = 500.0, endOdometer = 500.0) // 0.0 km

        // 2. Odômetro invertido (start > end) -> kmRodados coerceAtLeast(0.0) -> 0.0 km
        val recInverted = createMockRecord(2, 2026, Calendar.OCTOBER, 13, 60.0, 40.0, startOdometer = 550.0, endOdometer = 520.0) // 0.0 km

        // 3. Decimais de alta precisão (12.35 km + 7.65 km = 20.0 km)
        val recDec1 = createMockRecord(3, 2026, Calendar.OCTOBER, 14, 100.0, 70.0, startOdometer = 10.0, endOdometer = 22.35)   // 12.35 km
        val recDec2 = createMockRecord(4, 2026, Calendar.OCTOBER, 15, 80.0, 50.0, startOdometer = 22.35, endOdometer = 30.00)   // 7.65 km

        val result = groupRecordsHierarchically(
            listOf(recZeroKm, recInverted, recDec1, recDec2),
            isAscending = false
        )

        val weekHeader = result.filterIsInstance<HistoryListItem.WeekHeader>().first()
        // 0.0 + 0.0 + 12.35 + 7.65 = 20.0 km
        assertEquals(20.0, weekHeader.totalKm, 0.001)
    }
}
