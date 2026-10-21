package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GiroCustoViewModel
import com.example.ui.ViaCepUiState
import com.example.util.BrazilianCepVisualTransformation
import com.example.util.BrazilianPhoneVisualTransformation
import kotlinx.coroutines.delay

// Design Tokens
private val ColorDarkBg = Color(0xFF121214)
private val ColorSurfaceCard = Color(0xFF1E1E22)
private val ColorSurfaceElevated = Color(0xFF24242A)
private val ColorSurfaceInput = Color(0xFF2A2A30)
private val ColorBorder = Color(0xFF2D2D34)
private val ColorBrandEmerald = Color(0xFF10B981)
private val ColorEmeraldBadgeBg = Color(0xFF064E3B)
private val ColorEmeraldBadgeText = Color(0xFF34D399)
private val ColorOnPrimary = Color(0xFF064E3B)
private val ColorTextPrimary = Color(0xFFF1F5F9)
private val ColorTextMuted = Color(0xFF94A3B8)
private val ColorError = Color(0xFFF43F5E)
private val ColorErrorBg = Color(0xFF4C1D24)

@Composable
fun OnboardingProfileScreen(
    onOnboardingFinished: () -> Unit = {},
    viewModel: GiroCustoViewModel = viewModel()
) {
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
            Spacer(modifier = Modifier.height(4.dp))

            // ==========================================
            // HEADER DE PROGRESSO & BOAS-VINDAS
            // ==========================================
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorEmeraldBadgeBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "PASSO 1 DE 1 • CONFIGURAÇÃO INICIAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorEmeraldBadgeText,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "Complete seu Cadastro",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary
            )

            Text(
                text = "Esses dados garantem o cálculo regional exato de combustível e personalizam seus relatórios de rendimento.",
                fontSize = 13.sp,
                color = ColorTextMuted,
                lineHeight = 18.sp
            )

            // Mensagem de erro de validação geral
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorErrorBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ColorError)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = ColorError,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = errorMessage.orEmpty(),
                            color = ColorError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ==========================================
            // SEÇÃO 1: DADOS PESSOAIS & CONTATO
            // ==========================================
            Text(
                text = "1. Dados Pessoais & Contato",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorBrandEmerald
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = { Text("Nome Completo") },
                        placeholder = { Text("ex: Carlos Silva", color = ColorTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = ColorTextMuted
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorSurfaceInput,
                            unfocusedContainerColor = ColorSurfaceInput,
                            focusedBorderColor = ColorBrandEmerald,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedLabelColor = ColorBrandEmerald,
                            unfocusedLabelColor = ColorTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 11) {
                                phone = digits
                                errorMessage = null
                            }
                        },
                        label = { Text("Telefone de Contato") },
                        placeholder = { Text("(11) 98765-4321", color = ColorTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = ColorTextMuted
                            )
                        },
                        visualTransformation = BrazilianPhoneVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorSurfaceInput,
                            unfocusedContainerColor = ColorSurfaceInput,
                            focusedBorderColor = ColorBrandEmerald,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedLabelColor = ColorBrandEmerald,
                            unfocusedLabelColor = ColorTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_phone_input")
                    )

                    // CARD INTEGRADO WHATSAPP
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ColorSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ColorBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = "WhatsApp",
                                    tint = ColorBrandEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Este número é WhatsApp?",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ColorTextPrimary
                                    )
                                    Text(
                                        text = "Para relatórios diários e alertas de manutenção",
                                        fontSize = 11.sp,
                                        color = ColorTextMuted
                                    )
                                }
                            }

                            Switch(
                                checked = isWhatsapp,
                                onCheckedChange = { isWhatsapp = it },
                                modifier = Modifier.testTag("onboarding_whatsapp_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ColorBrandEmerald,
                                    uncheckedThumbColor = ColorTextMuted,
                                    uncheckedTrackColor = ColorSurfaceInput
                                )
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SEÇÃO 2: LOCALIZAÇÃO & ENDEREÇO (VIACEP)
            // ==========================================
            Text(
                text = "2. Localização & Endereço",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorBrandEmerald
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = cep,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 8) {
                                cep = digits
                                errorMessage = null
                            }
                        },
                        label = { Text("CEP Residencial") },
                        placeholder = { Text("01001-000", color = ColorTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.PinDrop,
                                contentDescription = null,
                                tint = ColorTextMuted
                            )
                        },
                        trailingIcon = {
                            when (viaCepState) {
                                is ViaCepUiState.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .testTag("onboarding_cep_search_icon"),
                                        strokeWidth = 2.dp,
                                        color = ColorBrandEmerald
                                    )
                                }
                                is ViaCepUiState.Success -> {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "CEP Localizado",
                                        tint = ColorBrandEmerald,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .testTag("onboarding_cep_search_icon")
                                    )
                                }
                                is ViaCepUiState.Error -> {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Erro no CEP",
                                        tint = ColorError,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .testTag("onboarding_cep_search_icon")
                                    )
                                }
                                else -> {
                                    IconButton(
                                        onClick = { viewModel.searchCep(cep) },
                                        modifier = Modifier.testTag("onboarding_cep_search_icon")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = "Buscar CEP",
                                            tint = ColorTextMuted
                                        )
                                    }
                                }
                            }
                        },
                        visualTransformation = BrazilianCepVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorSurfaceInput,
                            unfocusedContainerColor = ColorSurfaceInput,
                            focusedBorderColor = ColorBrandEmerald,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedLabelColor = ColorBrandEmerald,
                            unfocusedLabelColor = ColorTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_cep_input")
                    )

                    // AÇÃO: NÃO SEI MEU CEP / PREENCHER MANUAL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.clearViaCepState()
                                try {
                                    numberFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.testTag("onboarding_manual_cep_toggle")
                        ) {
                            Text(
                                text = "Não sei meu CEP / Preencher manual",
                                fontSize = 12.sp,
                                color = ColorBrandEmerald
                            )
                        }
                    }

                    // BANNER DE FEEDBACK DO CEP
                    AnimatedVisibility(
                        visible = viaCepState is ViaCepUiState.Success || viaCepState is ViaCepUiState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        when (val state = viaCepState) {
                            is ViaCepUiState.Success -> {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_cep_feedback_banner"),
                                    color = ColorEmeraldBadgeBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ColorBrandEmerald)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = ColorBrandEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Endereço localizado e preenchido automaticamente!",
                                            color = ColorEmeraldBadgeText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            is ViaCepUiState.Error -> {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_cep_feedback_banner"),
                                    color = ColorErrorBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ColorError)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = ColorError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = state.message,
                                            color = ColorError,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    // CAMPOS DE ENDEREÇO
                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text("Rua / Avenida") },
                        placeholder = { Text("ex: Av. Paulista", color = ColorTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Signpost,
                                contentDescription = null,
                                tint = ColorTextMuted
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorSurfaceInput,
                            unfocusedContainerColor = ColorSurfaceInput,
                            focusedBorderColor = ColorBrandEmerald,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedLabelColor = ColorBrandEmerald,
                            unfocusedLabelColor = ColorTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_street_input")
                    )

                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Bairro") },
                        placeholder = { Text("ex: Bela Vista", color = ColorTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.HomeWork,
                                contentDescription = null,
                                tint = ColorTextMuted
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorSurfaceInput,
                            unfocusedContainerColor = ColorSurfaceInput,
                            focusedBorderColor = ColorBrandEmerald,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedLabelColor = ColorBrandEmerald,
                            unfocusedLabelColor = ColorTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_neighborhood_input")
                    )

                    OutlinedTextField(
                        value = addressNumber,
                        onValueChange = { addressNumber = it },
                        label = { Text("Número e Complemento") },
                        placeholder = { Text("ex: 1000, Apto 42", color = ColorTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Numbers,
                                contentDescription = null,
                                tint = ColorTextMuted
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorSurfaceInput,
                            unfocusedContainerColor = ColorSurfaceInput,
                            focusedBorderColor = ColorBrandEmerald,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedLabelColor = ColorBrandEmerald,
                            unfocusedLabelColor = ColorTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(numberFocusRequester)
                            .testTag("onboarding_number_input")
                    )

                    // LINHA CIDADE & UF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Cidade") },
                            placeholder = { Text("São Paulo", color = ColorTextMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorSurfaceInput,
                                unfocusedContainerColor = ColorSurfaceInput,
                                focusedBorderColor = ColorBrandEmerald,
                                unfocusedBorderColor = ColorBorder,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary,
                                focusedLabelColor = ColorBrandEmerald,
                                unfocusedLabelColor = ColorTextMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.72f)
                                .testTag("onboarding_city_input")
                        )

                        OutlinedTextField(
                            value = stateUf,
                            onValueChange = { input ->
                                if (input.length <= 2) {
                                    stateUf = input.uppercase()
                                }
                            },
                            label = { Text("UF") },
                            placeholder = { Text("SP", color = ColorTextMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorSurfaceInput,
                                unfocusedContainerColor = ColorSurfaceInput,
                                focusedBorderColor = ColorBrandEmerald,
                                unfocusedBorderColor = ColorBorder,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary,
                                focusedLabelColor = ColorBrandEmerald,
                                unfocusedLabelColor = ColorTextMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.28f)
                                .testTag("onboarding_uf_input")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // BOTÃO FINAL DE AÇÃO (CTA)
            // ==========================================
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "O nome completo é obrigatório."
                        return@Button
                    }
                    if (phone.isBlank()) {
                        errorMessage = "O telefone para contato é obrigatório."
                        return@Button
                    }
                    if (cep.length != 8) {
                        errorMessage = "Informe um CEP válido com 8 dígitos."
                        return@Button
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
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_submit_button")
                    .semantics { testTag = "onboarding_finish_btn" },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorBrandEmerald,
                    contentColor = ColorOnPrimary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ColorOnPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Salvar e Acessar o GiroCusto",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
