package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.FuelLocationRationaleDialog
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Suíte de testes automatizados para o diálogo de Rationale Prévia de Localização (T-11 [SEC-07]).
 * Valida a conformidade de UX, acessibilidade, LGPD e disparo correto dos callbacks de confirmação e cancelamento.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class FuelLocationRationaleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRationaleDialog_displaysAllExpectedTextsAndAccessibilityTags() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val expectedTitle = app.getString(R.string.fuel_location_rationale_title)
        val expectedMessage = app.getString(R.string.fuel_location_rationale_message)
        val expectedConfirm = app.getString(R.string.fuel_location_rationale_confirm)
        val expectedDismiss = app.getString(R.string.fuel_location_rationale_dismiss)

        composeTestRule.setContent {
            MyApplicationTheme {
                FuelLocationRationaleDialog(
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // Verifica a exibição do diálogo pela tag semântica
        composeTestRule.onNodeWithTag("fuel_location_rationale_dialog").assertIsDisplayed()

        // Verifica os textos obrigatórios
        composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedConfirm).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedDismiss).assertIsDisplayed()

        // Verifica as tags dos botões de ação
        composeTestRule.onNodeWithTag("confirm_location_rationale_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dismiss_location_rationale_btn").assertIsDisplayed()
    }

    @Test
    fun testRationaleDialog_confirmButtonClicked_triggersOnConfirm() {
        var confirmCalled = 0
        var dismissCalled = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                FuelLocationRationaleDialog(
                    onConfirm = { confirmCalled++ },
                    onDismiss = { dismissCalled++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("confirm_location_rationale_btn").performClick()

        assertEquals("onConfirm deve ser disparado exatamente uma vez ao clicar em Confirmar", 1, confirmCalled)
        assertEquals("onDismiss não deve ser chamado ao confirmar", 0, dismissCalled)
    }

    @Test
    fun testRationaleDialog_dismissButtonClicked_triggersOnDismiss() {
        var confirmCalled = 0
        var dismissCalled = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                FuelLocationRationaleDialog(
                    onConfirm = { confirmCalled++ },
                    onDismiss = { dismissCalled++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("dismiss_location_rationale_btn").performClick()

        assertEquals("onDismiss deve ser disparado exatamente uma vez ao clicar em Dispensar", 1, dismissCalled)
        assertEquals("onConfirm não deve ser chamado ao dispensar", 0, confirmCalled)
    }

    @Test
    fun testFuelLocationStrings_contentAndIntegrityCheck() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val title = app.getString(R.string.fuel_location_rationale_title)
        val message = app.getString(R.string.fuel_location_rationale_message)
        val confirm = app.getString(R.string.fuel_location_rationale_confirm)
        val dismiss = app.getString(R.string.fuel_location_rationale_dismiss)
        val denied = app.getString(R.string.fuel_location_permission_denied)

        assertTrue("Título não pode estar vazio", title.isNotBlank())
        assertTrue("Mensagem deve esclarecer uso exclusivo para postos próximos", message.contains("postos de combustível mais próximos"))
        assertTrue("Mensagem deve esclarecer cálculo de menor custo", message.contains("menor custo"))
        assertTrue("Mensagem deve garantir não compartilhamento externo (LGPD)", message.contains("não são compartilhados"))
        assertTrue("Botão de confirmação deve ser claro", confirm.isNotBlank())
        assertTrue("Botão de dispensa deve ser claro", dismiss.isNotBlank())
        assertTrue("Mensagem de negação deve orientar o usuário", denied.contains("localização necessária"))
    }
}
