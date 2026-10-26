package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GiroCustoDatabase
import com.example.network.fipe.FipeApi
import com.example.network.fipe.FipeDetailDto
import com.example.network.fipe.FipeItemDto
import com.example.network.fipe.FipeModelsResponseDto
import com.example.network.fipe.FipeNetworkClient
import com.example.network.viacep.ViaCepApi
import com.example.network.viacep.ViaCepNetworkClient
import com.example.network.viacep.ViaCepResponse
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/**
 * Suíte de testes unitários para a Tarefa T-14b [ARQ-01B].
 * Comprova o desacoplamento arquitetural, a eliminação de variáveis públicas de mock/teste
 * em código de produção e o funcionamento pleno da injeção via construtor e via métodos restritos.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NetworkAndDatabaseDecouplingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FipeNetworkClient.setInstance(null)
        ViaCepNetworkClient.setInstance(null)
        GiroCustoDatabase.resetForTesting()
    }

    @After
    fun tearDown() {
        FipeNetworkClient.setInstance(null)
        ViaCepNetworkClient.setInstance(null)
        GiroCustoDatabase.resetForTesting()
    }

    // =========================================================================
    // 1. FIPE NETWORK CLIENT - DESACOPLAMENTO E INJEÇÃO
    // =========================================================================

    @Test
    fun fipeNetworkClient_defaultImplementationUsesRealApiWithoutTestHooks() {
        val defaultApi = FipeNetworkClient.api
        assertNotNull("API padrão do FipeNetworkClient não deve ser nula", defaultApi)
    }

    @Test
    fun fipeNetworkClient_constructorInjection_createsIsolatedClientWithCustomApi() = runTest {
        var called = false
        val customApi = object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> {
                called = true
                return Response.success(listOf(FipeItemDto(nome = "Honda", codigoRaw = "59")))
            }
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        }

        val client = FipeNetworkClient(customApi)
        assertSame("O client deve expor exatamente a instância de FipeApi injetada via construtor", customApi, client.api)

        val response = client.api.getMarcas("motos")
        assertTrue("Deveria invocar a API injetada", called)
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.size)
        assertEquals("Honda", response.body()?.first()?.nome)
    }

    @Test
    fun fipeNetworkClient_setInstanceAndSetApi_updatesSingletonDeterministically() {
        val customApi = object : FipeApi {
            override suspend fun getMarcas(tipo: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getModelos(tipo: String, marcaId: String): Response<FipeModelsResponseDto> = throw NotImplementedError()
            override suspend fun getAnos(tipo: String, marcaId: String, modeloId: String): Response<List<FipeItemDto>> = throw NotImplementedError()
            override suspend fun getDetail(tipo: String, marcaId: String, modeloId: String, anoId: String): Response<FipeDetailDto> = throw NotImplementedError()
        }

        // Teste de setApi
        FipeNetworkClient.setApi(customApi)
        assertSame(customApi, FipeNetworkClient.api)

        // Teste de reset
        FipeNetworkClient.setApi(null)
        assertNotSame(customApi, FipeNetworkClient.api)

        // Teste de setInstance
        val customClient = FipeNetworkClient(customApi)
        FipeNetworkClient.setInstance(customClient)
        assertSame(customApi, FipeNetworkClient.api)

        FipeNetworkClient.setInstance(null)
        assertNotSame(customApi, FipeNetworkClient.api)
    }

    // =========================================================================
    // 2. VIACEP NETWORK CLIENT - DESACOPLAMENTO E INJEÇÃO
    // =========================================================================

    @Test
    fun viaCepNetworkClient_defaultImplementationUsesRealApiWithoutTestHooks() {
        val defaultApi = ViaCepNetworkClient.api
        assertNotNull("API padrão do ViaCepNetworkClient não deve ser nula", defaultApi)
    }

    @Test
    fun viaCepNetworkClient_constructorInjection_createsIsolatedClientWithCustomApi() = runTest {
        var called = false
        val customApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> {
                called = true
                return Response.success(ViaCepResponse(cep = cep, logradouro = "Av Paulista"))
            }

            override suspend fun getAddress(cep: String): Response<ViaCepResponse> {
                called = true
                return Response.success(ViaCepResponse(cep = cep, logradouro = "Av Paulista"))
            }
        }

        val client = ViaCepNetworkClient(customApi)
        assertSame("O client deve expor exatamente a instância de ViaCepApi injetada via construtor", customApi, client.api)

        val response = client.api.getAddressByCep("01311000")
        assertTrue("Deveria invocar a API injetada", called)
        assertTrue(response.isSuccessful)
        assertEquals("Av Paulista", response.body()?.logradouro)
    }

    @Test
    fun viaCepNetworkClient_setInstanceAndSetApi_updatesSingletonDeterministically() {
        val customApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> = throw NotImplementedError()
            override suspend fun getAddress(cep: String): Response<ViaCepResponse> = throw NotImplementedError()
        }

        // Teste de setApi
        ViaCepNetworkClient.setApi(customApi)
        assertSame(customApi, ViaCepNetworkClient.api)

        // Teste de reset
        ViaCepNetworkClient.setApi(null)
        assertNotSame(customApi, ViaCepNetworkClient.api)

        // Teste de setInstance
        val customClient = ViaCepNetworkClient(customApi)
        ViaCepNetworkClient.setInstance(customClient)
        assertSame(customApi, ViaCepNetworkClient.api)

        ViaCepNetworkClient.setInstance(null)
        assertNotSame(customApi, ViaCepNetworkClient.api)
    }

    // =========================================================================
    // 3. GIROCUSTO DATABASE - SETTESTDATABASE E RESETFORTESTING
    // =========================================================================

    @Test
    fun giroCustoDatabase_setTestDatabase_updatesSingletonAndResetForTestingClearsIt() {
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        GiroCustoDatabase.setTestDatabase(inMemoryDb)
        assertSame("getDatabase deve retornar o banco injetado", inMemoryDb, GiroCustoDatabase.getDatabase(context))

        GiroCustoDatabase.resetForTesting()
        inMemoryDb.close()

        val newDb = GiroCustoDatabase.getDatabase(context)
        assertNotNull("Deve recriar ou obter a instância padrão após reset", newDb)
        assertNotSame("A nova instância não pode ser a anterior que foi fechada", inMemoryDb, newDb)
    }
}
