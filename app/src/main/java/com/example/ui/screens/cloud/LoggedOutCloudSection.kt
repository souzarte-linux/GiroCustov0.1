package com.example.ui.screens.cloud

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GiroCustoViewModel
import com.example.ui.SyncState

@Composable
fun LoggedOutCloudSection(
    viewModel: GiroCustoViewModel,
    syncState: SyncState,
    syncError: String?,
    isDark: Boolean,
    onGoogleSignInClick: () -> Unit = {},
    onGoogleSignUpClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var selectedTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    val isLoading = syncState == SyncState.SYNCING

    // Card Hero Explicativo
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1E22) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = "Nuvem",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Sincronização em Nuvem Supabase",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Faça login ou crie sua conta gratuita para manter suas diárias, abastecimentos e manutenções protegidos e acessíveis em qualquer aparelho.",
                fontSize = 13.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BenefitItem(text = "Backup automático contínuo dos seus ganhos e despesas")
                BenefitItem(text = "Seus dados locais nunca são perdidos ao trocar de aparelho")
                BenefitItem(text = "Segurança com banco de dados em nuvem criptografado")
            }
        }
    }

    // Alternador de Abas (Entrar / Criar Conta)
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = if (isDark) Color(0xFF1E1E22) else Color(0xFFF1F5F9),
        contentColor = Color(0xFF10B981),
        indicator = {},
        divider = {},
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
    ) {
        Tab(
            selected = selectedTab == AuthTab.LOGIN,
            onClick = {
                selectedTab = AuthTab.LOGIN
                localError = null
                successNotice = null
            },
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selectedTab == AuthTab.LOGIN) Color(0xFF10B981) else Color.Transparent
                )
                .testTag("tab_auth_login"),
            text = {
                Text(
                    text = "Entrar",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == AuthTab.LOGIN) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        )

        Tab(
            selected = selectedTab == AuthTab.REGISTER,
            onClick = {
                selectedTab = AuthTab.REGISTER
                localError = null
                successNotice = null
            },
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selectedTab == AuthTab.REGISTER) Color(0xFF10B981) else Color.Transparent
                )
                .testTag("tab_auth_register"),
            text = {
                Text(
                    text = "Criar Conta",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == AuthTab.REGISTER) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }

    // CARD DE FORMULÁRIO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1E22) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (selectedTab == AuthTab.LOGIN) "Acesse sua conta" else "Crie sua nova conta",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // CAMPO E-MAIL
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                label = { Text("E-mail") },
                placeholder = { Text("seu.email@exemplo.com") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "E-mail",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_auth_email")
            )

            // CAMPO SENHA
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                label = { Text("Senha") },
                placeholder = { Text("Mínimo de 6 caracteres") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Senha",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (selectedTab == AuthTab.LOGIN) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_auth_password")
            )

            // CAMPO CONFIRMAR SENHA (apenas no cadastro)
            if (selectedTab == AuthTab.REGISTER) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    label = { Text("Confirmar Senha") },
                    placeholder = { Text("Repita sua senha") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.LockReset,
                            contentDescription = "Confirmar Senha",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_auth_confirm_password")
                )
            }

            // FEEDBACK DE ERRO
            val currentErrorMessage = localError ?: syncError
            if (!currentErrorMessage.isNullOrBlank() && (localError != null || syncState == SyncState.ERROR)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Erro",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = translateAuthError(currentErrorMessage),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // FEEDBACK DE SUCESSO
            if (!successNotice.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Sucesso",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = successNotice!!,
                            color = Color(0xFF10B981),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // BOTÃO DE AÇÃO PRINCIPAL
            Button(
                onClick = {
                    focusManager.clearFocus()
                    localError = null
                    successNotice = null

                    if (email.isBlank() || !email.contains("@")) {
                        localError = "Informe um endereço de e-mail válido."
                        return@Button
                    }
                    if (password.length < 6) {
                        localError = "A senha deve conter no mínimo 6 caracteres."
                        return@Button
                    }

                    if (selectedTab == AuthTab.REGISTER) {
                        if (password != confirmPassword) {
                            localError = "As senhas informadas não coincidem."
                            return@Button
                        }
                        viewModel.registerSupabase(email, password) { success, msg ->
                            if (success) {
                                successNotice = "Conta criada com sucesso! Sincronizando dados..."
                                Toast.makeText(context, "Bem-vindo ao GiroCusto em Nuvem!", Toast.LENGTH_SHORT).show()
                            } else {
                                localError = msg ?: "Falha ao criar conta."
                            }
                        }
                    } else {
                        viewModel.loginSupabase(email, password) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                localError = msg ?: "Credenciais inválidas."
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_submit_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processando...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = if (selectedTab == AuthTab.LOGIN) Icons.AutoMirrored.Filled.Login else Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedTab == AuthTab.LOGIN) "Entrar na Conta" else "Criar Minha Conta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // DIVISOR HORIZONTAL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)
                )
                Text(
                    text = if (selectedTab == AuthTab.LOGIN) "ou continue com" else "ou cadastre-se com",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)
                )
            }

            // BOTÃO GOOGLE
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    localError = null
                    successNotice = null
                    if (selectedTab == AuthTab.LOGIN) {
                        onGoogleSignInClick()
                        viewModel.signInWithGoogle(context) { ok, msg ->
                            if (ok) {
                                Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                localError = msg ?: "Falha ao autenticar com Google."
                            }
                        }
                    } else {
                        onGoogleSignUpClick()
                        viewModel.signInWithGoogle(context) { ok, msg ->
                            if (ok) {
                                Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                localError = msg ?: "Falha ao cadastrar com Google."
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag(if (selectedTab == AuthTab.REGISTER) "auth_google_signup_btn" else "auth_google_signin_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDark) Color(0xFF1E1E22) else Color.White,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "G",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedTab == AuthTab.LOGIN) "Continuar com o Google" else "Cadastrar com o Google",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
