package com.example.ui.screens.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design Tokens dinâmicos baseados no MaterialTheme.colorScheme para tela de Perfil
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

internal val ColorBorderActive: Color
    @Composable get() = MaterialTheme.colorScheme.primary

internal val ColorBrandEmerald: Color
    @Composable get() = MaterialTheme.colorScheme.primary

internal val ColorEmeraldDark: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary

internal val ColorEmeraldLight: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary

internal val ColorActiveVehicleCard: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer

internal val ColorSky: Color
    @Composable get() = MaterialTheme.colorScheme.secondary

internal val ColorAmber: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary

internal val ColorRose: Color
    @Composable get() = MaterialTheme.colorScheme.error

internal val ColorTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

internal val ColorTextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

internal val ColorTextMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

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
