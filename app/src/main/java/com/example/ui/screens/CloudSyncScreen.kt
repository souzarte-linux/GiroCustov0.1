package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.supabase.AuthSession
import com.example.ui.GiroCustoViewModel
import com.example.ui.SyncState

enum class AuthTab {
    LOGIN,
    REGISTER
}

@Composable
fun CloudSyncScreen(
    viewModel: GiroCustoViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val syncError by viewModel.syncErrorMessage.collectAsStateWithLifecycle()

    val records by viewModel.records.collectAsStateWithLifecycle()
    val fuelRefills by viewModel.fuelRefills.collectAsStateWithLifecycle()
    val maintenanceRecords by viewModel.maintenanceRecords.collectAsStateWithLifecycle()
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER COM TÍTULO E BOTÃO DE VOLTAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(36.dp).testTag("cloud_sync_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Nuvem & Backup",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.size(36.dp).testTag("theme_toggle_btn_cloud")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (currentUser == null) {
            // =================================================================
            // USUÁRIO NÃO AUTENTICADO: LOGIN / CADASTRO
            // =================================================================
            LoggedOutCloudSection(
                viewModel = viewModel,
                syncState = syncState,
                syncError = syncError,
                isDark = isDark
            )
        } else {
            // =================================================================
            // USUÁRIO AUTENTICADO: STATUS, SINCRONIZAÇÃO E LOGOUT
            // =================================================================
            LoggedInCloudSection(
                currentUser = currentUser!!,
                syncState = syncState,
                lastSyncTime = lastSyncTime,
                syncError = syncError,
                recordsCount = records.size,
                refillsCount = fuelRefills.size,
                maintenanceCount = maintenanceRecords.size,
                vehicleModel = vehicle?.model,
                isDark = isDark,
                onSyncClick = {
                    viewModel.triggerSync { success, errorMsg ->
                        if (success) {
                            Toast.makeText(context, "Sincronização concluída com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, errorMsg ?: "Falha ao sincronizar", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onLogoutClick = { showLogoutDialog = true },
                formatLastSyncTime = { viewModel.formatLastSyncTime(it) }
            )
        }
    }

    // DIÁLOGO DE CONFIRMAÇÃO DE LOGOUT
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Desconectar Conta?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Seus dados locais continuarão salvos com segurança neste dispositivo. Ao sair, a sincronização em nuvem ficará pausada até você entrar novamente.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logoutSupabase()
                        showLogoutDialog = false
                        Toast.makeText(context, "Conta desconectada.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Desconectar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// =============================================================================
// SEÇÃO DESLOGADO (LOGIN / CADASTRO COM MATERIAL 3)
// =============================================================================
@Composable
private fun LoggedOutCloudSection(
    viewModel: GiroCustoViewModel,
    syncState: SyncState,
    syncError: String?,
    isDark: Boolean
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

            // Benefícios rápidos
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
            if (!currentErrorMessage.isNullOrBlank() && syncState == SyncState.ERROR) {
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
                        imageVector = if (selectedTab == AuthTab.LOGIN) Icons.Filled.Login else Icons.Filled.PersonAdd,
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
        }
    }
}

// =============================================================================
// SEÇÃO LOGADO (DADOS DO USUÁRIO, SYNC E LOGOUT)
// =============================================================================
@Composable
private fun LoggedInCloudSection(
    currentUser: AuthSession,
    syncState: SyncState,
    lastSyncTime: Long?,
    syncError: String?,
    recordsCount: Int,
    refillsCount: Int,
    maintenanceCount: Int,
    vehicleModel: String?,
    isDark: Boolean,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit,
    formatLastSyncTime: (Long?) -> String
) {
    val isSyncing = syncState == SyncState.SYNCING

    // Animação de rotação para o ícone de sincronização
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // CARD DE CONTA & STATUS
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Perfil e Identificação
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF047857))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = currentUser.userEmail?.take(1)?.uppercase() ?: "U"
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conectado ao GiroCusto Cloud",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = currentUser.userEmail ?: "Usuário Autenticado",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)
            )

            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status da Conexão:",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (syncState) {
                        SyncState.SYNCING -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        SyncState.SUCCESS -> Color(0xFF10B981).copy(alpha = 0.15f)
                        SyncState.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        SyncState.OFFLINE -> Color(0xFF64748B).copy(alpha = 0.15f)
                        SyncState.IDLE -> Color(0xFF10B981).copy(alpha = 0.15f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (syncState) {
                            SyncState.SYNCING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFF59E0B)
                                )
                                Text("Sincronizando...", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                            }
                            SyncState.SUCCESS -> {
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Text("Sincronizado", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                            SyncState.ERROR -> {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                Text("Erro ao Sincronizar", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            SyncState.OFFLINE -> {
                                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                Text("Offline", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            }
                            SyncState.IDLE -> {
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Text("Conectado", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Última sincronização
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Última sincronização:",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Text(
                    text = formatLastSyncTime(lastSyncTime),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Mensagem de erro caso exista
            if (syncState == SyncState.ERROR && !syncError.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Aviso: ${translateAuthError(syncError)}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }

    // CARD DE RESUMO LOCAL PROTEGIDO NA NUVEM
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Resumo dos Dados em Nuvem",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatItem(label = "Diárias", value = recordsCount.toString(), isDark = isDark)
                SummaryStatItem(label = "Abastecimentos", value = refillsCount.toString(), isDark = isDark)
                SummaryStatItem(label = "Manutenções", value = maintenanceCount.toString(), isDark = isDark)
            }

            if (!vehicleModel.isNullOrBlank()) {
                Surface(
                    color = if (isDark) Color(0xFF2D2D34).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TwoWheeler,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Veículo vinculado: $vehicleModel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // BOTÃO SINCRONIZAR AGORA
    Button(
        onClick = onSyncClick,
        enabled = !isSyncing,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_sync_now"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF10B981),
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Sync,
            contentDescription = "Sincronizar",
            modifier = Modifier
                .size(20.dp)
                .then(if (isSyncing) Modifier.rotate(rotationAngle) else Modifier)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isSyncing) "Sincronizando com a Nuvem..." else "Sincronizar Agora",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }

    // BOTÃO DESCONECTAR / SAIR
    OutlinedButton(
        onClick = onLogoutClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_logout_supabase"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Logout,
            contentDescription = "Sair",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Desconectar / Sair da Conta",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

// =============================================================================
// COMPONENTES AUXILIARES
// =============================================================================
@Composable
private fun BenefitItem(text: String) {
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

@Composable
private fun SummaryStatItem(label: String, value: String, isDark: Boolean) {
    Surface(
        color = if (isDark) Color(0xFF2D2D34).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46).copy(alpha = 0.5f) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}

private fun translateAuthError(error: String): String {
    val lower = error.lowercase()
    return when {
        "invalid login credentials" in lower || "invalid_grant" in lower ->
            "E-mail ou senha incorretos. Verifique suas credenciais."
        "user already registered" in lower ->
            "Este e-mail já está cadastrado. Tente entrar."
        "password should be at least" in lower ->
            "A senha deve conter no mínimo 6 caracteres."
        "unknownhostexception" in lower || "connectexception" in lower || "failed to connect" in lower ->
            "Sem conexão com a internet. Verifique seu sinal ou Wi-Fi."
        else -> error
    }
}
