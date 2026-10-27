package com.example

import com.example.data.DailyRecord
import com.example.data.FuelRefill
import com.example.data.MaintenanceRecord
import com.example.data.Platform
import com.example.ui.screens.HistoryListItem
import com.example.ui.screens.getHistoryListItemKey
import com.example.ui.screens.groupRecordsHierarchically
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Suíte de testes unitários para a Tarefa T-09 [PERF-02].
 * Valida a estabilidade, determinismo e unicidade das chaves (keys)
 * utilizadas em todas as listas LazyColumn do Jetpack Compose.
 */
class LazyListKeysAndStabilityTest {

    // =========================================================================
    // 1. CHAVES DE ABASTECIMENTOS (FuelRefillsScreen)
    // =========================================================================

    @Test
    fun testFuelRefillKeys_areUniqueAndMatchEntityId() {
        val refills = listOf(
            FuelRefill(id = 101, vehicleId = 1L, dateTimestamp = 1000L, dateString = "01/01/2026", gasStation = "Posto A", fuelType = "Gasolina", pricePerLiter = 5.5, liters = 20.0, totalPaid = 110.0, odometer = 1000.0, isFullTank = true, paymentMethod = "PIX"),
            FuelRefill(id = 102, vehicleId = 1L, dateTimestamp = 2000L, dateString = "05/01/2026", gasStation = "Posto B", fuelType = "Etanol", pricePerLiter = 3.5, liters = 30.0, totalPaid = 105.0, odometer = 1500.0, isFullTank = false, paymentMethod = "Cartão"),
            FuelRefill(id = 103, vehicleId = 1L, dateTimestamp = 3000L, dateString = "10/01/2026", gasStation = "Posto C", fuelType = "Gasolina", pricePerLiter = 5.6, liters = 15.0, totalPaid = 84.0, odometer = 1800.0, isFullTank = true, paymentMethod = "Dinheiro")
        )

        // Expressão idêntica à utilizada em items(items = refills, key = { it.id })
        val keys = refills.map { it.id }

        assertEquals(3, keys.size)
        assertEquals(listOf(101L, 102L, 103L), keys)
        assertEquals("Todas as chaves devem ser distintas", keys.size, keys.distinct().size)
    }

    // =========================================================================
    // 2. CHAVES DE MANUTENÇÕES (MaintenanceScreen)
    // =========================================================================

    @Test
    fun testMaintenanceRecordKeys_areUniqueAndMatchEntityId() {
        val maintenanceRecords = listOf(
            MaintenanceRecord(id = 201, vehicleId = 1L, dateTimestamp = 1000L, dateString = "01/01/2026", description = "Troca de Óleo", location = "Oficina X", value = 150.0, odometer = 5000.0),
            MaintenanceRecord(id = 202, vehicleId = 1L, dateTimestamp = 2000L, dateString = "05/01/2026", description = "Pastilhas de Freio", location = "Oficina Y", value = 220.0, odometer = 8000.0),
            MaintenanceRecord(id = 203, vehicleId = 1L, dateTimestamp = 3000L, dateString = "10/01/2026", description = "Pneu Traseiro", location = "Borracharia Z", value = 350.0, odometer = 12000.0)
        )

        // Expressão idêntica à utilizada em items(items = records, key = { it.id })
        val keys = maintenanceRecords.map { it.id }

        assertEquals(3, keys.size)
        assertEquals(listOf(201L, 202L, 203L), keys)
        assertEquals("Todas as chaves devem ser distintas", keys.size, keys.distinct().size)
    }

    // =========================================================================
    // 3. CHAVES DE PLATAFORMAS (PlatformsScreen)
    // =========================================================================

    @Test
    fun testPlatformKeys_areUniqueAndMatchEntityId() {
        val platforms = listOf(
            Platform(id = 1, name = "iFood"),
            Platform(id = 2, name = "Uber Moto"),
            Platform(id = 3, name = "99 Moto"),
            Platform(id = 4, name = "Lalamove")
        )

        // Expressão idêntica à utilizada em items(items = platforms, key = { it.id })
        val keys = platforms.map { it.id }

        assertEquals(4, keys.size)
        assertEquals(listOf(1L, 2L, 3L, 4L), keys)
        assertEquals("Todas as chaves devem ser distintas", keys.size, keys.distinct().size)
    }

    // =========================================================================
    // 4. CHAVES DE HISTÓRICO - MODO PLANO (HistoryScreen)
    // =========================================================================

    @Test
    fun testDailyRecordFlatKeys_areUniqueAndMatchEntityId() {
        val records = listOf(
            createMockRecord(id = 301, year = 2026, month = Calendar.JANUARY, day = 10, gross = 250.0, net = 160.0),
            createMockRecord(id = 302, year = 2026, month = Calendar.JANUARY, day = 11, gross = 300.0, net = 200.0),
            createMockRecord(id = 303, year = 2026, month = Calendar.JANUARY, day = 12, gross = 180.0, net = 110.0)
        )

        // Expressão idêntica à utilizada em items(items = filteredAndSortedRecords, key = { it.id })
        val keys = records.map { it.id }

        assertEquals(3, keys.size)
        assertEquals(listOf(301L, 302L, 303L), keys)
        assertEquals("Todas as chaves devem ser distintas", keys.size, keys.distinct().size)
    }

    // =========================================================================
    // 5. CHAVES DE HISTÓRICO - MODO HIERÁRQUICO (HistoryScreen)
    // =========================================================================

    @Test
    fun testHistoryHierarchicalKeys_areUniqueWithMultipleRecordsPerDay() {
        // Cenário comum do entregador: dois turnos no mesmo dia (almoço e janta)
        val r1 = createMockRecord(id = 1L, year = 2026, month = Calendar.MARCH, day = 15, gross = 150.0, net = 100.0, hour = 11)
        val r2 = createMockRecord(id = 2L, year = 2026, month = Calendar.MARCH, day = 15, gross = 180.0, net = 120.0, hour = 19)
        val r3 = createMockRecord(id = 3L, year = 2026, month = Calendar.MARCH, day = 16, gross = 200.0, net = 140.0, hour = 14)

        val grouped = groupRecordsHierarchically(listOf(r1, r2, r3), isAscending = false)

        val keys = grouped.map { getHistoryListItemKey(it) }

        // Deve conter Ano, Mês, Semana, Dias e Linhas individuais de Turno
        assertTrue("Deve conter mais itens que os records originais devido aos headers", grouped.size > 3)
        assertEquals("Todas as chaves da lista agrupada devem ser estritamente únicas", keys.size, keys.distinct().size)

        // Valida que os records têm a chave com prefixo "record_"
        assertTrue(keys.contains("record_1"))
        assertTrue(keys.contains("record_2"))
        assertTrue(keys.contains("record_3"))

        // Valida que o ano tem chave "year_2026"
        assertTrue(keys.contains("year_2026"))

        // Valida que o mês tem chave "month_2026_2" (Calendar.MARCH == 2)
        assertTrue(keys.contains("month_2026_${Calendar.MARCH}"))
    }

    @Test
    fun testHistoryHierarchicalKeys_acrossMonthsAndYearsAreCompletelyUnique() {
        // Cenário de virada de ano: Dezembro/2025 e Janeiro/2026
        val decRecord = createMockRecord(id = 10L, year = 2025, month = Calendar.DECEMBER, day = 31, gross = 300.0, net = 210.0)
        val janRecord = createMockRecord(id = 11L, year = 2026, month = Calendar.JANUARY, day = 1, gross = 200.0, net = 130.0)
        val febRecord = createMockRecord(id = 12L, year = 2026, month = Calendar.FEBRUARY, day = 10, gross = 250.0, net = 170.0)

        val grouped = groupRecordsHierarchically(listOf(decRecord, janRecord, febRecord), isAscending = true)

        val keys = grouped.map { getHistoryListItemKey(it) }

        assertEquals("Nenhuma chave pode colidir entre anos, meses e semanas distintos", keys.size, keys.distinct().size)

        // Anos distintos
        assertTrue(keys.contains("year_2025"))
        assertTrue(keys.contains("year_2026"))

        // Meses distintos
        assertTrue(keys.contains("month_2025_${Calendar.DECEMBER}"))
        assertTrue(keys.contains("month_2026_${Calendar.JANUARY}"))
        assertTrue(keys.contains("month_2026_${Calendar.FEBRUARY}"))

        // Registros individuais
        assertTrue(keys.contains("record_10"))
        assertTrue(keys.contains("record_11"))
        assertTrue(keys.contains("record_12"))
    }

    @Test
    fun testHistoryHierarchicalKeys_areDeterministicAcrossMultipleInvocations() {
        val recs = listOf(
            createMockRecord(id = 1L, year = 2026, month = Calendar.APRIL, day = 5, gross = 100.0, net = 70.0),
            createMockRecord(id = 2L, year = 2026, month = Calendar.APRIL, day = 6, gross = 150.0, net = 90.0)
        )

        val grouped1 = groupRecordsHierarchically(recs, isAscending = false)
        val grouped2 = groupRecordsHierarchically(recs, isAscending = false)

        val keys1 = grouped1.map { getHistoryListItemKey(it) }
        val keys2 = grouped2.map { getHistoryListItemKey(it) }

        assertEquals("As chaves geradas devem ser rigorosamente determinísticas", keys1, keys2)
    }

    // =========================================================================
    // UTILITÁRIO HELPER
    // =========================================================================

    private fun createMockRecord(
        id: Long,
        year: Int,
        month: Int,
        day: Int,
        gross: Double,
        net: Double,
        hour: Int = 12
    ): DailyRecord {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
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
}
