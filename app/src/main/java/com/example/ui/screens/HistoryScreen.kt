package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DailyRecord
import com.example.util.CurrencyUtils
import com.example.ui.GiroCustoViewModel
import com.example.ui.Period
import com.example.ui.SyncState
import com.example.ui.components.CustomPeriodDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorNotificationCard
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.SyncLoadingBanner
import com.example.ui.util.filterByPeriod
import java.text.SimpleDateFormat
import java.util.*

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
    records: List<DailyRecord>,
    onNavigateToLaunch: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncErrorMessage by viewModel.syncErrorMessage.collectAsStateWithLifecycle()

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
        // 1. Filtrar por Período usando utilitário padronizado
        val dateFiltered = filterByPeriod(records, historyPeriod, historyCustomStart, historyCustomEnd) { it.dateTimestamp }

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
                modifier = Modifier.size(48.dp).testTag("theme_toggle_btn_history")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = stringResource(R.string.cd_theme_toggle),
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
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
                        .defaultMinSize(minHeight = 48.dp)
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
                        Period.MENSAL -> "Mês Atual"
                        Period.ULTIMOS_30_DIAS -> "30 Dias"
                        Period.PERSONALIZADO -> "Personaliz."
                    }
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.cd_select_period),
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
                        text = { Text("Mês Atual", fontSize = 13.sp) },
                        onClick = {
                            historyPeriod = Period.MENSAL
                            periodExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Últimos 30 dias", fontSize = 13.sp) },
                        onClick = {
                            historyPeriod = Period.ULTIMOS_30_DIAS
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
                        .defaultMinSize(minHeight = 48.dp)
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.cd_filter_platform),
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
                        .defaultMinSize(minHeight = 48.dp)
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.cd_sort_by),
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
                    .defaultMinSize(minHeight = 48.dp)
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
                        contentDescription = null,
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
                    contentDescription = stringResource(R.string.cd_change_custom_period),
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
                if (records.isEmpty()) {
                    EmptyStateCard(
                        title = stringResource(R.string.empty_history_title),
                        subtitle = stringResource(R.string.empty_history_no_records_subtitle),
                        icon = Icons.Default.History,
                        primaryActionText = stringResource(R.string.empty_action_launch),
                        onPrimaryAction = onNavigateToLaunch
                    )
                } else {
                    EmptyStateCard(
                        title = stringResource(R.string.empty_history_filter_title),
                        subtitle = stringResource(R.string.empty_history_filter_subtitle),
                        icon = Icons.Default.FilterList,
                        primaryActionText = stringResource(R.string.empty_action_launch),
                        onPrimaryAction = onNavigateToLaunch
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Se a ordenação for por data (crescente ou decrescente), usamos o agrupamento hierárquico.
                // Caso contrário (por lucro, km, entregas, etc.), usamos uma lista plana para não quebrar a lógica de ordenação.
                if (selectedSortOption == HistorySortOption.DATA_DECRESCENTE || selectedSortOption == HistorySortOption.DATA_CRESCENTE) {
                    val isAsc = selectedSortOption == HistorySortOption.DATA_CRESCENTE
                    val groupedList = groupRecordsHierarchically(filteredAndSortedRecords, isAsc)
                    
                    items(
                        items = groupedList,
                        key = { getHistoryListItemKey(it) }
                    ) { item ->
                        when (item) {
                            is HistoryListItem.YearHeader -> {
                                YearHeaderCard(
                                    year = item.year,
                                    totalGross = item.totalGross,
                                    totalNet = item.totalNet
                                )
                            }
                            is HistoryListItem.MonthHeader -> {
                                val monthLabel = "${ptMonths[item.month]} ${item.year}"
                                MonthHeaderCard(
                                    monthLabel = monthLabel,
                                    totalGross = item.totalGross,
                                    totalNet = item.totalNet
                                )
                            }
                            is HistoryListItem.WeekHeader -> {
                                val sdfWeek = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
                                val startStr = sdfWeek.format(Date(item.weekStartMillis))
                                val endStr = sdfWeek.format(Date(item.weekEndMillis))
                                val weekLabel = "Semana de $startStr a $endStr"
                                WeekHeaderCard(
                                    weekLabel = weekLabel,
                                    totalGross = item.totalGross,
                                    totalNet = item.totalNet
                                )
                            }
                            is HistoryListItem.DayHeader -> {
                                DayHeaderView(label = item.label)
                            }
                            is HistoryListItem.RecordRow -> {
                                HistoryRecordCard(
                                    record = item.record,
                                    onEditClick = {
                                        recordToEdit = item.record
                                        showEditDialog = true
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteDailyRecord(item.record)
                                        android.widget.Toast.makeText(context, "Lançamento excluído com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredAndSortedRecords,
                        key = { it.id }
                    ) { record ->
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
            onSave = { platform, startOdo, endOdo, gross, deliveries, fuelPrice, foodExpense, sTime, eTime, pDuration ->
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
                    startTime = sTime,
                    endTime = eTime,
                    pauseDuration = pDuration,
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
