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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
                        color = MaterialTheme.colorScheme.onBackground, // Dynamic color
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
                            .background(MaterialTheme.colorScheme.surface) // Dynamic Surface Charcoal
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
                        Period.SEMANA -> stringResource(R.string.dashboard_net_profit_week)
                        Period.QUINZENA -> stringResource(R.string.dashboard_net_profit_fortnight)
                        Period.MENSAL -> stringResource(R.string.dashboard_net_profit_month)
                        Period.ULTIMOS_30_DIAS -> stringResource(R.string.dashboard_net_profit_30_days)
                        Period.PERSONALIZADO -> {
                            val startStr = displayFormat.format(Date(customStart))
                            val endStr = displayFormat.format(Date(customEnd))
                            stringResource(R.string.dashboard_net_profit_custom, startStr, endStr)
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
                            text = stringResource(R.string.dashboard_badge_money_in_pocket),
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
                        text = String.format(Locale.GERMAN, "%,.2f", realNetProfit),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White // High contrast White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.dashboard_maintenance_included_notice,
                        String.format(Locale.GERMAN, "%,.2f", totalMaintenance)
                    ),
                    fontSize = 11.sp,
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dashboard_gross_revenue), fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
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
                        Text(stringResource(R.string.dashboard_operational_cost), fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format(Locale.GERMAN, "- R$ %,.2f", totalExpenses),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF43F5E) // Bright light rose/red for cost in dark mode
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.dashboard_expenses_summary_title),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.dashboard_expense_fuel), fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        Text(String.format(Locale.GERMAN, "R$ %,.2f", totalFuel), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.dashboard_expense_food), fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        Text(String.format(Locale.GERMAN, "R$ %,.2f", totalFood), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.dashboard_expense_wear), fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        Text(String.format(Locale.GERMAN, "R$ %,.2f", totalWear), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.dashboard_expense_fixed), fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        Text(String.format(Locale.GERMAN, "R$ %,.2f", totalFixed), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.dashboard_expense_maintenance), fontSize = 12.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
                        Text(String.format(Locale.GERMAN, "R$ %,.2f", totalMaintenance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
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
                        Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.dashboard_kpi_cost_per_km), fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
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
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.dashboard_kpi_net_profit_per_km), fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
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
                    text = stringResource(R.string.dashboard_maintenance_alert_title),
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
                                    text = stringResource(
                                        R.string.dashboard_part_wear_critical,
                                        part.wearPercentage,
                                        part.runKmSinceChange.toInt(),
                                        part.lifespanKm.toInt()
                                    ),
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
                                Text(stringResource(R.string.dashboard_part_button_replaced), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        
        // Conquistas Atuais
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

        // Botão para Ajustar Metas (Polished Green Outlined Style)
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
            GoalRow(label = stringResource(R.string.dashboard_goal_label_gross), value = grossAct, target = grossGoal, format = "R$ %,.2f")
            GoalRow(label = stringResource(R.string.dashboard_goal_label_net), value = netAct, target = netGoal, format = "R$ %,.2f")
            GoalRow(label = stringResource(R.string.dashboard_goal_label_km), value = kmAct, target = kmGoal, format = "%,.1f km")
            GoalRowInt(label = stringResource(R.string.goals_field_deliveries), value = delAct, target = delGoal)
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
                text = stringResource(R.string.dashboard_goal_deliveries_progress, value, target, (pct * 100).toInt()),
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

fun parsePtBrDouble(input: String, fallback: Double): Double {
    val clean = input.trim()
        .replace("R$", "")
        .replace(" ", "")
        .replace("\u00A0", "")
    if (clean.isEmpty()) return fallback

    return try {
        if (clean.contains('.') && clean.contains(',')) {
            if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) {
                clean.replace(".", "").replace(',', '.').toDouble()
            } else {
                clean.replace(",", "").toDouble()
            }
        } else if (clean.contains(',')) {
            clean.replace(',', '.').toDouble()
        } else {
            clean.toDouble()
        }
    } catch (_: Exception) {
        fallback
    }
}

fun parsePtBrInt(input: String, fallback: Int): Int {
    val clean = input.trim()
        .replace(" ", "")
        .replace("\u00A0", "")
        .replace(".", "")
        .replace(",", "")
    return clean.toIntOrNull() ?: fallback
}

fun formatMoneyGoal(value: Double): String =
    String.format(Locale.GERMAN, "%.2f", value)

fun formatKmGoal(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.GERMAN, "%.1f", value)

@Composable
fun EditGoalsDialog(
    viewModel: GiroCustoViewModel,
    onDismiss: () -> Unit,
    onGoalsSaved: (() -> Unit)? = null
) {
    // Pegar valores atuais formatados no padrão pt-BR
    var dg by remember { mutableStateOf(formatMoneyGoal(viewModel.dailyGoalGross.value)) }
    var dn by remember { mutableStateOf(formatMoneyGoal(viewModel.dailyGoalNet.value)) }
    var dk by remember { mutableStateOf(formatKmGoal(viewModel.dailyGoalKm.value)) }
    var dd by remember { mutableStateOf(viewModel.dailyGoalDeliveries.value.toString()) }

    var wg by remember { mutableStateOf(formatMoneyGoal(viewModel.weeklyGoalGross.value)) }
    var wn by remember { mutableStateOf(formatMoneyGoal(viewModel.weeklyGoalNet.value)) }
    var wk by remember { mutableStateOf(formatKmGoal(viewModel.weeklyGoalKm.value)) }
    var wd by remember { mutableStateOf(viewModel.weeklyGoalDeliveries.value.toString()) }

    var mg by remember { mutableStateOf(formatMoneyGoal(viewModel.monthlyGoalGross.value)) }
    var mn by remember { mutableStateOf(formatMoneyGoal(viewModel.monthlyGoalNet.value)) }
    var mk by remember { mutableStateOf(formatKmGoal(viewModel.monthlyGoalKm.value)) }
    var md by remember { mutableStateOf(viewModel.monthlyGoalDeliveries.value.toString()) }


    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(16.dp)
                .imePadding(),
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
                Text(
                    stringResource(R.string.goals_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                HorizontalDivider(color = Color(0xFF2D2D34))

                // Diária
                Text(
                    stringResource(R.string.goals_section_daily),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = dg,
                        onValueChange = { dg = it },
                        label = { Text(stringResource(R.string.goals_field_gross)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = dn,
                        onValueChange = { dn = it },
                        label = { Text(stringResource(R.string.goals_field_net)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = dk,
                        onValueChange = { dk = it },
                        label = { Text(stringResource(R.string.goals_field_km)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = dd,
                        onValueChange = { dd = it },
                        label = { Text(stringResource(R.string.goals_field_deliveries)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Semanal
                Text(
                    stringResource(R.string.goals_section_weekly),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = wg,
                        onValueChange = { wg = it },
                        label = { Text(stringResource(R.string.goals_field_gross)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = wn,
                        onValueChange = { wn = it },
                        label = { Text(stringResource(R.string.goals_field_net)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = wk,
                        onValueChange = { wk = it },
                        label = { Text(stringResource(R.string.goals_field_km)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = wd,
                        onValueChange = { wd = it },
                        label = { Text(stringResource(R.string.goals_field_deliveries)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Mensal
                Text(
                    stringResource(R.string.goals_section_monthly),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = mg,
                        onValueChange = { mg = it },
                        label = { Text(stringResource(R.string.goals_field_gross)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = mn,
                        onValueChange = { mn = it },
                        label = { Text(stringResource(R.string.goals_field_net)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = mk,
                        onValueChange = { mk = it },
                        label = { Text(stringResource(R.string.goals_field_km)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = md,
                        onValueChange = { md = it },
                        label = { Text(stringResource(R.string.goals_field_deliveries)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.goals_btn_cancel), color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateGoals(
                                dailyG = parsePtBrDouble(dg, viewModel.dailyGoalGross.value),
                                dailyN = parsePtBrDouble(dn, viewModel.dailyGoalNet.value),
                                dailyK = parsePtBrDouble(dk, viewModel.dailyGoalKm.value),
                                dailyD = parsePtBrInt(dd, viewModel.dailyGoalDeliveries.value),
                                weeklyG = parsePtBrDouble(wg, viewModel.weeklyGoalGross.value),
                                weeklyN = parsePtBrDouble(wn, viewModel.weeklyGoalNet.value),
                                weeklyK = parsePtBrDouble(wk, viewModel.weeklyGoalKm.value),
                                weeklyD = parsePtBrInt(wd, viewModel.weeklyGoalDeliveries.value),
                                monthlyG = parsePtBrDouble(mg, viewModel.monthlyGoalGross.value),
                                monthlyN = parsePtBrDouble(mn, viewModel.monthlyGoalNet.value),
                                monthlyK = parsePtBrDouble(mk, viewModel.monthlyGoalKm.value),
                                monthlyD = parsePtBrInt(md, viewModel.monthlyGoalDeliveries.value)
                            )
                            onGoalsSaved?.invoke()
                            onDismiss()
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF064E3B)
                        )
                    ) {
                        Text(stringResource(R.string.goals_btn_save), fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                    }
                }
            }
        }
    }
}
