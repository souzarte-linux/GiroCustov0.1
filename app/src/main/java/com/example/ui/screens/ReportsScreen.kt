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

    val metrics = remember(records, maintenanceRecords, selectedPeriod, customStart, customEnd, monthlyNGoal, vehicle?.plannedWorkDays, viewModelMetrics) {
        try {
            if (records.isNotEmpty()) {
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
                    text = stringResource(R.string.reports_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val displayFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
                val periodLabel = when (selectedPeriod) {
                    Period.SEMANA -> stringResource(R.string.reports_period_prefix_week)
                    Period.QUINZENA -> stringResource(R.string.reports_period_prefix_fortnight)
                    Period.MENSAL -> stringResource(R.string.reports_period_prefix_month)
                    Period.ULTIMOS_30_DIAS -> stringResource(R.string.reports_period_prefix_last_30_days)
                    Period.PERSONALIZADO -> stringResource(
                        R.string.reports_period_custom_range,
                        displayFormat.format(Date(customStart)),
                        displayFormat.format(Date(customEnd))
                    )
                }
                Text(
                    text = periodLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
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
                            Period.SEMANA -> stringResource(R.string.reports_period_week)
                            Period.QUINZENA -> stringResource(R.string.reports_period_fortnight)
                            Period.MENSAL -> stringResource(R.string.reports_period_month)
                            Period.ULTIMOS_30_DIAS -> stringResource(R.string.reports_period_last_30_days)
                            Period.PERSONALIZADO -> stringResource(R.string.reports_period_custom)
                        }
                        Text(
                            text = selectedLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.reports_cd_select_period),
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
                            text = { Text(stringResource(R.string.reports_period_week), fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.SEMANA)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reports_period_fortnight), fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.QUINZENA)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reports_period_month), fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.MENSAL)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reports_period_last_30_days), fontSize = 14.sp) },
                            onClick = {
                                viewModel.setReportsPeriod(Period.ULTIMOS_30_DIAS)
                                dropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reports_period_custom), fontSize = 14.sp) },
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
                        contentDescription = stringResource(R.string.reports_cd_toggle_theme),
                        tint = if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.reports_chart_profit_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.reports_chart_profit_subtitle),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            val displayedRecords = remember(metrics.chronologicalRecords) {
                                if (metrics.chronologicalRecords.size <= 7) {
                                    metrics.chronologicalRecords
                                } else {
                                    val size = metrics.chronologicalRecords.size
                                    val indices = listOf(0, size / 4, size / 2, 3 * size / 4, size - 1).distinct()
                                    indices.map { metrics.chronologicalRecords[it] }
                                }
                            }
                            displayedRecords.forEach { record ->
                                val cal = remember(record.dateTimestamp) {
                                    Calendar.getInstance().apply { timeInMillis = record.dateTimestamp }
                                }
                                Text(
                                    text = displayFormat.format(cal.time),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Text(
                                text = stringResource(R.string.reports_chart_profit_empty),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. GRAFICO DE DISTRIBUIÇÃO DE DESPESAS
            ReportsExpenseDistributionCard(metrics = metrics)

            // 3. COMPARATIVO DE PRODUTIVIDADE (Faturamento Bruto vs Lucro Líquido Real)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.reports_chart_productivity_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.reports_chart_productivity_subtitle),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (metrics.chronologicalRecords.isNotEmpty()) {
                        ProductivityComparisonChart(records = metrics.chronologicalRecords)

                        Spacer(modifier = Modifier.height(12.dp))

                        val (legendGross, legendNet) = getProductivityBarColors(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(legendGross))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.reports_legend_gross), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.size(10.dp).background(legendNet))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.reports_legend_net), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.reports_chart_productivity_empty),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
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
