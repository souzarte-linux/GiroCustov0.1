package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Platform
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.platforms.*
import kotlinx.coroutines.launch

typealias LocalCycleEntry = com.example.ui.screens.platforms.LocalCycleEntry

fun parseCycleEntries(json: String): List<LocalCycleEntry> =
    com.example.ui.screens.platforms.parseCycleEntries(json)

fun formatCycleEntries(entries: List<LocalCycleEntry>): String =
    com.example.ui.screens.platforms.formatCycleEntries(entries)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformsScreen(viewModel: GiroCustoViewModel) {
    val platforms by viewModel.allPlatforms.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation state within the screen:
    // null -> listing platforms
    // -1L -> adding a new platform
    // platformId -> editing existing platform
    var editingPlatformId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = editingPlatformId,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "platforms_screen_transition"
            ) { currentId ->
                if (currentId == null) {
                    // LIST VIEW
                    PlatformListSection(
                        platforms = platforms,
                        onAddClick = { editingPlatformId = -1L },
                        onEditClick = { id -> editingPlatformId = id },
                        onToggleActive = { platform, active ->
                            viewModel.savePlatform(platform.copy(active = active))
                        }
                    )
                } else {
                    // FORM VIEW (ADD OR EDIT)
                    val isEdit = currentId != -1L
                    val platformToEdit = if (isEdit) {
                        platforms.find { it.id == currentId } ?: Platform(name = "")
                    } else {
                        Platform(name = "")
                    }

                    PlatformFormSection(
                        platform = platformToEdit,
                        isEdit = isEdit,
                        onBack = { editingPlatformId = null },
                        onSave = { updatedPlatform ->
                            viewModel.savePlatform(updatedPlatform)
                            Toast.makeText(
                                context,
                                if (isEdit) context.getString(com.example.R.string.platforms_toast_save_success) else context.getString(com.example.R.string.platforms_toast_add_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            editingPlatformId = null
                        },
                        onDelete = {
                            coroutineScope.launch {
                                viewModel.deletePlatform(platformToEdit)
                                editingPlatformId = null
                                val result = snackbarHostState.showSnackbar(
                                    message = context.getString(com.example.R.string.snackbar_platform_deleted, platformToEdit.name),
                                    actionLabel = context.getString(com.example.R.string.snackbar_action_undo),
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restorePlatform(platformToEdit)
                                    Toast.makeText(context, context.getString(com.example.R.string.toast_platform_restored), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
