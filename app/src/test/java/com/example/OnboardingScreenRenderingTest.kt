package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GiroCustoDatabase
import com.example.data.GiroCustoRepository
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.OnboardingProfileScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Testes unitários com Robolectric Compose para renderização, interações e validações
 * da tela modularizada de Onboarding (OnboardingProfileScreen) - T-29.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class OnboardingScreenRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var viewModel: GiroCustoViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        GiroCustoDatabase.setTestDatabase(db)
        repository = GiroCustoRepository(db)
        viewModel = GiroCustoViewModel(context)
    }

    @After
    fun tearDown() {
        db.close()
        GiroCustoDatabase.resetForTesting()
    }

    @Test
    fun testOnboardingProfileScreen_rendersAllFormFieldsAndSections() {
        composeTestRule.setContent {
            MyApplicationTheme {
                OnboardingProfileScreen(
                    viewModel = viewModel
                )
            }
        }

        // 1. Header e Títulos
        composeTestRule.onNodeWithText("PASSO 1 DE 1 • CONFIGURAÇÃO INICIAL").assertExists()
        composeTestRule.onNodeWithText("Complete seu Cadastro").assertExists()

        // 2. Seção 1: Dados Pessoais
        composeTestRule.onNodeWithText("1. Dados Pessoais & Contato").assertExists()
        composeTestRule.onNodeWithTag("onboarding_name_input").assertExists()
        composeTestRule.onNodeWithTag("onboarding_phone_input").assertExists()
        composeTestRule.onNodeWithTag("onboarding_whatsapp_switch").assertExists()

        // 3. Seção 2: Localização & Endereço
        composeTestRule.onNodeWithText("2. Localização & Endereço").assertExists()
        composeTestRule.onNodeWithTag("onboarding_cep_input").assertExists()
        composeTestRule.onNodeWithTag("onboarding_manual_cep_toggle").assertExists()
        composeTestRule.onNodeWithTag("onboarding_street_input").assertExists()
        composeTestRule.onNodeWithTag("onboarding_neighborhood_input").assertExists()
        composeTestRule.onNodeWithTag("onboarding_number_input").assertExists()
        composeTestRule.onNodeWithTag("onboarding_city_input").assertExists()
        composeTestRule.onNodeWithTag("onboarding_uf_input").assertExists()

        // 4. Botão Final de Submissão
        composeTestRule.onNodeWithTag("onboarding_submit_button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun testOnboardingProfileScreen_validationsShowCorrectErrorMessages() {
        composeTestRule.setContent {
            MyApplicationTheme {
                OnboardingProfileScreen(
                    viewModel = viewModel
                )
            }
        }

        // 1. Tentar salvar com nome vazio deve exibir erro de nome obrigatório
        composeTestRule.onNodeWithTag("onboarding_submit_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("O nome completo é obrigatório.").assertExists()

        // 2. Preencher nome e tentar salvar com telefone vazio
        composeTestRule.onNodeWithTag("onboarding_name_input")
            .performScrollTo()
            .performTextInput("Carlos Silva")
        composeTestRule.onNodeWithTag("onboarding_submit_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("O telefone para contato é obrigatório.").assertExists()

        // 3. Preencher telefone e tentar salvar com CEP inválido (< 8 dígitos)
        composeTestRule.onNodeWithTag("onboarding_phone_input")
            .performScrollTo()
            .performTextInput("11987654321")
        composeTestRule.onNodeWithTag("onboarding_cep_input")
            .performScrollTo()
            .performTextInput("12345")
        composeTestRule.onNodeWithTag("onboarding_submit_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Informe um CEP válido com 8 dígitos.").assertExists()
    }

    @Test
    fun testOnboardingProfileScreen_whatsappSwitchToggle() {
        composeTestRule.setContent {
            MyApplicationTheme {
                OnboardingProfileScreen(
                    viewModel = viewModel
                )
            }
        }

        val switchNode = composeTestRule.onNodeWithTag("onboarding_whatsapp_switch")
            .performScrollTo()
        switchNode.assertIsDisplayed()
        switchNode.performClick()
    }
}
