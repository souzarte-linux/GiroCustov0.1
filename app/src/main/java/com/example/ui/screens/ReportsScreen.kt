package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DailyRecord
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.data.MaintenanceRecord
import com.example.ui.Period
import com.example.ui.SyncState
import com.example.ui.components.CustomPeriodDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorNotificationCard
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.SyncLoadingBanner
import com.example.ui.screens.reports.*
import com.example.ui.GiroCustoViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>,
    records: List<DailyRecord>,
    maintenanceRecords: List<MaintenanceRecord>,
    onNavigateToLaunch: () -> Unit = {},
    onNavigateToRefill: () -> Unit = {}
) {
    val selectedPeriod by viewModel.reportsSelectedPeriod.collectAsStateWithLifecycle()
    val customStart by viewModel.reportsCustomStartDate.collectAsStateWithLifecycle()
    val customEnd by viewModel.reportsCustomEndDate.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncErrorMessage by viewModel.syncErrorMessage.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showCustomPeriodDialog by remember { mutableStateOf(false) }

    // T-16 [ARQ-03]: Coleta métricas de relatório calculadas de forma reativa no ViewModel
    val viewModelMetrics by viewModel.reportMetrics.collectAsStateWithLifecycle()
    val monthlyNGoal by viewModel.monthlyGoalNet.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val metrics = remember(records, maintenanceRecords, selectedPeriod, customStart, customEnd, monthlyNGoal, viewModelMetrics, vehicle?.plannedWorkDays) {
        try {
            if (records.isNotEmpty() && viewModelMetrics.recordsCount == 0) {
                // Suporte para quando uma lista específica é passada diretamente (ex.: testes ou previews isolados)
                viewModel.calculateReportMetrics(
                    records = records,
                    maintenanceRecords = maintenanceRecords,
                    selectedPeriod = selectedPeriod,
                    customStart = customStart,
                    customEnd = customEnd,
                    plannedWorkDays = vehicle?.plannedWorkDays ?: 22,
                    monthlyGoalNet = monthlyNGoal
                )
            } else {
                viewModelMetrics
            }
        } catch (_: Exception) {
            viewModelMetrics
        }
    }

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
                    Period.MENSAL -> "Período: Mês Atual"
                    Period.ULTIMOS_30_DIAS -> "Período: Últimos 30 dias"
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
                            Period.MENSAL -> "Mês Atual"
                            Period.ULTIMOS_30_DIAS -> "Últimos 30 dias"
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
                            text = { Text("Mês Atual", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.MENSAL)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Últimos 30 dias", fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.ULTIMOS_30_DIAS)
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

        // Banners de Estado do Sistema (Sincronização, Modo Offline e Erro)
        if (syncState == SyncState.SYNCING) {
            SyncLoadingBanner()
        }

        if (syncState == SyncState.OFFLINE) {
            OfflineNoticeBanner()
        }

        if (syncState == SyncState.ERROR && !syncErrorMessage.isNullOrBlank()) {
            ErrorNotificationCard(
                errorMessage = syncErrorMessage ?: "",
                onRetry = { viewModel.syncNow() },
                onDismiss = { viewModel.clearSyncError() }
            )
        }

        if (records.isEmpty() && metrics.recordsCount == 0) {
            EmptyStateCard(
                title = stringResource(R.string.empty_reports_title),
                subtitle = stringResource(R.string.empty_reports_no_records_subtitle),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                primaryActionText = stringResource(R.string.empty_action_launch),
                onPrimaryAction = onNavigateToLaunch,
                secondaryActionText = stringResource(R.string.empty_action_refill),
                onSecondaryAction = onNavigateToRefill
            )
        } else if (metrics.filteredRecords.isEmpty()) {
            EmptyStateCard(
                title = stringResource(R.string.empty_reports_title),
                subtitle = stringResource(R.string.empty_reports_filter_subtitle),
                icon = Icons.Default.FilterList,
                primaryActionText = stringResource(R.string.empty_action_launch),
                onPrimaryAction = onNavigateToLaunch
            )
        } else {
            // 1. GRAFICO DE EVOLUÇÃO DO LUCRO LÍQUIDO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2D34)),
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
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (metrics.chronologicalRecords.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            NetProfitCurveChart(records = metrics.chronologicalRecords)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (metrics.chronologicalRecords.size <= 1) Arrangement.Center else Arrangement.SpaceBetween
                        ) {
                            val displayFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
                            metrics.chronologicalRecords.forEach { record ->
                                val cal = remember(record.dateTimestamp) {
                                    Calendar.getInstance().apply { timeInMillis = record.dateTimestamp }
                                }
                                Text(
                                    text = displayFormat.format(cal.time),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
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

            // 2. GRAFICO DE DISTRIBUIÇÃO DE DESPESAS
            ReportsExpenseDistributionCard(metrics = metrics)

            // 3. COMPARATIVO DE PRODUTIVIDADE (Faturamento Bruto vs Lucro Líquido Real)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2D34)),
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

                    if (metrics.chronologicalRecords.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            ProductivityComparisonChart(records = metrics.chronologicalRecords)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF00B0FF)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Faturamento Bruto", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lucro Líquido", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    } else {
                        Text("Sem histórico suficiente.", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }

            // 4. JORNADA DE TRABALHO E RENDIMENTO POR HORA
            ReportsWorkHoursCard(metrics = metrics)

            // 5. DESTAQUES DE RECORDES (Melhor e Pior turnos)
            ReportsHighlightsSection(metrics = metrics)

            // 6. RESUMO MENSAL E PROJEÇÕES FUTURAS
            ReportsProjectionsCard(metrics = metrics, monthlyNGoal = monthlyNGoal)
        }
    }
}

// Delegados de compatibilidade direta
@Composable
fun ExpenseLegendItem(label: String, color: Color, pct: Float, value: Double) =
    com.example.ui.screens.reports.ExpenseLegendItem(label, color, pct, value)

@Composable
fun NetProfitCurveChart(records: List<DailyRecord>) =
    com.example.ui.screens.reports.NetProfitCurveChart(records)

@Composable
fun ProductivityComparisonChart(records: List<DailyRecord>) =
    com.example.ui.screens.reports.ProductivityComparisonChart(records)
