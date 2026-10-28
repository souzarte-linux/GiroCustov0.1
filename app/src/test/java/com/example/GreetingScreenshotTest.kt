package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.screens.GoalCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Teste de regressão visual com Roborazzi sobre componente de produção real (T-17 [TST-01]).
 * Renderiza o GoalCard envolvido no MyApplicationTheme com valores representativos de metas diárias.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun goal_card_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        GoalCard(
          title = "Metas Diárias",
          grossAct = 250.0,
          grossGoal = 300.0,
          netAct = 180.0,
          netGoal = 200.0,
          kmAct = 120.0,
          kmGoal = 150.0,
          delAct = 15,
          delGoal = 20
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/goal_card.png")
  }
}
