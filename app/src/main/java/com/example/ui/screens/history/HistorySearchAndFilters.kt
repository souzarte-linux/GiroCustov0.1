package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.Period

@Composable
fun HistorySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.history_search_placeholder),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Limpar busca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    )
}

@Composable
fun HistoryQuickFilterChips(
    selectedQuickFilter: QuickFilterOption,
    onSelectQuickFilter: (QuickFilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chip: Todos
        val isAll = selectedQuickFilter == QuickFilterOption.TODOS
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAll) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onSelectQuickFilter(QuickFilterOption.TODOS) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .defaultMinSize(minHeight = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isAll) {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = stringResource(R.string.history_filter_quick_all),
                fontSize = 12.sp,
                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                color = if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Chip: Logística (ML/Shopee)
        val isLogistics = selectedQuickFilter == QuickFilterOption.LOGISTICA
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isLogistics) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    if (isLogistics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onSelectQuickFilter(QuickFilterOption.LOGISTICA) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .defaultMinSize(minHeight = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Text(
                text = stringResource(R.string.history_filter_quick_logistics),
                fontSize = 12.sp,
                fontWeight = if (isLogistics) FontWeight.Bold else FontWeight.Medium,
                color = if (isLogistics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Chip: Delivery/Apps (iFood/Uber)
        val isDelivery = selectedQuickFilter == QuickFilterOption.DELIVERY
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDelivery) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    if (isDelivery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onSelectQuickFilter(QuickFilterOption.DELIVERY) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .defaultMinSize(minHeight = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
            Text(
                text = stringResource(R.string.history_filter_quick_delivery),
                fontSize = 12.sp,
                fontWeight = if (isDelivery) FontWeight.Bold else FontWeight.Medium,
                color = if (isDelivery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HistoryFilterDropdowns(
    historyPeriod: Period,
    onPeriodSelected: (Period) -> Unit,
    onOpenCustomPeriodDialog: () -> Unit,
    selectedPlatformFilter: String,
    platformsList: List<String>,
    onPlatformSelected: (String) -> Unit,
    selectedSortOption: HistorySortOption,
    onSortSelected: (HistorySortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var periodExpanded by remember { mutableStateOf(false) }
    var platformExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
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
                    Period.SEMANA -> stringResource(R.string.period_week)
                    Period.QUINZENA -> stringResource(R.string.period_fortnight)
                    Period.MENSAL -> stringResource(R.string.period_month)
                    Period.ULTIMOS_30_DIAS -> stringResource(R.string.history_period_30_days_short)
                    Period.PERSONALIZADO -> stringResource(R.string.history_period_custom_short)
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
                    text = { Text(stringResource(R.string.period_week), fontSize = 13.sp) },
                    onClick = {
                        onPeriodSelected(Period.SEMANA)
                        periodExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.period_fortnight), fontSize = 13.sp) },
                    onClick = {
                        onPeriodSelected(Period.QUINZENA)
                        periodExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.period_month), fontSize = 13.sp) },
                    onClick = {
                        onPeriodSelected(Period.MENSAL)
                        periodExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.period_last_30_days), fontSize = 13.sp) },
                    onClick = {
                        onPeriodSelected(Period.ULTIMOS_30_DIAS)
                        periodExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.period_custom), fontSize = 13.sp) },
                    onClick = {
                        onPeriodSelected(Period.PERSONALIZADO)
                        periodExpanded = false
                        onOpenCustomPeriodDialog()
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
                            onPlatformSelected(p)
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
                    HistorySortOption.DATA_DECRESCENTE -> stringResource(R.string.history_sort_short_date_desc)
                    HistorySortOption.DATA_CRESCENTE -> stringResource(R.string.history_sort_short_date_asc)
                    HistorySortOption.LUCRO_MAIOR -> stringResource(R.string.history_sort_short_profit_desc)
                    HistorySortOption.LUCRO_MENOR -> stringResource(R.string.history_sort_short_profit_asc)
                    HistorySortOption.KM_MAIOR -> stringResource(R.string.history_sort_short_km_desc)
                    HistorySortOption.KM_MENOR -> stringResource(R.string.history_sort_short_km_asc)
                    HistorySortOption.ENTREGAS -> stringResource(R.string.history_sort_short_deliveries)
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
                        text = { Text(stringResource(option.labelResId), fontSize = 13.sp) },
                        onClick = {
                            onSortSelected(option)
                            sortExpanded = false
                        }
                    )
                }
            }
        }
    }
}
