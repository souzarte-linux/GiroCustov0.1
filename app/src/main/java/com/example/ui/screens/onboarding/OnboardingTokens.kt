package com.example.ui.screens.onboarding

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design Tokens Dark Theme Pro para a tela de Primeiro Cadastro (Onboarding)
internal val ColorDarkBg = Color(0xFF121214)
internal val ColorSurfaceCard = Color(0xFF1E1E22)
internal val ColorSurfaceElevated = Color(0xFF24242A)
internal val ColorSurfaceInput = Color(0xFF2A2A30)
internal val ColorBorder = Color(0xFF2D2D34)
internal val ColorBrandEmerald = Color(0xFF10B981)
internal val ColorEmeraldBadgeBg = Color(0xFF064E3B)
internal val ColorEmeraldBadgeText = Color(0xFF34D399)
internal val ColorOnPrimary = Color(0xFF064E3B)
internal val ColorTextPrimary = Color(0xFFF1F5F9)
internal val ColorTextMuted = Color(0xFF94A3B8)
internal val ColorError = Color(0xFFF43F5E)
internal val ColorErrorBg = Color(0xFF4C1D24)

@Composable
internal fun onboardingTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ColorSurfaceInput,
    unfocusedContainerColor = ColorSurfaceInput,
    focusedBorderColor = ColorBrandEmerald,
    unfocusedBorderColor = ColorBorder,
    focusedTextColor = ColorTextPrimary,
    unfocusedTextColor = ColorTextPrimary,
    focusedLabelColor = ColorBrandEmerald,
    unfocusedLabelColor = ColorTextMuted
)
