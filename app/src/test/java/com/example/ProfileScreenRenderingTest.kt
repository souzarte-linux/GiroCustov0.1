package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GiroCustoDatabase
import com.example.data.GiroCustoRepository
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class ProfileScreenRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        GiroCustoDatabase.setTestDatabase(db)
        repository = GiroCustoRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        GiroCustoDatabase.resetForTesting()
    }

    @Test
    fun testProfileScreen_rendersVehiclesAndPartsWithoutCrash() {
        // Insere veículo ativo e peça com valores monetários em centavos (Long)
        val vehicle = Vehicle(
            id = 1L,
            model = "CG 160",
            brand = "Honda",
            averageConsumption = 35.0,
            monthlyFixedCosts = 18000L, // R$ 180,00 em centavos
            plannedWorkDays = 22,
            currentOdometer = 12000.0,
            active = true
        )
        val part = VehiclePart(
            id = 1L,
            vehicleId = 1L,
            name = "Óleo do Motor",
            price = 4500L, // R$ 45,00 em centavos
            lifespanKm = 2000.0,
            runKmSinceChange = 500.0
        )
        runBlocking {
            db.vehicleDao().insertVehicle(vehicle)
            db.vehiclePartDao().insertPart(part)
        }

        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                ProfileScreen(viewModel = viewModel)
            }
        }

        // Deve renderizar sem lançar IllegalFormatConversionException e exibir os textos corretos
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Honda CG 160", substring = true).assertExists()
        composeTestRule.onNodeWithText("R$ 180,00/mês", substring = true).assertExists()
        composeTestRule.onNodeWithText("Óleo do Motor", substring = true).assertExists()
        composeTestRule.onNodeWithText("Troca: R$ 45,00", substring = true).assertExists()
    }
}
