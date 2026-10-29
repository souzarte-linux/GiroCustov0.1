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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DailyRecord
import com.example.util.CurrencyUtils
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.data.MaintenanceRecord
import com.example.ui.util.filterByPeriod
import com.example.ui.util.getCurrentMonthRange
import com.example.ui.GiroCustoViewModel
import com.example.ui.Period
import com.example.ui.SyncState
import com.example.ui.components.CustomPeriodDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorNotificationCard
import com.example.ui.components.GiroCustoLogo
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.SyncLoadingBanner
import com.example.ui.screens.dashboard.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>,
    records: List<DailyRecord>,
    maintenanceRecords: List<MaintenanceRecord>,
    onNavigateToLaunch: () -> Unit = {},
    onNavigateToRefill: () -> Unit = {}
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val customStart by viewModel.customStartDate.collectAsStateWithLifecycle()
    val customEnd by viewModel.customEndDate.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncErrorMessage by viewModel.syncErrorMessage.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showCustomPeriodDialog by remember { mutableStateOf(false) }

    // Filtrar registros por período usando o utilitário compartilhado
    val filteredRecords = remember(records, selectedPeriod, customStart, customEnd) {
        filterByPeriod(records, selectedPeriod, customStart, customEnd) { it.dateTimestamp }
    }

    // Filtrar manutenções corretivas pelo mesmo período
    val filteredMaintenanceRecords = remember(maintenanceRecords, selectedPeriod, customStart, customEnd) {
        filterByPeriod(maintenanceRecords, selectedPeriod, customStart, customEnd) { it.dateTimestamp }
    }

    // Cálculos de Métricas
    val totalGross = CurrencyUtils.centsToDouble(filteredRecords.sumOf { it.grossEarnings })
    val totalFuel = CurrencyUtils.centsToDouble(filteredRecords.sumOf { it.fuelCost })
    val totalWear = CurrencyUtils.centsToDouble(filteredRecords.sumOf { it.wearCost })
    val totalFixed = CurrencyUtils.centsToDouble(filteredRecords.sumOf { it.proportionalFixedCost })
    val totalFood = CurrencyUtils.centsToDouble(filteredRecords.sumOf { it.foodExpense })
    val totalExpenses = totalFuel + totalWear + totalFixed + totalFood
    val netProfit = totalGross - totalExpenses

    // Total gasto em manutenção corretiva no período
    val totalMaintenance = CurrencyUtils.centsToDouble(filteredMaintenanceRecords.sumOf { it.value })
    // Lucro líquido real do período (lucro operacional deduzido do gasto pontual de manutenção)
    val realNetProfit = netProfit - totalMaintenance

    val totalKm = filteredRecords.sumOf { it.kmRodados }
    val costPerKm = if (totalKm > 0) (totalExpenses + totalMaintenance) / totalKm else 0.0
    val netProfitPerKm = if (totalKm > 0) realNetProfit / totalKm else 0.0

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val savedFeedbackMessage = stringResource(R.string.goals_saved_feedback)

    Box(modifier = Modifier.fillMaxSize()) {
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
                            text = stringResource(R.string.app_name),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = (vehicle?.model ?: stringResource(R.string.dashboard_no_vehicle_registered)).uppercase(),
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
                        modifier = Modifier.size(48.dp).testTag("theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = stringResource(R.string.cd_theme_toggle),
                            tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Box {
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(10.dp))
                                .clickable { dropdownExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val selectedLabel = when (selectedPeriod) {
                                Period.SEMANA -> stringResource(R.string.period_week)
                                Period.QUINZENA -> stringResource(R.string.period_fortnight)
                                Period.MENSAL -> stringResource(R.string.period_month)
                                Period.ULTIMOS_30_DIAS -> stringResource(R.string.period_last_30_days)
                                Period.PERSONALIZADO -> stringResource(R.string.period_custom)
                            }
                            Text(
                                text = selectedLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = stringResource(R.string.cd_select_period),
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
                                text = { Text(stringResource(R.string.period_week), fontSize = 14.sp) },
                                onClick = {
                                    viewModel.setPeriod(Period.SEMANA)
                                    dropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.period_fortnight), fontSize = 14.sp) },
                                onClick = {
                                    viewModel.setPeriod(Period.QUINZENA)
                                    dropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.period_month), fontSize = 14.sp) },
                                onClick = {
                                    viewModel.setPeriod(Period.MENSAL)
                                    dropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.period_last_30_days), fontSize = 14.sp) },
                                onClick = {
                                    viewModel.setPeriod(Period.ULTIMOS_30_DIAS)
                                    dropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.period_custom), fontSize = 14.sp) },
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
                        .defaultMinSize(minHeight = 48.dp)
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
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.dashboard_custom_period_filter,
                                dateFormat.format(Date(customStart)),
                                dateFormat.format(Date(customEnd))
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.cd_change_custom_period),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
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

            if (records.isEmpty()) {
                EmptyStateCard(
                    title = stringResource(R.string.empty_dashboard_no_records_title),
                    subtitle = stringResource(R.string.empty_dashboard_no_records_subtitle),
                    icon = Icons.Default.DirectionsCar,
                    primaryActionText = stringResource(R.string.empty_action_launch),
                    onPrimaryAction = onNavigateToLaunch,
                    secondaryActionText = stringResource(R.string.empty_action_refill),
                    onSecondaryAction = onNavigateToRefill
                )
            } else if (filteredRecords.isEmpty()) {
                EmptyStateCard(
                    title = stringResource(R.string.empty_dashboard_filter_title),
                    subtitle = stringResource(R.string.empty_dashboard_filter_subtitle),
                    icon = Icons.Default.FilterList,
                    primaryActionText = stringResource(R.string.empty_action_launch),
                    onPrimaryAction = onNavigateToLaunch
                )
            } else {
                // Card de Lucro Líquido Real e Custos Operacionais
                DashboardNetProfitCard(
                    selectedPeriod = selectedPeriod,
                    customStart = customStart,
                    customEnd = customEnd,
                    realNetProfit = realNetProfit,
                    totalMaintenance = totalMaintenance,
                    totalGross = totalGross,
                    totalExpenses = totalExpenses,
                    totalFuel = totalFuel,
                    totalFood = totalFood,
                    totalWear = totalWear,
                    totalFixed = totalFixed
                )

                // Grid de Indicadores de Eficiência e Alertas Críticos de Peças
                DashboardEfficiencyAndAlertsSection(
                    costPerKm = costPerKm,
                    netProfitPerKm = netProfitPerKm,
                    parts = parts,
                    onResetPartWear = { partId -> viewModel.resetPartWear(partId) }
                )

                // Seção: Metas Ativas (Diária, Semanal e Mensal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_goals_section_title),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981), // Emerald accent
                        letterSpacing = 1.sp
                    )
                }

                // Carregar valores de conquista para as metas
                val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                val todayRecords = remember(records, todayStr) { records.filter { it.dateString == todayStr } }

                val dailyGrossAct = CurrencyUtils.centsToDouble(todayRecords.sumOf { it.grossEarnings })
                val dailyNetAct = CurrencyUtils.centsToDouble(todayRecords.sumOf { it.netProfit })
                val dailyKmAct = todayRecords.sumOf { it.kmRodados }
                val dailyDelAct = todayRecords.sumOf { it.deliveriesCount }

                val weeklyRecords = remember(records) {
                    val limit = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                    records.filter { it.dateTimestamp >= limit }
                }
                val weeklyGrossAct = CurrencyUtils.centsToDouble(weeklyRecords.sumOf { it.grossEarnings })
                val weeklyNetAct = CurrencyUtils.centsToDouble(weeklyRecords.sumOf { it.netProfit })
                val weeklyKmAct = weeklyRecords.sumOf { it.kmRodados }
                val weeklyDelAct = weeklyRecords.sumOf { it.deliveriesCount }

                val monthlyRecords = remember(records) {
                    val (startMonth, endMonth) = getCurrentMonthRange()
                    records.filter { it.dateTimestamp in startMonth..endMonth }
                }
                val monthlyGrossAct = CurrencyUtils.centsToDouble(monthlyRecords.sumOf { it.grossEarnings })
                val monthlyNetAct = CurrencyUtils.centsToDouble(monthlyRecords.sumOf { it.netProfit })
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
                GoalCard(title = stringResource(R.string.dashboard_goal_daily_title), grossAct = dailyGrossAct, grossGoal = dailyGGoal, netAct = dailyNetAct, netGoal = dailyNGoal, kmAct = dailyKmAct, kmGoal = dailyKGoal, delAct = dailyDelAct, delGoal = dailyDGoal)
                GoalCard(title = stringResource(R.string.dashboard_goal_weekly_title), grossAct = weeklyGrossAct, grossGoal = weeklyGGoal, netAct = weeklyNetAct, netGoal = weeklyNGoal, kmAct = weeklyKmAct, kmGoal = weeklyKGoal, delAct = weeklyDelAct, delGoal = weeklyDGoal)
                GoalCard(title = stringResource(R.string.dashboard_goal_monthly_title), grossAct = monthlyGrossAct, grossGoal = monthlyGGoal, netAct = monthlyNetAct, netGoal = monthlyNGoal, kmAct = monthlyKmAct, kmGoal = monthlyKGoal, delAct = monthlyDelAct, delGoal = monthlyDGoal)

                // Botão para Ajustar Metas
                OutlinedButton(
                    onClick = { showGoalsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.goals_adjust_button), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (showGoalsDialog) {
                    EditGoalsDialog(
                        viewModel = viewModel,
                        onDismiss = { showGoalsDialog = false },
                        onGoalsSaved = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(savedFeedbackMessage)
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

// Delegados de compatibilidade direta para manter 100% de integridade com testes existentes
@Composable
fun GoalCard(
    title: String,
    grossAct: Double, grossGoal: Double,
    netAct: Double, netGoal: Double,
    kmAct: Double, kmGoal: Double,
    delAct: Int, delGoal: Int
) = com.example.ui.screens.dashboard.GoalCard(title, grossAct, grossGoal, netAct, netGoal, kmAct, kmGoal, delAct, delGoal)

fun parsePtBrDouble(input: String, fallback: Double): Double =
    com.example.ui.screens.dashboard.parsePtBrDouble(input, fallback)
