package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.cloud.*

@Composable
fun CloudSyncScreen(
    viewModel: GiroCustoViewModel,
    onNavigateBack: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onGoogleSignUpClick: () -> Unit = {}
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
    var showPendingLogoutWarningDialog by remember { mutableStateOf(false) }

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
            LoggedOutCloudSection(
                viewModel = viewModel,
                syncState = syncState,
                syncError = syncError,
                isDark = isDark,
                onGoogleSignInClick = onGoogleSignInClick,
                onGoogleSignUpClick = onGoogleSignUpClick
            )
        } else {
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
                onLogoutClick = {
                    viewModel.checkPendingChangesBeforeLogout { hasPending ->
                        if (hasPending) {
                            showPendingLogoutWarningDialog = true
                        } else {
                            showLogoutDialog = true
                        }
                    }
                },
                formatLastSyncTime = { viewModel.formatLastSyncTime(it) }
            )
        }
    }

    if (showPendingLogoutWarningDialog) {
        PendingLogoutWarningDialog(
            onDismiss = { showPendingLogoutWarningDialog = false },
            onSyncAndLogout = {
                showPendingLogoutWarningDialog = false
                viewModel.triggerSync { success, err ->
                    if (success) {
                        Toast.makeText(context, "Sincronizado com sucesso! Agora você pode sair com segurança.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, err ?: "Falha ao sincronizar", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onForceLogout = {
                showPendingLogoutWarningDialog = false
                viewModel.logoutSupabase()
                Toast.makeText(context, "Conta desconectada.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showLogoutDialog) {
        CloudSyncLogoutConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirmLogout = {
                viewModel.logoutSupabase()
                showLogoutDialog = false
                Toast.makeText(context, "Conta desconectada.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
