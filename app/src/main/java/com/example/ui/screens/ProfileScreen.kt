package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Platform
import com.example.data.UserProfile
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.ui.GiroCustoViewModel
import com.example.ui.ViaCepUiState
import com.example.ui.components.LgpdDeleteAccountDialog
import com.example.ui.components.LgpdExportDialog
import com.example.ui.components.PrivacyAndDataSection
import com.example.ui.screens.profile.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: GiroCustoViewModel,
    onNavigateToPlatforms: () -> Unit = {}
) {
    val allVehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    val activeVehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val allPlatforms by viewModel.allPlatforms.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val viaCepState by viewModel.viaCepState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estado do formulário de veículo (null: lista / seções, -1L: novo veículo, >0L: edição de veículo)
    var editingVehicleId by remember { mutableStateOf<Long?>(null) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    var partToDelete by remember { mutableStateOf<VehiclePart?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = editingVehicleId,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "profile_vehicle_transition"
        ) { currentVehicleId ->
            if (currentVehicleId == null) {
                ProfileSectionsContent(
                    viewModel = viewModel,
                    userProfile = userProfileState ?: UserProfile(),
                    allPlatforms = allPlatforms,
                    allVehicles = allVehicles,
                    activeVehicle = activeVehicle,
                    parts = parts,
                    viaCepState = viaCepState,
                    isDark = isDark,
                    onNavigateToPlatforms = onNavigateToPlatforms,
                    onAddVehicleClick = { editingVehicleId = -1L },
                    onEditVehicleClick = { id -> editingVehicleId = id },
                    onDeleteVehicle = { vehicle -> vehicleToDelete = vehicle },
                    onDeletePart = { part -> partToDelete = part }
                )
            } else {
                val isEdit = currentVehicleId != -1L
                val vehicleToEdit = if (isEdit) {
                    allVehicles.find { it.id == currentVehicleId } ?: Vehicle(id = 0L)
                } else {
                    Vehicle(id = 0L)
                }

                VehicleFormSection(
                    vehicle = vehicleToEdit,
                    isEdit = isEdit,
                    viewModel = viewModel,
                    onBack = { editingVehicleId = null },
                    onSave = { updatedVehicle ->
                        viewModel.updateVehicle(updatedVehicle)
                        Toast.makeText(
                            context,
                            if (isEdit) "Veículo atualizado com sucesso!" else "Veículo cadastrado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        editingVehicleId = null
                    },
                    onDelete = {
                        vehicleToDelete = vehicleToEdit
                    }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Diálogo de confirmação de exclusão de veículo
    vehicleToDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_vehicle_title),
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_delete_vehicle_message, toDelete.model),
                    color = ColorTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vehicleToDelete = null
                        viewModel.deleteVehicle(toDelete)
                        if (editingVehicleId == toDelete.id) {
                            editingVehicleId = null
                        }
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.snackbar_vehicle_deleted, toDelete.model),
                                actionLabel = context.getString(R.string.snackbar_action_undo),
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreVehicle(toDelete)
                                Toast.makeText(context, context.getString(R.string.toast_vehicle_restored), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorRose,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.btn_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { vehicleToDelete = null }
                ) {
                    Text(stringResource(R.string.btn_cancel), color = ColorTextSecondary)
                }
            },
            containerColor = ColorSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo de confirmação de exclusão de peça
    partToDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { partToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_part_title),
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_delete_part_message, toDelete.name),
                    color = ColorTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        partToDelete = null
                        viewModel.deletePart(toDelete)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.snackbar_part_deleted, toDelete.name),
                                actionLabel = context.getString(R.string.snackbar_action_undo),
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restorePart(toDelete)
                                Toast.makeText(context, context.getString(R.string.toast_part_restored), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorRose,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.btn_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { partToDelete = null }
                ) {
                    Text(stringResource(R.string.btn_cancel), color = ColorTextSecondary)
                }
            },
            containerColor = ColorSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Conteúdo principal contendo as 5 seções ordenadas da tela de perfil.
 */
@Composable
private fun ProfileSectionsContent(
    viewModel: GiroCustoViewModel,
    userProfile: UserProfile,
    allPlatforms: List<Platform>,
    allVehicles: List<Vehicle>,
    activeVehicle: Vehicle?,
    parts: List<VehiclePart>,
    viaCepState: ViaCepUiState,
    isDark: Boolean,
    onNavigateToPlatforms: () -> Unit,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (Long) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    onDeletePart: (VehiclePart) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val currentActiveVehicle = activeVehicle ?: allVehicles.find { it.active }

    var showAddPartDialog by remember { mutableStateOf(false) }
    var showEditPartDialog by remember { mutableStateOf(false) }
    var partToEdit by remember { mutableStateOf<VehiclePart?>(null) }

    // Estados de Privacidade e Exclusão LGPD (T-10 [SEC-06])
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var exportedJsonContent by remember { mutableStateOf<String?>(null) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            try {
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    output.write((exportedJsonContent ?: "").toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, context.getString(R.string.lgpd_export_success), Toast.LENGTH_SHORT).show()
                showExportDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.lgpd_export_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Meu Perfil & Veículos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Gestão de infraestrutura operacional",
                    fontSize = 12.sp,
                    color = ColorTextMuted
                )
            }

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ColorSurfaceCard)
                    .testTag("theme_toggle_btn_profile")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = stringResource(R.string.cd_theme_toggle),
                    tint = if (isDark) ColorAmber else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // =========================================================================
        // SEÇÃO 1: CARD MEUS DADOS CADASTRAIS
        // =========================================================================
        CadastralDataCard(
            userProfile = userProfile,
            viaCepState = viaCepState,
            viewModel = viewModel
        )

        // =========================================================================
        // SEÇÃO 2: CARD PLATAFORMAS ATIVAS
        // =========================================================================
        ActivePlatformsCard(
            allPlatforms = allPlatforms,
            onPlatformCheckChange = { platform, checked ->
                viewModel.savePlatform(platform.copy(active = checked))
            },
            onNavigateToPlatforms = onNavigateToPlatforms
        )

        // =========================================================================
        // SEÇÃO 3: MEUS VEÍCULOS
        // =========================================================================
        MyVehiclesSection(
            allVehicles = allVehicles,
            onAddVehicleClick = onAddVehicleClick,
            onEditVehicleClick = onEditVehicleClick,
            onActivateVehicle = { vehicleId ->
                viewModel.setActiveVehicle(vehicleId)
                Toast.makeText(context, "Veículo operacional ativado!", Toast.LENGTH_SHORT).show()
            },
            onDeleteVehicle = onDeleteVehicle
        )

        // =========================================================================
        // SEÇÃO 4: MONITORAMENTO DE DESGASTE
        // =========================================================================
        PartsWearSection(
            activeVehicle = currentActiveVehicle,
            parts = parts,
            onAddPartClick = { showAddPartDialog = true },
            onEditPartClick = { part ->
                partToEdit = part
                showEditPartDialog = true
            },
            onResetWear = { partId ->
                viewModel.resetPartWear(partId)
                Toast.makeText(context, "Desgaste da peça zerado!", Toast.LENGTH_SHORT).show()
            },
            onDeletePart = onDeletePart
        )

        // =========================================================================
        // SEÇÃO 5: PRIVACIDADE & DADOS (LGPD - T-10 [SEC-06])
        // =========================================================================
        PrivacyAndDataSection(
            onExportClick = {
                viewModel.exportUserData { json ->
                    exportedJsonContent = json
                    showExportDialog = true
                }
            },
            onDeleteAccountClick = {
                showDeleteAccountDialog = true
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Diálogos de Peças
    if (showAddPartDialog) {
        ProfileAddPartDialog(
            activeVehicle = currentActiveVehicle,
            onDismiss = { showAddPartDialog = false },
            onAdd = { name: String, price: Double, lifespan: Double ->
                viewModel.addPart(name, price, lifespan, vehicleId = currentActiveVehicle?.id ?: 0L)
                showAddPartDialog = false
                Toast.makeText(context, "Nova peça adicionada!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEditPartDialog && partToEdit != null) {
        ProfileEditPartDialog(
            part = partToEdit!!,
            activeVehicle = currentActiveVehicle,
            onDismiss = {
                showEditPartDialog = false
                partToEdit = null
            },
            onSave = { name: String, price: Double, lifespan: Double, runKm: Double ->
                viewModel.updatePart(
                    partId = partToEdit!!.id,
                    name = name,
                    price = price,
                    lifespan = lifespan,
                    runKmSinceChange = runKm,
                    vehicleId = currentActiveVehicle?.id ?: partToEdit!!.vehicleId
                )
                showEditPartDialog = false
                partToEdit = null
                Toast.makeText(context, "Peça de desgaste atualizada!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Diálogos de Privacidade e Exclusão LGPD (T-10 [SEC-06])
    if (showExportDialog) {
        LgpdExportDialog(
            onDismiss = { showExportDialog = false },
            onShare = {
                val json = exportedJsonContent ?: return@LgpdExportDialog
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, json)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.lgpd_export_dialog_title))
                context.startActivity(shareIntent)
                showExportDialog = false
            },
            onSaveToDevice = {
                val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                saveFileLauncher.launch("girocusto_backup_$dateStr.json")
            }
        )
    }

    if (showDeleteAccountDialog) {
        LgpdDeleteAccountDialog(
            isDeleting = isDeletingAccount,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirmDelete = {
                isDeletingAccount = true
                viewModel.deleteAccountAndData { success, error ->
                    isDeletingAccount = false
                    if (success) {
                        showDeleteAccountDialog = false
                        Toast.makeText(context, context.getString(R.string.lgpd_delete_success), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.lgpd_delete_error, error ?: "Erro desconhecido"), Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}
