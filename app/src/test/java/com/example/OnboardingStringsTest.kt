package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitários para validar a integridade de internacionalização (i18n) e acessibilidade
 * das strings da tela de Onboarding (OnboardingProfileScreen) e subcomponentes (T-29).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingStringsTest {

    @Test
    fun testAllOnboardingStringsExistAndAreNonEmpty() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val stringResIds = listOf(
            R.string.onboarding_step_badge,
            R.string.onboarding_title,
            R.string.onboarding_subtitle,
            R.string.onboarding_section_personal,
            R.string.onboarding_name_label,
            R.string.onboarding_name_placeholder,
            R.string.onboarding_phone_label,
            R.string.onboarding_phone_placeholder,
            R.string.onboarding_whatsapp_title,
            R.string.onboarding_whatsapp_desc,
            R.string.onboarding_whatsapp_icon_desc,
            R.string.onboarding_section_address,
            R.string.onboarding_cep_label,
            R.string.onboarding_cep_placeholder,
            R.string.onboarding_cd_cep_found,
            R.string.onboarding_cd_cep_error,
            R.string.onboarding_cd_cep_search,
            R.string.onboarding_cep_manual_action,
            R.string.onboarding_cep_success_banner,
            R.string.onboarding_street_label,
            R.string.onboarding_street_placeholder,
            R.string.onboarding_neighborhood_label,
            R.string.onboarding_neighborhood_placeholder,
            R.string.onboarding_number_label,
            R.string.onboarding_number_placeholder,
            R.string.onboarding_city_label,
            R.string.onboarding_city_placeholder,
            R.string.onboarding_uf_label,
            R.string.onboarding_uf_placeholder,
            R.string.onboarding_btn_save,
            R.string.onboarding_error_name_required,
            R.string.onboarding_error_phone_required,
            R.string.onboarding_error_cep_invalid
        )

        for (resId in stringResIds) {
            val text = app.getString(resId)
            assertNotNull("Recurso de string não pode ser nulo: $resId", text)
            assertFalse("Recurso de string não pode estar em branco: $resId", text.isBlank())
            assertFalse("Recurso de string não pode conter TODO: $resId", text.contains("TODO", ignoreCase = true))
        }
    }

    @Test
    fun testOnboardingValidationStringsMatchExpectedPortuguese() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        assertEquals(
            "O nome completo é obrigatório.",
            app.getString(R.string.onboarding_error_name_required)
        )
        assertEquals(
            "O telefone para contato é obrigatório.",
            app.getString(R.string.onboarding_error_phone_required)
        )
        assertEquals(
            "Informe um CEP válido com 8 dígitos.",
            app.getString(R.string.onboarding_error_cep_invalid)
        )
    }

    @Test
    fun testOnboardingAccessibilityContentDescriptionsExist() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        assertEquals("WhatsApp", app.getString(R.string.onboarding_whatsapp_icon_desc))
        assertEquals("CEP Localizado", app.getString(R.string.onboarding_cd_cep_found))
        assertEquals("Erro no CEP", app.getString(R.string.onboarding_cd_cep_error))
        assertEquals("Buscar CEP", app.getString(R.string.onboarding_cd_cep_search))
    }
}
