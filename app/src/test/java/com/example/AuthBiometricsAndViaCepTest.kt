package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.network.supabase.AuthSession
import com.example.network.supabase.SupabaseClient
import com.example.network.viacep.ViaCepApi
import com.example.network.viacep.ViaCepNetworkClient
import com.example.network.viacep.ViaCepResponse
import com.example.ui.AuthUiState
import com.example.ui.GiroCustoViewModel
import com.example.ui.ViaCepUiState
import com.example.ui.navigation.RootNavState
import com.example.util.BiometricAvailability
import com.example.util.BiometricHelper
import com.example.util.BrazilianCepVisualTransformation
import com.example.util.BrazilianPhoneVisualTransformation
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class AuthBiometricsAndViaCepTest {

    private lateinit var testDispatcher: kotlinx.coroutines.test.TestDispatcher
    private lateinit var context: Context
    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModel: GiroCustoViewModel

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        // Inicia banco de dados Room in-memory v11 para isolamento total dos testes
        db = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        GiroCustoDatabase.setTestDatabase(db)
        repository = GiroCustoRepository(db)

        // Limpa estado anterior de Supabase e Biometria
        supabaseClient = SupabaseClient.resetForTesting(context)
        supabaseClient.setAuthSession(null)
        supabaseClient.testAuthApi = object : com.example.network.supabase.SupabaseAuthApi {
            override suspend fun signUp(request: com.example.network.supabase.AuthRequest): Response<com.example.network.supabase.AuthResponse> =
                Response.success(com.example.network.supabase.AuthResponse(accessToken = "token-test", refreshToken = "refresh-test"))

            override suspend fun signInWithPassword(grantType: String, request: com.example.network.supabase.AuthRequest): Response<com.example.network.supabase.AuthResponse> =
                Response.success(com.example.network.supabase.AuthResponse(accessToken = "token-test", refreshToken = "refresh-test"))

            override suspend fun refreshToken(grantType: String, request: com.example.network.supabase.RefreshTokenRequest): Response<com.example.network.supabase.AuthResponse> =
                Response.success(com.example.network.supabase.AuthResponse(accessToken = "token-refreshed", refreshToken = "refresh-refreshed"))

            override suspend fun logout(): Response<Unit> = Response.success(Unit)

            override suspend fun getUser(): Response<com.example.network.supabase.SupabaseUser> =
                Response.success(com.example.network.supabase.SupabaseUser("usr-test", "user@test.com"))
        }
        BiometricHelper.clearBiometrics(context)
        BiometricHelper.testHardwareAvailable = null
        ViaCepNetworkClient.testApi = null

        // Instancia ViewModel diretamente para isolamento determinístico
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModelStore = ViewModelStore()
        viewModel = GiroCustoViewModel(app)
    }

    @After
    fun tearDown() {
        ViaCepNetworkClient.testApi = null
        supabaseClient.testAuthApi = null
        supabaseClient.testRestApi = null
        supabaseClient.setAuthSession(null)
        BiometricHelper.clearBiometrics(context)
        BiometricHelper.testHardwareAvailable = null
        SupabaseClient.resetForTesting(context)
        viewModelStore.clear()
        Dispatchers.resetMain()
        db.close()
    }

    // =========================================================================
    // 1. TESTES DE RESPOSTAS E MODELOS VIACEP
    // =========================================================================

    @Test
    fun testViaCepResponse_deserialization_success() {
        val json = """
            {
                "cep": "01001-000",
                "logradouro": "Praça da Sé",
                "complemento": "lado ímpar",
                "bairro": "Sé",
                "localidade": "São Paulo",
                "uf": "SP",
                "ibge": "3550308",
                "ddd": "11"
            }
        """.trimIndent()

        val adapter = moshi.adapter(ViaCepResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals("01001-000", response?.cep)
        assertEquals("Praça da Sé", response?.logradouro)
        assertEquals("lado ímpar", response?.complemento)
        assertEquals("Sé", response?.bairro)
        assertEquals("São Paulo", response?.localidade)
        assertEquals("SP", response?.uf)
        assertEquals("3550308", response?.ibge)
        assertEquals("11", response?.ddd)
        assertFalse(response!!.isErro)
    }

    @Test
    fun testViaCepResponse_deserialization_error_boolean() {
        val json = """
            {
                "erro": true
            }
        """.trimIndent()

        val adapter = moshi.adapter(ViaCepResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.isErro)
    }

    @Test
    fun testViaCepResponse_deserialization_error_string() {
        val json = """
            {
                "erro": "true"
            }
        """.trimIndent()

        val adapter = moshi.adapter(ViaCepResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.isErro)
    }

    @Test
    fun testViaCepResponse_deserialization_error_caseInsensitive() {
        val json = """
            {
                "erro": "TRUE"
            }
        """.trimIndent()

        val adapter = moshi.adapter(ViaCepResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.isErro)
    }

    // =========================================================================
    // 2. TESTES DE FLUXO VIACEP NO VIEWMODEL (CENÁRIOS CRÍTICOS)
    // =========================================================================

    @Test
    fun testViaCep_invalidFormat_lessThan8Digits_noNetworkCall() = runTest(testDispatcher) {
        var networkCalled = false
        ViaCepNetworkClient.testApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> {
                networkCalled = true
                return Response.success(ViaCepResponse(cep = cep))
            }

            override suspend fun getAddress(cep: String): Response<ViaCepResponse> {
                networkCalled = true
                return Response.success(ViaCepResponse(cep = cep))
            }
        }

        // Teste com 3 dígitos
        viewModel.searchCep("123")
        assertFalse("Não deve disparar chamada de rede quando CEP < 8 dígitos", networkCalled)
        assertTrue(viewModel.viaCepState.value is ViaCepUiState.Error)
        assertEquals(
            "O CEP deve conter exatamente 8 dígitos numéricos.",
            (viewModel.viaCepState.value as ViaCepUiState.Error).message
        )

        // Teste com letras e caracteres inválidos
        viewModel.searchCep("abc-xyz")
        assertFalse(networkCalled)
        assertTrue(viewModel.viaCepState.value is ViaCepUiState.Error)

        // Teste com 7 dígitos
        viewModel.searchCep("0131010")
        assertFalse(networkCalled)
        assertTrue(viewModel.viaCepState.value is ViaCepUiState.Error)
    }

    @Test
    fun testViaCep_validCep_successResponse() = runTest(testDispatcher) {
        val expectedAddress = ViaCepResponse(
            cep = "01310-100",
            logradouro = "Avenida Paulista",
            complemento = "de 611 a 1045 - lado ímpar",
            bairro = "Bela Vista",
            localidade = "São Paulo",
            uf = "SP",
            ibge = "3550308",
            ddd = "11"
        )

        ViaCepNetworkClient.testApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> {
                assertEquals("01310100", cep)
                return Response.success(expectedAddress)
            }

            override suspend fun getAddress(cep: String): Response<ViaCepResponse> {
                return Response.success(expectedAddress)
            }
        }

        viewModel.searchCep("01310-100")
        runCurrent()

        val state = viewModel.viaCepState.value
        assertTrue("Estado deve ser ViaCepUiState.Success", state is ViaCepUiState.Success)
        val successState = state as ViaCepUiState.Success
        assertEquals("01310-100", successState.address.cep)
        assertEquals("Avenida Paulista", successState.address.logradouro)
        assertEquals("Bela Vista", successState.address.bairro)
        assertEquals("São Paulo", successState.address.localidade)
        assertEquals("SP", successState.address.uf)
    }

    @Test
    fun testViaCep_nonExistentCep_returnsErrorBoolean() = runTest(testDispatcher) {
        ViaCepNetworkClient.testApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> {
                return Response.success(ViaCepResponse(erroRaw = true))
            }

            override suspend fun getAddress(cep: String): Response<ViaCepResponse> {
                return Response.success(ViaCepResponse(erroRaw = true))
            }
        }

        viewModel.searchCep("99999-999")
        runCurrent()

        val state = viewModel.viaCepState.value
        assertTrue(state is ViaCepUiState.Error)
        assertEquals("CEP não encontrado na base dos Correios.", (state as ViaCepUiState.Error).message)
    }

    @Test
    fun testViaCep_nonExistentCep_returnsErrorString() = runTest(testDispatcher) {
        ViaCepNetworkClient.testApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> {
                return Response.success(ViaCepResponse(erroRaw = "true"))
            }

            override suspend fun getAddress(cep: String): Response<ViaCepResponse> {
                return Response.success(ViaCepResponse(erroRaw = "true"))
            }
        }

        viewModel.searchCep("99999-999")
        runCurrent()

        val state = viewModel.viaCepState.value
        assertTrue(state is ViaCepUiState.Error)
        assertEquals("CEP não encontrado na base dos Correios.", (state as ViaCepUiState.Error).message)
    }

    @Test
    fun testViaCep_timeoutException() = runTest(testDispatcher) {
        ViaCepNetworkClient.testApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> {
                throw SocketTimeoutException("Conexão expirou")
            }

            override suspend fun getAddress(cep: String): Response<ViaCepResponse> {
                throw SocketTimeoutException("Conexão expirou")
            }
        }

        viewModel.searchCep("01310-100")
        runCurrent()

        val state = viewModel.viaCepState.value
        assertTrue(state is ViaCepUiState.Error)
        assertEquals("Tempo limite excedido ao consultar o CEP.", (state as ViaCepUiState.Error).message)
    }

    @Test
    fun testViaCep_ioException_networkFailure() = runTest(testDispatcher) {
        ViaCepNetworkClient.testApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> {
                throw IOException("Network unreachable")
            }

            override suspend fun getAddress(cep: String): Response<ViaCepResponse> {
                throw IOException("Network unreachable")
            }
        }

        viewModel.searchCep("01310-100")
        runCurrent()

        val state = viewModel.viaCepState.value
        assertTrue(state is ViaCepUiState.Error)
        assertEquals("Sem conexão de internet para buscar o CEP.", (state as ViaCepUiState.Error).message)
    }

    @Test
    fun testViaCep_httpError500() = runTest(testDispatcher) {
        ViaCepNetworkClient.testApi = object : ViaCepApi {
            override suspend fun getAddressByCep(cep: String): Response<ViaCepResponse> {
                val errorBody = "Internal Server Error".toResponseBody("text/plain".toMediaTypeOrNull())
                return Response.error(500, errorBody)
            }

            override suspend fun getAddress(cep: String): Response<ViaCepResponse> {
                val errorBody = "Internal Server Error".toResponseBody("text/plain".toMediaTypeOrNull())
                return Response.error(500, errorBody)
            }
        }

        viewModel.searchCep("01310-100")
        runCurrent()

        val state = viewModel.viaCepState.value
        assertTrue(state is ViaCepUiState.Error)
        assertEquals("Serviço de CEP temporariamente indisponível (500).", (state as ViaCepUiState.Error).message)

        // Valida que clearViaCepState retorna para Idle
        viewModel.clearViaCepState()
        assertTrue(viewModel.viaCepState.value is ViaCepUiState.Idle)
    }

    // =========================================================================
    // 3. TESTES DE MÁSCARAS E OFFSETMAPPING (TELEFONE E CEP)
    // =========================================================================

    @Test
    fun testBrazilianPhoneVisualTransformation_fullOffsetMappingAndLimits() {
        val transformation = BrazilianPhoneVisualTransformation()

        // 1. Texto vazio
        val emptyResult = transformation.filter(AnnotatedString(""))
        assertEquals("", emptyResult.text.text)
        assertEquals(0, emptyResult.offsetMapping.originalToTransformed(0))
        assertEquals(0, emptyResult.offsetMapping.transformedToOriginal(0))

        // 2. Texto com 1 dígito "1" -> "(1"
        val oneDigit = transformation.filter(AnnotatedString("1"))
        assertEquals("(1", oneDigit.text.text)
        assertEquals(0, oneDigit.offsetMapping.originalToTransformed(0))
        assertEquals(2, oneDigit.offsetMapping.originalToTransformed(1))
        assertEquals(0, oneDigit.offsetMapping.transformedToOriginal(0))
        assertEquals(0, oneDigit.offsetMapping.transformedToOriginal(1))
        assertEquals(1, oneDigit.offsetMapping.transformedToOriginal(2))

        // 3. Texto com DDD completo "11" -> "(11) "
        val twoDigits = transformation.filter(AnnotatedString("11"))
        assertEquals("(11) ", twoDigits.text.text)
        assertEquals(5, twoDigits.offsetMapping.originalToTransformed(2))

        // 4. Texto completo de celular com 11 dígitos "11987654321" -> "(11) 98765-4321"
        val full = transformation.filter(AnnotatedString("11987654321"))
        val fullText = full.text.text
        assertEquals("(11) 98765-4321", fullText)
        assertEquals(15, fullText.length)

        // Verificação ponto a ponto de originalToTransformed
        // orig 0 -> 0
        assertEquals(0, full.offsetMapping.originalToTransformed(0))
        // orig 1 -> 2 (após '(' e primeiro dígito)
        assertEquals(2, full.offsetMapping.originalToTransformed(1))
        // orig 2 -> 5 (após ") ")
        assertEquals(5, full.offsetMapping.originalToTransformed(2))
        // orig 7 -> 11 (após '-')
        assertEquals(11, full.offsetMapping.originalToTransformed(7))
        // orig 11 -> 15 (fim da máscara)
        assertEquals(15, full.offsetMapping.originalToTransformed(11))

        // Verificação ponto a ponto de transformedToOriginal (cursor reverso)
        assertEquals(0, full.offsetMapping.transformedToOriginal(0))
        assertEquals(0, full.offsetMapping.transformedToOriginal(1))
        assertEquals(1, full.offsetMapping.transformedToOriginal(2))
        assertEquals(2, full.offsetMapping.transformedToOriginal(3))
        assertEquals(3, full.offsetMapping.transformedToOriginal(4))
        assertEquals(2, full.offsetMapping.transformedToOriginal(5))
        assertEquals(4, full.offsetMapping.transformedToOriginal(7))
        assertEquals(7, full.offsetMapping.transformedToOriginal(10))
        assertEquals(7, full.offsetMapping.transformedToOriginal(11))
        assertEquals(11, full.offsetMapping.transformedToOriginal(15))

        // Limites fora do range (não devem estourar exceção)
        assertEquals(0, full.offsetMapping.originalToTransformed(-5))
        assertEquals(15, full.offsetMapping.originalToTransformed(20))
        assertEquals(0, full.offsetMapping.transformedToOriginal(-5))
        assertEquals(11, full.offsetMapping.transformedToOriginal(30))

        // 5. Entrada excedente (> 11 dígitos): trunca para 11
        val excess = transformation.filter(AnnotatedString("119876543219999"))
        assertEquals("(11) 98765-4321", excess.text.text)
    }

    @Test
    fun testBrazilianCepVisualTransformation_fullOffsetMappingAndLimits() {
        val transformation = BrazilianCepVisualTransformation()

        // 1. Texto vazio
        val emptyResult = transformation.filter(AnnotatedString(""))
        assertEquals("", emptyResult.text.text)
        assertEquals(0, emptyResult.offsetMapping.originalToTransformed(0))
        assertEquals(0, emptyResult.offsetMapping.transformedToOriginal(0))

        // 2. Texto com 4 dígitos "0131" -> "0131"
        val fourDigits = transformation.filter(AnnotatedString("0131"))
        assertEquals("0131", fourDigits.text.text)
        assertEquals(4, fourDigits.offsetMapping.originalToTransformed(4))
        assertEquals(4, fourDigits.offsetMapping.transformedToOriginal(4))

        // 3. Texto com 5 dígitos "01310" -> "01310-"
        val fiveDigits = transformation.filter(AnnotatedString("01310"))
        assertEquals("01310-", fiveDigits.text.text)
        assertEquals(6, fiveDigits.offsetMapping.originalToTransformed(5))

        // 4. Texto completo de CEP com 8 dígitos "01310100" -> "01310-100"
        val full = transformation.filter(AnnotatedString("01310100"))
        val fullText = full.text.text
        assertEquals("01310-100", fullText)
        assertEquals(9, fullText.length)

        // Verificação ponto a ponto de originalToTransformed
        assertEquals(0, full.offsetMapping.originalToTransformed(0))
        assertEquals(1, full.offsetMapping.originalToTransformed(1))
        assertEquals(2, full.offsetMapping.originalToTransformed(2))
        assertEquals(3, full.offsetMapping.originalToTransformed(3))
        assertEquals(4, full.offsetMapping.originalToTransformed(4))
        assertEquals(6, full.offsetMapping.originalToTransformed(5)) // após o '-'
        assertEquals(7, full.offsetMapping.originalToTransformed(6))
        assertEquals(8, full.offsetMapping.originalToTransformed(7))
        assertEquals(9, full.offsetMapping.originalToTransformed(8))

        // Verificação ponto a ponto de transformedToOriginal
        assertEquals(0, full.offsetMapping.transformedToOriginal(0))
        assertEquals(4, full.offsetMapping.transformedToOriginal(4))
        assertEquals(5, full.offsetMapping.transformedToOriginal(5))
        assertEquals(5, full.offsetMapping.transformedToOriginal(6))
        assertEquals(6, full.offsetMapping.transformedToOriginal(7))
        assertEquals(7, full.offsetMapping.transformedToOriginal(8))
        assertEquals(8, full.offsetMapping.transformedToOriginal(9))

        // Limites fora do range (clamping)
        assertEquals(0, full.offsetMapping.originalToTransformed(-1))
        assertEquals(9, full.offsetMapping.originalToTransformed(10))
        assertEquals(0, full.offsetMapping.transformedToOriginal(-1))
        assertEquals(8, full.offsetMapping.transformedToOriginal(20))

        // 5. Entrada excedente (> 8 dígitos): trunca para 8
        val excess = transformation.filter(AnnotatedString("01310100888"))
        assertEquals("01310-100", excess.text.text)
    }

    @Test
    fun testCepAndPhoneSanitization() {
        // CEP
        val rawCep1 = "01001-000"
        val clean1 = rawCep1.replace("\\D".toRegex(), "")
        assertEquals("01001000", clean1)

        val rawCep2 = " 01.310-100 "
        val clean2 = rawCep2.replace("\\D".toRegex(), "")
        assertEquals("01310100", clean2)

        // Telefone
        val rawPhone = "(11) 98765-4321"
        val cleanPhone = rawPhone.replace("\\D".toRegex(), "")
        assertEquals("11987654321", cleanPhone)
        assertEquals(11, cleanPhone.length)
    }

    // =========================================================================
    // 4. MÁQUINA DE ESTADOS DO ROOTNAVSTATE E FLUXOS DE AUTENTICAÇÃO
    // =========================================================================

    @Test
    fun testRootNavState_transitions_integratedWithViewModel() = runTest(testDispatcher) {
        // 1. Sem usuário na sessão -> RootNavState.Unauthenticated
        supabaseClient.setAuthSession(null)
        runCurrent()
        assertEquals(RootNavState.Unauthenticated, viewModel.rootNavState.value)

        // 2. Usuário autenticado, mas perfil no Room ainda não completou onboarding -> RootNavState.OnboardingPending
        val fakeSession = AuthSession(
            accessToken = "test-token-123",
            refreshToken = "test-refresh-123",
            userId = "usr-uuid-test",
            userEmail = "entregador@girocusto.com"
        )
        supabaseClient.setAuthSession(fakeSession)
        runCurrent()
        assertEquals(RootNavState.OnboardingPending, viewModel.rootNavState.value)

        // 3. Usuário autenticado e onboarding completo no Room -> RootNavState.AuthenticatedReady
        val completedProfile = UserProfile(
            id = 1,
            name = "João da Silva",
            phone = "11988887777",
            city = "São Paulo",
            email = "entregador@girocusto.com",
            onboardingCompleted = true
        )
        repository.saveUserProfile(completedProfile)
        runCurrent()
        assertEquals(RootNavState.AuthenticatedReady, viewModel.rootNavState.value)
    }

    @Test
    fun testLogout_immediateTransitionToUnauthenticatedAndResetStates() = runTest(testDispatcher) {
        // 1. Configura estado logado e onboarding completo
        val session = AuthSession(
            accessToken = "token-ready",
            refreshToken = "refresh-ready",
            userId = "usr-ready",
            userEmail = "ready@girocusto.com"
        )
        supabaseClient.setAuthSession(session)
        repository.saveUserProfile(
            UserProfile(id = 1, name = "Logado", phone = "11999998888", city = "SP", onboardingCompleted = true)
        )
        runCurrent()
        assertEquals(RootNavState.AuthenticatedReady, viewModel.rootNavState.value)

        // Configura estados de UI temporários
        viewModel.searchCep("123") // vai deixar erro no viaCepState
        assertTrue(viewModel.viaCepState.value is ViaCepUiState.Error)

        // 2. Dispara logout
        viewModel.logout()
        advanceUntilIdle()

        // 3. Valida retorno imediato para Unauthenticated
        assertNull(supabaseClient.currentUserId)
        assertFalse(supabaseClient.isAuthenticated)
        assertEquals(RootNavState.Unauthenticated, viewModel.rootNavState.value)

        // 4. Valida reset de UI states
        assertEquals(AuthUiState.Idle, viewModel.authUiState.value)
        assertEquals(ViaCepUiState.Idle, viewModel.viaCepState.value)
    }

    // =========================================================================
    // 5. TESTES DE ONBOARDING (VALIDAÇÕES E PERSISTÊNCIA COMPLETA NO ROOM V11)
    // =========================================================================

    @Test
    fun testCompleteOnboarding_validationErrors() = runTest(testDispatcher) {
        var errorMessage: String? = null

        // 1. Nome em branco
        viewModel.completeOnboarding(
            name = "   ",
            phone = "11987654321",
            isWhatsapp = true,
            cep = "01310-100",
            street = "Paulista",
            addressNumber = "100",
            neighborhood = "Centro",
            city = "SP",
            stateUf = "SP",
            onError = { errorMessage = it }
        )
        assertEquals("O nome completo é obrigatório.", errorMessage)

        // 2. Telefone em branco
        errorMessage = null
        viewModel.completeOnboarding(
            name = "Carlos",
            phone = "   ",
            isWhatsapp = true,
            cep = "01310-100",
            street = "Paulista",
            addressNumber = "100",
            neighborhood = "Centro",
            city = "SP",
            stateUf = "SP",
            onError = { errorMessage = it }
        )
        assertEquals("O telefone para contato é obrigatório.", errorMessage)

        // 3. CEP com menos de 8 dígitos numéricos
        errorMessage = null
        viewModel.completeOnboarding(
            name = "Carlos",
            phone = "11987654321",
            isWhatsapp = true,
            cep = "12345",
            street = "Paulista",
            addressNumber = "100",
            neighborhood = "Centro",
            city = "SP",
            stateUf = "SP",
            onError = { errorMessage = it }
        )
        assertEquals("Informe um CEP válido com 8 dígitos.", errorMessage)
    }

    @Test
    fun testCompleteOnboarding_successPersistsToRoomV11AndTransitionsNavState() = runTest(testDispatcher) {
        // Sessão ativa
        val session = AuthSession(
            accessToken = "token-onboarding",
            refreshToken = "refresh-onboarding",
            userId = "usr-onboarding-123",
            userEmail = "carlos.motoboy@girocusto.com"
        )
        supabaseClient.setAuthSession(session)
        runCurrent()
        assertEquals(RootNavState.OnboardingPending, viewModel.rootNavState.value)

        var successCalled = false
        viewModel.completeOnboarding(
            name = "Carlos Santana",
            phone = "(11) 98765-4321",
            isWhatsapp = true,
            cep = "01310-100",
            street = "Avenida Paulista",
            addressNumber = "1578",
            neighborhood = "Bela Vista",
            city = "São Paulo",
            stateUf = "sp", // Deve ser convertido para maiúsculo
            onSuccess = { successCalled = true }
        )
        runCurrent()

        assertTrue("Callback onSuccess deve ter sido acionado", successCalled)

        // Valida persistência direta no Room v11 via Repository e DAO
        val savedProfile = repository.getUserProfile()
        assertNotNull(savedProfile)
        assertEquals(1, savedProfile!!.id)
        assertEquals("Carlos Santana", savedProfile.name)
        assertEquals("(11) 98765-4321", savedProfile.phone)
        assertTrue(savedProfile.isWhatsapp)
        assertEquals("01310100", savedProfile.cep) // sanitizado
        assertEquals("Avenida Paulista", savedProfile.street)
        assertEquals("1578", savedProfile.addressNumber)
        assertEquals("Bela Vista", savedProfile.neighborhood)
        assertEquals("São Paulo", savedProfile.city)
        assertEquals("SP", savedProfile.stateUf)
        assertEquals("carlos.motoboy@girocusto.com", savedProfile.email)
        assertTrue("onboardingCompleted deve ser true", savedProfile.onboardingCompleted)
        assertEquals(SyncStatus.PENDING_UPDATE, savedProfile.syncStatus)

        // Valida que o rootNavState transicionou para AuthenticatedReady
        assertEquals(RootNavState.AuthenticatedReady, viewModel.rootNavState.value)
    }

    // =========================================================================
    // 6. TESTES DE BIOMETRIA NATIVA (BIOMETRICHELPER E AUTENTICAÇÃO)
    // =========================================================================

    @Test
    fun testBiometricHelper_preferencesStorage() {
        assertFalse(BiometricHelper.isBiometricEnabled(context))
        assertNull(BiometricHelper.getBiometricEmail(context))

        BiometricHelper.setBiometricEnabled(context, true, "motoboy@girocusto.com")
        assertTrue(BiometricHelper.isBiometricEnabled(context))
        assertEquals("motoboy@girocusto.com", BiometricHelper.getBiometricEmail(context))

        BiometricHelper.setBiometricEnabled(context, false)
        assertFalse(BiometricHelper.isBiometricEnabled(context))
        assertNull(BiometricHelper.getBiometricEmail(context))

        BiometricHelper.setBiometricEnabled(context, true, "novo@girocusto.com")
        BiometricHelper.clearBiometrics(context)
        assertFalse(BiometricHelper.isBiometricEnabled(context))
        assertNull(BiometricHelper.getBiometricEmail(context))
    }

    @Test
    fun testSignInWithBiometrics_createsSessionAndTriggersSuccess() = runTest(testDispatcher) {
        val savedSession = AuthSession(
            accessToken = "token-persisted-real",
            refreshToken = "refresh-persisted-real",
            userId = "usr-persisted-real",
            userEmail = "biometric.user@girocusto.com"
        )
        supabaseClient.setAuthSession(savedSession)
        BiometricHelper.setBiometricEnabled(context, true, "biometric.user@girocusto.com")

        var successCalled = false
        var errorCalled: String? = null

        viewModel.signInWithBiometrics { success, err ->
            successCalled = success
            errorCalled = err
        }
        runCurrent()

        assertTrue(successCalled)
        assertNull(errorCalled)

        assertTrue(supabaseClient.isAuthenticated)
        assertEquals("biometric.user@girocusto.com", supabaseClient.sessionState.value?.userEmail)
        assertFalse(viewModel.isBiometricLocked.value)
        assertTrue(viewModel.authUiState.value is AuthUiState.Success)
        assertEquals(
            "Autenticado via biometria com sucesso!",
            (viewModel.authUiState.value as AuthUiState.Success).message
        )
    }

    @Test
    fun testBiometricHelper_canUnlockWithBiometrics() {
        BiometricHelper.clearBiometrics(context)
        assertFalse(BiometricHelper.canUnlockWithBiometrics(context))

        BiometricHelper.setBiometricEnabled(context, true, "motorista@girocusto.com")
        // canUnlockWithBiometrics requer tanto hardware quanto enabled
        val expected = BiometricHelper.isBiometricHardwareAvailable(context)
        assertEquals(expected, BiometricHelper.canUnlockWithBiometrics(context))

        BiometricHelper.clearBiometrics(context)
        assertFalse(BiometricHelper.canUnlockWithBiometrics(context))
    }

    @Test
    fun testSupabaseClient_savedSessionMethods() {
        supabaseClient.clearSavedSession()
        assertFalse(supabaseClient.hasSavedSession())
        assertNull(supabaseClient.getSavedSession())

        val session = AuthSession(
            accessToken = "access-123",
            refreshToken = "refresh-123",
            userId = "user-123",
            userEmail = "persist@girocusto.com"
        )
        supabaseClient.setAuthSession(session)
        assertTrue(supabaseClient.hasSavedSession())
        assertEquals(session, supabaseClient.getSavedSession())
        assertEquals("persist@girocusto.com", supabaseClient.getSavedUserEmail())

        supabaseClient.clearSavedSession()
        assertFalse(supabaseClient.hasSavedSession())
        assertNull(supabaseClient.getSavedSession())
    }

    @Test
    fun testUnlockWithBiometrics_restoresSessionAndRefreshesToken() = runTest(testDispatcher) {
        val savedSession = AuthSession(
            accessToken = "token-initial",
            refreshToken = "refresh-initial",
            userId = "usr-init",
            userEmail = "user.bio@girocusto.com"
        )
        supabaseClient.setAuthSession(savedSession)
        BiometricHelper.setBiometricEnabled(context, true, "user.bio@girocusto.com")

        // Dispara unlock
        var unlocked = false
        var errorMsg: String? = null
        viewModel.unlockWithBiometrics { ok, err ->
            unlocked = ok
            errorMsg = err
        }
        runCurrent()

        assertTrue(unlocked)
        assertNull(errorMsg)
        assertFalse(viewModel.isBiometricLocked.value)
        assertTrue(supabaseClient.isAuthenticated)
    }

    @Test
    fun testBiometricAvailabilityEnums() {
        val enums = BiometricAvailability.values()
        assertTrue(enums.contains(BiometricAvailability.AVAILABLE))
        assertTrue(enums.contains(BiometricAvailability.NO_HARDWARE))
        assertTrue(enums.contains(BiometricAvailability.HARDWARE_UNAVAILABLE))
        assertTrue(enums.contains(BiometricAvailability.NONE_ENROLLED))
        assertTrue(enums.contains(BiometricAvailability.SECURITY_UPDATE_REQUIRED))
        assertTrue(enums.contains(BiometricAvailability.UNSUPPORTED))
    }

    @Test
    fun testAuthUiState_instances() {
        val idle: AuthUiState = AuthUiState.Idle
        val loading: AuthUiState = AuthUiState.Loading
        val success = AuthUiState.Success("Pronto")
        val error = AuthUiState.Error("Falha")

        assertTrue(idle is AuthUiState.Idle)
        assertTrue(loading is AuthUiState.Loading)
        assertEquals("Pronto", success.message)
        assertEquals("Falha", error.message)
    }

    @Test
    fun testViaCepUiState_instances() {
        val idle: ViaCepUiState = ViaCepUiState.Idle
        val loading: ViaCepUiState = ViaCepUiState.Loading
        val success = ViaCepUiState.Success(ViaCepResponse(cep = "01001-000"))
        val error = ViaCepUiState.Error("Não encontrado")

        assertTrue(idle is ViaCepUiState.Idle)
        assertTrue(loading is ViaCepUiState.Loading)
        assertEquals("01001-000", success.address.cep)
        assertEquals("Não encontrado", error.message)
    }

    // =========================================================================
    // 7. CENÁRIOS ESPECÍFICOS DE BIOMETRIA VS SESSÃO PERSISTENTE (TC-BIO-01..06)
    // =========================================================================

    @Test
    fun testTC_BIO_01_initializationLockedWhenSessionExistsAndBiometricsEnabled() = runTest(testDispatcher) {
        // 1. Configura sessão prévia salva em disco (SharedPreferences)
        val savedSession = AuthSession(
            accessToken = "stored-access-token-01",
            refreshToken = "stored-refresh-token-01",
            userId = "usr-bio-01",
            userEmail = "motorista01@girocusto.com"
        )
        supabaseClient.setAuthSession(savedSession)
        assertTrue(supabaseClient.hasSavedSession())

        // 2. Configura biometria com hardware disponível e ativada pelo usuário
        BiometricHelper.testHardwareAvailable = true
        BiometricHelper.setBiometricEnabled(context, true, "motorista01@girocusto.com")
        assertTrue(BiometricHelper.canUnlockWithBiometrics(context))

        // 3. Simula cold start / abertura do app instanciando um novo ViewModel
        val app = ApplicationProvider.getApplicationContext<Application>()
        val coldStartViewModel = GiroCustoViewModel(app)

        runCurrent()

        // 4. Valida que o aplicativo inicia bloqueado
        assertTrue(coldStartViewModel.isBiometricLocked.value)
        assertEquals(RootNavState.BiometricLocked, coldStartViewModel.rootNavState.value)
        assertTrue(supabaseClient.hasSavedSession())
    }

    @Test
    fun testTC_BIO_02_unlockWithBiometricsRestoresRealSessionAndTransitionsToAuthenticatedReady() = runTest(testDispatcher) {
        // 1. Configura sessão prévia real e persistida
        val realSession = AuthSession(
            accessToken = "real-access-token-tc02",
            refreshToken = "real-refresh-token-tc02",
            userId = "usr-tc02-real",
            userEmail = "carlos.tc02@girocusto.com"
        )
        supabaseClient.setAuthSession(realSession)
        BiometricHelper.testHardwareAvailable = true
        BiometricHelper.setBiometricEnabled(context, true, "carlos.tc02@girocusto.com")

        // Perfil completo no Room
        repository.saveUserProfile(
            UserProfile(id = 1, name = "Carlos Real", phone = "11999990000", city = "SP", onboardingCompleted = true)
        )

        var capturedRefreshToken: String? = null
        supabaseClient.testAuthApi = object : com.example.network.supabase.SupabaseAuthApi {
            override suspend fun signUp(request: com.example.network.supabase.AuthRequest) = throw NotImplementedError()
            override suspend fun signInWithPassword(grantType: String, request: com.example.network.supabase.AuthRequest) = throw NotImplementedError()
            override suspend fun refreshToken(grantType: String, request: com.example.network.supabase.RefreshTokenRequest): Response<com.example.network.supabase.AuthResponse> {
                capturedRefreshToken = request.refreshToken
                return Response.success(com.example.network.supabase.AuthResponse(
                    accessToken = "real-access-token-tc02",
                    refreshToken = "real-refresh-token-tc02",
                    user = com.example.network.supabase.SupabaseUser("usr-tc02-real", "carlos.tc02@girocusto.com")
                ))
            }
            override suspend fun logout() = Response.success(Unit)
            override suspend fun getUser() = Response.success(com.example.network.supabase.SupabaseUser("usr-tc02-real", "carlos.tc02@girocusto.com"))
        }

        // Simula bloqueio inicial com nova instância
        val app = ApplicationProvider.getApplicationContext<Application>()
        val bioViewModel = GiroCustoViewModel(app)
        runCurrent()
        assertTrue(bioViewModel.isBiometricLocked.value)
        assertEquals(RootNavState.BiometricLocked, bioViewModel.rootNavState.value)

        // 2. Invoca unlockWithBiometrics()
        var unlocked = false
        var errorMsg: String? = null
        bioViewModel.unlockWithBiometrics { ok, err ->
            unlocked = ok
            errorMsg = err
        }
        runCurrent()

        // 3. Valida restauração fiel dos tokens reais e transição de estado
        assertTrue(unlocked)
        assertNull(errorMsg)
        assertFalse(bioViewModel.isBiometricLocked.value)
        assertEquals("real-refresh-token-tc02", capturedRefreshToken)

        // Tokens reais restaurados do disco
        assertEquals("real-access-token-tc02", supabaseClient.currentAccessToken)
        assertEquals("usr-tc02-real", supabaseClient.currentUserId)
        assertEquals("real-refresh-token-tc02", supabaseClient.sessionState.value?.refreshToken)
        assertEquals("carlos.tc02@girocusto.com", supabaseClient.sessionState.value?.userEmail)

        // Transição imediata para AuthenticatedReady
        assertEquals(RootNavState.AuthenticatedReady, bioViewModel.rootNavState.value)
    }

    @Test
    fun testTC_BIO_03_eliminationOfFakeTokens_strictRealPayloadValidation() = runTest(testDispatcher) {
        BiometricHelper.testHardwareAvailable = true
        BiometricHelper.clearBiometrics(context)
        supabaseClient.clearSavedSession()

        // 1. Tentar desbloqueio biométrico sem nenhuma sessão prévia no disco
        var unlockSuccess = false
        var unlockError: String? = null
        viewModel.unlockWithBiometrics { ok, err ->
            unlockSuccess = ok
            unlockError = err
        }
        runCurrent()

        // Valida que NÃO gera fake token nem autentica artificialmente
        assertFalse(unlockSuccess)
        assertNotNull(unlockError)
        assertNull(supabaseClient.currentAccessToken)
        assertNull(supabaseClient.currentUserId)
        assertFalse(supabaseClient.isAuthenticated)
        assertFalse(supabaseClient.hasSavedSession())
        assertNotEquals(RootNavState.AuthenticatedReady, viewModel.rootNavState.value)

        // 2. Realiza login com credenciais reais da API
        val expectedAccessToken = "real-api-access-token-9988"
        val expectedRefreshToken = "real-api-refresh-token-9988"
        val expectedUserId = "real-uuid-9988"
        val expectedEmail = "driver9988@girocusto.com"

        supabaseClient.testAuthApi = object : com.example.network.supabase.SupabaseAuthApi {
            override suspend fun signUp(request: com.example.network.supabase.AuthRequest) = Response.error<com.example.network.supabase.AuthResponse>(400, "error".toResponseBody())
            override suspend fun signInWithPassword(grantType: String, request: com.example.network.supabase.AuthRequest) =
                Response.success(com.example.network.supabase.AuthResponse(
                    accessToken = expectedAccessToken,
                    refreshToken = expectedRefreshToken,
                    user = com.example.network.supabase.SupabaseUser(expectedUserId, expectedEmail)
                ))
            override suspend fun refreshToken(grantType: String, request: com.example.network.supabase.RefreshTokenRequest) = Response.error<com.example.network.supabase.AuthResponse>(400, "error".toResponseBody())
            override suspend fun logout() = Response.success(Unit)
            override suspend fun getUser() = Response.success(com.example.network.supabase.SupabaseUser(expectedUserId, expectedEmail))
        }

        viewModel.signInWithEmail(expectedEmail, "password123")
        runCurrent()

        // 3. Validação rigorosa: nenhum token gravado contém prefixo 'biometric-token-' ou 'fake-'
        val currentAccess = supabaseClient.currentAccessToken
        val currentRefresh = supabaseClient.sessionState.value?.refreshToken
        val savedSession = supabaseClient.getSavedSession()

        assertNotNull(currentAccess)
        assertNotNull(currentRefresh)
        assertFalse("Token não pode conter prefixo fake", currentAccess!!.startsWith("biometric-token-"))
        assertFalse("Refresh token não pode conter prefixo fake", currentRefresh!!.startsWith("biometric-token-"))
        assertFalse("Token não pode conter prefixo fake", currentAccess.startsWith("fake-"))
        assertFalse("Refresh token não pode conter prefixo fake", currentRefresh.startsWith("fake-"))

        assertEquals(expectedAccessToken, currentAccess)
        assertEquals(expectedRefreshToken, currentRefresh)
        assertEquals(expectedAccessToken, savedSession?.accessToken)
        assertEquals(expectedRefreshToken, savedSession?.refreshToken)
        assertEquals(expectedUserId, savedSession?.userId)
        assertEquals(expectedEmail, savedSession?.userEmail)
    }

    @Test
    fun testTC_BIO_04_lockAppForBiometricsPreservesSessionTokensOnDisk() = runTest(testDispatcher) {
        val originalSession = AuthSession(
            accessToken = "stable-access-token-04",
            refreshToken = "stable-refresh-token-04",
            userId = "usr-stable-04",
            userEmail = "stable@girocusto.com"
        )
        supabaseClient.setAuthSession(originalSession)
        BiometricHelper.testHardwareAvailable = true
        BiometricHelper.setBiometricEnabled(context, true, "stable@girocusto.com")
        repository.saveUserProfile(
            UserProfile(id = 1, name = "Stable User", phone = "11988887777", city = "SP", onboardingCompleted = true)
        )
        runCurrent()

        // Desbloqueado inicialmente
        assertFalse(viewModel.isBiometricLocked.value)
        assertEquals(RootNavState.AuthenticatedReady, viewModel.rootNavState.value)

        // Executa lockAppForBiometrics()
        viewModel.lockAppForBiometrics()
        runCurrent()

        // Valida que o app foi bloqueado
        assertTrue(viewModel.isBiometricLocked.value)
        assertEquals(RootNavState.BiometricLocked, viewModel.rootNavState.value)

        // Valida que os tokens no disco e na sessão NUNCA foram apagados nem alterados
        assertTrue(supabaseClient.hasSavedSession())
        val persisted = supabaseClient.getSavedSession()
        assertNotNull(persisted)
        assertEquals("stable-access-token-04", persisted?.accessToken)
        assertEquals("stable-refresh-token-04", persisted?.refreshToken)
        assertEquals("usr-stable-04", persisted?.userId)
        assertEquals("stable@girocusto.com", persisted?.userEmail)
        assertEquals("stable-access-token-04", supabaseClient.currentAccessToken)
    }

    @Test
    fun testTC_BIO_05_logoutDestroysSessionOnMemoryAndDiskAndDisablesBiometrics() = runTest(testDispatcher) {
        val session = AuthSession(
            accessToken = "token-to-destroy",
            refreshToken = "refresh-to-destroy",
            userId = "usr-to-destroy",
            userEmail = "destroy@girocusto.com"
        )
        supabaseClient.setAuthSession(session)
        BiometricHelper.testHardwareAvailable = true
        BiometricHelper.setBiometricEnabled(context, true, "destroy@girocusto.com")
        repository.saveUserProfile(
            UserProfile(id = 1, name = "Destructive", phone = "11999991111", city = "SP", onboardingCompleted = true)
        )
        runCurrent()

        // Pré-condição: logado e biometria ativa
        assertTrue(supabaseClient.isAuthenticated)
        assertTrue(BiometricHelper.isBiometricEnabled(context))

        // Executa logout
        viewModel.logout()
        advanceUntilIdle()

        // Valida destruição total em memória e em disco
        assertNull(supabaseClient.currentUserId)
        assertNull(supabaseClient.currentAccessToken)
        assertFalse(supabaseClient.isAuthenticated)
        assertFalse(supabaseClient.hasSavedSession())
        assertNull(supabaseClient.getSavedSession())

        // Valida desativação da biometria
        assertFalse(BiometricHelper.isBiometricEnabled(context))
        assertNull(BiometricHelper.getBiometricEmail(context))
        assertFalse(viewModel.isBiometricLocked.value)

        // Valida transição imediata para Unauthenticated
        assertEquals(RootNavState.Unauthenticated, viewModel.rootNavState.value)
        assertEquals(AuthUiState.Idle, viewModel.authUiState.value)
    }

    @Test
    fun testTC_BIO_06_refreshCurrentSession_successAndFailureResilience() = runTest(testDispatcher) {
        val initialSession = AuthSession(
            accessToken = "access-token-v1",
            refreshToken = "refresh-token-v1",
            userId = "usr-refresh-06",
            userEmail = "refresh@girocusto.com"
        )
        supabaseClient.setAuthSession(initialSession)

        // Cenário 1: Sucesso na renovação do token
        supabaseClient.testAuthApi = object : com.example.network.supabase.SupabaseAuthApi {
            override suspend fun signUp(request: com.example.network.supabase.AuthRequest) = throw NotImplementedError()
            override suspend fun signInWithPassword(grantType: String, request: com.example.network.supabase.AuthRequest) = throw NotImplementedError()
            override suspend fun refreshToken(grantType: String, request: com.example.network.supabase.RefreshTokenRequest): Response<com.example.network.supabase.AuthResponse> {
                assertEquals("refresh-token-v1", request.refreshToken)
                return Response.success(com.example.network.supabase.AuthResponse(
                    accessToken = "access-token-v2-renewed",
                    refreshToken = "refresh-token-v2-renewed",
                    user = com.example.network.supabase.SupabaseUser("usr-refresh-06", "refresh@girocusto.com")
                ))
            }
            override suspend fun logout() = Response.success(Unit)
            override suspend fun getUser() = Response.success(com.example.network.supabase.SupabaseUser("usr-refresh-06", "refresh@girocusto.com"))
        }

        val refreshResult = supabaseClient.refreshCurrentSession()
        assertTrue(refreshResult.isSuccess)
        val newSession = refreshResult.getOrNull()
        assertNotNull(newSession)
        assertEquals("access-token-v2-renewed", newSession?.accessToken)
        assertEquals("refresh-token-v2-renewed", newSession?.refreshToken)
        assertEquals("access-token-v2-renewed", supabaseClient.currentAccessToken)
        assertEquals("access-token-v2-renewed", supabaseClient.getSavedSession()?.accessToken)

        // Cenário 2: Falha na renovação (erro 500 do servidor ou queda de conexão)
        supabaseClient.testAuthApi = object : com.example.network.supabase.SupabaseAuthApi {
            override suspend fun signUp(request: com.example.network.supabase.AuthRequest) = throw NotImplementedError()
            override suspend fun signInWithPassword(grantType: String, request: com.example.network.supabase.AuthRequest) = throw NotImplementedError()
            override suspend fun refreshToken(grantType: String, request: com.example.network.supabase.RefreshTokenRequest): Response<com.example.network.supabase.AuthResponse> {
                return Response.error(500, "Server Error".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
            override suspend fun logout() = Response.success(Unit)
            override suspend fun getUser() = throw NotImplementedError()
        }

        val failureResult = supabaseClient.refreshCurrentSession()
        assertTrue("Deve retornar failure", failureResult.isFailure)

        // Sessão local e tokens salvos no disco NÃO DEVEM ser corrompidos nem apagados
        assertTrue(supabaseClient.isAuthenticated)
        assertEquals("access-token-v2-renewed", supabaseClient.currentAccessToken)
        assertEquals("refresh-token-v2-renewed", supabaseClient.sessionState.value?.refreshToken)
        val stillPersisted = supabaseClient.getSavedSession()
        assertNotNull(stillPersisted)
        assertEquals("access-token-v2-renewed", stillPersisted?.accessToken)
        assertEquals("refresh-token-v2-renewed", stillPersisted?.refreshToken)

        // Cenário 3: Tentativa de renovação sem refresh token
        supabaseClient.clearSavedSession()
        val emptyResult = supabaseClient.refreshCurrentSession()
        assertTrue(emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun testTC_AUTH_googleSignInAndSignUpMockOrOAuth() = runTest(testDispatcher) {
        var signInSuccess: Boolean? = null
        var signInErrorMsg: String? = null
        viewModel.signInWithGoogleMockOrOAuth { success, msg ->
            signInSuccess = success
            signInErrorMsg = msg
        }
        advanceUntilIdle()
        assertFalse(signInSuccess == true)
        assertEquals("Login com Google em homologação.", signInErrorMsg)
        assertTrue(viewModel.authUiState.value is AuthUiState.Error)
        assertEquals("Login com Google em homologação.", (viewModel.authUiState.value as AuthUiState.Error).message)

        var signUpSuccess: Boolean? = null
        var signUpErrorMsg: String? = null
        viewModel.signUpWithGoogleMockOrOAuth { success, msg ->
            signUpSuccess = success
            signUpErrorMsg = msg
        }
        advanceUntilIdle()
        assertFalse(signUpSuccess == true)
        assertEquals("Cadastro com Google em homologação.", signUpErrorMsg)
        assertTrue(viewModel.authUiState.value is AuthUiState.Error)
        assertEquals("Cadastro com Google em homologação.", (viewModel.authUiState.value as AuthUiState.Error).message)
    }

    @Test
    fun testTC_AUTH_googleSignInAndSignUpWithTokenAndDefaultCallback() = runTest(testDispatcher) {
        // Test Google Sign-In with idToken and default callback
        viewModel.signInWithGoogleMockOrOAuth(idToken = "sample-google-id-token-123")
        advanceUntilIdle()
        assertTrue(viewModel.authUiState.value is AuthUiState.Error)
        assertEquals("Login com Google em homologação.", (viewModel.authUiState.value as AuthUiState.Error).message)

        // Test Google Sign-Up with idToken and default callback
        viewModel.signUpWithGoogleMockOrOAuth(idToken = "sample-google-id-token-456")
        advanceUntilIdle()
        assertTrue(viewModel.authUiState.value is AuthUiState.Error)
        assertEquals("Cadastro com Google em homologação.", (viewModel.authUiState.value as AuthUiState.Error).message)
    }

    @Test
    fun testTC_AUTH_signInWithPhone() = runTest(testDispatcher) {
        var phoneSuccess: Boolean? = null
        var phoneErrorMsg: String? = null
        viewModel.signInWithPhone(phone = "+5511999999999", otpCode = "123456") { success, msg ->
            phoneSuccess = success
            phoneErrorMsg = msg
        }
        advanceUntilIdle()
        assertFalse(phoneSuccess == true)
        assertEquals("Login por SMS em homologação.", phoneErrorMsg)
        assertTrue(viewModel.authUiState.value is AuthUiState.Error)
        assertEquals("Login por SMS em homologação.", (viewModel.authUiState.value as AuthUiState.Error).message)

        // Default callback
        viewModel.signInWithPhone(phone = "+5511988888888")
        advanceUntilIdle()
        assertTrue(viewModel.authUiState.value is AuthUiState.Error)
        assertEquals("Login por SMS em homologação.", (viewModel.authUiState.value as AuthUiState.Error).message)
    }
}

