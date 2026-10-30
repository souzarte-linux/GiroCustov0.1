package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DailyRecord
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ColorSurfaceContainer = Color(0xFF171F33)
private val ColorSurfaceContainerHigh = Color(0xFF222A3D)
private val ColorSurfaceVariant = Color(0xFF2D3449)
private val ColorTertiary = Color(0xFF4EE6AA)
private val ColorOnSurface = Color(0xFFDAE2FD)
private val ColorOnSurfaceVariant = Color(0xFFBDC8D1)
private val ColorAmberHighlight = Color(0xFFFFB020)

fun getPlatformBrandColor(platform: String): Color {
    val clean = platform.trim().lowercase(Locale.ROOT)
    return when {
        clean.contains("mercado") || clean.contains("livre") || clean.contains("meli") -> Color(0xFFFFE600)
        clean.contains("shopee") -> Color(0xFFEE4D2D)
        clean.contains("ifood") -> Color(0xFFEA1D2C)
        clean.contains("uber") -> Color(0xFF10B981)
        clean.contains("99") -> Color(0xFFFF9600)
        clean.contains("rappi") -> Color(0xFFFF441F)
        clean.contains("loggi") -> Color(0xFF0055FF)
        clean.contains("lalamove") -> Color(0xFFFF7700)
        else -> Color(0xFF38BDF8)
    }
}

@Composable
fun HistoryRecordCard(
    record: DailyRecord,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isPrivacyEnabled: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val brandColor = remember(record.platform) { getPlatformBrandColor(record.platform) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${record.id}")
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceContainer),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Friso lateral esquerdo de 4dp na cor da plataforma
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .matchParentSize()
                    .align(Alignment.CenterStart)
                    .background(brandColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Row: Plataforma + Data + Entregas + Ações Editar/Excluir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = brandColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = record.platform,
                            fontWeight = FontWeight.Bold,
                            color = brandColor,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )

                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = ColorOnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        val formattedDateShort = remember(record.dateTimestamp) {
                            try {
                                SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(record.dateTimestamp))
                            } catch (e: Exception) {
                                record.dateString
                            }
                        }
                        Text(
                            text = formattedDateShort,
                            fontWeight = FontWeight.Medium,
                            color = ColorOnSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )

                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = ColorOnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${record.deliveriesCount} Entreg.",
                            fontWeight = FontWeight.Medium,
                            color = ColorOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    // Botões de Ação: Editar e Excluir com hit-target >= 48dp
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("edit_record_btn_${record.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.cd_edit_record),
                                tint = ColorAmberHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_record_btn_${record.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.cd_delete_record),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Faixa de Horário, Duração e KM
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorSurfaceContainerHigh.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = ColorAmberHighlight,
                            modifier = Modifier.size(15.dp)
                        )
                        val hasTimes = record.startTime.isNotBlank() && record.endTime.isNotBlank()
                        val shiftRange = if (hasTimes) "${record.startTime} - ${record.endTime}" else "-- : --"
                        Text(
                            text = shiftRange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface
                        )

                        val workMins = record.totalWorkMinutes
                        if (workMins > 0) {
                            Text(
                                text = "•",
                                color = ColorOnSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            val durH = workMins / 60
                            val durM = workMins % 60
                            val durStr = if (durH > 0) "${durH}h ${durM}m" else "${durM}m"
                            Text(
                                text = durStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorTertiary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NearMe,
                            contentDescription = null,
                            tint = ColorOnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "%.1f km", record.kmRodados),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnSurface
                        )
                    }
                }

                // Seção de Detalhes Colapsável (Grade 2x3)
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 2.dp),
                            color = ColorSurfaceVariant.copy(alpha = 0.5f)
                        )

                        // Linha 1 da Grade: Ganhos Brutos | Km Rodados | Entregas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_gross_earnings),
                                    fontSize = 11.sp,
                                    color = ColorOnSurfaceVariant
                                )
                                Text(
                                    text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.grossEarnings), isPrivacyEnabled),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ColorOnSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_km_traveled),
                                    fontSize = 11.sp,
                                    color = ColorOnSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.GERMAN, "%.1f km", record.kmRodados),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ColorOnSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_deliveries),
                                    fontSize = 11.sp,
                                    color = ColorOnSurfaceVariant
                                )
                                Text(
                                    text = "${record.deliveriesCount} ent.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ColorOnSurface
                                )
                            }
                        }

                        // Linha 2 da Grade: Custo Combustível | Desgaste/Peças | Alimentação
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_fuel_cost),
                                    fontSize = 11.sp,
                                    color = ColorOnSurfaceVariant
                                )
                                Text(
                                    text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.fuelCost), isPrivacyEnabled),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ColorOnSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_wear_cost),
                                    fontSize = 11.sp,
                                    color = ColorOnSurfaceVariant
                                )
                                Text(
                                    text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.wearCost), isPrivacyEnabled),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ColorOnSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_food_expense),
                                    fontSize = 11.sp,
                                    color = ColorOnSurfaceVariant
                                )
                                Text(
                                    text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.foodExpense), isPrivacyEnabled),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ColorOnSurface
                                )
                            }
                        }
                    }
                }

                // Rodapé: LUCRO LÍQUIDO + Chevron + Valor Destacado
                HorizontalDivider(
                    modifier = Modifier.padding(top = 2.dp),
                    color = ColorSurfaceVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand),
                            tint = ColorAmberHighlight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.history_card_net_profit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ColorAmberHighlight,
                            letterSpacing = 0.5.sp
                        )
                    }

                    val netAmount = CurrencyUtils.centsToDouble(record.netProfit)
                    Text(
                        text = formatHistoryCurrency(netAmount, isPrivacyEnabled),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (record.netProfit >= 0) ColorTertiary else MaterialTheme.colorScheme.error,
                        letterSpacing = (-0.3).sp
                    )
                }
            }
        }
    }
}
