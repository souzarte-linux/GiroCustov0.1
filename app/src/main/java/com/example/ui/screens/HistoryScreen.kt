package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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


enum class HistorySortOption(val labelResId: Int) {
    DATA_DECRESCENTE(R.string.history_sort_date_desc),
    DATA_CRESCENTE(R.string.history_sort_date_asc),
    LUCRO_MAIOR(R.string.history_sort_profit_desc),
    LUCRO_MENOR(R.string.history_sort_profit_asc),
    KM_MAIOR(R.string.history_sort_km_desc),
    KM_MENOR(R.string.history_sort_km_asc),
    ENTREGAS(R.string.history_sort_deliveries_desc)
}

enum class QuickFilterOption {
    TODOS,
    LOGISTICA,
    DELIVERY
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

    var isPrivacyEnabled by rememberSaveable { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<DailyRecord?>(null) }

    // Busca Textual
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Filtro Rápido (Chips)
    var selectedQuickFilter by rememberSaveable { mutableStateOf(QuickFilterOption.TODOS) }

    // Filtros de Período
    var historyPeriod by remember { mutableStateOf(Period.SEMANA) }
    var historyCustomStart by remember { mutableStateOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000) }
    var historyCustomEnd by remember { mutableStateOf(System.currentTimeMillis()) }
    var showHistoryCustomPeriodDialog by remember { mutableStateOf(false) }

    // Dropdown Plataformas
    val allPlatformsLabel = stringResource(R.string.history_filter_all_platforms)
    val platformsList = remember(records, allPlatformsLabel) {
        listOf(allPlatformsLabel) + records.map { it.platform }.distinct().filter { it.isNotBlank() }
    }
    var selectedPlatformFilter by remember { mutableStateOf(allPlatformsLabel) }

    LaunchedEffect(platformsList, allPlatformsLabel) {
        if (selectedPlatformFilter !in platformsList) {
            selectedPlatformFilter = allPlatformsLabel
        }
    }

    var selectedSortOption by remember { mutableStateOf(HistorySortOption.DATA_DECRESCENTE) }

    // Estado do Acordeão de Meses Expandidos (o mais recente expandido por padrão)
    var expandedMonths by remember(records) {
        val initial = records.maxByOrNull { it.dateTimestamp }?.let {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }
            setOf("${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}")
        } ?: emptySet()
        mutableStateOf(initial)
    }

    // Filtragem combinada: Período + Quick Filter + Plataforma Dropdown + Busca Textual + Ordenação
    val filteredAndSortedRecords = remember(
        records,
        historyPeriod,
        historyCustomStart,
        historyCustomEnd,
        selectedQuickFilter,
        selectedPlatformFilter,
        searchQuery,
        selectedSortOption,
        allPlatformsLabel
    ) {
        // 1. Filtrar por Período
        val dateFiltered = filterByPeriod(records, historyPeriod, historyCustomStart, historyCustomEnd) { it.dateTimestamp }

        // 2. Filtrar por Quick Filter
        val quickFiltered = when (selectedQuickFilter) {
            QuickFilterOption.TODOS -> dateFiltered
            QuickFilterOption.LOGISTICA -> dateFiltered.filter {
                val p = it.platform.lowercase(Locale.ROOT)
                p.contains("mercado") || p.contains("livre") || p.contains("meli") ||
                    p.contains("shopee") || p.contains("loggi") || p.contains("lalamove")
            }
            QuickFilterOption.DELIVERY -> dateFiltered.filter {
                val p = it.platform.lowercase(Locale.ROOT)
                p.contains("ifood") || p.contains("uber") || p.contains("99") ||
                    p.contains("rappi") || p.contains("aiqfome") || p.contains("ze")
            }
        }

        // 3. Filtrar por Plataforma Dropdown
        val platformFiltered = if (selectedPlatformFilter == allPlatformsLabel) {
            quickFiltered
        } else {
            quickFiltered.filter { it.platform.equals(selectedPlatformFilter, ignoreCase = true) }
        }

        // 4. Filtrar por Busca Textual
        val searchFiltered = if (searchQuery.isBlank()) {
            platformFiltered
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            platformFiltered.filter { rec ->
                rec.platform.lowercase(Locale.ROOT).contains(q) ||
                    rec.dateString.lowercase(Locale.ROOT).contains(q) ||
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(rec.dateTimestamp)).contains(q)
                    } catch (e: Exception) { false }
            }
        }

        // 5. Ordenar
        when (selectedSortOption) {
            HistorySortOption.DATA_DECRESCENTE -> searchFiltered.sortedByDescending { it.dateTimestamp }
            HistorySortOption.DATA_CRESCENTE -> searchFiltered.sortedBy { it.dateTimestamp }
            HistorySortOption.LUCRO_MAIOR -> searchFiltered.sortedByDescending { it.netProfit }
            HistorySortOption.LUCRO_MENOR -> searchFiltered.sortedBy { it.netProfit }
            HistorySortOption.KM_MAIOR -> searchFiltered.sortedByDescending { it.kmRodados }
            HistorySortOption.KM_MENOR -> searchFiltered.sortedBy { it.kmRodados }
            HistorySortOption.ENTREGAS -> searchFiltered.sortedByDescending { it.deliveriesCount }
        }
    }

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

        // Barra de Busca Textual
        HistorySearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it }
        )

        // Chips de Filtro Rápido
        HistoryQuickFilterChips(
            selectedQuickFilter = selectedQuickFilter,
            onSelectQuickFilter = { selectedQuickFilter = it }
        )

        // Dropdowns de Filtros (Período, Plataforma, Ordenação)
        HistoryFilterDropdowns(
            historyPeriod = historyPeriod,
            onPeriodSelected = { historyPeriod = it },
            onOpenCustomPeriodDialog = { showHistoryCustomPeriodDialog = true },
            selectedPlatformFilter = selectedPlatformFilter,
            platformsList = platformsList,
            onPlatformSelected = { selectedPlatformFilter = it },
            selectedSortOption = selectedSortOption,
            onSortSelected = { selectedSortOption = it }
        )

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
                        text = stringResource(
                            R.string.history_custom_period_filter,
                            dateFormat.format(Date(historyCustomStart)),
                            dateFormat.format(Date(historyCustomEnd))
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
                    .weight(1f)
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Se a ordenação for por data, usamos agrupamento hierárquico com suporte ao acordeão de meses
                if (selectedSortOption == HistorySortOption.DATA_DECRESCENTE || selectedSortOption == HistorySortOption.DATA_CRESCENTE) {
                    val isAsc = selectedSortOption == HistorySortOption.DATA_CRESCENTE
                    val groupedList = groupRecordsHierarchically(filteredAndSortedRecords, isAsc)

                    // Filtragem visual baseada no acordeão de meses
                    var currentMonthKey = ""
                    val visibleItems = mutableListOf<HistoryListItem>()
                    for (item in groupedList) {
                        when (item) {
                            is HistoryListItem.YearHeader -> visibleItems.add(item)
                            is HistoryListItem.MonthHeader -> {
                                currentMonthKey = "${item.year}_${item.month}"
                                visibleItems.add(item)
                            }
                            else -> {
                                if (currentMonthKey.isEmpty() || expandedMonths.contains(currentMonthKey)) {
                                    visibleItems.add(item)
                                }
                            }
                        }
                    }

                    items(
                        items = visibleItems,
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
                                WeekHeaderCard(
                                    weekLabel = weekLabel,
                                    totalGross = item.totalGross,
                                    totalNet = item.totalNet,
                                    totalDeliveries = item.totalDeliveries,
                                    totalWorkMinutes = item.totalWorkMinutes,
                                    hourlyRate = item.hourlyRate,
                                    isPrivacyEnabled = isPrivacyEnabled
                                )
                            }
                            is HistoryListItem.DayHeader -> {
                                DayHeaderView(
                                    label = item.label,
                                    totalGross = item.totalGross,
                                    totalDeliveries = item.totalDeliveries,
                                    totalWorkMinutes = item.totalWorkMinutes,
                                    platformCount = item.platformCount,
                                    isPrivacyEnabled = isPrivacyEnabled
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
                                        viewModel.deleteDailyRecord(item.record)
                                        Toast.makeText(context, context.getString(R.string.history_toast_record_deleted), Toast.LENGTH_SHORT).show()
                                    },
                                    isPrivacyEnabled = isPrivacyEnabled
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
                                Toast.makeText(context, context.getString(R.string.history_toast_record_deleted), Toast.LENGTH_SHORT).show()
                            },
                            isPrivacyEnabled = isPrivacyEnabled
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
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}
