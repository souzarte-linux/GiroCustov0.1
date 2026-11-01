package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.Period
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryFilterBottomSheet(
    initialFilters: HistoryFilterState,
    allAvailablePlatforms: List<String>,
    onApplyFilters: (HistoryFilterState) -> Unit,
    onDismiss: () -> Unit,
    onOpenCustomPeriodDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftFilters by remember(initialFilters) { mutableStateOf(initialFilters) }

    val availablePlatforms = remember(allAvailablePlatforms, draftFilters.deliveryType) {
        getAvailablePlatforms(allAvailablePlatforms, draftFilters.deliveryType)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("history_filter_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Cabeçalho: Título + Botão "Limpar tudo"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_filter_sheet_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = {
                        draftFilters = HistoryFilterState()
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("filter_sheet_clear_all_btn")
                ) {
                    Text(
                        text = stringResource(R.string.history_filter_sheet_clear_all),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // SEÇÃO 1: PERÍODO
            FilterSectionTitle(stringResource(R.string.history_filter_section_period))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periodOptions = listOf(
                    Period.SEMANA to stringResource(R.string.period_week),
                    Period.QUINZENA to stringResource(R.string.period_fortnight),
                    Period.MENSAL to stringResource(R.string.period_month),
                    Period.ANUAL to stringResource(R.string.period_year),
                    Period.PERSONALIZADO to stringResource(R.string.period_custom)
                )

                periodOptions.forEach { (period, label) ->
                    val isSelected = draftFilters.period == period
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (period == Period.PERSONALIZADO) {
                                draftFilters = draftFilters.copy(period = period)
                                onOpenCustomPeriodDialog()
                            } else {
                                draftFilters = draftFilters.copy(period = period)
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("period_chip_${period.name}")
                    )
                }
            }

            // Exibição e ajuste de intervalo de data customizada quando Period.PERSONALIZADO
            if (draftFilters.period == Period.PERSONALIZADO) {
                Spacer(modifier = Modifier.height(8.dp))
                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenCustomPeriodDialog() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.cd_change_custom_period),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${dateFormat.format(Date(draftFilters.customStartDate))} até ${dateFormat.format(Date(draftFilters.customEndDate))}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEÇÃO 2: TIPO DE ENTREGA
            FilterSectionTitle(stringResource(R.string.history_filter_section_delivery_type))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val deliveryTypes = listOf(
                    DeliveryTypeFilter.TODOS to stringResource(R.string.history_filter_type_all),
                    DeliveryTypeFilter.LOGISTICA to stringResource(R.string.history_filter_type_logistics),
                    DeliveryTypeFilter.DELIVERY to stringResource(R.string.history_filter_type_delivery)
                )

                deliveryTypes.forEach { (type, label) ->
                    val isSelected = draftFilters.deliveryType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            draftFilters = draftFilters.withDeliveryType(type, allAvailablePlatforms)
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("delivery_type_chip_${type.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEÇÃO 3: PLATAFORMA (com cascata reativa)
            FilterSectionTitle(stringResource(R.string.history_filter_section_platform))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opção: Todas as plataformas
                val isAllPlatforms = draftFilters.selectedPlatform == null
                FilterChip(
                    selected = isAllPlatforms,
                    onClick = {
                        draftFilters = draftFilters.copy(selectedPlatform = null)
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.history_filter_all_platforms),
                            fontSize = 13.sp,
                            fontWeight = if (isAllPlatforms) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (isAllPlatforms) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("platform_chip_all")
                )

                // Chips para plataformas disponíveis no tipo de entrega filtrado
                availablePlatforms.forEach { platform ->
                    val isSelected = draftFilters.selectedPlatform == platform
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            draftFilters = draftFilters.copy(selectedPlatform = platform)
                        },
                        label = {
                            Text(
                                text = platform,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("platform_chip_$platform")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEÇÃO 4: ORDENAR POR
            FilterSectionTitle(stringResource(R.string.history_filter_section_sort))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistorySortOption.values().forEach { sortOption ->
                    val isSelected = draftFilters.sortOption == sortOption
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            draftFilters = draftFilters.copy(sortOption = sortOption)
                        },
                        label = {
                            Text(
                                text = stringResource(sortOption.labelResId),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("sort_chip_${sortOption.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // RODAPÉ: Ações Cancelar e Aplicar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("filter_sheet_cancel_btn")
                ) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        onApplyFilters(draftFilters)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("filter_sheet_apply_btn")
                ) {
                    Text(
                        text = stringResource(R.string.history_filter_sheet_apply),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
