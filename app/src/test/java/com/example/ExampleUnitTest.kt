package com.example

import com.example.ui.GiroCustoDomainCalculations
import com.example.ui.LaunchDistanceState
import com.example.ui.screens.parsePtBrDouble
import com.example.ui.screens.parsePtBrInt
import org.junit.Assert.*
import org.junit.Test

/**
 * Bateria de testes unitários puros JVM (sem dependência do framework Android / Context).
 * Valida com rigor as regras de negócio de hodômetro, combustível, abastecimento
 * e parsing numérico regional brasileiro (T-17 [TST-01]).
 */
class ExampleUnitTest {

    // =========================================================================
    // 1. Hodômetro e Distância Diária (LaunchScreen)
    // =========================================================================

    @Test
    fun testOdometer_validProgression_calculatesCorrectDistance() {
        val result = GiroCustoDomainCalculations.calculateDailyDistance("1000.0", "1050.0")
        assertTrue("Resultado esperado é LaunchDistanceState.Valid, mas foi $result", result is LaunchDistanceState.Valid)
        val valid = result as LaunchDistanceState.Valid
        assertEquals(50.0, valid.distanceKm, 0.001)
        assertEquals("50,0", valid.formattedKm)
    }

    @Test
    fun testOdometer_commaDecimalPtBr_calculatesCorrectDistance() {
        val result = GiroCustoDomainCalculations.calculateDailyDistance("1000,5", "1050,7")
        assertTrue("Resultado esperado é LaunchDistanceState.Valid, mas foi $result", result is LaunchDistanceState.Valid)
        val valid = result as LaunchDistanceState.Valid
        assertEquals(50.2, valid.distanceKm, 0.001)
        assertEquals("50,2", valid.formattedKm)
    }

    @Test
    fun testOdometer_endLessThanStart_returnsNegativeError() {
        val result = GiroCustoDomainCalculations.calculateDailyDistance("1100.0", "1050.0")
        assertEquals(LaunchDistanceState.NegativeError, result)
    }

    @Test
    fun testOdometer_startEqualsEnd_zeroKmReturnedAsValid() {
        val result = GiroCustoDomainCalculations.calculateDailyDistance("1200.0", "1200.0")
        assertTrue("Resultado esperado é LaunchDistanceState.Valid com 0.0 km, mas foi $result", result is LaunchDistanceState.Valid)
        val valid = result as LaunchDistanceState.Valid
        assertEquals(0.0, valid.distanceKm, 0.001)
        assertEquals("0,0", valid.formattedKm)
    }

    @Test
    fun testOdometer_blankOrSpaces_returnsNone() {
        assertEquals(LaunchDistanceState.None, GiroCustoDomainCalculations.calculateDailyDistance("", "1050.0"))
        assertEquals(LaunchDistanceState.None, GiroCustoDomainCalculations.calculateDailyDistance("1000.0", ""))
        assertEquals(LaunchDistanceState.None, GiroCustoDomainCalculations.calculateDailyDistance("   ", "   "))
    }

    @Test
    fun testOdometer_invalidCharacters_returnsNone() {
        assertEquals(LaunchDistanceState.None, GiroCustoDomainCalculations.calculateDailyDistance("abc", "1050.0"))
        assertEquals(LaunchDistanceState.None, GiroCustoDomainCalculations.calculateDailyDistance("1000.0", "xyz"))
        assertEquals(LaunchDistanceState.None, GiroCustoDomainCalculations.calculateDailyDistance("###", "???"))
    }

    // =========================================================================
    // 2. Combustível e Abastecimento (FuelRefillsScreen)
    // =========================================================================

    @Test
    fun testFuel_zeroOrNegativeLiters_isInvalid() {
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("5.50", "0.0", "10000"))
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("5.50", "-10.0", "10000"))
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("5.50", "", "10000"))
    }

    @Test
    fun testFuel_zeroOrNegativePrice_isInvalid() {
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("0.0", "10.0", "10000"))
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("-5.00", "10.0", "10000"))
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("", "10.0", "10000"))
    }

    @Test
    fun testFuel_zeroOrNegativeOdometer_isInvalid() {
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("5.50", "10.0", "0.0"))
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("5.50", "10.0", "-500"))
        assertFalse(GiroCustoDomainCalculations.isValidFuelInput("5.50", "10.0", ""))
    }

    @Test
    fun testFuel_validInputs_returnsTrue() {
        assertTrue(GiroCustoDomainCalculations.isValidFuelInput("5.89", "45.0", "15420"))
        assertTrue(GiroCustoDomainCalculations.isValidFuelInput("5,89", "45,5", "15420,0"))
    }

    @Test
    fun testFuel_exceedsTankCapacity_validation() {
        // Excede capacidade de 50L
        assertTrue(GiroCustoDomainCalculations.checkExceedsTankCapacity("55.0", 50.0))
        assertTrue(GiroCustoDomainCalculations.checkExceedsTankCapacity("50,5", 50.0))

        // Não excede (igual ou menor)
        assertFalse(GiroCustoDomainCalculations.checkExceedsTankCapacity("50.0", 50.0))
        assertFalse(GiroCustoDomainCalculations.checkExceedsTankCapacity("45.0", 50.0))

        // Capacidade não informada ou zerada
        assertFalse(GiroCustoDomainCalculations.checkExceedsTankCapacity("55.0", 0.0))

        // Entrada malformada
        assertFalse(GiroCustoDomainCalculations.checkExceedsTankCapacity("invalido", 50.0))
        assertFalse(GiroCustoDomainCalculations.checkExceedsTankCapacity("", 50.0))
    }

    @Test
    fun testFuel_computeRefillTotal_standardMath() {
        val total = GiroCustoDomainCalculations.computeRefillTotal("5.00", "10.00", "2.00", "")
        // (5.00 * 10.00) - 2.00 = 48.00
        assertEquals(48.00, total, 0.001)
    }

    @Test
    fun testFuel_computeRefillTotal_discountExceedsTotal_floorsToZero() {
        // Proteção contra valor financeiro negativo quando desconto supera total
        val total = GiroCustoDomainCalculations.computeRefillTotal("5.00", "10.00", "100.00", "")
        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun testFuel_computeRefillTotal_explicitTotalPrioritized() {
        val total = GiroCustoDomainCalculations.computeRefillTotal("5.00", "10.00", "2.00", "45.00")
        assertEquals(45.00, total, 0.001)

        val totalNegative = GiroCustoDomainCalculations.computeRefillTotal("5.00", "10.00", "2.00", "-10.00")
        assertEquals(0.0, totalNegative, 0.001)
    }

    @Test
    fun testFuel_multiDirectional_twoFields_calculatesThirdField() {
        // Preço + Litros -> Total
        val res1 = GiroCustoDomainCalculations.calculateRefillMultiDirectional("5.00", "10.00", "2.00", "", "")
        assertEquals("48.00", res1.updatedTotalPaid)
        assertNull(res1.updatedPricePerLiter)
        assertNull(res1.updatedLiters)

        // Litros + Total -> Preço
        val res2 = GiroCustoDomainCalculations.calculateRefillMultiDirectional("", "10.00", "0.00", "50.00", "")
        assertEquals("5.00", res2.updatedPricePerLiter)
        assertNull(res2.updatedLiters)
        assertNull(res2.updatedTotalPaid)

        // Preço + Total -> Litros
        val res3 = GiroCustoDomainCalculations.calculateRefillMultiDirectional("5.00", "", "0.00", "50.00", "")
        assertEquals("10.00", res3.updatedLiters)
        assertNull(res3.updatedPricePerLiter)
        assertNull(res3.updatedTotalPaid)
    }

    @Test
    fun testFuel_multiDirectional_threeFields_focusedRecalculation() {
        // Foco em total com 3 campos -> recalcula litros
        val resTotalFocus = GiroCustoDomainCalculations.calculateRefillMultiDirectional(
            pricePerLiterStr = "5.00",
            litersStr = "10.00",
            discountStr = "0.00",
            totalPaidStr = "75.00",
            focusedField = "total"
        )
        // 75.00 / 5.00 = 15.00 L
        assertEquals("15.00", resTotalFocus.updatedLiters)

        // Foco em preço com 3 campos -> recalcula total
        val resPriceFocus = GiroCustoDomainCalculations.calculateRefillMultiDirectional(
            pricePerLiterStr = "6.00",
            litersStr = "10.00",
            discountStr = "0.00",
            totalPaidStr = "50.00",
            focusedField = "price"
        )
        // 6.00 * 10.00 = 60.00
        assertEquals("60.00", resPriceFocus.updatedTotalPaid)
    }

    @Test
    fun testFuel_multiDirectional_invalidOrZeroInputs_returnsEmptyResult() {
        val result = GiroCustoDomainCalculations.calculateRefillMultiDirectional(
            pricePerLiterStr = "0.00",
            litersStr = "",
            discountStr = "0.00",
            totalPaidStr = "",
            focusedField = ""
        )
        assertNull(result.updatedPricePerLiter)
        assertNull(result.updatedLiters)
        assertNull(result.updatedTotalPaid)
    }

    // =========================================================================
    // 3. Parsing Numérico pt-BR (DashboardScreen)
    // =========================================================================

    @Test
    fun testPtBrParsing_doubleWithCommaDecimal() {
        assertEquals(250.0, parsePtBrDouble("250,00", 200.0), 0.001)
        assertEquals(250.5, parsePtBrDouble("250,5", 200.0), 0.001)
        assertEquals(250.5, parsePtBrDouble("250,50", 200.0), 0.001)
        assertEquals(250.0, parsePtBrDouble("250,", 200.0), 0.001)
    }

    @Test
    fun testPtBrParsing_doubleWithThousandSeparatorAndComma() {
        assertEquals(1250.5, parsePtBrDouble("1.250,50", 1200.0), 0.001)
        assertEquals(5000.0, parsePtBrDouble("5.000,00", 1000.0), 0.001)
        assertEquals(1000250.5, parsePtBrDouble("1.000.250,50", 1000.0), 0.001)
    }

    @Test
    fun testPtBrParsing_doubleWithCurrencyAndSpaces() {
        assertEquals(250.0, parsePtBrDouble("R$ 250,00", 200.0), 0.001)
        assertEquals(120.0, parsePtBrDouble("  120,0  ", 100.0), 0.001)
        assertEquals(160.0, parsePtBrDouble("160.00", 130.0), 0.001)
    }

    @Test
    fun testPtBrParsing_doubleMalformed_returnsFallback() {
        assertEquals(200.0, parsePtBrDouble("invalido", 200.0), 0.001)
        assertEquals(200.0, parsePtBrDouble("", 200.0), 0.001)
        assertEquals(200.0, parsePtBrDouble("   ", 200.0), 0.001)
    }

    @Test
    fun testPtBrParsing_intStandardAndMalformed() {
        assertEquals(18, parsePtBrInt("18", 15))
        assertEquals(25, parsePtBrInt(" 25 ", 15))
        assertEquals(1500, parsePtBrInt("1.500", 15))
        assertEquals(15, parsePtBrInt("invalido", 15))
        assertEquals(15, parsePtBrInt("", 15))
    }
}
