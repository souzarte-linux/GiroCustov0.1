package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class ProfileCepButtonLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testConsultarCorreiosButton_isRenderedHorizontallyNotVertically() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                ProfileScreen(viewModel = viewModel)
            }
        }

        val button = composeTestRule.onNodeWithTag("profile_search_cep_web_btn")
        button.assertIsDisplayed()

        val bounds = button.getUnclippedBoundsInRoot()

        // Em caso de anomalia de texto vertical esmagado:
        // A largura fica minúscula (< 60dp) e a altura infla para 17 linhas (> 100dp), tornando height > width.
        // No layout corrigido:
        // O botão deve ser horizontal (width > height), com largura confortável (>= 120dp) e altura não inflada (<= 56dp).
        assertTrue(
            "O botão 'Consultar Correios' não pode ser vertical (largura=${bounds.width}, altura=${bounds.height})",
            bounds.width > bounds.height
        )
        assertTrue(
            "O botão 'Consultar Correios' deve ter largura adequada para o texto horizontal (largura=${bounds.width})",
            bounds.width >= 120.dp
        )
        assertTrue(
            "A altura do botão 'Consultar Correios' não pode estar inflada em múltiplas linhas verticais (altura=${bounds.height})",
            bounds.height <= 56.dp
        )
    }
}
