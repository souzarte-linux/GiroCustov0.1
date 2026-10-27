package com.example

import android.app.Application
import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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
import kotlinx.coroutines.launch
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
import java.io.File
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
 * 6. Vínculo de VehiclePart ao veículo ativo, isolamento de desgaste no DailyRecord e alternância dinâmica no StateFlow
 * 7. Execução e validação da MIGRATION_11_12 (adicionando vehicleId em vehicle_parts e migrando registros legados)
 * 8. Salvamento de UserProfile com CEP em branco / preenchimento manual de endereço
 * 9. Confirmação de ausência de regressões com a remoção de VehicleScreen.kt
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

    // =========================================================================
    // 6. VÍNCULO DE PEÇAS AO VEÍCULO ATIVO, ISOLAMENTO DE WEAR COST E STATEFLOW
    // =========================================================================

    @Test
    fun testVehicleParts_linkedToVehicleId_isolatedPerVehicleInDaoAndRepository() = runTest(testDispatcher) {
        // Cadastrar Moto (id 1, ativa) e Carro (id 2, inativo)
        val moto = Vehicle(
            brand = "Honda",
            model = "CG 160 Fan Flex",
            vehicleType = "MOTO",
            active = true
        )
        val carro = Vehicle(
            brand = "Toyota",
            model = "Corolla GLi",
            vehicleType = "CARRO",
            active = false
        )
        val motoId = repository.addVehicle(moto)
        val carroId = repository.addVehicle(carro)

        // Criar peças vinculadas explicitamente a cada veículo
        val partMoto1 = VehiclePart(
            vehicleId = motoId,
            name = "Kit Relação (Coroa/Pinhão/Corrente)",
            price = 180.0,
            lifespanKm = 15000.0,
            runKmSinceChange = 0.0
        )
        val partMoto2 = VehiclePart(
            vehicleId = motoId,
            name = "Pneu Traseiro Moto",
            price = 220.0,
            lifespanKm = 12000.0,
            runKmSinceChange = 0.0
        )
        val partCarro1 = VehiclePart(
            vehicleId = carroId,
            name = "Óleo Sintético Carro",
            price = 250.0,
            lifespanKm = 10000.0,
            runKmSinceChange = 0.0
        )
        val partCarro2 = VehiclePart(
            vehicleId = carroId,
            name = "Pneu Aro 16 Carro",
            price = 450.0,
            lifespanKm = 40000.0,
            runKmSinceChange = 0.0
        )

        repository.savePart(partMoto1)
        repository.savePart(partMoto2)
        repository.savePart(partCarro1)
        repository.savePart(partCarro2)

        // 1. Validar consulta de peças da Moto no DAO (deve retornar apenas as 2 peças da moto)
        val motoParts = db.vehiclePartDao().getPartsForVehicle(motoId)
        assertEquals(2, motoParts.size)
        assertTrue(motoParts.any { it.name == "Kit Relação (Coroa/Pinhão/Corrente)" })
        assertTrue(motoParts.any { it.name == "Pneu Traseiro Moto" })
        assertFalse(motoParts.any { it.name.contains("Carro") })
        assertTrue(motoParts.all { it.vehicleId == motoId })

        // 2. Validar consulta de peças do Carro no DAO (deve retornar apenas as 2 peças do carro)
        val carroParts = db.vehiclePartDao().getPartsForVehicle(carroId)
        assertEquals(2, carroParts.size)
        assertTrue(carroParts.any { it.name == "Óleo Sintético Carro" })
        assertTrue(carroParts.any { it.name == "Pneu Aro 16 Carro" })
        assertFalse(carroParts.any { it.name.contains("Moto") || it.name.contains("Relação") })
        assertTrue(carroParts.all { it.vehicleId == carroId })

        // 3. Validar Flows reativos por veículo no DAO
        val motoPartsFlow = db.vehiclePartDao().getPartsForVehicleFlow(motoId).first()
        assertEquals(2, motoPartsFlow.size)
        val carroPartsFlow = db.vehiclePartDao().getPartsForVehicleFlow(carroId).first()
        assertEquals(2, carroPartsFlow.size)

        // 4. Testar auto-vinculação do vehicleId quando salvo com vehicleId = 0L (vincula ao ativo)
        // Moto está ativa no momento:
        val partAutoMoto = VehiclePart(
            vehicleId = 0L,
            name = "Vela de Ignição Moto",
            price = 35.0,
            lifespanKm = 12000.0
        )
        repository.savePart(partAutoMoto)

        val updatedMotoParts = db.vehiclePartDao().getPartsForVehicle(motoId)
        assertEquals(3, updatedMotoParts.size)
        assertTrue(updatedMotoParts.any { it.name == "Vela de Ignição Moto" && it.vehicleId == motoId })

        // Alternar para o Carro como ativo e salvar com vehicleId = 0L
        repository.setActiveVehicle(carroId)
        val partAutoCarro = VehiclePart(
            vehicleId = 0L,
            name = "Pastilha Dianteira Carro",
            price = 140.0,
            lifespanKm = 30000.0
        )
        repository.savePart(partAutoCarro)

        val updatedCarroParts = db.vehiclePartDao().getPartsForVehicle(carroId)
        assertEquals(3, updatedCarroParts.size)
        assertTrue(updatedCarroParts.any { it.name == "Pastilha Dianteira Carro" && it.vehicleId == carroId })

        // Assegurar que as peças da moto não foram contaminadas
        val finalMotoParts = db.vehiclePartDao().getPartsForVehicle(motoId)
        assertEquals(3, finalMotoParts.size)
    }

    @Test
    fun testDailyRecord_wearCostCalculation_isolatedByActiveVehicle() = runTest(testDispatcher) {
        // Cadastrar Moto (ativa) e Carro (inativo)
        val moto = Vehicle(
            brand = "Honda",
            model = "CG 160 Fan Flex",
            vehicleType = "MOTO",
            averageConsumption = 40.0,
            monthlyFixedCosts = 100.0,
            plannedWorkDays = 20,
            active = true
        )
        val carro = Vehicle(
            brand = "Toyota",
            model = "Corolla GLi",
            vehicleType = "CARRO",
            averageConsumption = 10.0,
            monthlyFixedCosts = 300.0,
            plannedWorkDays = 20,
            active = false
        )
        val motoId = repository.addVehicle(moto)
        val carroId = repository.addVehicle(carro)

        // Peças da Moto:
        // P1: R$ 150 / 15000 km = 0.010 R$/km
        // P2: R$ 240 / 12000 km = 0.020 R$/km
        // Total desgaste Moto = 0.030 R$/km
        repository.savePart(VehiclePart(vehicleId = motoId, name = "Relação Moto", price = 150.0, lifespanKm = 15000.0, runKmSinceChange = 0.0))
        repository.savePart(VehiclePart(vehicleId = motoId, name = "Pneu Moto", price = 240.0, lifespanKm = 12000.0, runKmSinceChange = 0.0))

        // Peças do Carro:
        // P3: R$ 300 / 10000 km = 0.030 R$/km
        // P4: R$ 1600 / 40000 km = 0.040 R$/km
        // Total desgaste Carro = 0.070 R$/km
        repository.savePart(VehiclePart(vehicleId = carroId, name = "Óleo Carro", price = 300.0, lifespanKm = 10000.0, runKmSinceChange = 0.0))
        repository.savePart(VehiclePart(vehicleId = carroId, name = "Pneus Carro", price = 1600.0, lifespanKm = 40000.0, runKmSinceChange = 0.0))

        // 1. Salvar DailyRecord com a MOTO ATIVA
        // 100 km rodados (1000 a 1100), gasolina a R$ 5,00
        repository.insertDailyRecord(
            dateString = "2026-10-21",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 200.0,
            deliveriesCount = 12,
            startOdometer = 1000.0,
            endOdometer = 1100.0,
            fuelPrice = 5.0,
            foodExpense = 20.0
        )

        val records = repository.allRecordsFlow.first()
        assertEquals(1, records.size)
        val recordMoto = records[0]

        // Cálculo esperado para a moto:
        // kmRodados = 100.0
        // wearCost = 100.0 * 0.030 = 3.00 R$ (se usasse peças do carro, seria 10.00 R$)
        assertEquals(100.0, recordMoto.endOdometer - recordMoto.startOdometer, 0.001)
        assertEquals(3.00, recordMoto.wearCost, 0.001)

        // Verificar que apenas as peças da Moto tiveram runKmSinceChange incrementado
        val motoPartsAfterRecord = db.vehiclePartDao().getPartsForVehicle(motoId)
        assertTrue(motoPartsAfterRecord.all { it.runKmSinceChange == 100.0 })

        val carroPartsAfterMotoRecord = db.vehiclePartDao().getPartsForVehicle(carroId)
        assertTrue("As peças do carro não devem sofrer desgaste com rodagem da moto",
            carroPartsAfterMotoRecord.all { it.runKmSinceChange == 0.0 })

        // 2. Alternar para o CARRO como veículo ativo
        repository.setActiveVehicle(carroId)

        // Salvar DailyRecord com o CARRO ATIVO
        // 50 km rodados (50000 a 50050), gasolina a R$ 5,00
        repository.insertDailyRecord(
            dateString = "2026-10-22",
            dateTimestamp = System.currentTimeMillis() + 86400000L,
            platform = "Uber",
            grossEarnings = 180.0,
            deliveriesCount = 8,
            startOdometer = 50000.0,
            endOdometer = 50050.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        val updatedRecords = repository.allRecordsFlow.first()
        assertEquals(2, updatedRecords.size)
        val recordCarro = updatedRecords.first { it.dateString == "2026-10-22" }

        // Cálculo esperado para o carro:
        // kmRodados = 50.0
        // wearCost = 50.0 * 0.070 = 3.50 R$
        assertEquals(50.0, recordCarro.endOdometer - recordCarro.startOdometer, 0.001)
        assertEquals(3.50, recordCarro.wearCost, 0.001)

        // Verificar desgaste após expediente do carro:
        // Peças do carro devem ter recebido 50.0 km
        val carroPartsAfterCarroRecord = db.vehiclePartDao().getPartsForVehicle(carroId)
        assertTrue(carroPartsAfterCarroRecord.all { it.runKmSinceChange == 50.0 })

        // Peças da moto devem ter permanecido com 100.0 km (não foram alteradas)
        val motoPartsAfterCarroRecord = db.vehiclePartDao().getPartsForVehicle(motoId)
        assertTrue("As peças da moto devem manter 100.0 km intactos",
            motoPartsAfterCarroRecord.all { it.runKmSinceChange == 100.0 })
    }

    @Test
    fun testViewModel_partsStateFlow_emitsOnlyActiveVehiclePartsUponSwitching() = runTest(testDispatcher) {
        // Cadastrar Moto (ativa) e Carro (inativo)
        val motoId = repository.addVehicle(
            Vehicle(brand = "Honda", model = "CG 160 Fan Flex", vehicleType = "MOTO", active = true)
        )
        val carroId = repository.addVehicle(
            Vehicle(brand = "Toyota", model = "Corolla GLi", vehicleType = "CARRO", active = false)
        )

        // Adicionar peças vinculadas a cada veículo
        repository.savePart(VehiclePart(vehicleId = motoId, name = "Corrente DID Moto", price = 100.0, lifespanKm = 10000.0))
        repository.savePart(VehiclePart(vehicleId = motoId, name = "Pastilha Freio Moto", price = 40.0, lifespanKm = 6000.0))

        repository.savePart(VehiclePart(vehicleId = carroId, name = "Filtro de Cabine Carro", price = 80.0, lifespanKm = 15000.0))
        repository.savePart(VehiclePart(vehicleId = carroId, name = "Velas de Ignição Carro", price = 160.0, lifespanKm = 40000.0))

        // Inicia coleta no StateFlow parts do ViewModel para ativar o WhileSubscribed
        val collectedEmissions = mutableListOf<List<VehiclePart>>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.parts.collect { collectedEmissions.add(it) }
        }

        // 1. Inicialmente, Moto está ativa: StateFlow parts deve emitir apenas as peças da moto
        val initialParts = viewModel.parts.value
        assertEquals(2, initialParts.size)
        assertTrue(initialParts.any { it.name == "Corrente DID Moto" })
        assertTrue(initialParts.any { it.name == "Pastilha Freio Moto" })
        assertFalse(initialParts.any { it.name.contains("Carro") })
        assertTrue(initialParts.all { it.vehicleId == motoId })

        // 2. Alternar para o Carro via ViewModel: setActiveVehicle
        viewModel.setActiveVehicle(carroId)

        // StateFlow parts deve atualizar para conter unicamente as peças do carro
        val carParts = viewModel.parts.value
        assertEquals(2, carParts.size)
        assertTrue(carParts.any { it.name == "Filtro de Cabine Carro" })
        assertTrue(carParts.any { it.name == "Velas de Ignição Carro" })
        assertFalse(carParts.any { it.name.contains("Moto") })
        assertTrue(carParts.all { it.vehicleId == carroId })

        // 3. Alternar de volta para a Moto
        viewModel.setActiveVehicle(motoId)

        val backToMotoParts = viewModel.parts.value
        assertEquals(2, backToMotoParts.size)
        assertTrue(backToMotoParts.any { it.name == "Corrente DID Moto" })
        assertTrue(backToMotoParts.any { it.name == "Pastilha Freio Moto" })
        assertFalse(backToMotoParts.any { it.name.contains("Carro") })

        collectJob.cancel()
    }

    @Test
    fun testMigration_11_to_12_addsVehicleIdColumnAndMigratesExistingPartsToActiveVehicle() = runTest(testDispatcher) {
        val dbName = "test_fipe_migration_11_12.db"
        val dbFile = File(context.cacheDir, dbName)
        if (dbFile.exists()) dbFile.delete()

        // 1. Cria o banco SQLite versão 11 puro (sem vehicleId em vehicle_parts)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Tabela vehicle v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`brand` TEXT NOT NULL DEFAULT '', " +
                                "`model` TEXT NOT NULL, " +
                                "`vehicleType` TEXT NOT NULL DEFAULT 'MOTO', " +
                                "`manufacturingYear` TEXT NOT NULL DEFAULT '', " +
                                "`carBodyType` TEXT NOT NULL DEFAULT '', " +
                                "`hasTopBox` INTEGER NOT NULL DEFAULT 0, " +
                                "`hasDeliveryBag` INTEGER NOT NULL DEFAULT 0, " +
                                "`averageConsumption` REAL NOT NULL, " +
                                "`fuelType` TEXT NOT NULL, " +
                                "`monthlyFixedCosts` REAL NOT NULL, " +
                                "`plannedWorkDays` INTEGER NOT NULL, " +
                                "`tankCapacity` REAL NOT NULL DEFAULT 0.0, " +
                                "`currentOdometer` REAL NOT NULL, " +
                                "`active` INTEGER NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // Tabela vehicle_parts v11 (SEM a coluna vehicleId)
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle_parts` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`price` REAL NOT NULL, " +
                                "`lifespanKm` REAL NOT NULL, " +
                                "`runKmSinceChange` REAL NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // daily_records v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `daily_records` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`platform` TEXT NOT NULL, " +
                                "`grossEarnings` REAL NOT NULL, " +
                                "`deliveriesCount` INTEGER NOT NULL, " +
                                "`startOdometer` REAL NOT NULL, " +
                                "`endOdometer` REAL NOT NULL, " +
                                "`fuelPrice` REAL NOT NULL, " +
                                "`foodExpense` REAL NOT NULL, " +
                                "`fuelCost` REAL NOT NULL, " +
                                "`wearCost` REAL NOT NULL, " +
                                "`proportionalFixedCost` REAL NOT NULL, " +
                                "`netProfit` REAL NOT NULL, " +
                                "`startTime` TEXT NOT NULL DEFAULT '', " +
                                "`endTime` TEXT NOT NULL DEFAULT '', " +
                                "`pauseDuration` TEXT NOT NULL DEFAULT '', " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // user_profile v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                                "`id` INTEGER NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`phone` TEXT NOT NULL, " +
                                "`city` TEXT NOT NULL, " +
                                "`isWhatsapp` INTEGER NOT NULL, " +
                                "`cep` TEXT NOT NULL DEFAULT '', " +
                                "`street` TEXT NOT NULL DEFAULT '', " +
                                "`addressNumber` TEXT NOT NULL DEFAULT '', " +
                                "`neighborhood` TEXT NOT NULL DEFAULT '', " +
                                "`stateUf` TEXT NOT NULL DEFAULT '', " +
                                "`onboardingCompleted` INTEGER NOT NULL DEFAULT 0, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0, " +
                                "`email` TEXT NOT NULL DEFAULT '', " +
                                "PRIMARY KEY(`id`))"
                        )

                        // platforms v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `platforms` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`segment` TEXT NOT NULL, " +
                                "`paymentModel` TEXT NOT NULL, " +
                                "`cycle` TEXT NOT NULL, " +
                                "`paymentDay` TEXT NOT NULL, " +
                                "`fixedPayDelay` INTEGER NOT NULL, " +
                                "`cycleEntriesJson` TEXT NOT NULL, " +
                                "`bankName` TEXT NOT NULL, " +
                                "`bankAgency` TEXT NOT NULL, " +
                                "`bankAccount` TEXT NOT NULL, " +
                                "`pixKeyType` TEXT NOT NULL, " +
                                "`pixKey` TEXT NOT NULL, " +
                                "`active` INTEGER NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // fuel_refills v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `fuel_refills` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`vehicleId` INTEGER NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`gasStation` TEXT NOT NULL, " +
                                "`fuelType` TEXT NOT NULL, " +
                                "`pricePerLiter` REAL NOT NULL, " +
                                "`liters` REAL NOT NULL, " +
                                "`discount` REAL NOT NULL, " +
                                "`totalPaid` REAL NOT NULL, " +
                                "`odometer` REAL NOT NULL, " +
                                "`isFullTank` INTEGER NOT NULL, " +
                                "`paymentMethod` TEXT NOT NULL, " +
                                "`isInstallment` INTEGER NOT NULL, " +
                                "`installmentsCount` INTEGER NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // maintenance_records v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `maintenance_records` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`vehicleId` INTEGER NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`description` TEXT NOT NULL, " +
                                "`location` TEXT NOT NULL, " +
                                "`value` REAL NOT NULL, " +
                                "`odometer` REAL NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val rawDb = helper.writableDatabase

        // Inserir veículo ativo com id = 42
        val cvActive = ContentValues().apply {
            put("id", 42L)
            put("model", "Honda CB 500F")
            put("averageConsumption", 25.0)
            put("fuelType", "Gasolina")
            put("monthlyFixedCosts", 250.0)
            put("plannedWorkDays", 22)
            put("currentOdometer", 15000.0)
            put("active", 1)
        }
        rawDb.insert("vehicle", 0, cvActive)

        // Inserir veículo inativo com id = 99
        val cvInactive = ContentValues().apply {
            put("id", 99L)
            put("model", "VW Polo")
            put("averageConsumption", 13.0)
            put("fuelType", "Flex")
            put("monthlyFixedCosts", 400.0)
            put("plannedWorkDays", 20)
            put("currentOdometer", 40000.0)
            put("active", 0)
        }
        rawDb.insert("vehicle", 0, cvInactive)

        // Inserir 2 peças de desgaste v11 (sem a coluna vehicleId)
        val cvPart1 = ContentValues().apply {
            put("name", "Filtro de Óleo")
            put("price", 60.0)
            put("lifespanKm", 5000.0)
            put("runKmSinceChange", 1000.0)
        }
        rawDb.insert("vehicle_parts", 0, cvPart1)

        val cvPart2 = ContentValues().apply {
            put("name", "Pastilha Traseira")
            put("price", 85.0)
            put("lifespanKm", 8000.0)
            put("runKmSinceChange", 2000.0)
        }
        rawDb.insert("vehicle_parts", 0, cvPart2)

        rawDb.close()
        helper.close()

        // 2. Abrir o banco com Room versão 14 executando as migrações até a versão 14
        val roomDb = Room.databaseBuilder(context, GiroCustoDatabase::class.java, dbName)
            .addMigrations(GiroCustoDatabase.MIGRATION_11_12, GiroCustoDatabase.MIGRATION_12_13, GiroCustoDatabase.MIGRATION_13_14)
            .allowMainThreadQueries()
            .build()

        val partDao = roomDb.vehiclePartDao()
        val allParts = partDao.getAllParts()
        assertEquals(2, allParts.size)

        // As peças antigas devem ter sido atribuídas ao veículo ativo (id = 42)
        assertTrue("Todas as peças devem ter recebido vehicleId = 42", allParts.all { it.vehicleId == 42L })

        val partsForActive = partDao.getPartsForVehicle(42L)
        assertEquals(2, partsForActive.size)

        // Inserir nova peça explicitamente no veículo inativo 99
        partDao.insertPart(
            VehiclePart(
                vehicleId = 99L,
                name = "Pneu Aro 15 Carro",
                price = 320.0,
                lifespanKm = 50000.0
            )
        )

        val partsForCar = partDao.getPartsForVehicle(99L)
        assertEquals(1, partsForCar.size)
        assertEquals("Pneu Aro 15 Carro", partsForCar[0].name)

        roomDb.close()
    }

    @Test
    fun testUserProfile_saveWithBlankOrManualCep_noExceptionAndPersistsCorrectly() = runTest(testDispatcher) {
        // Cenário de preenchimento manual do endereço (sem informar CEP)
        viewModel.updateUserProfile(
            name = "Carlos Entregador",
            phone = "11999998888",
            city = "Campinas",
            email = "carlos@girocusto.com",
            isWhatsapp = true,
            cep = "", // CEP em branco
            street = "Avenida Francisco Glicério",
            addressNumber = "450",
            neighborhood = "Centro",
            stateUf = "SP",
            onboardingCompleted = true
        )

        // Valida que o perfil foi persistido sem nenhuma exceção e que os campos manuais foram preservados
        val savedProfile = repository.getUserProfile()
        assertNotNull(savedProfile)
        assertEquals("Carlos Entregador", savedProfile?.name)
        assertEquals("11999998888", savedProfile?.phone)
        assertEquals("Campinas", savedProfile?.city)
        assertEquals("", savedProfile?.cep)
        assertEquals("Avenida Francisco Glicério", savedProfile?.street)
        assertEquals("450", savedProfile?.addressNumber)
        assertEquals("Centro", savedProfile?.neighborhood)
        assertEquals("SP", savedProfile?.stateUf)
        assertTrue(savedProfile?.onboardingCompleted == true)

        // Valida também via inserção direta no repository de perfil com cep vazio
        val manualProfile = UserProfile(
            name = "Mariana Silva",
            phone = "21977776666",
            city = "Niterói",
            cep = "",
            street = "Rua Coronel Moreira César",
            addressNumber = "100",
            neighborhood = "Icaraí",
            stateUf = "RJ",
            isWhatsapp = true,
            onboardingCompleted = true
        )
        repository.saveUserProfile(manualProfile)

        val reloadedProfile = repository.getUserProfile()
        assertNotNull(reloadedProfile)
        assertEquals("Mariana Silva", reloadedProfile?.name)
        assertEquals("", reloadedProfile?.cep)
        assertEquals("Icaraí", reloadedProfile?.neighborhood)
        assertEquals("RJ", reloadedProfile?.stateUf)
    }

    @Test
    fun testVehicleScreen_removalIntegrityCheck() {
        // 1. Confirmar que a antiga classe VehicleScreenKt foi completamente removida do classpath
        try {
            Class.forName("com.example.ui.screens.VehicleScreenKt")
            fail("VehicleScreenKt não deveria mais existir no classpath")
        } catch (e: ClassNotFoundException) {
            // Sucesso esperado: classe não existe
            assertNotNull(e)
        }

        // 2. Confirmar que o arquivo físico VehicleScreen.kt não existe no diretório do projeto
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val vehicleScreenFile = File(projectDir, "app/src/main/java/com/example/ui/screens/VehicleScreen.kt")
        assertFalse("VehicleScreen.kt deve ser inexistente no sistema de arquivos", vehicleScreenFile.exists())

        // 3. Confirmar que o DrawerDestination possui apenas PERFIL, PLATAFORMAS, ABASTECIMENTOS, MANUTENCAO, NUVEM
        val destinations = DrawerDestination.values().map { it.name }
        assertFalse("DrawerDestination não deve conter VEICULO", destinations.contains("VEICULO"))
    }
}
