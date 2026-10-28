package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class CurrencyUtilsTest {

    // =========================================================================
    // 1. CONVERSÃO BÁSICA (CENTAVOS - ESCALA 100)
    // =========================================================================

    @Test
    fun testToCents_happyPathsAndRounding() {
        assertEquals(15050L, CurrencyUtils.toCents(150.50))
        assertEquals(100L, CurrencyUtils.toCents(1.0))
        assertEquals(1L, CurrencyUtils.toCents(0.01))
        assertEquals(0L, CurrencyUtils.toCents(0.0))
        assertEquals(-4520L, CurrencyUtils.toCents(-45.20))

        // Arredondamento matemático (HALF_UP)
        assertEquals(15056L, CurrencyUtils.toCents(150.556))
        assertEquals(15055L, CurrencyUtils.toCents(150.554))
    }

    @Test
    fun testToCents_specialValues() {
        assertEquals(0L, CurrencyUtils.toCents(Double.NaN))
        assertEquals(0L, CurrencyUtils.toCents(Double.POSITIVE_INFINITY))
        assertEquals(0L, CurrencyUtils.toCents(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun testCentsToDouble() {
        assertEquals(150.50, CurrencyUtils.centsToDouble(15050L), 0.0001)
        assertEquals(1.0, CurrencyUtils.centsToDouble(100L), 0.0001)
        assertEquals(0.01, CurrencyUtils.centsToDouble(1L), 0.0001)
        assertEquals(0.0, CurrencyUtils.centsToDouble(0L), 0.0001)
        assertEquals(-45.20, CurrencyUtils.centsToDouble(-4520L), 0.0001)
    }

    // =========================================================================
    // 2. FORMATAÇÃO MONETÁRIA PT-BR
    // =========================================================================

    @Test
    fun testFormatCents_positiveNegativeAndZero() {
        assertEquals("R$ 1.234,56", CurrencyUtils.formatCents(123456L))
        assertEquals("R$ 150,50", CurrencyUtils.formatCents(15050L))
        assertEquals("R$ 0,00", CurrencyUtils.formatCents(0L))
        assertEquals("- R$ 45,20", CurrencyUtils.formatCents(-4520L))
    }

    @Test
    fun testFormatCentsWithoutSymbol() {
        assertEquals("1.234,56", CurrencyUtils.formatCentsWithoutSymbol(123456L))
        assertEquals("150,50", CurrencyUtils.formatCentsWithoutSymbol(15050L))
        assertEquals("0,00", CurrencyUtils.formatCentsWithoutSymbol(0L))
        assertEquals("- 45,20", CurrencyUtils.formatCentsWithoutSymbol(-4520L))
    }

    @Test
    fun testFormatCentsCompact() {
        assertEquals("R$ 1.235", CurrencyUtils.formatCentsCompact(123456L))
        assertEquals("R$ 151", CurrencyUtils.formatCentsCompact(15050L))
        assertEquals("R$ 0", CurrencyUtils.formatCentsCompact(0L))
        assertEquals("- R$ 45", CurrencyUtils.formatCentsCompact(-4520L))
    }

    // =========================================================================
    // 3. PARSING RESILIENTE (parseToCents)
    // =========================================================================

    @Test
    fun testParseToCents_standardPtBrInputs() {
        assertEquals(123456L, CurrencyUtils.parseToCents("R$ 1.234,56"))
        assertEquals(123456L, CurrencyUtils.parseToCents("1.234,56"))
        assertEquals(15050L, CurrencyUtils.parseToCents("150,50"))
        assertEquals(10000L, CurrencyUtils.parseToCents("100"))
        assertEquals(5020L, CurrencyUtils.parseToCents("  R$  50,2 \u00A0"))
        assertEquals(-4520L, CurrencyUtils.parseToCents("-45,20"))
    }

    @Test
    fun testParseToCents_usFormatWithCommaOrDot() {
        assertEquals(123456L, CurrencyUtils.parseToCents("1,234.56"))
        assertEquals(15050L, CurrencyUtils.parseToCents("150.50"))
        assertEquals(150L, CurrencyUtils.parseToCents("1.50"))
        assertEquals(150L, CurrencyUtils.parseToCents("1.5"))
    }

    /**
     * CONDIÇÃO 1 DO USUÁRIO:
     * Em CurrencyUtils.parseToCents, testar explicitamente entrada com ponto de milhar e sem vírgula
     * (ex.: "1.500") para que seja lido como R$ 1.500,00 (150000L) e NUNCA como 1,50 (150L).
     */
    @Test
    fun testParseToCents_thousandDotWithoutComma() {
        // "1.500" tem 3 dígitos após o ponto -> separador de milhar -> R$ 1.500,00
        val result1500 = CurrencyUtils.parseToCents("1.500")
        assertEquals(150000L, result1500)

        // Comprovando que NÃO é lido como 1,50 (150 centavos)
        org.junit.Assert.assertNotEquals(150L, result1500)

        // Casos adicionais com ponto de milhar
        assertEquals(1500000L, CurrencyUtils.parseToCents("15.000")) // R$ 15.000,00
        assertEquals(25000000L, CurrencyUtils.parseToCents("250.000")) // R$ 250.000,00
        assertEquals(150000000L, CurrencyUtils.parseToCents("1.500.000")) // R$ 1.500.000,00 (múltiplos pontos)
        assertEquals(-150000L, CurrencyUtils.parseToCents("-1.500")) // R$ -1.500,00
    }

    @Test
    fun testParseToCents_fallbacksOnInvalidInput() {
        assertEquals(0L, CurrencyUtils.parseToCents(null))
        assertEquals(0L, CurrencyUtils.parseToCents(""))
        assertEquals(0L, CurrencyUtils.parseToCents("   "))
        assertEquals(999L, CurrencyUtils.parseToCents("invalido", fallbackCents = 999L))
    }

    // =========================================================================
    // 4. COMBUSTÍVEL - MILÉSIMOS DE REAL (ESCALA 1000 - 3 CASAS)
    // =========================================================================

    @Test
    fun testFuelPrice_conversions() {
        assertEquals(5899L, CurrencyUtils.toFuelPriceMillis(5.899))
        assertEquals(5890L, CurrencyUtils.toFuelPriceMillis(5.89))
        assertEquals(6000L, CurrencyUtils.toFuelPriceMillis(6.0))
        assertEquals(0L, CurrencyUtils.toFuelPriceMillis(Double.NaN))

        assertEquals(5.899, CurrencyUtils.fuelPriceMillisToDouble(5899L), 0.00001)
        assertEquals(5.890, CurrencyUtils.fuelPriceMillisToDouble(5890L), 0.00001)
        assertEquals(6.0, CurrencyUtils.fuelPriceMillisToDouble(6000L), 0.00001)
    }

    @Test
    fun testFormatFuelPrice() {
        assertEquals("R$ 5,899", CurrencyUtils.formatFuelPrice(5899L))
        assertEquals("R$ 5,890", CurrencyUtils.formatFuelPrice(5890L))
        assertEquals("R$ 6,000", CurrencyUtils.formatFuelPrice(6000L))
        assertEquals("5,899", CurrencyUtils.formatFuelPriceWithoutSymbol(5899L))
        assertEquals("- R$ 5,899", CurrencyUtils.formatFuelPrice(-5899L))
    }

    @Test
    fun testParseToFuelPriceMillis_inputs() {
        assertEquals(5899L, CurrencyUtils.parseToFuelPriceMillis("5,899"))
        assertEquals(5899L, CurrencyUtils.parseToFuelPriceMillis("5.899"))
        assertEquals(5890L, CurrencyUtils.parseToFuelPriceMillis("5,89"))
        assertEquals(5890L, CurrencyUtils.parseToFuelPriceMillis("5.89"))
        assertEquals(5900L, CurrencyUtils.parseToFuelPriceMillis("5,9"))
        assertEquals(5900L, CurrencyUtils.parseToFuelPriceMillis("5.9"))
        assertEquals(6000L, CurrencyUtils.parseToFuelPriceMillis("6"))
        assertEquals(5899L, CurrencyUtils.parseToFuelPriceMillis("R$ 5,899"))
        assertEquals(0L, CurrencyUtils.parseToFuelPriceMillis(null))
        assertEquals(5000L, CurrencyUtils.parseToFuelPriceMillis("abc", fallbackMillis = 5000L))
    }

    /**
     * CONDIÇÃO 1 DO USUÁRIO (Combustível):
     * Definição e teste de entrada unitária em parseToFuelPriceMillis.
     */
    @Test
    fun testParseToFuelPriceMillis_unitaryAndThousandSeparators() {
        // No domínio de preço de combustível por litro, "1.500" representa R$ 1,500/litro -> 1500L
        assertEquals(1500L, CurrencyUtils.parseToFuelPriceMillis("1.500"))

        // Se houver múltiplos pontos ou vírgula decimal, pontos são milhar
        assertEquals(1500000000L, CurrencyUtils.parseToFuelPriceMillis("1.500.000"))
        assertEquals(1500000L, CurrencyUtils.parseToFuelPriceMillis("1.500,000"))
        assertEquals(1500000L, CurrencyUtils.parseToFuelPriceMillis("1.500,00"))
    }

    @Test
    fun testComputeFuelTotalCents() {
        // 40 litros a R$ 5,899 (5899L) sem desconto = 23596 centavos (R$ 235,96)
        assertEquals(23596L, CurrencyUtils.computeFuelTotalCents(40.0, 5899L, 0L))

        // 40 litros a R$ 5,899 com R$ 2,00 (200L) de desconto = 23396 centavos (R$ 233,96)
        assertEquals(23396L, CurrencyUtils.computeFuelTotalCents(40.0, 5899L, 200L))

        // 10 litros a R$ 5,50 (5500L) com R$ 2,00 de desconto = 5300 centavos (R$ 53,00)
        assertEquals(5300L, CurrencyUtils.computeFuelTotalCents(10.0, 5500L, 200L))

        // Litros zero ou preço zero
        assertEquals(0L, CurrencyUtils.computeFuelTotalCents(0.0, 5899L, 0L))
        assertEquals(0L, CurrencyUtils.computeFuelTotalCents(40.0, 0L, 0L))
    }

    // =========================================================================
    // 5. OPERAÇÕES ARITMÉTICAS ESCALARES SEGURAS
    // =========================================================================

    @Test
    fun testDivideByInt() {
        // R$ 180,00 em 22 dias = 18000 / 22 = 818.1818... -> 818 centavos (R$ 8,18/dia)
        assertEquals(818L, CurrencyUtils.divideByInt(18000L, 22))

        // Divisor zero ou negativo
        assertEquals(0L, CurrencyUtils.divideByInt(18000L, 0))
        assertEquals(0L, CurrencyUtils.divideByInt(18000L, -5))
    }

    @Test
    fun testDivideByDouble_hourlyRate() {
        // R$ 250,50 (25050L) em 8.5 horas = 25050 / 8.5 = 2947.0588... -> 2947 centavos (R$ 29,47/h)
        assertEquals(2947L, CurrencyUtils.divideByDouble(25050L, 8.5))

        // Divisor zero, NaN ou negativo
        assertEquals(0L, CurrencyUtils.divideByDouble(25050L, 0.0))
        assertEquals(0L, CurrencyUtils.divideByDouble(25050L, Double.NaN))
        assertEquals(0L, CurrencyUtils.divideByDouble(25050L, -2.0))
        assertEquals(0L, CurrencyUtils.divideByDouble(0L, 8.5))
    }

    @Test
    fun testMultiplyByDouble() {
        // 589 centavos * 1.25 litros = 736.25 -> 736 centavos
        assertEquals(736L, CurrencyUtils.multiplyByDouble(589L, 1.25))

        // Taxa zero ou NaN
        assertEquals(0L, CurrencyUtils.multiplyByDouble(589L, 0.0))
        assertEquals(0L, CurrencyUtils.multiplyByDouble(589L, Double.NaN))
        assertEquals(0L, CurrencyUtils.multiplyByDouble(0L, 1.25))
    }

    // =========================================================================
    // 6. TESTE DE CONCORRÊNCIA (THREAD-SAFETY)
    // =========================================================================

    @Test
    fun testThreadSafety_concurrentFormatting() {
        val executor = Executors.newFixedThreadPool(8)
        val tasks = (1..100).map { i ->
            Callable {
                val cents = (i * 1000L) + 50L
                val formatted = CurrencyUtils.formatCents(cents)
                val fuel = CurrencyUtils.formatFuelPrice(i * 100L)
                formatted.isNotEmpty() && fuel.isNotEmpty()
            }
        }
        val results = executor.invokeAll(tasks)
        executor.shutdown()
        for (f in results) {
            org.junit.Assert.assertTrue(f.get())
        }
    }
}
