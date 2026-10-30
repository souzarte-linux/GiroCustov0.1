package com.example.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design Tokens dinâmicos baseados no MaterialTheme.colorScheme para a tela de Primeiro Cadastro (Onboarding)
internal val ColorDarkBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

internal val ColorSurfaceCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

internal val ColorSurfaceElevated: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

internal val ColorSurfaceInput: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

internal val ColorBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outline

internal val ColorBrandEmerald: Color
    @Composable get() = MaterialTheme.colorScheme.primary

internal val ColorEmeraldBadgeBg: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer

internal val ColorEmeraldBadgeText: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer

internal val ColorOnPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary

internal val ColorTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

internal val ColorTextMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

internal val ColorError: Color
    @Composable get() = MaterialTheme.colorScheme.error

internal val ColorErrorBg: Color
    @Composable get() = MaterialTheme.colorScheme.errorContainer

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
