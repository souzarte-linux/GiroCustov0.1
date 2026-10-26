package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.network.fipe.*
import com.example.network.supabase.*
import com.example.ui.FipeUiState
import com.example.ui.GiroCustoViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FipeNetworkAndViewModelTest {

    private lateinit var testDispatcher: kotlinx.coroutines.test.TestDispatcher
    private lateinit var context: Context
    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var viewModel: GiroCustoViewModel

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        db = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        GiroCustoDatabase.setTestDatabase(db)
        repository = GiroCustoRepository(db)

        supabaseClient = SupabaseClient.resetForTesting(context)
        supabaseClient.setAuthSession(null)
        FipeNetworkClient.setInstance(null)

        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GiroCustoViewModel(app)
    }

    @After
    fun tearDown() {
        FipeNetworkClient.setInstance(null)
        supabaseClient.setAuthSession(null)
        db.close()
    }

    // =========================================================================
    // 1. TESTES DOS MODELOS E SERIALIZAÇÃO FIPE
    // =========================================================================

    @Test
    fun testFipeVehicleType_mapping() {
        assertEquals(FipeVehicleType.CARS, FipeVehicleType.fromString("carros"))
        assertEquals(FipeVehicleType.CARS, FipeVehicleType.fromString("CARRO"))
        assertEquals(FipeVehicleType.CARS, FipeVehicleType.fromString("car"))
        assertEquals(FipeVehicleType.MOTORCYCLES, FipeVehicleType.fromString("motos"))
        assertEquals(FipeVehicleType.MOTORCYCLES, FipeVehicleType.fromString("MOTO"))
        assertEquals(FipeVehicleType.MOTORCYCLES, FipeVehicleType.fromString("motocicleta"))
        assertEquals(FipeVehicleType.MOTORCYCLES, FipeVehicleType.fromString("motorcycle"))
        assertEquals(FipeVehicleType.TRUCKS, FipeVehicleType.fromString("caminhoes"))
        assertEquals(FipeVehicleType.TRUCKS, FipeVehicleType.fromString("caminhão"))
        assertEquals(FipeVehicleType.TRUCKS, FipeVehicleType.fromString("truck"))
        assertEquals(FipeVehicleType.CARS, FipeVehicleType.fromString("outro"))
    }

    @Test
    fun testFipeItemDto_codigoFlexivel() {
        val itemString = FipeItemDto(nome = "Honda", codigoRaw = "59")
        assertEquals("59", itemString.codigo)

        val itemInt = FipeItemDto(nome = "Honda", codigoRaw = 59)
        assertEquals("59", itemInt.codigo)

        val itemLong = FipeItemDto(nome = "Honda", codigoRaw = 59L)
        assertEquals("59", itemLong.codigo)

        val itemDouble = FipeItemDto(nome = "Honda", codigoRaw = 59.0)
        assertEquals("59", itemDouble.codigo)

        val itemNull = FipeItemDto(nome = "Honda", codigoRaw = null)
        assertEquals("", itemNull.codigo)
    }

    @Test
    fun testFipeItemDto_moshiDeserialization_stringAndNumber() {
        val jsonString = """{"nome": "Honda", "codigo": "59"}"""
        val adapter = moshi.adapter(FipeItemDto::class.java)
        val dto1 = adapter.fromJson(jsonString)
        assertNotNull(dto1)
        assertEquals("Honda", dto1?.nome)
        assertEquals("59", dto1?.codigo)

        val jsonNumber = """{"nome": "Integra GS 1.8", "codigo": 1234}"""
        val dto2 = adapter.fromJson(jsonNumber)
        assertNotNull(dto2)
        assertEquals("Integra GS 1.8", dto2?.nome)
        assertEquals("1234", dto2?.codigo)
    }

    @Test
    fun testFipeModelsResponseDto_moshiDeserialization() {
        val json = """
            {
                "modelos": [
                    {"nome": "Civic Sedan", "codigo": 1},
                    {"nome": "Fit EX", "codigo": "2"}
                ],
                "anos": [
                    {"nome": "2020 Gasolina", "codigo": "2020-1"}
                ]
            }
        """.trimIndent()
        val adapter = moshi.adapter(FipeModelsResponseDto::class.java)
        val response = adapter.fromJson(json)
        assertNotNull(response)
        assertEquals(2, response?.modelos?.size)
        assertEquals("Civic Sedan", response?.modelos?.get(0)?.nome)
        assertEquals("1", response?.modelos?.get(0)?.codigo)
        assertEquals("Fit EX", response?.modelos?.get(1)?.nome)
        assertEquals("2", response?.modelos?.get(1)?.codigo)
        assertEquals(1, response?.anos?.size)
        assertEquals("2020-1", response?.anos?.get(0)?.codigo)
    }

    @Test
    fun testFipeDetailDto_propertiesAndValues() {
        val json = """
            {
                "TipoVeiculo": 1,
                "Valor": "R$ 85.500,00",
                "Marca": "Honda",
                "Modelo": "Civic Sedan LX 2.0 Flex 16V Aut. 4p",
                "AnoModelo": 2018,
                "Combustivel": "Gasolina",
                "CodigoFipe": "014091-0",
                "MesReferencia": "outubro de 2026",
                "SiglaCombustivel": "G"
            }
        """.trimIndent()
        val adapter = moshi.adapter(FipeDetailDto::class.java)
        val detail = adapter.fromJson(json)
        assertNotNull(detail)
        assertEquals("Honda", detail?.marca)
        assertEquals("Civic Sedan LX 2.0 Flex 16V Aut. 4p", detail?.modelo)
        assertEquals(2018, detail?.anoModelo)
        assertEquals(1, detail?.tipoVeiculo)
        assertEquals(85500.0, detail?.valorNumerico ?: 0.0, 0.001)
    }

    // =========================================================================
    // 2. TESTES DE VIEWMODEL E CLIENTE DE REDE (FIPE)
    // =========================================================================

    @Test
    fun testLoadFipeMarcas_success() = runTest(testDispatcher) {
        val fakeMarcas = listOf(
            FipeItemDto(nome = "Honda", codigoRaw = "59"),
            FipeItemDto(nome = "Yamaha", codigoRaw = "60")
        )

        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> {
                assertEquals("motos", tipo)
                return Response.success(fakeMarcas)
            }
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        })

        viewModel.loadFipeMarcas("motos")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Success)
        val marcas = viewModel.fipeMarcas.value
        assertEquals(2, marcas.size)
        assertEquals("Honda", marcas[0].nome)
        assertEquals("59", marcas[0].codigo)
    }

    @Test
    fun testLoadFipeMarcas_timeoutException() = runTest(testDispatcher) {
        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> {
                throw SocketTimeoutException("Timed out")
            }
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        })

        viewModel.loadFipeMarcas("carros")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Error)
        assertEquals("Tempo limite excedido ao consultar a Tabela FIPE. Verifique sua conexão.", (state as FipeUiState.Error).message)
        assertTrue(viewModel.fipeMarcas.value.isEmpty())
    }

    @Test
    fun testLoadFipeMarcas_ioException_offline() = runTest(testDispatcher) {
        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> {
                throw IOException("No internet connection")
            }
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        })

        viewModel.loadFipeMarcas("carros")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Error)
        assertEquals("Sem conexão com a internet para consultar a Tabela FIPE.", (state as FipeUiState.Error).message)
    }

    @Test
    fun testLoadFipeMarcas_httpError500() = runTest(testDispatcher) {
        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> {
                val errorBody = "Internal Server Error".toResponseBody("text/plain".toMediaTypeOrNull())
                return Response.error(500, errorBody)
            }
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        })

        viewModel.loadFipeMarcas("carros")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Error)
        assertEquals("Não foi possível carregar as marcas da Tabela FIPE (500).", (state as FipeUiState.Error).message)
    }

    @Test
    fun testLoadFipeModelos_success() = runTest(testDispatcher) {
        val fakeModelsResponse = FipeModelsResponseDto(
            modelos = listOf(
                FipeItemDto(nome = "CG 160 Titan", codigoRaw = 101),
                FipeItemDto(nome = "CB 500F", codigoRaw = 102)
            ),
            anos = listOf(
                FipeItemDto(nome = "2022 Gasolina", codigoRaw = "2022-1")
            )
        )

        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> {
                assertEquals("motos", tipo)
                assertEquals("59", marcaId)
                return Response.success(fakeModelsResponse)
            }
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        })

        viewModel.loadFipeModelos("motos", "59")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Success)
        val modelos = viewModel.fipeModelos.value
        assertEquals(2, modelos.size)
        assertEquals("CG 160 Titan", modelos[0].nome)
        assertEquals("101", modelos[0].codigo)
    }

    @Test
    fun testLoadFipeModelos_blankBrand_doesNotCallNetwork() = runTest(testDispatcher) {
        var networkCalled = false
        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> {
                networkCalled = true
                return Response.success(FipeModelsResponseDto())
            }
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        })

        viewModel.loadFipeModelos("carros", "  ")
        assertFalse(networkCalled)
        assertTrue(viewModel.fipeModelos.value.isEmpty())
    }

    @Test
    fun testLoadFipeAnos_success() = runTest(testDispatcher) {
        val fakeAnos = listOf(
            FipeItemDto(nome = "2021 Gasolina", codigoRaw = "2021-1"),
            FipeItemDto(nome = "2022 Gasolina", codigoRaw = "2022-1")
        )

        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> {
                assertEquals("carros", tipo)
                assertEquals("59", marcaId)
                assertEquals("101", modeloId)
                return Response.success(fakeAnos)
            }
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        })

        viewModel.loadFipeAnos("carros", "59", "101")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Success)
        val anos = viewModel.fipeAnos.value
        assertEquals(2, anos.size)
        assertEquals("2021 Gasolina", anos[0].nome)
        assertEquals("2021-1", anos[0].codigo)
    }

    @Test
    fun testLoadFipeDetail_success() = runTest(testDispatcher) {
        val fakeDetail = FipeDetailDto(
            marca = "Honda",
            modelo = "Civic Sedan",
            valor = "R$ 90.000,00",
            anoModeloRaw = 2021,
            combustivel = "Gasolina",
            codigoFipe = "014091-0"
        )

        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> {
                assertEquals("carros", tipo)
                assertEquals("59", marcaId)
                assertEquals("101", modeloId)
                assertEquals("2021-1", anoId)
                return Response.success(fakeDetail)
            }
        })

        viewModel.loadFipeDetail("carros", "59", "101", "2021-1")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Success)
        val detail = viewModel.fipeDetail.value
        assertNotNull(detail)
        assertEquals("Honda", detail?.marca)
        assertEquals("Civic Sedan", detail?.modelo)
        assertEquals(90000.0, detail?.valorNumerico ?: 0.0, 0.001)
    }

    @Test
    fun testClearFipe_resetsAllStates() = runTest(testDispatcher) {
        val fakeMarcas = listOf(FipeItemDto(nome = "Honda", codigoRaw = "59"))
        FipeNetworkClient.setApi(object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> = Response.success(fakeMarcas)
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        })

        viewModel.loadFipeMarcas("motos")
        assertTrue(viewModel.fipeMarcas.value.isNotEmpty())

        viewModel.clearFipe()
        assertTrue(viewModel.fipeUiState.value is FipeUiState.Idle)
        assertTrue(viewModel.fipeMarcas.value.isEmpty())
        assertTrue(viewModel.fipeModelos.value.isEmpty())
        assertTrue(viewModel.fipeAnos.value.isEmpty())
        assertNull(viewModel.fipeDetail.value)
    }

    // =========================================================================
    // 3. TESTES DE PERSISTÊNCIA DE USERPROFILE COM CAMPOS DE ENDEREÇO E SYNC
    // =========================================================================

    @Test
    fun testUpdateUserProfile_persistsAllAddressFieldsAndMarksPendingUpdate() = runTest(testDispatcher) {
        viewModel.updateUserProfile(
            name = "Carlos Santana",
            phone = "11977778888",
            city = "Osasco",
            email = "carlos@girocusto.com",
            isWhatsapp = true,
            cep = "06010-000",
            street = "Rua Antônio Agu",
            addressNumber = "450",
            neighborhood = "Centro",
            stateUf = "SP",
            onboardingCompleted = true
        )

        val profile = repository.getUserProfile()
        assertNotNull(profile)
        assertEquals("Carlos Santana", profile?.name)
        assertEquals("11977778888", profile?.phone)
        assertEquals("Osasco", profile?.city)
        assertEquals("carlos@girocusto.com", profile?.email)
        assertEquals(true, profile?.isWhatsapp)
        assertEquals("06010-000", profile?.cep)
        assertEquals("Rua Antônio Agu", profile?.street)
        assertEquals("450", profile?.addressNumber)
        assertEquals("Centro", profile?.neighborhood)
        assertEquals("SP", profile?.stateUf)
        assertEquals(true, profile?.onboardingCompleted)
        assertEquals(SyncStatus.PENDING_UPDATE, profile?.syncStatus)
        assertTrue((profile?.updatedAt ?: 0L) > 0L)
    }

    @Test
    fun testSaveUserProfile_persistsAllFieldsAndMarksPendingUpdate() = runTest(testDispatcher) {
        val userProfile = UserProfile(
            name = "Maria Souza",
            phone = "21988887777",
            email = "maria@girocusto.com",
            isWhatsapp = false,
            cep = "20040-002",
            street = "Avenida Rio Branco",
            addressNumber = "156",
            neighborhood = "Centro",
            city = "Rio de Janeiro",
            stateUf = "RJ",
            onboardingCompleted = true
        )

        viewModel.saveUserProfile(userProfile)

        val saved = repository.getUserProfile()
        assertNotNull(saved)
        assertEquals("Maria Souza", saved?.name)
        assertEquals("21988887777", saved?.phone)
        assertEquals("maria@girocusto.com", saved?.email)
        assertEquals(false, saved?.isWhatsapp)
        assertEquals("20040-002", saved?.cep)
        assertEquals("Avenida Rio Branco", saved?.street)
        assertEquals("156", saved?.addressNumber)
        assertEquals("Centro", saved?.neighborhood)
        assertEquals("Rio de Janeiro", saved?.city)
        assertEquals("RJ", saved?.stateUf)
        assertEquals(SyncStatus.PENDING_UPDATE, saved?.syncStatus)
    }
}
