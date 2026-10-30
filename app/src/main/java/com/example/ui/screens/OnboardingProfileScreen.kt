package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.GiroCustoViewModel
import com.example.ui.ViaCepUiState
import com.example.ui.screens.onboarding.*
import kotlinx.coroutines.delay

@Composable
fun OnboardingProfileScreen(
    onOnboardingFinished: () -> Unit = {},
    viewModel: GiroCustoViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val viaCepState by viewModel.viaCepState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Estados do Formulário
    var name by remember { mutableStateOf(userProfile?.name.orEmpty()) }
    var phone by remember { mutableStateOf(userProfile?.phone.orEmpty().filter { it.isDigit() }) }
    var isWhatsapp by remember { mutableStateOf(userProfile?.isWhatsapp ?: true) }

    var cep by remember { mutableStateOf(userProfile?.cep.orEmpty().filter { it.isDigit() }) }
    var street by remember { mutableStateOf(userProfile?.street.orEmpty()) }
    var neighborhood by remember { mutableStateOf(userProfile?.neighborhood.orEmpty()) }
    var addressNumber by remember { mutableStateOf(userProfile?.addressNumber.orEmpty()) }
    var city by remember { mutableStateOf(userProfile?.city.orEmpty()) }
    var stateUf by remember { mutableStateOf(userProfile?.stateUf.orEmpty()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val numberFocusRequester = remember { FocusRequester() }

    // Reage ao retorno do ViaCEP para autopreenchimento e foco inteligente
    LaunchedEffect(viaCepState) {
        when (val state = viaCepState) {
            is ViaCepUiState.Success -> {
                val addr = state.address
                if (!addr.logradouro.isNullOrBlank()) street = addr.logradouro
                if (!addr.bairro.isNullOrBlank()) neighborhood = addr.bairro
                if (!addr.localidade.isNullOrBlank()) city = addr.localidade
                if (!addr.uf.isNullOrBlank()) stateUf = addr.uf.uppercase()

                // Transfere foco automaticamente para o número
                delay(100)
                try {
                    numberFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
            else -> {}
        }
    }

    // Disparo automático da busca de CEP quando atingir 8 dígitos
    LaunchedEffect(cep) {
        val clean = cep.filter { it.isDigit() }
        if (clean.length == 8) {
            viewModel.searchCep(clean)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header com Badge, Título, Subtítulo e Alerta de Erro
            OnboardingHeaderSection(errorMessage = errorMessage)

            // Seção 1: Dados Pessoais & Contato
            OnboardingPersonalInfoCard(
                name = name,
                onNameChange = {
                    name = it
                    errorMessage = null
                },
                phone = phone,
                onPhoneChange = {
                    phone = it
                    errorMessage = null
                },
                isWhatsapp = isWhatsapp,
                onWhatsappChange = { isWhatsapp = it }
            )

            // Seção 2: Localização & Endereço (ViaCEP)
            OnboardingAddressCard(
                cep = cep,
                onCepChange = {
                    cep = it
                    errorMessage = null
                },
                viaCepState = viaCepState,
                onSearchCep = { viewModel.searchCep(it) },
                onManualCepClick = {
                    viewModel.clearViaCepState()
                    try {
                        numberFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                },
                street = street,
                onStreetChange = { street = it },
                neighborhood = neighborhood,
                onNeighborhoodChange = { neighborhood = it },
                addressNumber = addressNumber,
                onAddressNumberChange = { addressNumber = it },
                city = city,
                onCityChange = { city = it },
                stateUf = stateUf,
                onStateUfChange = { stateUf = it },
                numberFocusRequester = numberFocusRequester
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botão Final de Ação (CTA)
            OnboardingSubmitButton(
                isSubmitting = isSubmitting,
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = context.getString(R.string.onboarding_error_name_required)
                        return@OnboardingSubmitButton
                    }
                    if (phone.isBlank()) {
                        errorMessage = context.getString(R.string.onboarding_error_phone_required)
                        return@OnboardingSubmitButton
                    }
                    if (cep.length != 8) {
                        errorMessage = context.getString(R.string.onboarding_error_cep_invalid)
                        return@OnboardingSubmitButton
                    }

                    isSubmitting = true
                    errorMessage = null
                    viewModel.completeOnboarding(
                        name = name,
                        phone = phone,
                        isWhatsapp = isWhatsapp,
                        cep = cep,
                        street = street,
                        addressNumber = addressNumber,
                        neighborhood = neighborhood,
                        city = city,
                        stateUf = stateUf,
                        onSuccess = {
                            isSubmitting = false
                            onOnboardingFinished()
                        },
                        onError = { err ->
                            isSubmitting = false
                            errorMessage = err
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
