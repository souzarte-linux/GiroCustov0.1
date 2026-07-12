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

    private fun createMockRecord(id: Long, year: Int, month: Int, day: Int, gross: Double, net: Double): DailyRecord {
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
            startOdometer = 100.0,
            endOdometer = 150.0,
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
        assertEquals(firstChar.uppercase(Locale("pt", "BR")), firstChar)
        assertTrue(label.contains("Quinta-feira") || label.contains("Quinta"))
    }
}
