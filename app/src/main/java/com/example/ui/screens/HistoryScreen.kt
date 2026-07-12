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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyRecord
import com.example.ui.GiroCustoViewModel
import com.example.ui.Period
import com.example.ui.components.CustomPeriodDialog
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
                        overflow = TextOverflow.Ellipsis
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
                        overflow = TextOverflow.Ellipsis
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
                        overflow = TextOverflow.Ellipsis
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
                // Se a ordenação for por data (crescente ou decrescente), usamos o agrupamento hierárquico.
                // Caso contrário (por lucro, km, entregas, etc.), usamos uma lista plana para não quebrar a lógica de ordenação.
                if (selectedSortOption == HistorySortOption.DATA_DECRESCENTE || selectedSortOption == HistorySortOption.DATA_CRESCENTE) {
                    val isAsc = selectedSortOption == HistorySortOption.DATA_CRESCENTE
                    val groupedList = groupRecordsHierarchically(filteredAndSortedRecords, isAsc)
                    
                    items(groupedList) { item ->
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
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${record.id}")
            .clickable { expanded = !expanded },
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

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

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
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            // Bottom row: Lucro Líquido (always visible, holds expand indicator icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "LUCRO LÍQUIDO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

// -----------------------------------------------------------------------------
// HIERARCHICAL GROUPING & RENDERING FOR HISTORY LIST
// -----------------------------------------------------------------------------

val ptMonths = arrayOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

sealed class HistoryListItem {
    data class YearHeader(val year: Int, val totalGross: Double, val totalNet: Double) : HistoryListItem()
    data class MonthHeader(val year: Int, val month: Int, val totalGross: Double, val totalNet: Double) : HistoryListItem()
    data class WeekHeader(val weekStartMillis: Long, val weekEndMillis: Long, val totalGross: Double, val totalNet: Double) : HistoryListItem()
    data class DayHeader(val dateTimestamp: Long, val label: String) : HistoryListItem()
    data class RecordRow(val record: DailyRecord) : HistoryListItem()
}

/**
 * Calculates start and end of a week subdivision truncated by month.
 */
fun getWeekSubdivisionBoundaries(recordTimestamp: Long, year: Int, month: Int): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = recordTimestamp
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysToMonday = when (dayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> -1
        Calendar.WEDNESDAY -> -2
        Calendar.THURSDAY -> -3
        Calendar.FRIDAY -> -4
        Calendar.SATURDAY -> -5
        Calendar.SUNDAY -> -6
        else -> 0
    }

    val mondayCal = Calendar.getInstance().apply {
        timeInMillis = cal.timeInMillis
        add(Calendar.DAY_OF_YEAR, daysToMonday)
    }

    val sundayCal = Calendar.getInstance().apply {
        timeInMillis = mondayCal.timeInMillis
        add(Calendar.DAY_OF_YEAR, 6)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val startOfMonth = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val endOfMonth = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val finalStart = maxOf(mondayCal.timeInMillis, startOfMonth.timeInMillis)
    val finalEnd = minOf(sundayCal.timeInMillis, endOfMonth.timeInMillis)

    return Pair(finalStart, finalEnd)
}

fun formatDayLabel(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, dd 'de' MMM", Locale("pt", "BR"))
    val raw = sdf.format(Date(timestamp))
    return if (raw.isNotEmpty()) {
        raw.substring(0, 1).uppercase(Locale("pt", "BR")) + raw.substring(1)
    } else {
        raw
    }
}

/**
 * Groups daily records hierarchically into a list of list items.
 * A week that spans across two months is divided; the days that fall in each month
 * count towards that specific month's totals (so week groups are subdivisions within the calendar month).
 */
fun groupRecordsHierarchically(records: List<DailyRecord>, isAscending: Boolean): List<HistoryListItem> {
    if (records.isEmpty()) return emptyList()

    // 1. Sort records first
    val sortedRecords = if (isAscending) {
        records.sortedWith(compareBy<DailyRecord> { it.dateTimestamp }.thenBy { it.id })
    } else {
        records.sortedWith(compareByDescending<DailyRecord> { it.dateTimestamp }.thenByDescending { it.id })
    }

    data class DayKey(val year: Int, val month: Int, val dayOfMonth: Int)
    data class WeekKey(val year: Int, val month: Int, val weekStart: Long, val weekEnd: Long)

    val yearToMonths = LinkedHashMap<Int, LinkedHashMap<Int, LinkedHashMap<WeekKey, LinkedHashMap<DayKey, List<DailyRecord>>>>>()

    for (record in sortedRecords) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = record.dateTimestamp
            firstDayOfWeek = Calendar.MONDAY
        }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)

        val boundaries = getWeekSubdivisionBoundaries(record.dateTimestamp, y, m)
        val weekKey = WeekKey(y, m, boundaries.first, boundaries.second)
        val dayKey = DayKey(y, m, d)

        val monthsMap = yearToMonths.getOrPut(y) { LinkedHashMap() }
        val weeksMap = monthsMap.getOrPut(m) { LinkedHashMap() }
        val daysMap = weeksMap.getOrPut(weekKey) { LinkedHashMap() }
        val dayRecords = daysMap[dayKey] ?: emptyList()
        daysMap[dayKey] = dayRecords + record
    }

    val resultList = mutableListOf<HistoryListItem>()

    for ((y, monthsMap) in yearToMonths) {
        var yearGross = 0.0
        var yearNet = 0.0
        val yearItems = mutableListOf<HistoryListItem>()

        for ((m, weeksMap) in monthsMap) {
            var monthGross = 0.0
            var monthNet = 0.0
            val monthItems = mutableListOf<HistoryListItem>()

            for ((weekKey, daysMap) in weeksMap) {
                var weekGross = 0.0
                var weekNet = 0.0
                val weekItems = mutableListOf<HistoryListItem>()

                for ((dayKey, dayRecords) in daysMap) {
                    val representativeTimestamp = dayRecords.first().dateTimestamp
                    val dayLabel = formatDayLabel(representativeTimestamp)
                    
                    weekItems.add(HistoryListItem.DayHeader(representativeTimestamp, dayLabel))
                    for (rec in dayRecords) {
                        weekItems.add(HistoryListItem.RecordRow(rec))
                        weekGross += rec.grossEarnings
                        weekNet += rec.netProfit
                    }
                }

                monthItems.add(HistoryListItem.WeekHeader(weekKey.weekStart, weekKey.weekEnd, weekGross, weekNet))
                monthItems.addAll(weekItems)

                monthGross += weekGross
                monthNet += weekNet
            }

            yearItems.add(HistoryListItem.MonthHeader(y, m, monthGross, monthNet))
            yearItems.addAll(monthItems)

            yearGross += monthGross
            yearNet += monthNet
        }

        resultList.add(HistoryListItem.YearHeader(y, yearGross, yearNet))
        resultList.addAll(yearItems)
    }

    return resultList
}

@Composable
fun YearHeaderCard(year: Int, totalGross: Double, totalNet: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("year_header_$year"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = year.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Faturamento",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", totalGross),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Lucro Líquido",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", totalNet),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalNet >= 0) Color(0xFF10B981) else Color(0xFFF43F5E)
                    )
                }
            }
        }
    }
}

@Composable
fun MonthHeaderCard(monthLabel: String, totalGross: Double, totalNet: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
            .testTag("month_header_$monthLabel"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = monthLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Faturamento",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", totalGross),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Lucro Líquido",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", totalNet),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totalNet >= 0) Color(0xFF10B981) else Color(0xFFF43F5E)
                    )
                }
            }
        }
    }
}

@Composable
fun WeekHeaderCard(weekLabel: String, totalGross: Double, totalNet: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .testTag("week_header_$weekLabel"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weekLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Fat.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.0f", totalGross),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Lucro",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.0f", totalNet),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalNet >= 0) Color(0xFF10B981) else Color(0xFFF43F5E)
                    )
                }
            }
        }
    }
}

@Composable
fun DayHeaderView(label: String) {
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            .testTag("day_header_$label")
    )
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

    val focusManager = LocalFocusManager.current

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
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                TextField(
                    value = platformState,
                    onValueChange = { platformState = it },
                    label = { Text("Plataforma", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("edit_platform"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = "Plataforma",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = startOdo,
                        onValueChange = { startOdo = it },
                        label = { Text("Hodôm. Inicial", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_start_odo")
                    )
                    TextField(
                        value = endOdo,
                        onValueChange = { endOdo = it },
                        label = { Text("Hodôm. Final", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_end_odo")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = gross,
                        onValueChange = { gross = it },
                        label = { Text("Ganhos (R$)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_gross")
                    )
                    TextField(
                        value = deliveries,
                        onValueChange = { deliveries = it },
                        label = { Text("Entregas", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_deliveries")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = fuelPrice,
                        onValueChange = { fuelPrice = it },
                        label = { Text("Preço Comb. (R$)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_fuel_price")
                    )
                    TextField(
                        value = foodExpense,
                        onValueChange = { foodExpense = it },
                        label = { Text("Alimentação (R$)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_food_expense")
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
