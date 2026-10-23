package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthUiState
import com.example.ui.GiroCustoViewModel
import com.example.util.BiometricHelper
import com.example.util.BrazilianPhoneVisualTransformation

// Design Tokens
private val ColorDarkBg = Color(0xFF121214)
private val ColorSurfaceCard = Color(0xFF1E1E22)
private val ColorSurfaceInput = Color(0xFF2A2A30)
private val ColorBorder = Color(0xFF2D2D34)
private val ColorBrandEmerald = Color(0xFF10B981)
private val ColorEmeraldDeep = Color(0xFF059669)
private val ColorOnPrimary = Color(0xFF064E3B)
private val ColorTextPrimary = Color(0xFFF1F5F9)
private val ColorTextMuted = Color(0xFF94A3B8)
private val ColorError = Color(0xFFF43F5E)
private val ColorGoogleBlue = Color(0xFF3B82F6)

@Composable
fun AuthGateScreen(
    onAuthSuccess: () -> Unit = {},
    viewModel: GiroCustoViewModel = viewModel(),
    isBiometricLocked: Boolean = false,
    isBiometricSupported: Boolean = false,
    onTriggerBiometric: ((onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onGoogleSignInClick: () -> Unit = {},
    onGoogleSignUpClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val isBiometricAvailableState by viewModel.isBiometricAvailable.collectAsStateWithLifecycle()
    val canAuthenticateBiometric = isBiometricAvailableState || isBiometricSupported

    var hasAutoTriggeredBiometric by rememberSaveable { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Email, 1: Celular
    var isRegisterMode by remember { mutableStateOf(false) }

    // Campos de E-mail
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Campo de Celular
    var phone by remember { mutableStateOf("") }

    val isLoading = authState is AuthUiState.Loading

    // Função utilitária para acionar biometria nativa
    val triggerBiometricAuth: () -> Unit = {
        if (onTriggerBiometric != null) {
            onTriggerBiometric(
                { viewModel.unlockWithBiometrics { ok, _ -> if (ok) onAuthSuccess() } },
                { /* Erro tratado no callback */ }
            )
        } else {
            val activity = context as? FragmentActivity
            if (activity != null) {
                val switchMethodText = context.getString(R.string.auth_biometric_switch_method)
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = "Desbloqueio GiroCusto",
                    subtitle = "Confirme sua digital ou reconhecimento facial",
                    negativeButtonText = switchMethodText,
                    onSuccess = {
                        viewModel.unlockWithBiometrics { ok, _ ->
                            if (ok) onAuthSuccess()
                        }
                    },
                    onError = { errorCode, _ ->
                        if (errorCode == BiometricHelper.ERROR_KEY_INVALIDATED) {
                            viewModel.handleBiometricKeyInvalidated()
                        }
                    },
                    onFailed = { }
                )
            } else {
                // Modo fallback para emulador ou preview sem FragmentActivity
                viewModel.unlockWithBiometrics { ok, _ ->
                    if (ok) onAuthSuccess()
                }
            }
        }
    }

    LaunchedEffect(isBiometricLocked) {
        if (isBiometricLocked) {
            if (!hasAutoTriggeredBiometric) {
                hasAutoTriggeredBiometric = true
                triggerBiometricAuth()
            }
        } else {
            hasAutoTriggeredBiometric = false
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
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // HEADER INSTITUCIONAL (BRANDING)
            // ==========================================
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ColorBrandEmerald, ColorEmeraldDeep)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = "GiroCusto Logo",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = "GiroCusto",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "Controle financeiro inteligente na palma da sua mão",
                fontSize = 13.sp,
                color = ColorTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // ==========================================
            // BANNER DE ERRO REATIVO
            // ==========================================
            AnimatedVisibility(
                visible = authState is AuthUiState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val errorMsg = (authState as? AuthUiState.Error)?.message.orEmpty()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_error_banner"),
                    color = Color(0xFF4C1D24),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorError)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Erro de Autenticação",
                            tint = ColorError,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMsg,
                            color = ColorError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isBiometricLocked) {
                // ==========================================
                // TELA LIMPA DE DESBLOQUEIO COM BIOMETRIA
                // ==========================================
                val savedEmail = remember(context) {
                    BiometricHelper.getBiometricEmail(context)
                        ?: viewModel.currentUser.value?.userEmail
                        ?: "Motorista"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_biometric_locked_card"),
                    colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ColorBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(ColorBrandEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Bloqueio Biométrico",
                                tint = ColorBrandEmerald,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Olá, $savedEmail",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Confirme sua digital ou biometria facial para acessar o GiroCusto com total segurança.",
                            fontSize = 13.sp,
                            color = ColorTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        if (BiometricHelper.getProtectionLevel(context) == com.example.util.BiometricProtectionLevel.WEAK) {
                            Text(
                                text = stringResource(R.string.auth_biometric_weak_notice),
                                fontSize = 12.sp,
                                color = ColorBrandEmerald,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botão largo de digital "Desbloquear com Biometria"
                        Button(
                            onClick = { triggerBiometricAuth() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("auth_biometric_button")
                                .semantics { testTag = "auth_gate_biometric_btn" },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorBrandEmerald,
                                contentColor = ColorOnPrimary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("auth_loading_indicator"),
                                    color = ColorOnPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = "Digital",
                                        modifier = Modifier.size(26.dp),
                                        tint = ColorOnPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Desbloquear com Biometria",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Botão secundário "Entrar com Google, e-mail ou senha"
                        TextButton(
                            onClick = { viewModel.dismissBiometricLockForFallback() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_switch_account_btn")
                        ) {
                            Text(
                                text = stringResource(R.string.auth_biometric_switch_method),
                                color = ColorBrandEmerald,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {

            // ==========================================
            // ABAS SEGMENTADAS (EMAIL / CELULAR)
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = ColorSurfaceCard,
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = ColorBrandEmerald,
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTabIndex == 0) ColorBrandEmerald else Color.Transparent)
                            .testTag("auth_tab_email"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTabIndex == 0) ColorOnPrimary else ColorTextMuted
                                )
                                Text(
                                    text = "E-mail",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == 0) ColorOnPrimary else ColorTextMuted
                                )
                            }
                        }
                    )

                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTabIndex == 1) ColorBrandEmerald else Color.Transparent)
                            .testTag("auth_tab_phone"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTabIndex == 1) ColorOnPrimary else ColorTextMuted
                                )
                                Text(
                                    text = stringResource(R.string.auth_tab_phone),
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == 1) ColorOnPrimary else ColorTextMuted
                                )
                            }
                        }
                    )
                }
            }

            // ==========================================
            // CARD DE FORMULÁRIO DE ENTRADA
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTabIndex == 0) {
                        // ----------------------------------------------------
                        // ABA 1: E-MAIL & SENHA
                        // ----------------------------------------------------
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail profissional") },
                            placeholder = { Text("exemplo@email.com", color = ColorTextMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.AlternateEmail,
                                    contentDescription = null,
                                    tint = ColorTextMuted
                                )
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
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
                                .testTag("auth_email_input")
                                .semantics { testTag = "auth_gate_email_input" }
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            placeholder = { Text("••••••••", color = ColorTextMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = ColorTextMuted
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.testTag("auth_password_toggle_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Ocultar senha" else "Ver senha",
                                        tint = ColorTextMuted
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isRegisterMode) {
                                        viewModel.signUpWithEmail(email, password) { ok, _ -> if (ok) onAuthSuccess() }
                                    } else {
                                        viewModel.signInWithEmail(email, password) { ok, _ -> if (ok) onAuthSuccess() }
                                    }
                                }
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
                                .testTag("auth_password_input")
                                .semantics { testTag = "auth_gate_password_input" }
                        )

                        Button(
                            onClick = {
                                if (isRegisterMode) {
                                    viewModel.signUpWithEmail(email, password) { ok, _ -> if (ok) onAuthSuccess() }
                                } else {
                                    viewModel.signInWithEmail(email, password) { ok, _ -> if (ok) onAuthSuccess() }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("auth_submit_email_btn")
                                .semantics { testTag = "auth_gate_submit_btn" },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorBrandEmerald,
                                contentColor = ColorOnPrimary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("auth_loading_indicator"),
                                    color = ColorOnPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (isRegisterMode) "Cadastrar Gratuitamente" else "Entrar no GiroCusto",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        TextButton(
                            onClick = { isRegisterMode = !isRegisterMode },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_toggle_mode_btn")
                                .semantics { testTag = "auth_gate_tab_register" }
                        ) {
                            Text(
                                text = if (isRegisterMode) "Já tem conta? Entrar agora" else "Não tem conta? Cadastre-se grátis",
                                color = ColorBrandEmerald,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // ----------------------------------------------------
                        // ABA 2: CELULAR COM OTP
                        // ----------------------------------------------------
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                if (digits.length <= 11) {
                                    phone = digits
                                }
                            },
                            label = { Text("Número do Celular") },
                            placeholder = { Text("(11) 98765-4321", color = ColorTextMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = null,
                                    tint = ColorTextMuted
                                )
                            },
                            visualTransformation = BrazilianPhoneVisualTransformation(),
                            singleLine = true,
                            enabled = false,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions.Default,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorSurfaceInput,
                                unfocusedContainerColor = ColorSurfaceInput,
                                disabledContainerColor = ColorSurfaceInput,
                                focusedBorderColor = ColorBrandEmerald,
                                unfocusedBorderColor = ColorBorder,
                                disabledBorderColor = ColorBorder,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary,
                                disabledTextColor = ColorTextMuted,
                                focusedLabelColor = ColorBrandEmerald,
                                unfocusedLabelColor = ColorTextMuted,
                                disabledLabelColor = ColorTextMuted,
                                disabledLeadingIconColor = ColorTextMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_phone_input")
                        )

                        Text(
                            text = stringResource(R.string.auth_sms_coming_soon_notice),
                            fontSize = 12.sp,
                            color = ColorTextMuted,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("auth_submit_phone_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorBrandEmerald,
                                contentColor = ColorOnPrimary,
                                disabledContainerColor = ColorSurfaceInput,
                                disabledContentColor = ColorTextMuted
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.auth_btn_sms_coming_soon),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // DIVISOR HORIZONTAL
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ColorBorder
                )
                Text(
                    text = if (isRegisterMode) "ou cadastre-se com" else "ou continue com",
                    fontSize = 12.sp,
                    color = ColorTextMuted,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ColorBorder
                )
            }

            // ==========================================
            // BOTÃO GOOGLE SIGN-IN / SIGN-UP
            // ==========================================
            OutlinedButton(
                onClick = {
                    if (isRegisterMode) {
                        onGoogleSignUpClick()
                    } else {
                        onGoogleSignInClick()
                    }
                    viewModel.signInWithGoogle(context) { ok, _ ->
                        if (ok) onAuthSuccess()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(if (isRegisterMode) "auth_google_signup_btn" else "auth_google_signin_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ColorSurfaceCard,
                    contentColor = ColorTextPrimary
                ),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "G",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorGoogleBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isRegisterMode) "Cadastrar com o Google" else "Continuar com o Google",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextPrimary
                    )
                }
            }
        }

            // ==========================================
            // RODAPÉ LEGAL
            // ==========================================
            Text(
                text = "Ao continuar, você concorda com nossos Termos de Uso e Política de Privacidade.",
                fontSize = 11.sp,
                color = ColorTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
