// TODO: Deixe um comentário // TODO no topo de ui/navigation/GiroCustoMainApp.kt indicando que este é o lugar previsto para futura implementação de menu lateral (drawer) e navegação para tela de perfil de usuário.

package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GiroCustoViewModel
import com.example.ui.components.SmartphoneSimulator
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LaunchScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.VehicleScreen

enum class GiroTab(val title: String, val icon: ImageVector) {
    PAINEL("Painel", Icons.Filled.Dashboard),
    LANCAR("Lançar", Icons.Filled.AddCircle),
    RELATORIOS("Relatórios", Icons.Filled.Analytics),
    VEICULO("Veículo", Icons.Filled.TwoWheeler),
    HISTORICO("Histórico", Icons.Filled.History)
}

@Composable
fun GiroCustoMainApp(viewModel: GiroCustoViewModel) {
    var currentTab by remember { mutableStateOf(GiroTab.PAINEL) }
    
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    // Configuração de Paleta de Cores do GiroCusto (Professional Dark charcoal and Emerald green theme, or soft slate light theme)
    val customColorScheme = if (isDark) {
        darkColorScheme(
            primary = Color(0xFF10B981), // Emerald green brand color
            onPrimary = Color(0xFF064E3B), // Dark green on emerald
            primaryContainer = Color(0xFF064E3B), // Dark green container
            onPrimaryContainer = Color(0xFF34D399), // Soft green text
            secondary = Color(0xFF10B981), 
            onSecondary = Color(0xFF064E3B),
            secondaryContainer = Color(0xFF022C22),
            onSecondaryContainer = Color(0xFF6EE7B7),
            background = Color(0xFF121214), // Charcoal black
            surface = Color(0xFF1E1E22), // Charcoal card surface
            onBackground = Color(0xFFF1F5F9), // Slate-100
            onSurface = Color(0xFFE2E8F0), // Slate-200
            error = Color(0xFFF43F5E), // Rose-500
            onError = Color.White,
            outline = Color(0xFF2D2D34), // Zinc/charcoal border
            onSurfaceVariant = Color(0xFF94A3B8)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF10B981), // Emerald green brand color
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD1FAE5), // Soft green container
            onPrimaryContainer = Color(0xFF065F46), // Dark green text
            secondary = Color(0xFF059669),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFECFDF5),
            onSecondaryContainer = Color(0xFF047857),
            background = Color(0xFFF8FAFC), // soft slate 50
            surface = Color.White, // pure white cards
            onBackground = Color(0xFF0F172A), // Slate-900
            onSurface = Color(0xFF1E293B), // Slate-800
            error = Color(0xFFE11D48),
            onError = Color.White,
            outline = Color(0xFFE2E8F0), // Slate-200 border
            onSurfaceVariant = Color(0xFF64748B)
        )
    }

    MaterialTheme(colorScheme = customColorScheme) {
        SmartphoneSimulator(isDark = isDark) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface, // matching surface background
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape) // Subtle line border
                    ) {
                        GiroTab.values().forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF10B981),
                                    selectedTextColor = Color(0xFF10B981),
                                    unselectedIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8), // slate-500 or slate-400
                                    unselectedTextColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    indicatorColor = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5) // soft dark/light emerald capsule
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "tab_animation"
                    ) { tab ->
                        when (tab) {
                            GiroTab.PAINEL -> DashboardScreen(viewModel, vehicle, parts, records)
                            GiroTab.LANCAR -> LaunchScreen(viewModel, vehicle, parts)
                            GiroTab.RELATORIOS -> ReportsScreen(viewModel, vehicle, parts, records)
                            GiroTab.VEICULO -> VehicleScreen(viewModel, vehicle, parts)
                            GiroTab.HISTORICO -> HistoryScreen(viewModel, vehicle, records)
                        }
                    }
                }
            }
        }
    }
}
