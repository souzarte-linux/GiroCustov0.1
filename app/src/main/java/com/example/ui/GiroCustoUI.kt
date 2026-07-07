package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyRecord
import com.example.data.Vehicle
import com.example.data.VehiclePart
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Enum de Telas do GiroCusto
enum class GiroTab(val title: String, val icon: ImageVector) {
    PAINEL("Painel", Icons.Filled.Dashboard),
    LANCAR("Lançar", Icons.Filled.AddCircle),
    RELATORIOS("Relatórios", Icons.Filled.Analytics),
    VEICULO("Veículo", Icons.Filled.TwoWheeler),
    HISTORICO("Histórico", Icons.Filled.History)
}

// Smartphone Simulator Wrapper
@Composable
fun SmartphoneSimulator(
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0E)), // Ultra dark charcoal/black surrounding frame for premium feel
        contentAlignment = Alignment.Center
    ) {
        val maxWidth = maxWidth
        val isDesktop = maxWidth > 480.dp

        if (isDesktop) {
            val phoneBg = if (isDark) Color(0xFF121214) else Color(0xFFF8FAFC)
            val phoneBorder = if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)
            val phoneText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

            // Desenha um mock realista de smartphone centralizado na tela
            Box(
                modifier = Modifier
                    .width(390.dp)
                    .height(820.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .border(8.dp, phoneBorder, RoundedCornerShape(40.dp)) // Adaptive border
                    .background(phoneBg) // Adaptive background
                    .shadow(16.dp, RoundedCornerShape(40.dp))
            ) {
                // Layout interno do celular simulado
                Column(modifier = Modifier.fillMaxSize().background(phoneBg)) {
                    // Barra superior com Notch / Ilha Dinâmica simulada
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(phoneBg) // Seamless adaptive background
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Horário Simulado
                        Text(
                            text = "19:45",
                            color = phoneText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )

                        // Notch da Câmera (Ilha Dinâmica compacta)
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)) // Notch
                        )

                        // Ícones de Status (Bateria, Wifi, 5G)
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "5G",
                                color = phoneText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Filled.Wifi, "Wifi", tint = phoneText, modifier = Modifier.size(12.dp))
                            Icon(Icons.Filled.Battery5Bar, "Bateria", tint = phoneText, modifier = Modifier.size(12.dp))
                        }
                    }

                    // Conteúdo Principal do Applet
                    Box(modifier = Modifier.weight(1f)) {
                        content()
                    }

                    // Linha Inferior do Gesto do Smartphone
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .background(phoneBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)) // gesture bar
                        )
                    }
                }
            }
        } else {
            // Em smartphone real, roda em tela cheia direta
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

// Logo do App GiroCusto
@Composable
fun GiroCustoLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF10B981), Color(0xFF059669)) // Emerald gradient
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = size.minDimension / 1.5f,
                center = Offset(0f, 0f)
            )
        }
        Icon(
            imageVector = Icons.Filled.TwoWheeler,
            contentDescription = "GiroCusto Logo Icon",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

// Elemento Principal GiroCusto
@OptIn(ExperimentalMaterial3Api::class)
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

// ------------------- TELA 1: DASHBOARD (PAINEL) -------------------
@Composable
fun DashboardScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>,
    records: List<DailyRecord>
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val customStart by viewModel.customStartDate.collectAsStateWithLifecycle()
    val customEnd by viewModel.customEndDate.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showCustomPeriodDialog by remember { mutableStateOf(false) }

    // Filtrar registros por período
    val filteredRecords = remember(records, selectedPeriod, customStart, customEnd) {
        val now = System.currentTimeMillis()
        if (selectedPeriod == Period.PERSONALIZADO) {
            val calStart = Calendar.getInstance().apply {
                timeInMillis = customStart
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calEnd = Calendar.getInstance().apply {
                timeInMillis = customEnd
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            records.filter { it.dateTimestamp in calStart.timeInMillis..calEnd.timeInMillis }
        } else {
            val limit = when (selectedPeriod) {
                Period.SEMANA -> now - 7L * 24 * 60 * 60 * 1000
                Period.QUINZENA -> now - 15L * 24 * 60 * 60 * 1000
                Period.MENSAL -> now - 30L * 24 * 60 * 60 * 1000
                else -> 0L
            }
            records.filter { it.dateTimestamp >= limit }
        }
    }

    // Cálculos de Métricas
    val totalGross = filteredRecords.sumOf { it.grossEarnings }
    val totalFuel = filteredRecords.sumOf { it.fuelCost }
    val totalWear = filteredRecords.sumOf { it.wearCost }
    val totalFixed = filteredRecords.sumOf { it.proportionalFixedCost }
    val totalFood = filteredRecords.sumOf { it.foodExpense }
    val totalExpenses = totalFuel + totalWear + totalFixed + totalFood
    val netProfit = totalGross - totalExpenses
    
    val totalKm = filteredRecords.sumOf { it.kmRodados }
    val costPerKm = if (totalKm > 0) totalExpenses / totalKm else 0.0
    val netProfitPerKm = if (totalKm > 0) netProfit / totalKm else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título e Header (Professional Polish Header Style with App Logo and Theme Toggle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GiroCustoLogo()
                Column {
                    Text(
                        text = "GiroCusto",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground, // Dynamic color
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = (vehicle?.model ?: "Sem veículo cadastrado").uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981), // Emerald accent color
                        letterSpacing = 1.5.sp
                    )
                }
            }
            
            // Theme Toggle and Period Selector Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.toggleTheme() },
                    modifier = Modifier.size(36.dp).testTag("theme_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Alternar Tema",
                        tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                var dropdownExpanded by remember { mutableStateOf(false) }

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface) // Dynamic Surface Charcoal
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(10.dp))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val selectedLabel = when (selectedPeriod) {
                            Period.SEMANA -> "Semana"
                            Period.QUINZENA -> "Quinzena"
                            Period.MENSAL -> "Mensal"
                            Period.PERSONALIZADO -> "Personalizado"
                        }
                        Text(
                            text = selectedLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Selecionar Período",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semana", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setPeriod(Period.SEMANA)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Quinzena", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setPeriod(Period.QUINZENA)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mensal", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setPeriod(Period.MENSAL)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Personalizado", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setPeriod(Period.PERSONALIZADO)
                                dropdownExpanded = false
                                showCustomPeriodDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (showCustomPeriodDialog) {
            CustomPeriodDialog(
                initialStart = customStart,
                initialEnd = customEnd,
                onDismiss = { showCustomPeriodDialog = false },
                onConfirm = { start, end ->
                    viewModel.setCustomPeriod(start, end)
                    showCustomPeriodDialog = false
                }
            )
        }

        if (selectedPeriod == Period.PERSONALIZADO) {
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .clickable { showCustomPeriodDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = "Calendário",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Filtro: ${dateFormat.format(Date(customStart))} até ${dateFormat.format(Date(customEnd))}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Alterar Período",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Card de Lucro Líquido Real (Polished Charcoal & Glowing Emerald Style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)), // Deep dark green/emerald
            shape = RoundedCornerShape(24.dp), // Rounded-3xl
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), // Emerald soft border glow
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
                    val labelPeriod = when (selectedPeriod) {
                        Period.SEMANA -> "LUCRO LÍQUIDO (SEMANA)"
                        Period.QUINZENA -> "LUCRO LÍQUIDO (QUINZENA)"
                        Period.MENSAL -> "LUCRO LÍQUIDO (MENSAL)"
                        Period.PERSONALIZADO -> {
                            val startStr = displayFormat.format(Date(customStart))
                            val endStr = displayFormat.format(Date(customEnd))
                            "LUCRO LÍQUIDO ($startStr - $endStr)"
                        }
                    }
                    Text(
                        text = labelPeriod,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399), // Soft vibrant emerald
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Dinheiro no Bolso",
                            color = Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "R$ ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981), // Emerald
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "%,.2f", netProfit),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White // High contrast White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFF10B981).copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Faturamento Bruto", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format(Locale.GERMAN, "R$ %,.2f", totalGross),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Custo Operacional", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format(Locale.GERMAN, "- R$ %,.2f", totalExpenses),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF43F5E) // Bright light rose/red for cost in dark mode
                        )
                    }
                }
            }
        }

        // Grid de Indicadores de Eficiência (Polished Charcoal Cards with borders & mini progress tracks)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2D34)), // Dark Slate border
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalGasStation, "Custo por km", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custo Real / Km", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", costPerKm),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (costPerKm / 1.0).coerceIn(0.0, 1.0).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = Color(0xFFFBBF24), // amber-400
                        trackColor = Color(0xFF2D2D34) // dark charcoal track
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2D34)), // Dark Slate border
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TrendingUp, "Lucro por km", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lucro Líq / Km", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", netProfitPerKm),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (netProfitPerKm / 2.0).coerceIn(0.0, 1.0).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = Color(0xFF10B981), // Emerald
                        trackColor = Color(0xFF2D2D34) // dark charcoal track
                    )
                }
            }
        }

        // Alertas Críticos de Desgaste de Peças (> 85%) (Professional Polish Alert Banner Style)
        val criticalParts = remember(parts) {
            parts.filter { it.wearPercentage >= 85.0 }
        }

        if (criticalParts.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ATENÇÃO MANUTENÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E), // rose-500 for high visibility
                    letterSpacing = 1.sp
                )
                
                criticalParts.forEach { part ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1018)), // deep ruby/dark charcoal
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f)) // rose glow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = part.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.GERMAN, "Desgaste crítico de %.1f%% (%d/%d km)", part.wearPercentage, part.runKmSinceChange.toInt(), part.lifespanKm.toInt()),
                                    color = Color(0xFFFDA4AF), // rose-300 soft pink
                                    fontSize = 11.sp
                                )
                            }
                            
                            Button(
                                onClick = { viewModel.resetPartWear(part.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)), // rose-500
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Troquei", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Seção: Metas Ativas (Diária, Semanal e Mensal) (Professional Polish Sections Style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "METAS DE DESEMPENHO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981), // Emerald accent
                letterSpacing = 1.sp
            )
        }

        // Carregar valores de conquista para as metas
        val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
        val todayRecords = remember(records, todayStr) { records.filter { it.dateString == todayStr } }
        
        // Conquistas Atuais
        val dailyGrossAct = todayRecords.sumOf { it.grossEarnings }
        val dailyNetAct = todayRecords.sumOf { it.netProfit }
        val dailyKmAct = todayRecords.sumOf { it.kmRodados }
        val dailyDelAct = todayRecords.sumOf { it.deliveriesCount }

        val weeklyRecords = remember(records) {
            val limit = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            records.filter { it.dateTimestamp >= limit }
        }
        val weeklyGrossAct = weeklyRecords.sumOf { it.grossEarnings }
        val weeklyNetAct = weeklyRecords.sumOf { it.netProfit }
        val weeklyKmAct = weeklyRecords.sumOf { it.kmRodados }
        val weeklyDelAct = weeklyRecords.sumOf { it.deliveriesCount }

        val monthlyRecords = remember(records) {
            val limit = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            records.filter { it.dateTimestamp >= limit }
        }
        val monthlyGrossAct = monthlyRecords.sumOf { it.grossEarnings }
        val monthlyNetAct = monthlyRecords.sumOf { it.netProfit }
        val monthlyKmAct = monthlyRecords.sumOf { it.kmRodados }
        val monthlyDelAct = monthlyRecords.sumOf { it.deliveriesCount }

        // Carregar metas configuradas
        val dailyGGoal by viewModel.dailyGoalGross.collectAsStateWithLifecycle()
        val dailyNGoal by viewModel.dailyGoalNet.collectAsStateWithLifecycle()
        val dailyKGoal by viewModel.dailyGoalKm.collectAsStateWithLifecycle()
        val dailyDGoal by viewModel.dailyGoalDeliveries.collectAsStateWithLifecycle()

        val weeklyGGoal by viewModel.weeklyGoalGross.collectAsStateWithLifecycle()
        val weeklyNGoal by viewModel.weeklyGoalNet.collectAsStateWithLifecycle()
        val weeklyKGoal by viewModel.weeklyGoalKm.collectAsStateWithLifecycle()
        val weeklyDGoal by viewModel.weeklyGoalDeliveries.collectAsStateWithLifecycle()

        val monthlyGGoal by viewModel.monthlyGoalGross.collectAsStateWithLifecycle()
        val monthlyNGoal by viewModel.monthlyGoalNet.collectAsStateWithLifecycle()
        val monthlyKGoal by viewModel.monthlyGoalKm.collectAsStateWithLifecycle()
        val monthlyDGoal by viewModel.monthlyGoalDeliveries.collectAsStateWithLifecycle()

        // Estado para Edição de Metas
        var showGoalsDialog by remember { mutableStateOf(false) }

        // Renderizar os 3 Cards de Meta
        GoalCard(title = "Meta Diária (Hoje)", grossAct = dailyGrossAct, grossGoal = dailyGGoal, netAct = dailyNetAct, netGoal = dailyNGoal, kmAct = dailyKmAct, kmGoal = dailyKGoal, delAct = dailyDelAct, delGoal = dailyDGoal)
        GoalCard(title = "Meta Semanal (7 dias)", grossAct = weeklyGrossAct, grossGoal = weeklyGGoal, netAct = weeklyNGoal, netGoal = weeklyNGoal, kmAct = weeklyKmAct, kmGoal = weeklyKGoal, delAct = weeklyDelAct, delGoal = weeklyDGoal)
        GoalCard(title = "Meta Mensal (30 dias)", grossAct = monthlyGrossAct, grossGoal = monthlyGGoal, netAct = monthlyNetAct, netGoal = monthlyNGoal, kmAct = monthlyKmAct, kmGoal = monthlyKGoal, delAct = monthlyDelAct, delGoal = monthlyDGoal)

        // Botão para Ajustar Metas (Polished Green Outlined Style)
        OutlinedButton(
            onClick = { showGoalsDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
            border = BorderStroke(1.2.dp, Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Edit, "Editar Metas", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ajustar Valores de Metas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        if (showGoalsDialog) {
            EditGoalsDialog(
                viewModel = viewModel,
                onDismiss = { showGoalsDialog = false }
            )
        }
    }
}

@Composable
fun CustomPeriodDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    var startTimestamp by remember { mutableStateOf(initialStart) }
    var endTimestamp by remember { mutableStateOf(initialEnd) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Período Personalizado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Start Date selector
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = startTimestamp }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                startTimestamp = newCal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("De:", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 14.sp)
                        Text(
                            text = dateFormat.format(Date(startTimestamp)),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 14.sp
                        )
                    }
                }

                // End Date selector
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = endTimestamp }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }
                                endTimestamp = newCal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Até:", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 14.sp)
                        Text(
                            text = dateFormat.format(Date(endTimestamp)),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (startTimestamp <= endTimestamp) {
                                onConfirm(startTimestamp, endTimestamp)
                            } else {
                                android.widget.Toast.makeText(context, "A data inicial deve ser anterior à data final!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Aplicar", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    grossAct: Double, grossGoal: Double,
    netAct: Double, netGoal: Double,
    kmAct: Double, kmGoal: Double,
    delAct: Int, delGoal: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark charcoal border
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Linhas de Progresso das sub-metas
            GoalRow(label = "Faturamento", value = grossAct, target = grossGoal, format = "R$ %.0f")
            GoalRow(label = "Lucro Líquido", value = netAct, target = netGoal, format = "R$ %.0f")
            GoalRow(label = "Kms Rodados", value = kmAct, target = kmGoal, format = "%.0f km")
            GoalRowInt(label = "Entregas", value = delAct, target = delGoal)
        }
    }
}

@Composable
fun GoalRow(label: String, value: Double, target: Double, format: String) {
    val pct = if (target > 0) (value / target).coerceIn(0.0, 1.0) else 1.0
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8)) // slate-400
            Text(
                text = "${String.format(Locale.GERMAN, format, value)} / ${String.format(Locale.GERMAN, format, target)} (${(pct * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (value >= target) Color(0xFF10B981) else Color(0xFFCBD5E1) // Emerald or Slate-300
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = pct.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = if (value >= target) Color(0xFF10B981) else Color(0xFF34D399), // Emerald/Mint
            trackColor = Color(0xFF2D2D34) // Dark charcoal
        )
    }
}

@Composable
fun GoalRowInt(label: String, value: Int, target: Int) {
    val pct = if (target > 0) (value.toDouble() / target).coerceIn(0.0, 1.0) else 1.0
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
            Text(
                text = "$value / $target entregas (${(pct * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (value >= target) Color(0xFF10B981) else Color(0xFFCBD5E1)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = pct.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = if (value >= target) Color(0xFF10B981) else Color(0xFF34D399), // Emerald/Mint
            trackColor = Color(0xFF2D2D34) // Dark charcoal
        )
    }
}

@Composable
fun EditGoalsDialog(
    viewModel: GiroCustoViewModel,
    onDismiss: () -> Unit
) {
    // Pegar valores atuais
    var dg by remember { mutableStateOf(viewModel.dailyGoalGross.value.toString()) }
    var dn by remember { mutableStateOf(viewModel.dailyGoalNet.value.toString()) }
    var dk by remember { mutableStateOf(viewModel.dailyGoalKm.value.toString()) }
    var dd by remember { mutableStateOf(viewModel.dailyGoalDeliveries.value.toString()) }

    var wg by remember { mutableStateOf(viewModel.weeklyGoalGross.value.toString()) }
    var wn by remember { mutableStateOf(viewModel.weeklyGoalNet.value.toString()) }
    var wk by remember { mutableStateOf(viewModel.weeklyGoalKm.value.toString()) }
    var wd by remember { mutableStateOf(viewModel.weeklyGoalDeliveries.value.toString()) }

    var mg by remember { mutableStateOf(viewModel.monthlyGoalGross.value.toString()) }
    var mn by remember { mutableStateOf(viewModel.monthlyGoalNet.value.toString()) }
    var mk by remember { mutableStateOf(viewModel.monthlyGoalKm.value.toString()) }
    var md by remember { mutableStateOf(viewModel.monthlyGoalDeliveries.value.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)), // Charcoal background
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)) // Dark border
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ajustar Metas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Divider(color = Color(0xFF2D2D34))

                // Diária
                Text("METAS DIÁRIAS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = dg, onValueChange = { dg = it }, label = { Text("Faturamento") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(value = dn, onValueChange = { dn = it }, label = { Text("Lucro Líq") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = dk, onValueChange = { dk = it }, label = { Text("Km") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(value = dd, onValueChange = { dd = it }, label = { Text("Entregas") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Semanal
                Text("METAS SEMANAIS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = wg, onValueChange = { wg = it }, label = { Text("Faturamento") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(value = wn, onValueChange = { wn = it }, label = { Text("Lucro Líq") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = wk, onValueChange = { wk = it }, label = { Text("Km") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(value = wd, onValueChange = { wd = it }, label = { Text("Entregas") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Mensal
                Text("METAS MENSAIS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = mg, onValueChange = { mg = it }, label = { Text("Faturamento") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(value = mn, onValueChange = { mn = it }, label = { Text("Lucro Líq") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = mk, onValueChange = { mk = it }, label = { Text("Km") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(value = md, onValueChange = { md = it }, label = { Text("Entregas") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color(0xFF94A3B8)) // Slate-400 for cancel in dark mode
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateGoals(
                                dg.toDoubleOrNull() ?: 200.0,
                                dn.toDoubleOrNull() ?: 130.0,
                                dk.toDoubleOrNull() ?: 100.0,
                                dd.toIntOrNull() ?: 15,
                                wg.toDoubleOrNull() ?: 1200.0,
                                wn.toDoubleOrNull() ?: 800.0,
                                wk.toDoubleOrNull() ?: 600.0,
                                wd.toIntOrNull() ?: 90,
                                mg.toDoubleOrNull() ?: 5000.0,
                                mn.toDoubleOrNull() ?: 3200.0,
                                mk.toDoubleOrNull() ?: 2500.0,
                                md.toIntOrNull() ?: 400
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF064E3B)
                        )
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                    }
                }
            }
        }
    }
}


// ------------------- TELA 2: LANÇAR NOVO DIA -------------------
@Composable
fun LaunchScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val startOdo by viewModel.startOdometer.collectAsStateWithLifecycle()
    val endOdo by viewModel.endOdometer.collectAsStateWithLifecycle()
    val gross by viewModel.grossEarnings.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveriesCount.collectAsStateWithLifecycle()
    val fuelPr by viewModel.fuelPrice.collectAsStateWithLifecycle()
    val foodExp by viewModel.foodExpense.collectAsStateWithLifecycle()
    val platformVal by viewModel.platform.collectAsStateWithLifecycle()

    val estimate by viewModel.realTimeEstimation.collectAsStateWithLifecycle(initialValue = EstimationDetail())

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            android.widget.Toast.makeText(context, snackbarMessage, android.widget.Toast.LENGTH_LONG).show()
            snackbarMessage = null
        }
    }

    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lançar Turno do Dia",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, // Dynamic color
                fontFamily = FontFamily.SansSerif
            )

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.size(36.dp).testTag("theme_toggle_btn_launch")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = "Insira as informações de hodômetro e faturamento do seu fechamento de turno.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Dynamic color
        )

        // Formulário de Entradas (Polished Charcoal Input Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)), // Dark border
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val launchDate by viewModel.launchDateTimestamp.collectAsStateWithLifecycle()
                val dateDisplayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                val dateStr = remember(launchDate) { dateDisplayFormat.format(Date(launchDate)) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = launchDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    viewModel.setLaunchDate(newCal.timeInMillis)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("launch_date_picker_btn"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Selecionar Data",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Data do Lançamento",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar Data",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                TextField(
                    value = platformVal,
                    onValueChange = { viewModel.platform.value = it },
                    label = { Text("Plataforma (ex: iFood, Uber, Rappi)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_platform"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = "Plataforma",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = startOdo,
                        onValueChange = { viewModel.startOdometer.value = it },
                        label = { Text("Hodômetro Inicial") },
                        modifier = Modifier.weight(1f).testTag("input_start_odometer"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    TextField(
                        value = endOdo,
                        onValueChange = { viewModel.endOdometer.value = it },
                        label = { Text("Hodômetro Final") },
                        modifier = Modifier.weight(1f).testTag("input_end_odometer"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = gross,
                        onValueChange = { viewModel.grossEarnings.value = it },
                        label = { Text("Ganhos Brutos (R$)") },
                        modifier = Modifier.weight(1f).testTag("input_gross_earnings"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    TextField(
                        value = deliveries,
                        onValueChange = { viewModel.deliveriesCount.value = it },
                        label = { Text("Nº Entregas") },
                        modifier = Modifier.weight(1f).testTag("input_deliveries_count"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = fuelPr,
                        onValueChange = { viewModel.fuelPrice.value = it },
                        label = { Text("Preço do Litro (R$)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    TextField(
                        value = foodExp,
                        onValueChange = { viewModel.foodExpense.value = it },
                        label = { Text("Alimentação / Outros") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }

        // ESTIMADOR EM TEMPO REAL (Painel de Alta Tecnologia em tons de Slate e Verde Esmeralda)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), // Glowing emerald border
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTIMADOR EM TEMPO REAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981), // Emerald
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF064E3B)) // Soft emerald dark background
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.GERMAN, "%.1f", estimate.distance)} KM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF34D399) // Mint/soft emerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lucro Líquido Estimado com destaque grande
                Text("Lucro Líquido Projetado:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                    text = String.format(Locale.GERMAN, "R$ %.2f", estimate.netProfit),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (estimate.netProfit >= 0) Color(0xFF10B981) else Color(0xFFF43F5E) // Emerald or Rose
                )

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFF2D2D34))
                Spacer(modifier = Modifier.height(14.dp))

                // Detalhamento de Despesas Projetadas
                Text("Detalhamento de Custos Operacionais:", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Combustível
                EstimationRow(label = "Combustível (Gasolina/Álcool)", value = estimate.fuelCost)
                // Desgaste oculto
                EstimationRow(
                    label = "Desgaste Oculto (Manutenção/Peças)", 
                    value = estimate.wearCost, 
                    tooltip = "Calculado de forma dinâmica dividindo o preço das peças pela sua vida útil"
                )
                // Custos fixos
                EstimationRow(label = "Custos Fixos Mensais Proporcionais", value = estimate.fixedCost)
                // Alimentação
                EstimationRow(label = "Alimentação / Despesas do dia", value = estimate.foodCost)

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color(0xFF2D2D34))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total de Gastos Diários:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", estimate.totalExpenses),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFBBF24) // Bright Amber
                    )
                }
            }
        }

        // Botão de Salvar Lançamento (Professional Emerald Style)
        Button(
            onClick = {
                isSaving = true
                viewModel.saveDailyRecord(
                    onSuccess = {
                        isSaving = false
                        snackbarMessage = "Turno de trabalho salvo com sucesso!"
                    },
                    onError = { err ->
                        isSaving = false
                        snackbarMessage = err
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                contentColor = Color(0xFF064E3B)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving
        ) {
            Icon(Icons.Filled.Save, "Salvar", tint = Color(0xFF064E3B))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Salvar Turno do Dia",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF064E3B)
            )
        }
    }
}

@Composable
fun EstimationRow(label: String, value: Double, tooltip: String? = null) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981))) // Emerald bullet
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label, 
                    fontSize = 11.sp, 
                    color = Color(0xFF94A3B8), // slate-400
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = String.format(Locale.GERMAN, "R$ %.2f", value),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (tooltip != null) {
            Text(tooltip, fontSize = 9.sp, color = Color(0xFF64748B), modifier = Modifier.padding(start = 12.dp))
        }
    }
}


// ------------------- TELA 3: RELATÓRIOS E DESEMPENHO -------------------
@Composable
fun ReportsScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>,
    records: List<DailyRecord>
) {
    val scrollState = rememberScrollState()

    // 1. Gráfico de Evolução de Lucro Líquido
    // Ordenar registros de forma cronológica (do mais antigo para o mais novo) para desenhar o gráfico da esquerda para a direita
    val chronologicalRecords = remember(records) {
        records.take(7).sortedBy { it.dateTimestamp }
    }

    // Cálculos Gerais do Período
    val totalEarnings = records.sumOf { it.grossEarnings }
    val totalExpenses = records.sumOf { it.fuelCost + it.wearCost + it.proportionalFixedCost + it.foodExpense }
    val netProfit = totalEarnings - totalExpenses
    val totalDeliveries = records.sumOf { it.deliveriesCount }

    // Destaques de Recordes
    val bestDay = remember(records) {
        records.maxByOrNull { it.netProfit }
    }
    val worstDay = remember(records) {
        // Dia com menor margem ou menor lucro
        records.minByOrNull { it.netProfit }
    }

    // Projeções futuras (Próximos 30 dias se trabalhar no mesmo ritmo médio)
    val avgEarningsPerDay = if (records.isNotEmpty()) totalEarnings / records.size else 0.0
    val avgProfitPerDay = if (records.isNotEmpty()) netProfit / records.size else 0.0
    val avgKmPerDay = if (records.isNotEmpty()) records.sumOf { it.kmRodados } / records.size else 0.0

    // Meta Mensal configurada para ver se alcança nas projeções
    val monthlyNGoal by viewModel.monthlyGoalNet.collectAsStateWithLifecycle()
    val monthlyWorkingDays = vehicle?.plannedWorkDays ?: 22
    val projectedMonthlyProfit = avgProfitPerDay * monthlyWorkingDays
    val projectedMonthlyEarnings = avgEarningsPerDay * monthlyWorkingDays

    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Relatórios de Desempenho",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground // Dynamic color
            )

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.size(36.dp).testTag("theme_toggle_btn_reports")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 1. GRAFICO DE EVOLUÇÃO DO LUCRO LÍQUIDO (Bézier personalizado com Gradiente Verde Esmeralda)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark charcoal border
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Evolução do Lucro Líquido Diário",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Ganhos reais limpos nos últimos 7 dias de entregas",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8) // slate-400
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (chronologicalRecords.isNotEmpty()) {
                    // Render do Gráfico Vetorial em Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        NetProfitCurveChart(records = chronologicalRecords)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Datas no rodapé
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                        chronologicalRecords.forEach { record ->
                            val cal = Calendar.getInstance().apply { timeInMillis = record.dateTimestamp }
                            Text(
                                text = displayFormat.format(cal.time),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8) // Slate-400
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Histórico sem registros suficientes para gráfico.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        // 2. GRAFICO DE DISTRIBUIÇÃO DE DESPESAS (Barra Segmentada Colorida)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark charcoal border
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Distribuição de Despesas Operacionais",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "A fatia de cada custo no orçamento total rodado",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Valores de Custo
                val totalFuelCost = records.sumOf { it.fuelCost }
                val totalWearCost = records.sumOf { it.wearCost }
                val totalFixedCost = records.sumOf { it.proportionalFixedCost }
                val totalFoodCost = records.sumOf { it.foodExpense }
                val sumAllExpenses = totalFuelCost + totalWearCost + totalFixedCost + totalFoodCost

                if (sumAllExpenses > 0) {
                    val pctFuel = (totalFuelCost / sumAllExpenses).toFloat()
                    val pctWear = (totalWearCost / sumAllExpenses).toFloat()
                    val pctFixed = (totalFixedCost / sumAllExpenses).toFloat()
                    val pctFood = (totalFoodCost / sumAllExpenses).toFloat()

                    // Barra Segmentada
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        if (pctFuel > 0) Box(modifier = Modifier.fillMaxHeight().weight(pctFuel.coerceAtLeast(0.01f)).background(Color(0xFFFF9100)))
                        if (pctFood > 0) Box(modifier = Modifier.fillMaxHeight().weight(pctFood.coerceAtLeast(0.01f)).background(Color(0xFF00B0FF)))
                        if (pctWear > 0) Box(modifier = Modifier.fillMaxHeight().weight(pctWear.coerceAtLeast(0.01f)).background(Color(0xFFEF5350)))
                        if (pctFixed > 0) Box(modifier = Modifier.fillMaxHeight().weight(pctFixed.coerceAtLeast(0.01f)).background(Color(0xFFAB47BC)))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Legendas com percentual e valor
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpenseLegendItem(label = "Combustível", color = Color(0xFFFF9100), pct = pctFuel, value = totalFuelCost)
                        ExpenseLegendItem(label = "Alimentação", color = Color(0xFF00B0FF), pct = pctFood, value = totalFoodCost)
                        ExpenseLegendItem(label = "Desgaste Oculto / Peças", color = Color(0xFFEF5350), pct = pctWear, value = totalWearCost)
                        ExpenseLegendItem(label = "Custos Fixos Proporcionais", color = Color(0xFFAB47BC), pct = pctFixed, value = totalFixedCost)
                    }
                } else {
                    Text("Nenhuma despesa registrada para cálculo.", fontSize = 12.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // 3. COMPARATIVO DE PRODUTIVIDADE (Faturamento Bruto vs Lucro Líquido Real)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark charcoal border
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Produtividade: Faturamento vs Lucro Líquido",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Visualização da eficiência financeira de cada turno lançado",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (chronologicalRecords.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        ProductivityComparisonChart(records = chronologicalRecords)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Legenda do gráfico
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF00B0FF)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Faturamento Bruto", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981))) // Emerald accent
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lucro Líquido", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                } else {
                    Text("Sem histórico suficiente.", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }

        // 4. DESTAQUES DE RECORDES (Melhor e Pior turnos) (Professional Polish Slate/Green Cards Style)
        if (bestDay != null || worstDay != null) {
            Text(
                text = "DESTAQUES E RECORDES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981), // Emerald accent
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (bestDay != null) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF022C22)), // Dark emerald container
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), // Emerald soft border glow
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.ArrowUpward, "Melhor Dia", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Melhor Turno", fontSize = 11.sp, color = Color(0xFF34D399))
                            Text(
                                text = String.format(Locale.GERMAN, "R$ %.2f", bestDay.netProfit),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = "no dia ${bestDay.dateString}",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (worstDay != null) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1018)), // deep ruby/dark charcoal
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)), // rose border glow
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.ArrowDownward, "Menor Turno", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Menor Turno", fontSize = 11.sp, color = Color(0xFFFDA4AF))
                            Text(
                                text = String.format(Locale.GERMAN, "R$ %.2f", worstDay.netProfit),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = "no dia ${worstDay.dateString}",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        // 5. RESUMO MENSAL E PROJEÇÕES FUTURAS (Light styled economy predictions card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark charcoal border
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShowChart, "Projeções", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Economia Acumulada e Projeções",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Simulação para o mês se trabalhar os $monthlyWorkingDays dias planejados no ritmo atual:",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Projeção de Faturamento", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = String.format(Locale.GERMAN, "R$ %.2f", projectedMonthlyEarnings),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8) // sky-400
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Projeção de Lucro Limpo", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = String.format(Locale.GERMAN, "R$ %.2f", projectedMonthlyProfit),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF10B981) // Emerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFF2D2D34))
                Spacer(modifier = Modifier.height(12.dp))

                // Progresso em relação ao Alvo Mensal
                val pctOfGoal = if (monthlyNGoal > 0) (projectedMonthlyProfit / monthlyNGoal).coerceIn(0.0, 1.5) else 1.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Alcançará ${(pctOfGoal*100).toInt()}% da meta mensal de Lucro", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = String.format(Locale.GERMAN, "Meta: R$ %.0f", monthlyNGoal),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (pctOfGoal).coerceIn(0.0, 1.0).toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF2D2D34)
                )
            }
        }
    }
}

@Composable
fun ExpenseLegendItem(label: String, color: Color, pct: Float, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8)) // slate-400 for dark mode
        }
        Text(
            text = "${String.format(Locale.GERMAN, "R$ %.2f", value)} (${(pct * 100).toInt()}%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
    }
}

// Custom SVG line-style graph with Bezier curve
@Composable
fun NetProfitCurveChart(records: List<DailyRecord>) {
    val maxProfit = (records.maxOfOrNull { it.netProfit } ?: 100.0).coerceAtLeast(10.0)
    val minProfit = (records.minOfOrNull { it.netProfit } ?: 0.0).coerceAtMost(0.0)
    val profitRange = (maxProfit - minProfit).coerceAtLeast(10.0)

    val emerald = Color(0xFF10B981) // Emerald accent green
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        val stepX = width / (records.size - 1).coerceAtLeast(1)
        
        val points = records.mapIndexed { idx, record ->
            val x = idx * stepX
            val yPct = (record.netProfit - minProfit) / profitRange
            val y = height - (yPct * height).toFloat()
            Offset(x, y)
        }

        // Desenhar grades horizontais simples (dark charcoal line for dark mode grid)
        val gridLines = 3
        for (i in 0..gridLines) {
            val yGrid = height * i / gridLines
            drawLine(
                color = Color(0xFF2D2D34),
                start = Offset(0f, yGrid),
                end = Offset(width, yGrid),
                strokeWidth = 1f
            )
        }

        // Criar caminho do gráfico de lucros
        val path = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, height)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 1 until points.size) {
                // Curva de Bézier suave
                val p0 = points[i - 1]
                val p1 = points[i]
                val controlX = (p0.x + p1.x) / 2
                path.cubicTo(
                    controlX, p0.y,
                    controlX, p1.y,
                    p1.x, p1.y
                )
                fillPath.cubicTo(
                    controlX, p0.y,
                    controlX, p1.y,
                    p1.x, p1.y
                )
            }

            fillPath.lineTo(points.last().x, height)
            fillPath.close()

            // Desenhar preenchimento de gradiente verde esmeralda
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(emerald.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Desenhar traçado da linha
            drawPath(
                path = path,
                color = emerald,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Desenhar pontos de destaque (Dots com borda branca e centro verde)
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = emerald,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
    }
}


// Custom bar-based comparison chart (Faturamento vs Lucro)
@Composable
fun ProductivityComparisonChart(records: List<DailyRecord>) {
    val maxVal = records.maxOfOrNull { it.grossEarnings.coerceAtLeast(it.netProfit) } ?: 100.0
    val maxScale = (maxVal * 1.15).coerceAtLeast(10.0)

    val colorGross = Color(0xFF00B0FF)
    val colorNet = Color(0xFF10B981) // Emerald accent green

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val totalBars = records.size
        
        val sectionWidth = width / totalBars
        val barWidth = (sectionWidth * 0.35f).coerceAtLeast(8f)
        val spacing = sectionWidth * 0.08f

        records.forEachIndexed { idx, record ->
            val sectionX = idx * sectionWidth
            
            // Faturamento Bar
            val grossH = (record.grossEarnings / maxScale) * height
            val grossX = sectionX + (sectionWidth / 2f) - barWidth - spacing
            val grossY = height - grossH
            drawRect(
                color = colorGross,
                topLeft = Offset(grossX, grossY.toFloat()),
                size = Size(barWidth, grossH.toFloat())
            )

            // Lucro Líquido Bar
            val netH = (record.netProfit.coerceAtLeast(0.0) / maxScale) * height
            val netX = sectionX + (sectionWidth / 2f) + spacing
            val netY = height - netH
            drawRect(
                color = colorNet,
                topLeft = Offset(netX, netY.toFloat()),
                size = Size(barWidth, netH.toFloat())
            )
        }
    }
}


// ------------------- TELA 4: VEÍCULO E CONFIGURAÇÃO -------------------
@Composable
fun VehicleScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    // Estado do veículo formulário
    var model by remember(vehicle) { mutableStateOf(vehicle?.model ?: "") }
    var consumption by remember(vehicle) { mutableStateOf(vehicle?.averageConsumption?.toString() ?: "") }
    var fuelType by remember(vehicle) { mutableStateOf(vehicle?.fuelType ?: "Gasolina") }
    var fixedCosts by remember(vehicle) { mutableStateOf(vehicle?.monthlyFixedCosts?.toString() ?: "") }
    var plannedDays by remember(vehicle) { mutableStateOf(vehicle?.plannedWorkDays?.toString() ?: "") }

    var showAddPartDialog by remember { mutableStateOf(false) }
    var showEditPartDialog by remember { mutableStateOf(false) }
    var partToEdit by remember { mutableStateOf<VehiclePart?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestão do Veículo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground // Dynamic color
            )

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.size(36.dp).testTag("theme_toggle_btn_vehicle")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // CARD: Cadastrar Veículo (Professional Polished Input Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark border
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Especificações do Veículo",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )

                TextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo do Veículo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = consumption,
                        onValueChange = { consumption = it },
                        label = { Text("Consumo Médio (km/L)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    // Combustível Tipo Dropdown simplificado
                    TextField(
                        value = fuelType,
                        onValueChange = { fuelType = it },
                        label = { Text("Tipo Combustível") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = fixedCosts,
                        onValueChange = { fixedCosts = it },
                        label = { Text("Custos Fixos Mensais (R$)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    TextField(
                        value = plannedDays,
                        onValueChange = { plannedDays = it },
                        label = { Text("Dias Trabalhados / Mês") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Salvar Configurações (Emerald Solid Button)
                Button(
                    onClick = {
                        val consVal = consumption.toDoubleOrNull() ?: 40.0
                        val fCostsVal = fixedCosts.toDoubleOrNull() ?: 180.0
                        val pDaysVal = plannedDays.toIntOrNull() ?: 22

                        viewModel.updateVehicle(model, consVal, fuelType, fCostsVal, pDaysVal)
                        android.widget.Toast.makeText(context, "Configurações do veículo atualizadas!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF064E3B)
                    )
                ) {
                    Icon(Icons.Filled.Save, "Salvar Veículo", tint = Color(0xFF064E3B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar Especificações", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                }
            }
        }

        // SEÇÃO: Peças de Desgaste Oculto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONITORAMENTO DE DESGASTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981), // Emerald
                letterSpacing = 1.sp
            )
            
            IconButton(onClick = { showAddPartDialog = true }) {
                Icon(Icons.Filled.Add, "Adicionar Peça", tint = Color(0xFF10B981))
            }
        }

        // Listagem de Peças com Barras de Progresso Coloridas
        if (parts.isNotEmpty()) {
            parts.forEach { part ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // Dynamic border
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(part.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text(
                                    text = String.format(Locale.GERMAN, "Troca: R$ %.2f | Vida útil: %d km", part.price, part.lifespanKm.toInt()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // Slate-400
                                )
                            }

                            // Botão rápido de reset, edição e exclusão
                            Row {
                                IconButton(
                                    onClick = {
                                        partToEdit = part
                                        showEditPartDialog = true
                                    },
                                    modifier = Modifier.size(28.dp).testTag("edit_part_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, "Editar Peça", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.resetPartWear(part.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Build, "Zerar Desgaste", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.deletePart(part) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, "Remover Peça", tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp)) // Rose/Red
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Barra de Progresso Colorida de Saúde
                        val health = part.healthPercentage
                        val barColor = when {
                            health <= 15.0 -> Color(0xFFF43F5E) // Vermelho crítico
                            health <= 35.0 -> Color(0xFFF59E0B) // Laranja atenção (Amber)
                            else -> Color(0xFF10B981) // Emerald
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.GERMAN, "Uso: %.0f km rodados", part.runKmSinceChange),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // slate-400
                            )
                            Text(
                                text = String.format(Locale.GERMAN, "Saúde: %.0f%%", health),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = (health / 100.0).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.outline // Dynamic track
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // Dynamic border
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma peça ou componente monitorado.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }

        if (showAddPartDialog) {
            AddPartDialog(
                onDismiss = { showAddPartDialog = false },
                onAdd = { name, price, lifespan ->
                    viewModel.addPart(name, price, lifespan)
                    showAddPartDialog = false
                    android.widget.Toast.makeText(context, "Nova peça monitorada cadastrada!", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showEditPartDialog && partToEdit != null) {
            EditPartDialog(
                part = partToEdit!!,
                onDismiss = {
                    showEditPartDialog = false
                    partToEdit = null
                },
                onSave = { name, price, lifespan, runKm ->
                    viewModel.updatePart(partToEdit!!.id, name, price, lifespan, runKm)
                    showEditPartDialog = false
                    partToEdit = null
                    android.widget.Toast.makeText(context, "Peça de desgaste atualizada!", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun AddPartDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var lifespan by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Dynamic background
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline) // Dynamic border
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Adicionar Peça de Desgaste", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Divider(color = MaterialTheme.colorScheme.outline)

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Peça (ex: Pneu Traseiro)") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço de Troca (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = lifespan,
                    onValueChange = { lifespan = it },
                    label = { Text("Durabilidade Estimada (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) // Dynamic color
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1.000
                            if (name.isNotBlank() && p > 0.0) {
                                onAdd(name, p, l)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Adicionar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPartDialog(
    part: VehiclePart,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(part.name) }
    var price by remember { mutableStateOf(part.price.toString()) }
    var lifespan by remember { mutableStateOf(part.lifespanKm.toString()) }
    var runKm by remember { mutableStateOf(part.runKmSinceChange.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Dynamic background
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline) // Dynamic border
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Editar Peça de Desgaste", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Divider(color = MaterialTheme.colorScheme.outline)

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Peça") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço de Troca (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = lifespan,
                    onValueChange = { lifespan = it },
                    label = { Text("Durabilidade Estimada (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = runKm,
                    onValueChange = { runKm = it },
                    label = { Text("Km Rodados Desde a Troca") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) // Dynamic color
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1.000
                            val r = runKm.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && p > 0.0) {
                                onSave(name, p, l, r)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

// ------------------- TELA 5: HISTÓRICO DE LANÇAMENTOS -------------------
enum class HistorySortOption(val label: String) {
    DATA_DECRESCENTE("Data decrescente"),
    DATA_CRESCENTE("Data crescente"),
    LUCRO_MAIOR("Lucro do Maior"),
    LUCRO_MENOR("Lucro do Menor"),
    KM_MAIOR("Por Maior KM"),
    KM_MENOR("Por Menor KM"),
    ENTREGAS("Quantidade Entregas")
}

@Composable
fun HistoryScreen(
    viewModel: GiroCustoViewModel,
    vehicle: com.example.data.Vehicle?,
    records: List<DailyRecord>
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<DailyRecord?>(null) }

    // Estados dos novos filtros
    var historyPeriod by remember { mutableStateOf(Period.SEMANA) }
    var historyCustomStart by remember { mutableStateOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000) }
    var historyCustomEnd by remember { mutableStateOf(System.currentTimeMillis()) }
    var showHistoryCustomPeriodDialog by remember { mutableStateOf(false) }

    val platformsList = remember(records) {
        listOf("Todas") + records.map { it.platform }.distinct().filter { it.isNotBlank() }
    }
    var selectedPlatformFilter by remember { mutableStateOf("Todas") }

    LaunchedEffect(platformsList) {
        if (selectedPlatformFilter !in platformsList) {
            selectedPlatformFilter = "Todas"
        }
    }

    var selectedSortOption by remember { mutableStateOf(HistorySortOption.DATA_DECRESCENTE) }

    var periodExpanded by remember { mutableStateOf(false) }
    var platformExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    // Filtragem e Ordenação
    val filteredAndSortedRecords = remember(records, historyPeriod, historyCustomStart, historyCustomEnd, selectedPlatformFilter, selectedSortOption) {
        val now = System.currentTimeMillis()
        
        // 1. Filtrar por Período
        val dateFiltered = if (historyPeriod == Period.PERSONALIZADO) {
            val calStart = Calendar.getInstance().apply {
                timeInMillis = historyCustomStart
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calEnd = Calendar.getInstance().apply {
                timeInMillis = historyCustomEnd
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            records.filter { it.dateTimestamp in calStart.timeInMillis..calEnd.timeInMillis }
        } else {
            val limit = when (historyPeriod) {
                Period.SEMANA -> now - 7L * 24 * 60 * 60 * 1000
                Period.QUINZENA -> now - 15L * 24 * 60 * 60 * 1000
                Period.MENSAL -> now - 30L * 24 * 60 * 60 * 1000
                else -> 0L
            }
            records.filter { it.dateTimestamp >= limit }
        }

        // 2. Filtrar por Plataforma
        val platformFiltered = if (selectedPlatformFilter == "Todas") {
            dateFiltered
        } else {
            dateFiltered.filter { it.platform.equals(selectedPlatformFilter, ignoreCase = true) }
        }

        // 3. Ordenar
        when (selectedSortOption) {
            HistorySortOption.DATA_DECRESCENTE -> platformFiltered.sortedByDescending { it.dateTimestamp }
            HistorySortOption.DATA_CRESCENTE -> platformFiltered.sortedBy { it.dateTimestamp }
            HistorySortOption.LUCRO_MAIOR -> platformFiltered.sortedByDescending { it.netProfit }
            HistorySortOption.LUCRO_MENOR -> platformFiltered.sortedBy { it.netProfit }
            HistorySortOption.KM_MAIOR -> platformFiltered.sortedByDescending { it.kmRodados }
            HistorySortOption.KM_MENOR -> platformFiltered.sortedBy { it.kmRodados }
            HistorySortOption.ENTREGAS -> platformFiltered.sortedByDescending { it.deliveriesCount }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Histórico",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.size(36.dp).testTag("theme_toggle_btn_history")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Dropdowns de Filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dropdown 1: Período
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(10.dp))
                        .clickable { periodExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val label = when (historyPeriod) {
                        Period.SEMANA -> "Semana"
                        Period.QUINZENA -> "Quinzena"
                        Period.MENSAL -> "Mensal"
                        Period.PERSONALIZADO -> "Personaliz."
                    }
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Selecionar Período",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = periodExpanded,
                    onDismissRequest = { periodExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Semana", fontSize = 13.sp) },
                        onClick = {
                            historyPeriod = Period.SEMANA
                            periodExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Quinzena", fontSize = 13.sp) },
                        onClick = {
                            historyPeriod = Period.QUINZENA
                            periodExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mensal", fontSize = 13.sp) },
                        onClick = {
                            historyPeriod = Period.MENSAL
                            periodExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Personalizado", fontSize = 13.sp) },
                        onClick = {
                            historyPeriod = Period.PERSONALIZADO
                            periodExpanded = false
                            showHistoryCustomPeriodDialog = true
                        }
                    )
                }
            }

            // Dropdown 2: Plataforma
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(10.dp))
                        .clickable { platformExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedPlatformFilter,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Filtrar Plataforma",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = platformExpanded,
                    onDismissRequest = { platformExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    platformsList.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p, fontSize = 13.sp) },
                            onClick = {
                                selectedPlatformFilter = p
                                platformExpanded = false
                            }
                        )
                    }
                }
            }

            // Dropdown 3: Ordenação
            Box(modifier = Modifier.weight(1.2f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(10.dp))
                        .clickable { sortExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val sortShortLabel = when (selectedSortOption) {
                        HistorySortOption.DATA_DECRESCENTE -> "Data Recente"
                        HistorySortOption.DATA_CRESCENTE -> "Data Antiga"
                        HistorySortOption.LUCRO_MAIOR -> "Maior Lucro"
                        HistorySortOption.LUCRO_MENOR -> "Menor Lucro"
                        HistorySortOption.KM_MAIOR -> "Maior KM"
                        HistorySortOption.KM_MENOR -> "Menor KM"
                        HistorySortOption.ENTREGAS -> "Entregas"
                    }
                    Text(
                        text = sortShortLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Ordenar por",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    HistorySortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label, fontSize = 13.sp) },
                            onClick = {
                                selectedSortOption = option
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (historyPeriod == Period.PERSONALIZADO) {
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .clickable { showHistoryCustomPeriodDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = "Calendário",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Filtro: ${dateFormat.format(Date(historyCustomStart))} até ${dateFormat.format(Date(historyCustomEnd))}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Alterar Período",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (filteredAndSortedRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (records.isEmpty()) "Nenhum turno lançado ainda." else "Nenhum lançamento corresponde aos filtros.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAndSortedRecords) { record ->
                    HistoryRecordCard(
                        record = record,
                        onEditClick = {
                            recordToEdit = record
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            viewModel.deleteDailyRecord(record)
                            android.widget.Toast.makeText(context, "Lançamento excluído com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showHistoryCustomPeriodDialog) {
        CustomPeriodDialog(
            initialStart = historyCustomStart,
            initialEnd = historyCustomEnd,
            onDismiss = { showHistoryCustomPeriodDialog = false },
            onConfirm = { start, end ->
                historyCustomStart = start
                historyCustomEnd = end
                showHistoryCustomPeriodDialog = false
            }
        )
    }

    if (showEditDialog && recordToEdit != null) {
        EditRecordDialog(
            record = recordToEdit!!,
            onDismiss = {
                showEditDialog = false
                recordToEdit = null
            },
            onSave = { platform, startOdo, endOdo, gross, deliveries, fuelPrice, foodExpense ->
                viewModel.updateDailyRecord(
                    recordId = recordToEdit!!.id,
                    dateString = recordToEdit!!.dateString,
                    dateTimestamp = recordToEdit!!.dateTimestamp,
                    platform = platform,
                    grossEarnings = gross,
                    deliveriesCount = deliveries,
                    startOdometer = startOdo,
                    endOdometer = endOdo,
                    fuelPrice = fuelPrice,
                    foodExpense = foodExpense,
                    onSuccess = {
                        showEditDialog = false
                        recordToEdit = null
                    },
                    onError = { error ->
                        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun HistoryRecordCard(
    record: DailyRecord,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("history_item_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Date & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = "Plataforma",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = record.platform,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "•",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )

                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = "Data",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    val formattedDate = remember(record.dateTimestamp) {
                        try {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(record.dateTimestamp))
                        } catch (e: Exception) {
                            record.dateString
                        }
                    }
                    Text(
                        text = formattedDate,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp).testTag("edit_record_btn_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar Lançamento",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp).testTag("delete_record_btn_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Excluir Lançamento",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Body Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Ganhos Brutos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", record.grossEarnings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text("Km Rodados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.GERMAN, "%.1f km", record.kmRodados),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text("Entregas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${record.deliveriesCount} ent.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Custo Combustível", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", record.fuelCost),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text("Desgaste/Peças", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", record.wearCost),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text("Alimentação", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", record.foodExpense),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LUCRO LÍQUIDO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(Locale.GERMAN, "R$ %.2f", record.netProfit),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = if (record.netProfit >= 0) Color(0xFF10B981) else Color(0xFFF43F5E)
                )
            }
        }
    }
}

@Composable
fun EditRecordDialog(
    record: DailyRecord,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Int, Double, Double) -> Unit
) {
    var platformState by remember { mutableStateOf(record.platform) }
    var startOdo by remember { mutableStateOf(record.startOdometer.toString()) }
    var endOdo by remember { mutableStateOf(record.endOdometer.toString()) }
    var gross by remember { mutableStateOf(record.grossEarnings.toString()) }
    var deliveries by remember { mutableStateOf(record.deliveriesCount.toString()) }
    var fuelPrice by remember { mutableStateOf(record.fuelPrice.toString()) }
    var foodExpense by remember { mutableStateOf(record.foodExpense.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Editar Lançamento",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Divider(color = MaterialTheme.colorScheme.outline)

                TextField(
                    value = platformState,
                    onValueChange = { platformState = it },
                    label = { Text("Plataforma") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_platform"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = "Plataforma",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = startOdo,
                        onValueChange = { startOdo = it },
                        label = { Text("Hodôm. Inicial") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_start_odo")
                    )
                    TextField(
                        value = endOdo,
                        onValueChange = { endOdo = it },
                        label = { Text("Hodôm. Final") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_end_odo")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = gross,
                        onValueChange = { gross = it },
                        label = { Text("Ganhos (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_gross")
                    )
                    TextField(
                        value = deliveries,
                        onValueChange = { deliveries = it },
                        label = { Text("Entregas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_deliveries")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = fuelPrice,
                        onValueChange = { fuelPrice = it },
                        label = { Text("Preço Comb. (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_fuel_price")
                    )
                    TextField(
                        value = foodExpense,
                        onValueChange = { foodExpense = it },
                        label = { Text("Alimentação (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_food_expense")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val startOdoVal = startOdo.toDoubleOrNull() ?: 0.0
                            val endOdoVal = endOdo.toDoubleOrNull() ?: 0.0
                            val grossVal = gross.toDoubleOrNull() ?: 0.0
                            val deliveriesVal = deliveries.toIntOrNull() ?: 0
                            val fuelPriceVal = fuelPrice.toDoubleOrNull() ?: 0.0
                            val foodExpenseVal = foodExpense.toDoubleOrNull() ?: 0.0

                            if (grossVal > 0.0 && fuelPriceVal > 0.0 && platformState.isNotBlank()) {
                                onSave(platformState, startOdoVal, endOdoVal, grossVal, deliveriesVal, fuelPriceVal, foodExpenseVal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
