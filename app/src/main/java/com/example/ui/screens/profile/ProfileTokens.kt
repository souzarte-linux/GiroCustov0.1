package com.example.ui.screens.profile

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design Tokens Dark Theme Pro para tela de Perfil
internal val ColorDarkBg = Color(0xFF121214)
internal val ColorSurfaceCard = Color(0xFF1E1E22)
internal val ColorSurfaceElevated = Color(0xFF24242A)
internal val ColorSurfaceInput = Color(0xFF2A2A30)
internal val ColorBorder = Color(0xFF2D2D34)
internal val ColorBorderActive = Color(0xFF10B981)
internal val ColorBrandEmerald = Color(0xFF10B981)
internal val ColorEmeraldDark = Color(0xFF064E3B)
internal val ColorEmeraldLight = Color(0xFF34D399)
internal val ColorActiveVehicleCard = Color(0xFF132D21)
internal val ColorSky = Color(0xFF38BDF8)
internal val ColorAmber = Color(0xFFF59E0B)
internal val ColorRose = Color(0xFFF43F5E)
internal val ColorTextPrimary = Color(0xFFF1F5F9)
internal val ColorTextSecondary = Color(0xFFCBD5E1)
internal val ColorTextMuted = Color(0xFF94A3B8)

@Composable
internal fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ColorSurfaceInput,
    unfocusedContainerColor = ColorSurfaceInput,
    disabledContainerColor = ColorSurfaceInput.copy(alpha = 0.5f),
    focusedBorderColor = ColorBrandEmerald,
    unfocusedBorderColor = ColorBorder,
    focusedTextColor = ColorTextPrimary,
    unfocusedTextColor = ColorTextPrimary,
    focusedLabelColor = ColorBrandEmerald,
    unfocusedLabelColor = ColorTextMuted
)
