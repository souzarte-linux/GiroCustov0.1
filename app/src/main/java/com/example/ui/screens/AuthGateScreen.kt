package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.AuthUiState
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.auth.*
import com.example.util.BiometricHelper

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
    val authErrorResId by viewModel.authErrorResId.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var hasAutoTriggeredBiometric by rememberSaveable { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Email, 1: Celular
    var isRegisterMode by remember { mutableStateOf(false) }

    // Campos de E-mail
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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

            // HEADER INSTITUCIONAL (BRANDING)
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

            // BANNER DE ERRO REATIVO
            AnimatedVisibility(
                visible = authState is AuthUiState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val errorMsg = authErrorResId?.let { stringResource(it) }
                    ?: (authState as? AuthUiState.Error)?.message.orEmpty()
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

            // BANNER DE SUCESSO REATIVO
            AnimatedVisibility(
                visible = authState is AuthUiState.Success && !(authState as AuthUiState.Success).message.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val successMsg = (authState as? AuthUiState.Success)?.message.orEmpty()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_success_banner"),
                    color = Color(0xFF143E2E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorBrandEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Sucesso",
                            tint = ColorBrandEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = successMsg,
                            color = ColorBrandEmerald,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isBiometricLocked) {
                // TELA DE DESBLOQUEIO COM BIOMETRIA
                AuthBiometricLockedCard(
                    userEmail = currentUser?.userEmail,
                    isLoading = isLoading,
                    onTriggerBiometric = triggerBiometricAuth,
                    onDismissBiometricLock = { viewModel.dismissBiometricLockForFallback() }
                )
            } else {
                // ABAS SEGMENTADAS (EMAIL / CELULAR)
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
                            onClick = {
                                selectedTabIndex = 0
                                viewModel.resetAuthError()
                            },
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
                            onClick = {
                                selectedTabIndex = 1
                                viewModel.resetAuthError()
                            },
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

                // CARD DE FORMULÁRIO DE ENTRADA
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
                            AuthEmailFormSection(
                                email = email,
                                onEmailChange = { email = it },
                                password = password,
                                onPasswordChange = { password = it },
                                isRegisterMode = isRegisterMode,
                                onToggleMode = {
                                    isRegisterMode = !isRegisterMode
                                    viewModel.resetAuthError()
                                },
                                isLoading = isLoading,
                                onSubmit = {
                                    if (isRegisterMode) {
                                        viewModel.signUpWithEmail(email, password) { ok, _ -> if (ok) onAuthSuccess() }
                                    } else {
                                        viewModel.signInWithEmail(email, password) { ok, _ -> if (ok) onAuthSuccess() }
                                    }
                                }
                            )
                        } else {
                            AuthPhoneFormSection(
                                phone = phone,
                                onPhoneChange = { phone = it }
                            )
                        }
                    }
                }

                // DIVISOR, GOOGLE SIGN-IN E RODAPÉ LEGAL
                AuthSocialAndFooterSection(
                    isRegisterMode = isRegisterMode,
                    isLoading = isLoading,
                    onGoogleClick = {
                        if (isRegisterMode) {
                            onGoogleSignUpClick()
                        } else {
                            onGoogleSignInClick()
                        }
                        viewModel.signInWithGoogle(context) { ok, _ ->
                            if (ok) onAuthSuccess()
                        }
                    }
                )
            }
        }
    }
}
