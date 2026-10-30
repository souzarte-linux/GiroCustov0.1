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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.foundation.clickable
import com.example.ui.AccountConflictState
import com.example.ui.GiroCustoViewModel
import com.example.ui.components.CloudSyncTopBarIcon
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LaunchScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PlatformsScreen
import com.example.ui.screens.FuelRefillsScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.ThemeSelectionScreen
import com.example.ui.theme.getThemeColorScheme
import kotlinx.coroutines.launch

enum class GiroTab(val title: String, val icon: ImageVector) {
    PAINEL("Painel", Icons.Filled.Dashboard),
    LANCAR("Lançar", Icons.Filled.AddCircle),
    RELATORIOS("Relatórios", Icons.Filled.Analytics),
    HISTORICO("Histórico", Icons.Filled.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiroCustoMainApp(viewModel: GiroCustoViewModel) {
    var currentTab by remember { mutableStateOf(GiroTab.PAINEL) }
    var drawerDestination by remember { mutableStateOf(DrawerDestination.NONE) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val maintenanceRecords by viewModel.maintenanceRecords.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val accountConflict by viewModel.accountConflictState.collectAsStateWithLifecycle()
    val showDiscardConfirm by viewModel.showDiscardDoubleConfirmationDialog.collectAsStateWithLifecycle()
    val showClaimConfirm by viewModel.showClaimDataConfirmationDialog.collectAsStateWithLifecycle()
    val isSwitchingAccount by viewModel.isAccountSwitching.collectAsStateWithLifecycle()

    // Resolução de Tema Dinâmico GiroCusto (4 temas personalizáveis com modo Claro/Escuro)
    val customColorScheme = getThemeColorScheme(currentTheme, isDark)

    val currentScreen = remember(currentTab, drawerDestination) {
        if (drawerDestination != DrawerDestination.NONE) {
            drawerDestination
        } else {
            currentTab
        }
    }

    MaterialTheme(colorScheme = customColorScheme) {
        AppDrawer(
            drawerState = drawerState,
            selectedDestination = drawerDestination,
            onDestinationSelected = { dest ->
                drawerDestination = dest
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = when (currentScreen) {
                                        DrawerDestination.PERFIL -> "Meu Perfil & Veículos"
                                        DrawerDestination.PLATAFORMAS -> "Plataformas"
                                        DrawerDestination.ABASTECIMENTOS -> "Abastecimentos"
                                        DrawerDestination.MANUTENCAO -> "Manutenção"
                                        DrawerDestination.NUVEM -> "Nuvem & Backup"
                                        DrawerDestination.TEMAS -> "Temas"
                                        GiroTab.PAINEL -> "Painel"
                                        GiroTab.LANCAR -> "Lançar"
                                        GiroTab.RELATORIOS -> "Relatórios"
                                        GiroTab.HISTORICO -> "Histórico"
                                        else -> "GiroCusto"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            navigationIcon = {
                                if (drawerDestination != DrawerDestination.NONE) {
                                    IconButton(
                                        onClick = { drawerDestination = DrawerDestination.NONE },
                                        modifier = Modifier.testTag("topbar_back_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Voltar"
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                drawerState.open()
                                            }
                                        },
                                        modifier = Modifier.testTag("drawer_menu_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Abrir Menu"
                                        )
                                    }
                                }
                            },
                            actions = {
                                CloudSyncTopBarIcon(
                                    currentUser = currentUser,
                                    syncState = syncState,
                                    onClick = {
                                        drawerDestination = DrawerDestination.NUVEM
                                    }
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface, // matching surface background
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape) // Subtle line border
                        ) {
                            GiroTab.values().forEach { tab ->
                                val selected = drawerDestination == DrawerDestination.NONE && currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        drawerDestination = DrawerDestination.NONE
                                        currentTab = tab
                                    },
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
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8), // slate-500 or slate-400
                                        unselectedTextColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
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
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_animation"
                        ) { screen ->
                            when (screen) {
                                DrawerDestination.PERFIL -> ProfileScreen(viewModel, onNavigateToPlatforms = { drawerDestination = DrawerDestination.PLATAFORMAS })
                                DrawerDestination.PLATAFORMAS -> PlatformsScreen(viewModel)
                                DrawerDestination.ABASTECIMENTOS -> FuelRefillsScreen(viewModel)
                                DrawerDestination.MANUTENCAO -> MaintenanceScreen(viewModel)
                                DrawerDestination.NUVEM -> CloudSyncScreen(viewModel, onNavigateBack = { drawerDestination = DrawerDestination.NONE })
                                DrawerDestination.TEMAS -> ThemeSelectionScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { drawerDestination = DrawerDestination.NONE }
                                )
                                GiroTab.PAINEL -> DashboardScreen(
                                    viewModel = viewModel,
                                    vehicle = vehicle,
                                    parts = parts,
                                    records = records,
                                    maintenanceRecords = maintenanceRecords,
                                    onNavigateToLaunch = { currentTab = GiroTab.LANCAR },
                                    onNavigateToRefill = { drawerDestination = DrawerDestination.ABASTECIMENTOS }
                                )
                                GiroTab.LANCAR -> LaunchScreen(viewModel, vehicle, parts)
                                GiroTab.RELATORIOS -> ReportsScreen(
                                    viewModel = viewModel,
                                    vehicle = vehicle,
                                    parts = parts,
                                    records = records,
                                    maintenanceRecords = maintenanceRecords,
                                    onNavigateToLaunch = { currentTab = GiroTab.LANCAR },
                                    onNavigateToRefill = { drawerDestination = DrawerDestination.ABASTECIMENTOS }
                                )
                                GiroTab.HISTORICO -> HistoryScreen(
                                    viewModel = viewModel,
                                    vehicle = vehicle,
                                    records = records,
                                    onNavigateToLaunch = { currentTab = GiroTab.LANCAR }
                                )
                            }
                        }

                        // Overlay de carregamento durante troca de conta (nunca projeta dados da conta anterior)
                        if (isSwitchingAccount) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                                    .clickable(enabled = true, onClick = {}),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Carregando dados da nova conta...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        // Diálogo central de resolução de conflito de dados locais
                        if (accountConflict != null) {
                            val conflict = accountConflict
                            AlertDialog(
                                onDismissRequest = { /* Bloqueante: exige decisão do usuário */ },
                                title = {
                                    Text(
                                        text = if (conflict is AccountConflictState.UnknownOwnerWithPending)
                                            "Lançamentos não sincronizados detectados"
                                        else
                                            "Lançamentos de outra conta detectados",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Este celular contém ${conflict?.pendingCount ?: 0} lançamento(s) que ainda não foram sincronizados com a nuvem. Se você continuar na conta atual sem sincronizar, esses registros não enviados poderão ser perdidos.",
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "O que deseja fazer?",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                confirmButton = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.resolveConflictReturnToPreviousAccount() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Voltar e entrar com a conta anterior")
                                        }
                                        if (conflict is AccountConflictState.UnknownOwnerWithPending) {
                                            OutlinedButton(
                                                onClick = { viewModel.openClaimDataConfirmation() },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Estes dados são meus, usar nesta conta")
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.openDiscardConfirmation() },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("Descartar dados")
                                        }
                                    }
                                },
                                dismissButton = {}
                            )
                        }

                        // Confirmação dupla para descarte permanente
                        if (showDiscardConfirm) {
                            AlertDialog(
                                onDismissRequest = { viewModel.dismissDiscardConfirmation() },
                                title = {
                                    Text(
                                        text = "Confirmação de Descarte Permanente",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Tem certeza absoluta? Os dados não sincronizados serão excluídos deste aparelho de forma permanente e não poderão ser recuperados.",
                                        fontSize = 14.sp
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { viewModel.confirmDiscardData() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Sim, descartar permanentemente")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { viewModel.dismissDiscardConfirmation() }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }

                        // Confirmação para apropriação dos dados ("Estes dados são meus")
                        if (showClaimConfirm) {
                            AlertDialog(
                                onDismissRequest = { viewModel.dismissClaimDataConfirmation() },
                                title = {
                                    Text(
                                        text = "Vincular dados à conta atual?",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Deseja vincular todos os lançamentos locais à conta atual e enviá-los para a sua nuvem no Supabase?",
                                        fontSize = 14.sp
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { viewModel.confirmClaimData() }
                                    ) {
                                        Text("Sim, vincular e sincronizar")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { viewModel.dismissClaimDataConfirmation() }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
