package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyRecord
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.data.MaintenanceRecord
import com.example.ui.Period
import com.example.ui.components.CustomPeriodDialog
import com.example.ui.util.filterByPeriod
import com.example.ui.GiroCustoViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>,
    records: List<DailyRecord>,
    maintenanceRecords: List<MaintenanceRecord>
) {
    val selectedPeriod by viewModel.reportsSelectedPeriod.collectAsStateWithLifecycle()
    val customStart by viewModel.reportsCustomStartDate.collectAsStateWithLifecycle()
    val customEnd by viewModel.reportsCustomEndDate.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showCustomPeriodDialog by remember { mutableStateOf(false) }

    // Filtrar registros por período usando o utilitário compartilhado para o Reports
    val filteredRecords = remember(records, selectedPeriod, customStart, customEnd) {
        filterByPeriod(records, selectedPeriod, customStart, customEnd) { it.dateTimestamp }
    }

    // Filtrar manutenções corretivas pelo mesmo período para o Reports
    val filteredMaintenanceRecords = remember(maintenanceRecords, selectedPeriod, customStart, customEnd) {
        filterByPeriod(maintenanceRecords, selectedPeriod, customStart, customEnd) { it.dateTimestamp }
    }

    // 1. Gráfico de Evolução de Lucro Líquido (Baseado nos registros filtrados)
    val chronologicalRecords = remember(filteredRecords) {
        filteredRecords.take(7).sortedBy { it.dateTimestamp }
    }

    // Cálculos Gerais do Período
    val totalEarnings = filteredRecords.sumOf { it.grossEarnings }
    val totalFuel = filteredRecords.sumOf { it.fuelCost }
    val totalWear = filteredRecords.sumOf { it.wearCost }
    val totalFixed = filteredRecords.sumOf { it.proportionalFixedCost }
    val totalFood = filteredRecords.sumOf { it.foodExpense }
    val totalExpenses = totalFuel + totalWear + totalFixed + totalFood

    // Total gasto em manutenção corretiva no período
    val totalMaintenance = filteredMaintenanceRecords.sumOf { it.value }
    val realExpenses = totalExpenses + totalMaintenance
    val netProfit = totalEarnings - totalExpenses
    val realNetProfit = netProfit - totalMaintenance

    val totalDeliveries = filteredRecords.sumOf { it.deliveriesCount }

    // Destaques de Recordes
    val bestDay = remember(filteredRecords) {
        filteredRecords.maxByOrNull { it.netProfit }
    }
    val worstDay = remember(filteredRecords) {
        filteredRecords.minByOrNull { it.netProfit }
    }

    // Projeções futuras baseadas nos registros filtrados
    val avgEarningsPerDay = if (filteredRecords.isNotEmpty()) totalEarnings / filteredRecords.size else 0.0
    val avgProfitPerDay = if (filteredRecords.isNotEmpty()) realNetProfit / filteredRecords.size else 0.0
    val avgKmPerDay = if (filteredRecords.isNotEmpty()) filteredRecords.sumOf { it.kmRodados } / filteredRecords.size else 0.0

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Relatórios de Desempenho",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val displayFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
                val periodLabel = when (selectedPeriod) {
                    Period.SEMANA -> "Período: Semana"
                    Period.QUINZENA -> "Período: Quinzena"
                    Period.MENSAL -> "Período: Mensal"
                    Period.PERSONALIZADO -> "Filtro: ${displayFormat.format(Date(customStart))} até ${displayFormat.format(Date(customEnd))}"
                }
                Text(
                    text = periodLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF10B981)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var dropdownExpanded by remember { mutableStateOf(false) }

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
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
                                viewModel.setReportsPeriod(Period.SEMANA)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Quinzena", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.QUINZENA)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mensal", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.MENSAL)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Personalizado", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.PERSONALIZADO)
                                dropdownExpanded = false
                                showCustomPeriodDialog = true
                            }
                        )
                    }
                }

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
        }

        if (showCustomPeriodDialog) {
            CustomPeriodDialog(
                initialStart = customStart,
                initialEnd = customEnd,
                onDismiss = { showCustomPeriodDialog = false },
                onConfirm = { start, end ->
                    viewModel.setReportsCustomPeriod(start, end)
                    showCustomPeriodDialog = false
                }
            )
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
                val totalFuelCost = filteredRecords.sumOf { it.fuelCost }
                val totalWearCost = filteredRecords.sumOf { it.wearCost }
                val totalFixedCost = filteredRecords.sumOf { it.proportionalFixedCost }
                val totalFoodCost = filteredRecords.sumOf { it.foodExpense }
                val totalMaintenanceCost = filteredMaintenanceRecords.sumOf { it.value }
                val sumAllExpenses = totalFuelCost + totalWearCost + totalFixedCost + totalFoodCost + totalMaintenanceCost

                if (sumAllExpenses > 0) {
                    val pctFuel = (totalFuelCost / sumAllExpenses).toFloat()
                    val pctWear = (totalWearCost / sumAllExpenses).toFloat()
                    val pctFixed = (totalFixedCost / sumAllExpenses).toFloat()
                    val pctFood = (totalFoodCost / sumAllExpenses).toFloat()
                    val pctMaintenance = (totalMaintenanceCost / sumAllExpenses).toFloat()

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
                        if (pctMaintenance > 0) Box(modifier = Modifier.fillMaxHeight().weight(pctMaintenance.coerceAtLeast(0.01f)).background(Color(0xFFFBBF24)))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Legendas com percentual e valor
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpenseLegendItem(label = "Combustível", color = Color(0xFFFF9100), pct = pctFuel, value = totalFuelCost)
                        ExpenseLegendItem(label = "Alimentação", color = Color(0xFF00B0FF), pct = pctFood, value = totalFoodCost)
                        ExpenseLegendItem(label = "Desgaste Oculto / Peças", color = Color(0xFFEF5350), pct = pctWear, value = totalWearCost)
                        ExpenseLegendItem(label = "Custos Fixos Proporcionais", color = Color(0xFFAB47BC), pct = pctFixed, value = totalFixedCost)
                        ExpenseLegendItem(label = "Manutenção Corretiva (Gasto Real)", color = Color(0xFFFBBF24), pct = pctMaintenance, value = totalMaintenanceCost)
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

        // 4. DESTAKES DE RECORDES (Melhor e Pior turnos) (Professional Polish Slate/Green Cards Style)
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
                HorizontalDivider(color = Color(0xFF2D2D34))
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
                    progress = { (pctOfGoal).coerceIn(0.0, 1.0).toFloat() },
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
