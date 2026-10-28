package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

/**
 * Suíte de testes automatizados para resiliência e tratamento de falhas da Overpass API (T-13 e T-14 [INT-03]).
 * Valida timeouts sincronizados, fallback entre espelhos de servidores, método POST FormUrlEncoded e mensagens amigáveis em português do Brasil.
 */
@RunWith(RobolectricTestRunner::class)
class OverpassApiResilienceTest {

    private lateinit var mockServer: MockWebServer
    private val originalEndpoints = GasStationRepository.endpoints
    private val originalService = GasStationRepository.apiService

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        GasStationRepository.endpoints = originalEndpoints
        GasStationRepository.apiService = originalService
    }

    @Test
    fun testOverpassQuery_specifies25SecondsTimeoutDirective() {
        val query = GasStationRepository.buildOverpassQuery(-23.5505, -46.6333, 3000)
        assertTrue("A query Overpass deve declarar [timeout:25]", query.contains("[timeout:25]"))
        assertTrue("A query Overpass deve filtrar postos de combustível", query.contains("""amenity"="fuel"""))
    }

    @Test
    fun testOverpassEndpoints_doesNotContainDeadRussianEndpointAndHasActiveMirrors() {
        val endpoints = GasStationRepository.OVERPASS_ENDPOINTS

        assertFalse(
            "O endpoint descontinuado openstreetmap.ru deve ser removido da lista",
            endpoints.any { it.contains("openstreetmap.ru") }
        )
        assertFalse(
            "O endpoint legado kumi.systems deve ser atualizado",
            endpoints.any { it.contains("kumi.systems") }
        )
        assertTrue(
            "A lista deve conter o servidor ativo openstreetmap.fr como primário",
            endpoints.first().contains("openstreetmap.fr")
        )
        assertTrue(
            "A lista deve conter o servidor principal overpass-api.de",
            endpoints.any { it.contains("overpass-api.de") }
        )
        assertTrue(
            "A lista deve conter o servidor ativo private.coffee",
            endpoints.any { it.contains("private.coffee") }
        )
    }

    @Test
    fun testOverpassApi_usesHttpPostAndFormUrlEncoded() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"elements":[]}"""))

        val retrofit = Retrofit.Builder()
            .baseUrl(mockServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val service = retrofit.create(OverpassApiService::class.java)
        service.getNearbyStations(mockServer.url("/api/interpreter").toString(), "test-query")

        val recorded = mockServer.takeRequest()
        assertEquals("Método HTTP deve ser POST para evitar limitação de URL GET", "POST", recorded.method)
        assertTrue(
            "Content-Type deve ser application/x-www-form-urlencoded",
            recorded.getHeader("Content-Type")?.contains("application/x-www-form-urlencoded") == true
        )
        assertEquals("data=test-query", recorded.body.readUtf8())
    }

    @Test
    fun testOverpassApi_firstEndpointFails_fallbackToSecondEndpointSucceeds() = runTest {
        val fakeSuccessJson = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 12345,
                  "lat": -23.5510,
                  "lon": -46.6340,
                  "tags": {
                    "name": "Posto Shell Central",
                    "brand": "Shell",
                    "addr:street": "Av. Principal",
                    "addr:housenumber": "100"
                  }
                }
              ]
            }
        """.trimIndent()

        // Primeira chamada (servidor 1) falha com 504 Gateway Timeout
        mockServer.enqueue(MockResponse().setResponseCode(504).setBody("Gateway Timeout"))
        // Segunda chamada (servidor 2) responde com sucesso 200 OK
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(fakeSuccessJson))

        val url1 = mockServer.url("/mirror1/interpreter").toString()
        val url2 = mockServer.url("/mirror2/interpreter").toString()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        GasStationRepository.apiService = retrofit.create(OverpassApiService::class.java)
        GasStationRepository.endpoints = listOf(url1, url2)

        val result = GasStationRepository.findNearbyGasStations(-23.5505, -46.6333, 3000)

        assertTrue("Resultado deve ser de sucesso via fallback no segundo endpoint", result is GasStationSearchResult.Success)
        val success = result as GasStationSearchResult.Success
        assertEquals(1, success.stations.size)
        assertEquals("Posto Shell Central", success.stations[0].name)
        assertEquals("Shell", success.stations[0].brand)
    }

    @Test
    fun testOverpassApi_allEndpointsFail_returnsFriendlyPortugueseErrorMessageWithoutLeaks() = runTest {
        // Ambas as chamadas falham (ex: 500 / Timeout)
        mockServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        mockServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))

        val url1 = mockServer.url("/mirror1/interpreter").toString()
        val url2 = mockServer.url("/mirror2/interpreter").toString()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        GasStationRepository.apiService = retrofit.create(OverpassApiService::class.java)
        GasStationRepository.endpoints = listOf(url1, url2)

        val result = GasStationRepository.findNearbyGasStations(-23.5505, -46.6333, 3000)

        assertTrue("Resultado deve ser erro controlado", result is GasStationSearchResult.Error)
        val error = result as GasStationSearchResult.Error

        assertEquals(
            "Mensagem deve ser amigável e em português",
            "Não foi possível buscar postos próximos no momento. Verifique sua conexão com a internet e tente novamente.",
            error.message
        )
        assertFalse("Não deve vazar termos técnicos como 'failed to connect'", error.message.contains("failed to connect"))
        assertFalse("Não deve vazar portas de socket", error.message.contains("port"))
        assertFalse("Não deve vazar IPs de servidor", error.message.contains("185."))
    }
}
