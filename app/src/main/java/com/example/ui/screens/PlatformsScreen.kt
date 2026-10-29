package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Platform
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.platforms.*

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

    // Navigation state within the screen:
    // null -> listing platforms
    // -1L -> adding a new platform
    // platformId -> editing existing platform
    var editingPlatformId by remember { mutableStateOf<Long?>(null) }

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
                onEditClick = { id -> editingPlatformId = id }
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
                        if (isEdit) "Plataforma atualizada com sucesso!" else "Plataforma vinculada com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    editingPlatformId = null
                },
                onDelete = {
                    viewModel.deletePlatform(platformToEdit)
                    Toast.makeText(context, "Plataforma removida.", Toast.LENGTH_SHORT).show()
                    editingPlatformId = null
                }
            )
        }
    }
}
