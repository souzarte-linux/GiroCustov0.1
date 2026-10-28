package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Testes Robolectric com contexto Android real (SDK 34).
 * Valida a resolução de strings parametrizadas, mensagens de erro de hodômetro,
 * alertas de capacidade de tanque, feedback de metas e integridade de caracteres pt-BR (T-17 [TST-01]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // =========================================================================
    // 1. Mensagens de Erro de Hodômetro
    // =========================================================================

    @Test
    fun testResolveInvertedOdometerString_returnsExactPtBrText() {
        val message = context.getString(R.string.err_inverted_odometer)
        assertEquals("Hodômetro final não pode ser menor que o inicial.", message)

        val launchError = context.getString(R.string.launch_distance_odometer_error)
        assertEquals("Hodômetro final não pode ser menor que o inicial.", launchError)
    }

    // =========================================================================
    // 2. Alerta Parametrizado de Capacidade de Tanque
    // =========================================================================

    @Test
    fun testResolveExceedsTankCapacityString_formatsParameterizedArguments() {
        val alert = context.getString(R.string.err_exceeds_tank_capacity, 55.0, 50.0, "Honda CG 160")

        assertTrue("Deveria conter cabeçalho de atenção", alert.startsWith("Atenção: A quantidade informada"))
        assertTrue("Deveria conter descrição de capacidade máxima", alert.contains("ultrapassa a capacidade máxima do tanque"))
        assertTrue("Deveria conter o modelo do veículo parametrizado", alert.contains("cadastrada para o Honda CG 160."))
        assertTrue("Deveria conter o volume informado (55)", alert.contains("55"))
        assertTrue("Deveria conter a capacidade limite (50)", alert.contains("50"))
    }

    // =========================================================================
    // 3. Labels e Feedback Calculados
    // =========================================================================

    @Test
    fun testResolveCalculatedDistanceLabel_formatsCorrectly() {
        val distText = context.getString(R.string.calculated_distance_km, 75.5)
        assertTrue(distText.startsWith("Distância calculada:"))
        assertTrue(distText.contains("75") && distText.endsWith("km"))

        val labelOnly = context.getString(R.string.launch_distance_calculated_label)
        assertEquals("Distância calculada no dia:", labelOnly)
    }

    @Test
    fun testResolveCalculatedTotalPrice_formatsCorrectly() {
        val totalText = context.getString(R.string.calculated_total_price, 150.75)
        assertTrue(totalText.startsWith("Total calculado: R$"))
        assertTrue(totalText.contains("150"))
    }

    @Test
    fun testResolveCalculatedLiters_formatsCorrectly() {
        val litersText = context.getString(R.string.calculated_liters, 25.50)
        assertTrue(litersText.startsWith("Litros calculados:"))
        assertTrue(litersText.contains("25") && litersText.endsWith("L"))
    }

    @Test
    fun testResolveCalculatedPricePerLiter_formatsCorrectly() {
        val priceText = context.getString(R.string.calculated_price_per_liter, 5.899)
        assertTrue(priceText.startsWith("Preço por litro calculado: R$"))
        assertTrue(priceText.contains("5"))
    }

    // =========================================================================
    // 4. Feedback de Metas e Integridade de Acentuação pt-BR
    // =========================================================================

    @Test
    fun testResolveGoalsSavedFeedback_returnsExactPtBrMessage() {
        val feedback = context.getString(R.string.goals_saved_feedback)
        assertEquals("Metas salvas com sucesso", feedback)
    }

    @Test
    fun testPtBrAccentedCharactersIntegrity_doesNotCorruptUtf8() {
        val stringsToCheck = listOf(
            context.getString(R.string.err_inverted_odometer),
            context.getString(R.string.err_exceeds_tank_capacity, 10.0, 10.0, "Veículo"),
            context.getString(R.string.calculated_distance_km, 10.0),
            context.getString(R.string.calculated_price_per_liter, 5.0),
            context.getString(R.string.launch_distance_calculated_label),
            context.getString(R.string.goals_saved_feedback)
        )

        for (str in stringsToCheck) {
            assertFalse("String não deve conter caractere de substituição UTF-8 corrompido (\\uFFFD)", str.contains("\uFFFD"))
            assertFalse("String não deve conter interrogação residual por encoding incorreto", str.contains("??"))
        }

        // Valida que caracteres acentuados específicos do português estão presentes
        assertTrue(context.getString(R.string.err_inverted_odometer).contains("ô")) // Hodômetro
        assertTrue(context.getString(R.string.err_inverted_odometer).contains("não")) // não
        assertTrue(context.getString(R.string.err_exceeds_tank_capacity, 1.0, 1.0, "V").contains("Atenção")) // ã, ç
        assertTrue(context.getString(R.string.err_exceeds_tank_capacity, 1.0, 1.0, "V").contains("máxima")) // á
        assertTrue(context.getString(R.string.calculated_price_per_liter, 1.0).contains("Preço")) // ç
    }
}
