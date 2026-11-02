package com.example

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.GiroCustoViewModel
import com.example.ui.ViaCepUiState
import com.example.ui.screens.LaunchScreen
import com.example.ui.screens.platforms.PlatformFormSection
import com.example.ui.screens.platforms.PlatformListSection
import com.example.ui.screens.profile.CadastralDataCard
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class PlatformsScreenAndSectionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var app: Application
    private lateinit var viewModel: GiroCustoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        GiroCustoDatabase.setTestDatabase(db)
        repository = GiroCustoRepository(db)
        viewModel = GiroCustoViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        GiroCustoDatabase.resetForTesting()
    }

    @Test
    fun testPlatformListSection_switchToggleAndAccessibility() {
        val platform1 = Platform(id = 1L, name = "iFood", active = true)
        val platform2 = Platform(id = 2L, name = "Rappi", active = false)
        var toggledPlatform: Platform? = null
        var toggledActiveValue: Boolean? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                PlatformListSection(
                    platforms = listOf(platform1, platform2),
                    onAddClick = {},
                    onEditClick = {},
                    onToggleActive = { plat, newActive ->
                        toggledPlatform = plat
                        toggledActiveValue = newActive
                    }
                )
            }
        }

        // Verifica switch da plataforma 1 (Ativa)
        val switch1 = composeTestRule.onNodeWithTag("platform_switch_1")
        switch1.assertIsDisplayed()
        val bounds1 = switch1.getUnclippedBoundsInRoot()
        assertTrue("Switch deve respeitar altura mínima de 48dp", bounds1.height >= 47.9.dp)
        assertTrue("Switch deve respeitar largura mínima de 48dp", bounds1.width >= 47.9.dp)

        // Alterna o switch da plataforma 1
        switch1.performClick()
        assertEquals(platform1.id, toggledPlatform?.id)
        assertEquals(false, toggledActiveValue)

        // Alterna o switch da plataforma 2
        val switch2 = composeTestRule.onNodeWithTag("platform_switch_2")
        switch2.performClick()
        assertEquals(platform2.id, toggledPlatform?.id)
        assertEquals(true, toggledActiveValue)
    }

    @Test
    fun testPlatformFormSection_viewModeAndTransitionToEditMode() {
        val platform = Platform(
            id = 10L,
            name = "Loggi",
            active = true,
            paymentModel = "diaria",
            cycle = "semanal",
            paymentDay = "QUA",
            pixKey = "loggi@pix.com"
        )
        var backClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                PlatformFormSection(
                    platform = platform,
                    isEdit = true,
                    onBack = { backClicked = true },
                    onSave = { _ -> },
                    onDelete = { }
                )
            }
        }

        // No modo visualização inicial (isEdit=true e id>0):
        // Top bar exibe os botões de ação (Lápis e Lixeira)
        composeTestRule.onNodeWithTag("platform_form_edit_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("platform_form_delete_btn").assertIsDisplayed()

        // Clicar no botão de lápis para entrar no modo de edição
        composeTestRule.onNodeWithTag("platform_form_edit_btn").performClick()
        composeTestRule.waitForIdle()

        // Agora deve estar no modo de edição
        composeTestRule.onNodeWithText(app.getString(R.string.platforms_form_title_edit)).assertIsDisplayed()
        val saveBtn = composeTestRule.onNodeWithTag("platform_save_btn")
        saveBtn.performScrollTo()
        saveBtn.assertIsDisplayed()

        val cancelBtn = composeTestRule.onNodeWithTag("platform_form_cancel_btn")
        cancelBtn.performScrollTo()
        cancelBtn.assertIsDisplayed()

        // Cancelar deve retornar para o modo de visualização sem invocar onBack
        cancelBtn.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("platform_form_edit_btn").assertIsDisplayed()
        assertFalse(backClicked)
    }

    @Test
    fun testPlatformFormSection_deleteDialogConfirmation() {
        val platform = Platform(id = 5L, name = "Zé Delivery", active = true)
        var deleteConfirmed = false

        composeTestRule.setContent {
            MyApplicationTheme {
                PlatformFormSection(
                    platform = platform,
                    isEdit = true,
                    onBack = {},
                    onSave = {},
                    onDelete = { deleteConfirmed = true }
                )
            }
        }

        // Clica na lixeira para abrir o diálogo de confirmação (visível no modo VIEW e EDIT)
        composeTestRule.onNodeWithTag("platform_form_delete_btn").performClick()
        composeTestRule.waitForIdle()

        // Verifica o diálogo de exclusão
        composeTestRule.onNodeWithText(app.getString(R.string.dialog_delete_platform_title)).assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_confirm_delete_platform_btn").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Callback onDelete deve ser executado após confirmação", deleteConfirmed)
    }

    @Test
    fun testPlatformAccordion_expandAndCollapseSections() {
        val platform = Platform(
            id = 7L,
            name = "Cornershop",
            active = true,
            bankName = "Nubank",
            pixKey = "chave-pix-123"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                PlatformFormSection(
                    platform = platform,
                    isEdit = true,
                    onBack = {},
                    onSave = {},
                    onDelete = {}
                )
            }
        }

        // "Dados Gerais" inicia expandido
        composeTestRule.onNodeWithText(app.getString(R.string.platform_section_general)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Cornershop").assertCountEquals(2) // No top bar e no card geral

        // Clicar no accordion "Dados Bancários" para expandir
        val bankingHeader = composeTestRule.onNodeWithTag("platform_accordion_header_banking")
        bankingHeader.performScrollTo()
        bankingHeader.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nubank").performScrollTo().assertIsDisplayed()

        // Clicar no accordion "Chaves Pix" para expandir
        val pixHeader = composeTestRule.onNodeWithTag("platform_accordion_header_pix")
        pixHeader.performScrollTo()
        pixHeader.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("chave-pix-123").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testLaunchScreen_filtersOnlyActivePlatforms() = runTest {
        // Prepara plataformas: 1 ativa e 1 inativa no repositório
        val activePlatform = Platform(id = 101L, name = "Plataforma Ativa", active = true)
        val inactivePlatform = Platform(id = 102L, name = "Plataforma Inativa", active = false)
        repository.savePlatform(activePlatform)
        repository.savePlatform(inactivePlatform)

        composeTestRule.setContent {
            MyApplicationTheme {
                LaunchScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    parts = emptyList()
                )
            }
        }

        // Abre o dropdown de plataformas em LaunchScreen
        val expandButton = composeTestRule.onNodeWithContentDescription(app.getString(R.string.cd_expand_platform_options))
        expandButton.performClick()
        composeTestRule.waitForIdle()

        // Somente a plataforma ativa deve aparecer nas opções do menu
        composeTestRule.onNodeWithTag("platform_option_Plataforma Ativa").assertIsDisplayed()
        composeTestRule.onNodeWithTag("platform_option_Plataforma Inativa").assertDoesNotExist()
    }

    @Test
    fun testProfileCadastralDataCard_inPlaceViewAndEditMode() {
        val userProfile = UserProfile(
            id = 1,
            name = "Carlos Silva",
            phone = "11987654321",
            city = "São Paulo",
            cep = "01001000",
            street = "Praça da Sé",
            addressNumber = "100",
            neighborhood = "Sé",
            stateUf = "SP"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())
                ) {
                    CadastralDataCard(
                        userProfile = userProfile,
                        viaCepState = ViaCepUiState.Idle,
                        viewModel = viewModel,
                        initialEditing = false
                    )
                }
            }
        }

        // Modo visualização (VIEW) inicial
        composeTestRule.onNodeWithText("Carlos Silva").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_cadastral_edit_btn").assertIsDisplayed()

        // Clica no lápis de editar
        composeTestRule.onNodeWithTag("profile_cadastral_edit_btn").performClick()
        composeTestRule.waitForIdle()

        // Agora em modo EDIT: campos de texto e botões visíveis
        composeTestRule.onNodeWithTag("profile_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_save_cadastrais_btn").performScrollTo().assertIsDisplayed()
        val cancelBtn = composeTestRule.onNodeWithTag("profile_cadastral_cancel_btn")
        cancelBtn.performScrollTo()
        cancelBtn.assertIsDisplayed()

        // Cancelar volta ao modo VIEW
        cancelBtn.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_cadastral_edit_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_save_cadastrais_btn").assertDoesNotExist()
    }

    @Test
    fun testViewModel_restorePlatform() = runTest {
        val platform = Platform(
            id = 55L,
            name = "Rappi Entregas",
            active = true,
            cycle = "mensal"
        )

        // Salva a plataforma
        viewModel.savePlatform(platform)
        var platforms = viewModel.allPlatforms.first()
        assertTrue(platforms.any { it.id == 55L })

        // Exclui a plataforma
        viewModel.deletePlatform(platform)
        platforms = viewModel.allPlatforms.first()
        assertFalse(platforms.any { it.id == 55L })

        // Restaura a plataforma usando restorePlatform (com mesmo ID)
        viewModel.restorePlatform(platform)
        platforms = viewModel.allPlatforms.first()
        assertTrue(platforms.any { it.id == 55L && it.name == "Rappi Entregas" })
    }

    @Test
    fun testDailyRecord_historyPreservedWhenPlatformDeactivatedOrDeleted() = runTest {
        // 1. Cria e salva uma plataforma ativa
        val platform = Platform(
            id = 42L,
            name = "iFood Delivery",
            active = true,
            segment = "delivery",
            paymentModel = "producao"
        )
        viewModel.savePlatform(platform)
        var platforms = viewModel.allPlatforms.first()
        assertTrue(platforms.any { it.id == 42L && it.name == "iFood Delivery" && it.active })

        // 2. Insere um DailyRecord associado a essa plataforma
        val record = DailyRecord(
            id = 100L,
            dateString = "2026-11-02",
            dateTimestamp = 1762118400000L,
            platform = "iFood Delivery",
            grossEarnings = 250.0,
            deliveriesCount = 14,
            startOdometer = 12000.0,
            endOdometer = 12080.0,
            fuelPrice = 5.89
        )
        db.dailyRecordDao().insertRecord(record)

        // Verifica que o DailyRecord foi persistido com a plataforma correta
        val initialRecord = db.dailyRecordDao().getRecordById(100L)
        assertNotNull("DailyRecord inicial deve existir", initialRecord)
        assertEquals("iFood Delivery", initialRecord?.platform)
        assertEquals(25000L, initialRecord?.grossEarnings)
        assertEquals(14, initialRecord?.deliveriesCount)

        // 3. Caso de borda A: Desativação da plataforma (active = false)
        viewModel.savePlatform(platform.copy(active = false))
        platforms = viewModel.allPlatforms.first()
        val deactivatedPlat = platforms.find { it.id == 42L }
        assertNotNull(deactivatedPlat)
        assertFalse("Plataforma deve estar inativa", deactivatedPlat!!.active)

        // Comprova que o DailyRecord no histórico preserva os dados e o nome da plataforma intactos
        val recordAfterDeactivation = db.dailyRecordDao().getRecordById(100L)
        assertNotNull("DailyRecord deve continuar existindo após desativação da plataforma", recordAfterDeactivation)
        assertEquals("iFood Delivery", recordAfterDeactivation?.platform)
        assertEquals(25000L, recordAfterDeactivation?.grossEarnings)
        assertEquals(14, recordAfterDeactivation?.deliveriesCount)
        assertEquals(12000.0, recordAfterDeactivation?.startOdometer ?: 0.0, 0.001)
        assertEquals(12080.0, recordAfterDeactivation?.endOdometer ?: 0.0, 0.001)

        // 4. Caso de borda B: Exclusão lógica (soft delete) da plataforma
        viewModel.deletePlatform(platform)
        platforms = viewModel.allPlatforms.first()
        assertFalse("Plataforma excluída não deve aparecer na lista ativa", platforms.any { it.id == 42L })

        // Comprova que o DailyRecord no histórico continua 100% íntegro
        val recordAfterSoftDelete = db.dailyRecordDao().getRecordById(100L)
        assertNotNull("DailyRecord deve continuar existindo após exclusão da plataforma", recordAfterSoftDelete)
        assertEquals("iFood Delivery", recordAfterSoftDelete?.platform)
        assertEquals(25000L, recordAfterSoftDelete?.grossEarnings)
        assertEquals(14, recordAfterSoftDelete?.deliveriesCount)

        // 5. Caso de borda C: Exclusão física (hard delete) da plataforma da tabela platforms
        db.platformDao().hardDeletePlatform(42L)
        assertNull(db.platformDao().getPlatformById(42L))

        // Comprova que não há cascade delete entre platforms e daily_records
        val recordAfterHardDelete = db.dailyRecordDao().getRecordById(100L)
        assertNotNull("DailyRecord deve manter integridade e histórico intacto mesmo com hard delete da plataforma", recordAfterHardDelete)
        assertEquals("iFood Delivery", recordAfterHardDelete?.platform)
        assertEquals(25000L, recordAfterHardDelete?.grossEarnings)
        assertEquals(14, recordAfterHardDelete?.deliveriesCount)
    }
}
