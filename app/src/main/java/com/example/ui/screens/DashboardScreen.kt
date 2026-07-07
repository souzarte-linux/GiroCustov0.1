package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyRecord
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.ui.GiroCustoViewModel
import com.example.ui.Period
import com.example.ui.components.GiroCustoLogo
import java.text.SimpleDateFormat
import java.util.*

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
                HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.2f))
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
                        progress = { (costPerKm / 1.0).coerceIn(0.0, 1.0).toFloat() },
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
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, "Lucro por km", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
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
                        progress = { (netProfitPerKm / 2.0).coerceIn(0.0, 1.0).toFloat() },
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
        GoalCard(title = "Meta Semanal (7 dias)", grossAct = weeklyGrossAct, grossGoal = weeklyGGoal, netAct = weeklyNetAct, netGoal = weeklyNGoal, kmAct = weeklyKmAct, kmGoal = weeklyKGoal, delAct = weeklyDelAct, delGoal = weeklyDGoal)
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

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
            progress = { pct.toFloat() },
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
            progress = { pct.toFloat() },
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
                HorizontalDivider(color = Color(0xFF2D2D34))

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
