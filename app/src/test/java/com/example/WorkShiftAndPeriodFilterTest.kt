package com.example

import com.example.data.DailyRecord
import com.example.ui.Period
import com.example.ui.util.filterByPeriod
import com.example.ui.util.getCurrentMonthRange
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class WorkShiftAndPeriodFilterTest {

    @Test
    fun testWorkShiftCalculationsNormalShift() {
        val record = DailyRecord(
            dateTimestamp = System.currentTimeMillis(),
            dateString = "2026-10-04",
            grossEarnings = 340.0,
            deliveriesCount = 15,
            startOdometer = 10000.0,
            endOdometer = 10120.0,
            fuelPrice = 5.80,
            fuelCost = 50.0,
            wearCost = 20.0,
            proportionalFixedCost = 15.0,
            foodExpense = 0.0,
            netProfit = 255.0,
            startTime = "08:00",
            endTime = "17:30",
            pauseDuration = "01:00"
        )

        assertEquals(120.0, record.kmRodados, 0.001)
        assertEquals("Pausa deve ser de 60 minutos", 60, record.pauseMinutes)
        assertEquals("Jornada líquida deve ser de 510 minutos (8h30m)", 510, record.totalWorkMinutes)
        assertEquals(8.5, record.totalWorkHours, 0.001)
        assertEquals("8h 30m", record.formattedWorkDuration)
        assertEquals(40.0, record.grossPerHour, 0.001)
        assertEquals(30.0, record.netPerHour, 0.001)
    }

    @Test
    fun testWorkShiftCalculationsOvernightShift() {
        val record = DailyRecord(
            dateTimestamp = System.currentTimeMillis(),
            dateString = "2026-10-04",
            grossEarnings = 220.0,
            deliveriesCount = 10,
            startOdometer = 10000.0,
            endOdometer = 10080.0,
            fuelPrice = 5.80,
            fuelCost = 40.0,
            wearCost = 15.0,
            proportionalFixedCost = 10.0,
            foodExpense = 0.0,
            netProfit = 155.0,
            startTime = "22:00",
            endTime = "04:00",
            pauseDuration = "00:30"
        )

        assertEquals(80.0, record.kmRodados, 0.001)
        assertEquals("Pausa deve ser de 30 minutos", 30, record.pauseMinutes)
        // 22:00 -> 04:00 (+1 dia) = 6h (360 min) - 30 min de pausa = 330 min (5h30m)
        assertEquals("Turno noturno com pausa deve ser de 330 minutos", 330, record.totalWorkMinutes)
        assertEquals(5.5, record.totalWorkHours, 0.001)
        assertEquals("5h 30m", record.formattedWorkDuration)
        assertEquals(40.0, record.grossPerHour, 0.001)
    }

    @Test
    fun testGetCurrentMonthRangeStartsAtFirstDayAndEndsAtLastDay() {
        val (start, end) = getCurrentMonthRange()
        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        val now = Calendar.getInstance()

        assertEquals("O mês de início deve ser o mês corrente", now.get(Calendar.MONTH), calStart.get(Calendar.MONTH))
        assertEquals("O ano de início deve ser o ano corrente", now.get(Calendar.YEAR), calStart.get(Calendar.YEAR))
        assertEquals("O dia de início deve ser exatamente 1", 1, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals("Hora inicial deve ser 00:00:00", 0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minuto inicial deve ser 0", 0, calStart.get(Calendar.MINUTE))
        assertEquals("Segundo inicial deve ser 0", 0, calStart.get(Calendar.SECOND))

        assertEquals("O mês de término deve ser o mês corrente", now.get(Calendar.MONTH), calEnd.get(Calendar.MONTH))
        assertEquals("O dia final deve ser o último dia do mês corrente", calEnd.getActualMaximum(Calendar.DAY_OF_MONTH), calEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals("Hora final deve ser 23:59:59", 23, calEnd.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testPeriodFilterMonthlyVsLast30DaysDistinction() {
        val nowCal = Calendar.getInstance()

        // Registro no dia 15 do mês corrente
        val currentMonthCal = (nowCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val recordCurrentMonth = DailyRecord(
            dateTimestamp = currentMonthCal.timeInMillis,
            dateString = "2026-10-15",
            grossEarnings = 100.0, deliveriesCount = 5, startOdometer = 1000.0, endOdometer = 1050.0,
            fuelPrice = 5.5, fuelCost = 20.0, wearCost = 5.0, proportionalFixedCost = 5.0,
            foodExpense = 0.0, netProfit = 70.0
        )

        // Registro em 25 dias atrás
        val twentyFiveDaysAgoCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -25)
        }
        val record25DaysAgo = DailyRecord(
            dateTimestamp = twentyFiveDaysAgoCal.timeInMillis,
            dateString = "2026-09-09",
            grossEarnings = 100.0, deliveriesCount = 5, startOdometer = 900.0, endOdometer = 950.0,
            fuelPrice = 5.5, fuelCost = 20.0, wearCost = 5.0, proportionalFixedCost = 5.0,
            foodExpense = 0.0, netProfit = 70.0
        )

        // Registro de 45 dias atrás (fora dos 30 dias e fora do mês)
        val fortyFiveDaysAgoCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -45)
        }
        val record45DaysAgo = DailyRecord(
            dateTimestamp = fortyFiveDaysAgoCal.timeInMillis,
            dateString = "2026-08-20",
            grossEarnings = 100.0, deliveriesCount = 5, startOdometer = 800.0, endOdometer = 850.0,
            fuelPrice = 5.5, fuelCost = 20.0, wearCost = 5.0, proportionalFixedCost = 5.0,
            foodExpense = 0.0, netProfit = 70.0
        )

        val allRecords = listOf(recordCurrentMonth, record25DaysAgo, record45DaysAgo)

        val filtered30Days = filterByPeriod(allRecords, Period.ULTIMOS_30_DIAS) { it.dateTimestamp }
        // 25 dias atrás está dentro dos 30 dias
        assertTrue("Últimos 30 dias deve conter o registro de 25 dias atrás", filtered30Days.contains(record25DaysAgo))
        assertFalse("Últimos 30 dias NÃO deve conter o registro de 45 dias atrás", filtered30Days.contains(record45DaysAgo))

        val filteredMonthly = filterByPeriod(allRecords, Period.MENSAL) { it.dateTimestamp }
        assertTrue("Mês Atual deve conter o registro do mês atual", filteredMonthly.contains(recordCurrentMonth))

        // Se 25 dias atrás foi no mês anterior, Mês Atual NÃO deve conter o registro de 25 dias atrás
        if (twentyFiveDaysAgoCal.get(Calendar.MONTH) != nowCal.get(Calendar.MONTH)) {
            assertFalse("Mês Atual NÃO deve conter registro do mês anterior, mesmo dentro de 30 dias", filteredMonthly.contains(record25DaysAgo))
        }
    }
}
