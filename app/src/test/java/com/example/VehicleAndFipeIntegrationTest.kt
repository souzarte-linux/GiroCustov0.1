package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.network.fipe.*
import com.example.network.supabase.SupabaseClient
import com.example.ui.FipeUiState
import com.example.ui.GiroCustoViewModel
import com.example.ui.navigation.DrawerDestination
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
import java.net.UnknownHostException

/**
 * Suíte de testes unitários e de integração dedicada cobrindo:
 * 1. Parsing da API FIPE com Moshi (marcas, modelos com código numérico e string, anos, edge cases)
 * 2. Tratamento de fallback offline e cenários de erro da API FIPE (timeout, offline, erro HTTP 500/404)
 * 3. Persistência de múltiplos veículos no GiroCustoRepository / Room (motos baú/bag, carros sedan/hatch/utilitário)
 *    e alternância de veículo ativo (setActiveVehicle) garantindo exclusividade de active=true
 * 4. Cálculos de saúde, custo por km e desgaste de peças (VehiclePart) incluindo resetPartWear
 * 5. Integridade do enum DrawerDestination (ausência de VEICULO e presença dos 5 destinos válidos)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VehicleAndFipeIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var viewModel: GiroCustoViewModel

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Before
    fun setUp() {
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
        FipeNetworkClient.testApi = null

        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GiroCustoViewModel(app)
    }

    @After
    fun tearDown() {
        FipeNetworkClient.testApi = null
        supabaseClient.setAuthSession(null)
        db.close()
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. PARSING DA API FIPE COM MOSHI (DTOs com String e Número)
    // =========================================================================

    @Test
    fun testFipeItemDto_parsingMarcas_stringCode() {
        val json = """
            [
                {"nome": "Acura", "codigo": "1"},
                {"nome": "Agrale", "codigo": "2"},
                {"nome": "Honda", "codigo": "59"}
            ]
        """.trimIndent()

        val adapter = moshi.adapter<List<FipeItemDto>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, FipeItemDto::class.java)
        )
        val marcas = adapter.fromJson(json)

        assertNotNull(marcas)
        assertEquals(3, marcas?.size)
        assertEquals("Acura", marcas?.get(0)?.nome)
        assertEquals("1", marcas?.get(0)?.codigo)
        assertEquals("Agrale", marcas?.get(1)?.nome)
        assertEquals("2", marcas?.get(1)?.codigo)
        assertEquals("Honda", marcas?.get(2)?.nome)
        assertEquals("59", marcas?.get(2)?.codigo)
    }

    @Test
    fun testFipeModelsResponseDto_parsingModelos_mixedNumericAndStringCodes() {
        // A API da FIPE às vezes devolve 'codigo' como número inteiro (ex: 1234) e outras vezes como String ("5678")
        val json = """
            {
                "modelos": [
                    {"nome": "Civic Sedan EX 2.0 Flex 16V Aut. 4p", "codigo": 105},
                    {"nome": "City Sedan LX 1.5 Flex 16V 4p", "codigo": "106"},
                    {"nome": "CG 160 Titan Flex", "codigo": 8920},
                    {"nome": "Biz 125 Flex", "codigo": "8921"}
                ],
                "anos": [
                    {"nome": "2023 Gasolina", "codigo": "2023-1"},
                    {"nome": "2022 Gasolina", "codigo": "2022-1"},
                    {"nome": "32000 Gasolina", "codigo": "32000-1"}
                ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(FipeModelsResponseDto::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        val modelos = response!!.modelos
        val anos = response.anos

        assertEquals(4, modelos.size)
        assertEquals("Civic Sedan EX 2.0 Flex 16V Aut. 4p", modelos[0].nome)
        assertEquals("105", modelos[0].codigo)

        assertEquals("City Sedan LX 1.5 Flex 16V 4p", modelos[1].nome)
        assertEquals("106", modelos[1].codigo)

        assertEquals("CG 160 Titan Flex", modelos[2].nome)
        assertEquals("8920", modelos[2].codigo)

        assertEquals("Biz 125 Flex", modelos[3].nome)
        assertEquals("8921", modelos[3].codigo)

        assertEquals(3, anos.size)
        assertEquals("2023 Gasolina", anos[0].nome)
        assertEquals("2023-1", anos[0].codigo)
        assertEquals("2022 Gasolina", anos[1].nome)
        assertEquals("2022-1", anos[1].codigo)
    }

    @Test
    fun testFipeItemDto_edgeCasesInCodigoRaw() {
        // Codigo Double inteiro
        val itemDoubleInt = FipeItemDto(nome = "Teste", codigoRaw = 105.0)
        assertEquals("105", itemDoubleInt.codigo)

        // Codigo Double fracionado
        val itemDoubleFrac = FipeItemDto(nome = "Teste", codigoRaw = 105.5)
        assertEquals("105.5", itemDoubleFrac.codigo)

        // Codigo String com espacos
        val itemTrim = FipeItemDto(nome = "Teste", codigoRaw = "  42  ")
        assertEquals("42", itemTrim.codigo)

        // Codigo nulo
        val itemNull = FipeItemDto(nome = "Teste", codigoRaw = null)
        assertEquals("", itemNull.codigo)
    }

    @Test
    fun testFipeDetailDto_parsingAndMonetaryConversion() {
        val json = """
            {
                "TipoVeiculo": 1,
                "Valor": "R$ 115.450,00",
                "Marca": "Toyota",
                "Modelo": "Corolla Altis 2.0 Flex 16V Aut.",
                "AnoModelo": 2021,
                "Combustivel": "Gasolina",
                "CodigoFipe": "002130-0",
                "MesReferencia": "outubro de 2026",
                "SiglaCombustivel": "G"
            }
        """.trimIndent()

        val adapter = moshi.adapter(FipeDetailDto::class.java)
        val detail = adapter.fromJson(json)

        assertNotNull(detail)
        assertEquals(1, detail?.tipoVeiculo)
        assertEquals("Toyota", detail?.marca)
        assertEquals("Corolla Altis 2.0 Flex 16V Aut.", detail?.modelo)
        assertEquals(2021, detail?.anoModelo)
        assertEquals("Gasolina", detail?.combustivel)
        assertEquals("002130-0", detail?.codigoFipe)
        assertEquals("outubro de 2026", detail?.mesReferencia)
        assertEquals(115450.0, detail?.valorNumerico ?: 0.0, 0.001)

        // Testes adicionais de conversão de moeda
        val motoDetail = FipeDetailDto(valor = "R$ 18.250,50", anoModeloRaw = "2023", tipoVeiculoRaw = "2")
        assertEquals(18250.50, motoDetail.valorNumerico, 0.001)
        assertEquals(2023, motoDetail.anoModelo)
        assertEquals(2, motoDetail.tipoVeiculo)

        val invalidDetail = FipeDetailDto(valor = "Não cotado", anoModeloRaw = null, tipoVeiculoRaw = null)
        assertEquals(0.0, invalidDetail.valorNumerico, 0.001)
        assertEquals(0, invalidDetail.anoModelo)
        assertEquals(0, invalidDetail.tipoVeiculo)
    }

    // =========================================================================
    // 2. TRATAMENTO DE FALLBACK OFFLINE E DE ERRO (TIMEOUT, ERRO 500, IO)
    // =========================================================================

    @Test
    fun testFipeApi_fallbackTimeoutException() = runTest(testDispatcher) {
        FipeNetworkClient.testApi = object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> {
                throw SocketTimeoutException("Connection timed out after 15000ms")
            }
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        }

        viewModel.loadFipeMarcas("motos")

        val state = viewModel.fipeUiState.value
        assertTrue("Deveria ser estado de Erro", state is FipeUiState.Error)
        val errorMessage = (state as FipeUiState.Error).message
        assertEquals("Tempo limite excedido ao consultar a Tabela FIPE. Verifique sua conexão.", errorMessage)
        assertTrue(viewModel.fipeMarcas.value.isEmpty())
    }

    @Test
    fun testFipeApi_fallbackOfflineIOException() = runTest(testDispatcher) {
        FipeNetworkClient.testApi = object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> {
                throw UnknownHostException("Unable to resolve host parallelum.com.br")
            }
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        }

        viewModel.loadFipeMarcas("carros")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Error)
        assertEquals("Sem conexão com a internet para consultar a Tabela FIPE.", (state as FipeUiState.Error).message)
    }

    @Test
    fun testFipeApi_fallbackHttp500InternalServerError() = runTest(testDispatcher) {
        FipeNetworkClient.testApi = object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> {
                val errorBody = "{\"error\": \"Internal Server Error\"}".toResponseBody("application/json".toMediaTypeOrNull())
                return Response.error(500, errorBody)
            }
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        }

        viewModel.loadFipeModelos("carros", "59")

        val state = viewModel.fipeUiState.value
        assertTrue(state is FipeUiState.Error)
        assertEquals("Não foi possível carregar os modelos da Tabela FIPE (500).", (state as FipeUiState.Error).message)
        assertTrue(viewModel.fipeModelos.value.isEmpty())
    }

    @Test
    fun testFipeApi_recoveryAfterErrorToSuccess() = runTest(testDispatcher) {
        var callCount = 0
        val fakeMarcas = listOf(FipeItemDto(nome = "Yamaha", codigoRaw = "60"))

        FipeNetworkClient.testApi = object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> {
                callCount++
                if (callCount == 1) {
                    throw IOException("Network fail")
                }
                return Response.success(fakeMarcas)
            }
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        }

        // Primeira chamada falha
        viewModel.loadFipeMarcas("motos")
        assertTrue(viewModel.fipeUiState.value is FipeUiState.Error)

        // Segunda chamada sucede e recupera estado
        viewModel.loadFipeMarcas("motos")
        assertTrue(viewModel.fipeUiState.value is FipeUiState.Success)
        assertEquals(1, viewModel.fipeMarcas.value.size)
        assertEquals("Yamaha", viewModel.fipeMarcas.value[0].nome)
    }

    // =========================================================================
    // 3. PERSISTÊNCIA E ALTERNÂNCIA DE MÚLTIPLOS VEÍCULOS NO REPOSITORY / ROOM
    // =========================================================================

    @Test
    fun testMultiVehicle_persistenceOfMotorcyclesAndCars() = runTest(testDispatcher) {
        // 1. Cadastrar Moto com baú e bag
        val moto1 = Vehicle(
            brand = "Honda",
            model = "CG 160 Fan Flex",
            vehicleType = "MOTO",
            manufacturingYear = "2022",
            averageConsumption = 41.5,
            fuelType = "Gasolina",
            monthlyFixedCosts = 190.0,
            plannedWorkDays = 24,
            tankCapacity = 16.1,
            hasTopBox = true,
            hasDeliveryBag = true,
            carBodyType = "",
            active = true
        )
        val moto1Id = repository.addVehicle(moto1)
        assertTrue(moto1Id > 0)

        // 2. Cadastrar Moto apenas com baú
        val moto2 = Vehicle(
            brand = "Yamaha",
            model = "Fazer FZ25",
            vehicleType = "MOTO",
            manufacturingYear = "2023",
            averageConsumption = 32.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 240.0,
            plannedWorkDays = 22,
            tankCapacity = 14.0,
            hasTopBox = true,
            hasDeliveryBag = false,
            carBodyType = "",
            active = false
        )
        val moto2Id = repository.addVehicle(moto2)
        assertTrue(moto2Id > 0)

        // 3. Cadastrar Carro Sedan
        val carroSedan = Vehicle(
            brand = "Toyota",
            model = "Corolla GLi",
            vehicleType = "CARRO",
            manufacturingYear = "2020",
            averageConsumption = 12.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 450.0,
            plannedWorkDays = 20,
            tankCapacity = 50.0,
            hasTopBox = false,
            hasDeliveryBag = false,
            carBodyType = "Sedan",
            active = false
        )
        val carroSedanId = repository.addVehicle(carroSedan)
        assertTrue(carroSedanId > 0)

        // 4. Cadastrar Carro Hatch
        val carroHatch = Vehicle(
            brand = "Volkswagen",
            model = "Gol 1.0 MPI",
            vehicleType = "CARRO",
            manufacturingYear = "2019",
            averageConsumption = 13.5,
            fuelType = "Etanol",
            monthlyFixedCosts = 320.0,
            plannedWorkDays = 22,
            tankCapacity = 55.0,
            hasTopBox = false,
            hasDeliveryBag = false,
            carBodyType = "Hatch",
            active = false
        )
        val carroHatchId = repository.addVehicle(carroHatch)
        assertTrue(carroHatchId > 0)

        // 5. Cadastrar Utilitário
        val carroUtilitario = Vehicle(
            brand = "Fiat",
            model = "Fiorino 1.4 EVO",
            vehicleType = "CARRO",
            manufacturingYear = "2021",
            averageConsumption = 11.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 380.0,
            plannedWorkDays = 25,
            tankCapacity = 55.0,
            hasTopBox = false,
            hasDeliveryBag = false,
            carBodyType = "Utilitário",
            active = false
        )
        val utilitarioId = repository.addVehicle(carroUtilitario)
        assertTrue(utilitarioId > 0)

        // Validação da lista completa persistida no Room
        val allVehicles = repository.allVehiclesFlow.first()
        assertEquals(5, allVehicles.size)

        // Verifica moto1
        val savedMoto1 = allVehicles.first { it.id == moto1Id }
        assertEquals("Honda", savedMoto1.brand)
        assertEquals("CG 160 Fan Flex", savedMoto1.model)
        assertEquals("MOTO", savedMoto1.vehicleType)
        assertTrue("Moto 1 deve ter baú", savedMoto1.hasTopBox)
        assertTrue("Moto 1 deve ter delivery bag", savedMoto1.hasDeliveryBag)
        assertEquals("", savedMoto1.carBodyType)
        assertTrue("Moto 1 começou ativa", savedMoto1.active)

        // Verifica moto2
        val savedMoto2 = allVehicles.first { it.id == moto2Id }
        assertTrue("Moto 2 deve ter baú", savedMoto2.hasTopBox)
        assertFalse("Moto 2 não deve ter delivery bag", savedMoto2.hasDeliveryBag)

        // Verifica carro sedan
        val savedSedan = allVehicles.first { it.id == carroSedanId }
        assertEquals("CARRO", savedSedan.vehicleType)
        assertEquals("Sedan", savedSedan.carBodyType)
        assertFalse(savedSedan.hasTopBox)
        assertFalse(savedSedan.hasDeliveryBag)

        // Verifica carro hatch
        val savedHatch = allVehicles.first { it.id == carroHatchId }
        assertEquals("CARRO", savedHatch.vehicleType)
        assertEquals("Hatch", savedHatch.carBodyType)

        // Verifica utilitário
        val savedUtilitario = allVehicles.first { it.id == utilitarioId }
        assertEquals("CARRO", savedUtilitario.vehicleType)
        assertEquals("Utilitário", savedUtilitario.carBodyType)
    }

    @Test
    fun testMultiVehicle_switchingActiveVehicle_guaranteesExclusivity() = runTest(testDispatcher) {
        // Inserir 3 veículos
        val v1 = Vehicle(model = "Moto Ativa Inicial", active = true)
        val v2 = Vehicle(model = "Carro Sedan Inativo", carBodyType = "Sedan", active = false)
        val v3 = Vehicle(model = "Carro Hatch Inativo", carBodyType = "Hatch", active = false)

        val id1 = repository.addVehicle(v1)
        val id2 = repository.addVehicle(v2)
        val id3 = repository.addVehicle(v3)

        // Validação inicial
        var vehicles = repository.allVehiclesFlow.first()
        assertEquals(1, vehicles.count { it.active })
        assertTrue(vehicles.first { it.id == id1 }.active)
        assertFalse(vehicles.first { it.id == id2 }.active)
        assertFalse(vehicles.first { it.id == id3 }.active)
        assertEquals(id1, repository.getVehicle()?.id)

        // Alternar para o veículo 2 (Carro Sedan)
        repository.setActiveVehicle(id2)
        vehicles = repository.allVehiclesFlow.first()

        assertEquals("Exatamente 1 veículo deve estar ativo", 1, vehicles.count { it.active })
        assertFalse(vehicles.first { it.id == id1 }.active)
        assertTrue(vehicles.first { it.id == id2 }.active)
        assertFalse(vehicles.first { it.id == id3 }.active)
        assertEquals(id2, repository.getVehicle()?.id)

        // Alternar para o veículo 3 (Carro Hatch)
        repository.setActiveVehicle(id3)
        vehicles = repository.allVehiclesFlow.first()

        assertEquals("Exatamente 1 veículo deve estar ativo", 1, vehicles.count { it.active })
        assertFalse(vehicles.first { it.id == id1 }.active)
        assertFalse(vehicles.first { it.id == id2 }.active)
        assertTrue(vehicles.first { it.id == id3 }.active)
        assertEquals(id3, repository.getVehicle()?.id)

        // Alternar de volta para o veículo 1 (Moto) via ViewModel
        viewModel.setActiveVehicle(id1)
        vehicles = repository.allVehiclesFlow.first()

        assertEquals("Exatamente 1 veículo deve estar ativo", 1, vehicles.count { it.active })
        assertTrue(vehicles.first { it.id == id1 }.active)
        assertFalse(vehicles.first { it.id == id2 }.active)
        assertFalse(vehicles.first { it.id == id3 }.active)
        assertEquals(id1, repository.getVehicle()?.id)
    }

    @Test
    fun testMultiVehicle_softDeletePreservesExclusion() = runTest(testDispatcher) {
        val v1 = Vehicle(model = "Veículo 1", active = true)
        val v2 = Vehicle(model = "Veículo 2", active = false)
        val id1 = repository.addVehicle(v1)
        val id2 = repository.addVehicle(v2)

        val v2Saved = repository.allVehiclesFlow.first().first { it.id == id2 }
        repository.deleteVehicle(v2Saved)

        val activeList = repository.allVehiclesFlow.first()
        assertEquals(1, activeList.size)
        assertEquals(id1, activeList[0].id)
    }

    // =========================================================================
    // 4. CÁLCULOS DE SAÚDE, CUSTO POR KM E DESGASTE DE PEÇAS (VehiclePart)
    // =========================================================================

    @Test
    fun testVehiclePart_healthWearCostCalculations() {
        // 1. Peça zerada (óleo de motor)
        val oleoNovo = VehiclePart(
            name = "Óleo do Motor",
            price = 45.0,
            lifespanKm = 1500.0,
            runKmSinceChange = 0.0
        )
        assertEquals(100.0, oleoNovo.healthPercentage, 0.001)
        assertEquals(0.0, oleoNovo.wearPercentage, 0.001)
        assertEquals(0.03, oleoNovo.wearCostPerKm, 0.0001) // 45 / 1500

        // 2. Peça a meia-vida
        val oleoMeiaVida = oleoNovo.copy(runKmSinceChange = 750.0)
        assertEquals(50.0, oleoMeiaVida.healthPercentage, 0.001)
        assertEquals(50.0, oleoMeiaVida.wearPercentage, 0.001)
        assertEquals(0.03, oleoMeiaVida.wearCostPerKm, 0.0001)

        // 3. Peça no limite da vida útil (100% desgastada)
        val oleoVencido = oleoNovo.copy(runKmSinceChange = 1500.0)
        assertEquals(0.0, oleoVencido.healthPercentage, 0.001)
        assertEquals(100.0, oleoVencido.wearPercentage, 0.001)

        // 4. Peça além do limite (over-run): health não fica negativo, wear não passa de 100%
        val oleoPassado = oleoNovo.copy(runKmSinceChange = 2000.0)
        assertEquals(0.0, oleoPassado.healthPercentage, 0.001)
        assertEquals(100.0, oleoPassado.wearPercentage, 0.001)

        // 5. Peça de longa durabilidade (pneu traseiro)
        val pneu = VehiclePart(
            name = "Pneu Traseiro 100/90-18",
            price = 240.0,
            lifespanKm = 12000.0,
            runKmSinceChange = 3000.0
        )
        assertEquals(75.0, pneu.healthPercentage, 0.001)
        assertEquals(25.0, pneu.wearPercentage, 0.001)
        assertEquals(0.02, pneu.wearCostPerKm, 0.0001) // 240 / 12000

        // 6. Edge case durabilidade zero: evita divisão por zero
        val pecaZero = VehiclePart(
            name = "Peça Sem Lifespan",
            price = 100.0,
            lifespanKm = 0.0,
            runKmSinceChange = 0.0
        )
        assertEquals(0.0, pecaZero.wearCostPerKm, 0.001)
    }

    @Test
    fun testVehiclePart_resetPartWearInRepositoryAndRoom() = runTest(testDispatcher) {
        val part = VehiclePart(
            name = "Kit Relação (Coroa/Pinhão/Corrente)",
            price = 180.0,
            lifespanKm = 15000.0,
            runKmSinceChange = 12500.0,
            syncStatus = SyncStatus.SYNCED
        )
        repository.savePart(part)

        // 1. Peça nova não sincronizada (PENDING_INSERT): ao resetar, preserva PENDING_INSERT
        val partsBefore = repository.allPartsFlow.first()
        assertEquals(1, partsBefore.size)
        val savedPart = partsBefore[0]
        assertEquals(12500.0, savedPart.runKmSinceChange, 0.001)
        assertEquals(16.666, savedPart.healthPercentage, 0.1)
        assertEquals(SyncStatus.PENDING_INSERT, savedPart.syncStatus)

        repository.resetPartWear(savedPart.id)

        var partsAfter = repository.allPartsFlow.first()
        assertEquals(1, partsAfter.size)
        var resetPart = partsAfter[0]
        assertEquals(0.0, resetPart.runKmSinceChange, 0.001)
        assertEquals(100.0, resetPart.healthPercentage, 0.001)
        assertEquals(0.0, resetPart.wearPercentage, 0.001)
        assertEquals("Peça não sincronizada deve manter PENDING_INSERT", SyncStatus.PENDING_INSERT, resetPart.syncStatus)

        // 2. Simular que a peça foi sincronizada com a nuvem (SYNCED) e depois teve mais rodagem
        db.vehiclePartDao().updatePart(resetPart.copy(syncStatus = SyncStatus.SYNCED, runKmSinceChange = 5000.0))
        val syncedPart = repository.allPartsFlow.first()[0]
        assertEquals(SyncStatus.SYNCED, syncedPart.syncStatus)
        assertEquals(5000.0, syncedPart.runKmSinceChange, 0.001)

        // Resetar desgaste da peça já sincronizada: agora deve marcar PENDING_UPDATE
        repository.resetPartWear(syncedPart.id)
        partsAfter = repository.allPartsFlow.first()
        resetPart = partsAfter[0]
        assertEquals(0.0, resetPart.runKmSinceChange, 0.001)
        assertEquals(100.0, resetPart.healthPercentage, 0.001)
        assertEquals(0.0, resetPart.wearPercentage, 0.001)
        assertEquals("Peça já sincronizada deve transitar para PENDING_UPDATE", SyncStatus.PENDING_UPDATE, resetPart.syncStatus)
    }

    @Test
    fun testVehiclePart_resetPartWearViaViewModel() = runTest(testDispatcher) {
        val part = VehiclePart(
            name = "Pastilha de Freio",
            price = 50.0,
            lifespanKm = 5000.0,
            runKmSinceChange = 4500.0
        )
        repository.savePart(part)

        val savedPart = repository.allPartsFlow.first()[0]
        assertEquals(4500.0, savedPart.runKmSinceChange, 0.001)

        // Executar reset via ViewModel
        viewModel.resetPartWear(savedPart.id)

        val updatedPart = repository.allPartsFlow.first()[0]
        assertEquals(0.0, updatedPart.runKmSinceChange, 0.001)
        assertEquals(100.0, updatedPart.healthPercentage, 0.001)
    }

    @Test
    fun testVehiclePartDao_addKmToAllParts() = runTest(testDispatcher) {
        val p1 = VehiclePart(name = "Peça 1", price = 100.0, lifespanKm = 10000.0, runKmSinceChange = 500.0)
        val p2 = VehiclePart(name = "Peça 2", price = 200.0, lifespanKm = 20000.0, runKmSinceChange = 1500.0)
        repository.savePart(p1)
        repository.savePart(p2)

        db.vehiclePartDao().addKmToAllParts(250.0)

        val allParts = repository.allPartsFlow.first()
        assertEquals(750.0, allParts.first { it.name == "Peça 1" }.runKmSinceChange, 0.001)
        assertEquals(1750.0, allParts.first { it.name == "Peça 2" }.runKmSinceChange, 0.001)
    }

    // =========================================================================
    // 5. INTEGRIDADE DA NAVEGAÇÃO E DO ENUM DrawerDestination
    // =========================================================================

    @Test
    fun testDrawerDestination_doesNotContainVeiculo() {
        val destinations = DrawerDestination.values().map { it.name }

        // Deve conter exatamente NONE e os 5 destinos válidos
        val expectedDestinations = listOf("NONE", "PERFIL", "PLATAFORMAS", "ABASTECIMENTOS", "MANUTENCAO", "NUVEM")
        assertEquals(expectedDestinations.sorted(), destinations.sorted())

        // Assegurar explicitamente que VEICULO foi removido do DrawerDestination
        assertFalse("DrawerDestination não deve conter VEICULO", destinations.contains("VEICULO"))

        // Tentativa de deserializar ou buscar VEICULO no enum deve lançar IllegalArgumentException
        try {
            DrawerDestination.valueOf("VEICULO")
            fail("Deveria ter lançado IllegalArgumentException para DrawerDestination.VEICULO")
        } catch (e: IllegalArgumentException) {
            // Sucesso esperado: exceção lançada indicando que constante não existe
            assertNotNull(e.message)
        }
    }

    @Test
    fun testDrawerDestination_containsAllFiveOperationalDestinations() {
        val operationalDestinations = listOf(
            DrawerDestination.PERFIL,
            DrawerDestination.PLATAFORMAS,
            DrawerDestination.ABASTECIMENTOS,
            DrawerDestination.MANUTENCAO,
            DrawerDestination.NUVEM
        )

        assertEquals(5, operationalDestinations.distinct().size)

        // Validação dos nomes literais esperados
        assertEquals("PERFIL", DrawerDestination.PERFIL.name)
        assertEquals("PLATAFORMAS", DrawerDestination.PLATAFORMAS.name)
        assertEquals("ABASTECIMENTOS", DrawerDestination.ABASTECIMENTOS.name)
        assertEquals("MANUTENCAO", DrawerDestination.MANUTENCAO.name)
        assertEquals("NUVEM", DrawerDestination.NUVEM.name)
    }
}
