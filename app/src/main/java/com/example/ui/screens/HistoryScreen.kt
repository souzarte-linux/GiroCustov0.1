package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DailyRecord
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
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun HistoryScreen(
    viewModel: GiroCustoViewModel,
    vehicle: com.example.data.Vehicle?,
    records: List<DailyRecord>,
    initialFilterState: HistoryFilterState = HistoryFilterState(),
    onNavigateToLaunch: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncErrorMessage by viewModel.syncErrorMessage.collectAsStateWithLifecycle()
    val annualGoalNet by viewModel.annualGoalNet.collectAsStateWithLifecycle()

    var isPrivacyEnabled by rememberSaveable { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var recordToDelete by remember { mutableStateOf<DailyRecord?>(null) }
    val registeredPlatforms by viewModel.allPlatforms.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<DailyRecord?>(null) }

    // Busca Textual
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Estado dos Filtros Avançados e Modal BottomSheet
    var filterState by remember { mutableStateOf(initialFilterState) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showHistoryCustomPeriodDialog by remember { mutableStateOf(false) }

    // Lista de plataformas únicas dos lançamentos
    val platformsList = remember(records) {
        records.map { it.platform }.distinct().filter { it.isNotBlank() }
    }

    // Lista combinada de plataformas cadastradas e já lançadas para a combobox
    val allAvailablePlatforms = remember(registeredPlatforms, records) {
        val set = linkedSetOf<String>()
        registeredPlatforms.map { it.name.trim() }.filter { it.isNotBlank() }.forEach { set.add(it) }
        records.map { it.platform.trim() }.filter { it.isNotBlank() }.forEach { set.add(it) }
        set.toList()
    }

    // Estado do Acordeão de Meses Expandidos (o mais recente expandido por padrão)
    var expandedMonths by remember(records) {
        val initial = records.maxByOrNull { it.dateTimestamp }?.let {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }
            setOf("${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}")
        } ?: emptySet()
        mutableStateOf(initial)
    }

    // Gerenciamento de Semanas e Dias Expandidos (apenas a semana e o dia mais recentes expandidos por padrão)
    var expandedWeeks by remember(records) {
        val grouped = groupRecordsHierarchically(records, false)
        val initial = grouped.filterIsInstance<HistoryListItem.WeekHeader>()
            .maxByOrNull { it.weekStartMillis }
            ?.let { setOf(getHistoryListItemKey(it)) } ?: emptySet()
        mutableStateOf(initial)
    }

    var expandedDays by remember(records) {
        val grouped = groupRecordsHierarchically(records, false)
        val initial = grouped.filterIsInstance<HistoryListItem.DayHeader>()
            .maxByOrNull { it.dateTimestamp }
            ?.let { setOf(getHistoryListItemKey(it)) } ?: emptySet()
        mutableStateOf(initial)
    }

    // Filtragem combinada pura: Período + Tipo de Entrega + Plataforma + Busca Textual + Ordenação
    val filteredAndSortedRecords = remember(records, filterState, searchQuery) {
        filterAndSortHistoryRecords(records, filterState, searchQuery)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // TopBar: Título + Botão de Privacidade + Botão de Tema
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Botão de Privacidade com min 48dp e contentDescription dinâmico
                IconButton(
                    onClick = { isPrivacyEnabled = !isPrivacyEnabled },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("privacy_toggle_btn_history")
                ) {
                    Icon(
                        imageVector = if (isPrivacyEnabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (isPrivacyEnabled) R.string.cd_privacy_toggle_show else R.string.cd_privacy_toggle_hide
                        ),
                        tint = if (isPrivacyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Botão de Tema com min 48dp
            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("theme_toggle_btn_history")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = stringResource(R.string.cd_theme_toggle),
                    tint = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
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

        // Barra de Busca Textual integrada com botão de filtro (BadgedBox, min 48dp)
        HistorySearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            filterState = filterState,
            onOpenFilterSheet = { showFilterBottomSheet = true }
        )

        // Chips horizontais de filtros ativos com remoção em 1 toque
        HistoryActiveFilterChips(
            filterState = filterState,
            onRemovePeriod = { filterState = filterState.copy(period = Period.SEMANA) },
            onRemoveDeliveryType = {
                filterState = filterState.withDeliveryType(DeliveryTypeFilter.TODOS, platformsList)
            },
            onRemovePlatform = { filterState = filterState.copy(selectedPlatform = null) },
            onRemoveSort = { filterState = filterState.copy(sortOption = HistorySortOption.DATA_DECRESCENTE) }
        )

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
            val isDateSort = filterState.sortOption == HistorySortOption.DATA_DECRESCENTE || filterState.sortOption == HistorySortOption.DATA_CRESCENTE
            val isAsc = filterState.sortOption == HistorySortOption.DATA_CRESCENTE
            val groupedList = remember(filteredAndSortedRecords, isAsc, isDateSort) {
                if (isDateSort) groupRecordsHierarchically(filteredAndSortedRecords, isAsc) else emptyList()
            }

            // Mapeamento de índices para alternância de cores dos nós do dia
            val dayIndexMap = remember(groupedList) {
                val map = mutableMapOf<String, Int>()
                var idx = 0
                for (item in groupedList) {
                    if (item is HistoryListItem.DayHeader) {
                        map[getHistoryListItemKey(item)] = idx++
                    }
                }
                map
            }

            // Filtragem visual baseada no acordeão de meses, semanas e dias
            val visibleGroupedItems = remember(groupedList, expandedMonths, expandedWeeks, expandedDays) {
                var currentMonthKey = ""
                var currentWeekKey = ""
                var currentDayKey = ""
                val visibleItems = mutableListOf<HistoryListItem>()
                for (item in groupedList) {
                    when (item) {
                        is HistoryListItem.YearHeader -> visibleItems.add(item)
                        is HistoryListItem.MonthHeader -> {
                            currentMonthKey = "${item.year}_${item.month}"
                            visibleItems.add(item)
                        }
                        is HistoryListItem.WeekHeader -> {
                            currentWeekKey = getHistoryListItemKey(item)
                            val monthExpanded = currentMonthKey.isEmpty() || expandedMonths.contains(currentMonthKey)
                            if (monthExpanded) {
                                visibleItems.add(item)
                            }
                        }
                        is HistoryListItem.DayHeader -> {
                            currentDayKey = getHistoryListItemKey(item)
                            val monthExpanded = currentMonthKey.isEmpty() || expandedMonths.contains(currentMonthKey)
                            val weekExpanded = expandedWeeks.contains(currentWeekKey)
                            if (monthExpanded && weekExpanded) {
                                visibleItems.add(item)
                            }
                        }
                        is HistoryListItem.RecordRow -> {
                            val monthExpanded = currentMonthKey.isEmpty() || expandedMonths.contains(currentMonthKey)
                            val weekExpanded = expandedWeeks.contains(currentWeekKey)
                            val dayExpanded = expandedDays.contains(currentDayKey)
                            if (monthExpanded && weekExpanded && dayExpanded) {
                                visibleItems.add(item)
                            }
                        }
                    }
                }
                visibleItems
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Se a ordenação for por data, usamos agrupamento hierárquico com suporte ao acordeão de meses, semanas e dias
                if (isDateSort) {
                    items(
                        items = visibleGroupedItems,
                        key = { getHistoryListItemKey(it) }
                    ) { item ->
                        when (item) {
                            is HistoryListItem.YearHeader -> {
                                YearHeaderCard(
                                    year = item.year,
                                    totalGross = item.totalGross,
                                    totalNet = item.totalNet,
                                    totalDeliveries = item.totalDeliveries,
                                    dailyAverage = item.dailyAverage,
                                    growthPercentage = item.growthPercentage,
                                    netGrowthPercentage = item.netGrowthPercentage,
                                    annualGoalNet = annualGoalNet,
                                    isPrivacyEnabled = isPrivacyEnabled
                                )
                            }
                            is HistoryListItem.MonthHeader -> {
                                val monthLabel = "${ptMonths[item.month]} ${item.year}"
                                val monthKey = "${item.year}_${item.month}"
                                val isExpanded = expandedMonths.contains(monthKey)
                                MonthHeaderCard(
                                    monthLabel = monthLabel,
                                    totalGross = item.totalGross,
                                    totalNet = item.totalNet,
                                    activeDays = item.activeDays,
                                    totalDeliveries = item.totalDeliveries,
                                    totalKm = item.totalKm,
                                    isExpanded = isExpanded,
                                    isPrivacyEnabled = isPrivacyEnabled,
                                    onToggleExpand = {
                                        expandedMonths = if (isExpanded) {
                                            expandedMonths - monthKey
                                        } else {
                                            expandedMonths + monthKey
                                        }
                                    }
                                )
                            }
                            is HistoryListItem.WeekHeader -> {
                                val sdfWeek = SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR"))
                                val startStr = sdfWeek.format(Date(item.weekStartMillis))
                                val endStr = sdfWeek.format(Date(item.weekEndMillis))
                                val weekLabel = stringResource(R.string.history_week_header, startStr, endStr)
                                val weekKey = getHistoryListItemKey(item)
                                val isExpanded = expandedWeeks.contains(weekKey)
                                WeekHeaderCard(
                                    weekLabel = weekLabel,
                                    totalGross = item.totalGross,
                                    totalNet = item.totalNet,
                                    totalDeliveries = item.totalDeliveries,
                                    totalWorkMinutes = item.totalWorkMinutes,
                                    hourlyRate = item.hourlyRate,
                                    totalKm = item.totalKm,
                                    isPrivacyEnabled = isPrivacyEnabled,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        expandedWeeks = if (isExpanded) {
                                            expandedWeeks - weekKey
                                        } else {
                                            expandedWeeks + weekKey
                                        }
                                    }
                                )
                            }
                            is HistoryListItem.DayHeader -> {
                                val dayKey = getHistoryListItemKey(item)
                                val isExpanded = expandedDays.contains(dayKey)
                                val dayIdx = dayIndexMap[dayKey] ?: 0
                                val nodeColor = if (dayIdx % 2 == 0) Color(0xFF38BDF8) else Color(0xFF818CF8)

                                DayHeaderView(
                                    label = item.label,
                                    totalGross = item.totalGross,
                                    totalNet = item.totalNet,
                                    totalDeliveries = item.totalDeliveries,
                                    totalWorkMinutes = item.totalWorkMinutes,
                                    platformCount = item.platformCount,
                                    isPrivacyEnabled = isPrivacyEnabled,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        expandedDays = if (isExpanded) {
                                            expandedDays - dayKey
                                        } else {
                                            expandedDays + dayKey
                                        }
                                    },
                                    showTopLine = dayIdx > 0,
                                    showBottomLine = true,
                                    nodeColor = nodeColor
                                )
                            }
                            is HistoryListItem.RecordRow -> {
                                HistoryRecordCard(
                                    record = item.record,
                                    onEditClick = {
                                        recordToEdit = item.record
                                        showEditDialog = true
                                    },
                                    onDeleteClick = {
                                        recordToDelete = item.record
                                    },
                                    isPrivacyEnabled = isPrivacyEnabled,
                                    isInTimeline = true
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
                                recordToDelete = record
                            },
                            isPrivacyEnabled = isPrivacyEnabled
                        )
                    }
                }
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
            .testTag("history_snackbar_host")
    )
}

    if (showFilterBottomSheet) {
        HistoryFilterBottomSheet(
            initialFilters = filterState,
            allAvailablePlatforms = platformsList,
            onApplyFilters = { newFilters ->
                filterState = newFilters
            },
            onDismiss = { showFilterBottomSheet = false },
            onOpenCustomPeriodDialog = { showHistoryCustomPeriodDialog = true }
        )
    }

    if (showHistoryCustomPeriodDialog) {
        CustomPeriodDialog(
            initialStart = filterState.customStartDate,
            initialEnd = filterState.customEndDate,
            onDismiss = { showHistoryCustomPeriodDialog = false },
            onConfirm = { start, end ->
                filterState = filterState.copy(
                    period = Period.PERSONALIZADO,
                    customStartDate = start,
                    customEndDate = end
                )
                showHistoryCustomPeriodDialog = false
            }
        )
    }

    // Diálogo de confirmação de exclusão do registro (Trava 1.3.a)
    recordToDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            modifier = Modifier.testTag("delete_record_confirm_dialog"),
            title = {
                Text(
                    text = stringResource(R.string.history_dialog_delete_record_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.history_dialog_delete_record_message, toDelete.platform),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = toDelete
                        recordToDelete = null
                        viewModel.deleteDailyRecord(target)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.history_snackbar_record_deleted, target.platform),
                                actionLabel = context.getString(R.string.snackbar_action_undo),
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreDailyRecord(target)
                                Toast.makeText(context, context.getString(R.string.history_toast_record_restored), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_record")
                ) {
                    Text(stringResource(R.string.btn_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { recordToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_record")
                ) {
                    Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    val currentRecordToEdit = recordToEdit
    if (showEditDialog && currentRecordToEdit != null) {
        EditRecordDialog(
            record = currentRecordToEdit,
            availablePlatforms = allAvailablePlatforms,
            onDismiss = {
                showEditDialog = false
                recordToEdit = null
            },
            onSave = { newDateStr, newDateTimestamp, platform, startOdo, endOdo, gross, deliveries, fuelPrice, foodExpense, sTime, eTime, pDuration ->
                viewModel.updateDailyRecord(
                    recordId = currentRecordToEdit.id,
                    dateString = newDateStr,
                    dateTimestamp = newDateTimestamp,
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
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}
