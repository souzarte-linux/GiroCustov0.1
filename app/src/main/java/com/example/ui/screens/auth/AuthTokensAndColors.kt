package com.example.ui.screens.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design Tokens Compartilhados de Autenticação dinâmicos baseados no MaterialTheme.colorScheme
val ColorDarkBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

val ColorSurfaceCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val ColorSurfaceInput: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val ColorBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val ColorBrandEmerald: Color
    @Composable get() = MaterialTheme.colorScheme.primary

val ColorEmeraldDeep: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer

val ColorOnPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary

val ColorTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

val ColorTextMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val ColorError: Color
    @Composable get() = MaterialTheme.colorScheme.error

val ColorGoogleBlue: Color
    @Composable get() = MaterialTheme.colorScheme.secondary

