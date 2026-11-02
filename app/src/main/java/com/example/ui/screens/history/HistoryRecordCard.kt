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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DailyRecord
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlatformBadgeInfo(
    val backgroundColor: Color,
    val textColor: Color,
    val line1: String,
    val line2: String
)

fun getPlatformBrandBadge(platform: String): PlatformBadgeInfo {
    val clean = platform.trim().lowercase(Locale.ROOT)
    return when {
        clean.contains("uber") -> PlatformBadgeInfo(
            backgroundColor = Color(0xFF00D09C), // verde menta
            textColor = Color(0xFF090D16),
            line1 = "UBER",
            line2 = if (clean.contains("moto")) "MOTO" else "FLASH"
        )
        clean.contains("99") -> PlatformBadgeInfo(
            backgroundColor = Color(0xFFFF6600), // laranja
            textColor = Color(0xFF090D16),
            line1 = "99",
            line2 = "ENTREGA"
        )
        clean.contains("mercado") || clean.contains("livre") || clean.contains("meli") || clean.contains("ml") -> PlatformBadgeInfo(
            backgroundColor = Color(0xFFFFE600), // amarelo
            textColor = Color(0xFF090D16),
            line1 = "ML",
            line2 = "ENVIOS"
        )
        clean.contains("shopee") -> PlatformBadgeInfo(
            backgroundColor = Color(0xFFEE4D2D), // laranja avermelhado
            textColor = Color.White,
            line1 = "SHOPEE",
            line2 = "ENTREGA"
        )
        clean.contains("ifood") -> PlatformBadgeInfo(
            backgroundColor = Color(0xFFEA1D2C), // vermelho
            textColor = Color.White,
            line1 = "IFOOD",
            line2 = "DELIVERY"
        )
        clean.contains("loggi") -> PlatformBadgeInfo(
            backgroundColor = Color(0xFF0055FF), // azul
            textColor = Color.White,
            line1 = "LOGGI",
            line2 = "ENVIO"
        )
        clean.contains("lalamove") -> PlatformBadgeInfo(
            backgroundColor = Color(0xFFFF7700), // laranja
            textColor = Color.White,
            line1 = "LALAMOVE",
            line2 = "ENTREGA"
        )
        clean.contains("rappi") -> PlatformBadgeInfo(
            backgroundColor = Color(0xFFFF441F),
            textColor = Color.White,
            line1 = "RAPPI",
            line2 = "ENTREGA"
        )
        else -> {
            val words = platform.trim().split(" ", "-", "_").filter { it.isNotBlank() }
            if (words.size >= 2) {
                PlatformBadgeInfo(
                    backgroundColor = Color(0xFF38BDF8),
                    textColor = Color(0xFF090D16),
                    line1 = words[0].uppercase(Locale.ROOT).take(8),
                    line2 = words[1].uppercase(Locale.ROOT).take(8)
                )
            } else {
                val p = platform.trim().uppercase(Locale.ROOT)
                PlatformBadgeInfo(
                    backgroundColor = Color(0xFF38BDF8),
                    textColor = Color(0xFF090D16),
                    line1 = p.take(8),
                    line2 = "APP"
                )
            }
        }
    }
}

fun getPlatformBrandColor(platform: String): Color = getPlatformBrandBadge(platform).backgroundColor

@Composable
fun HistoryRecordCard(
    record: DailyRecord,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isPrivacyEnabled: Boolean = false,
    isInTimeline: Boolean = false,
    isLastInDay: Boolean = false,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val badgeInfo = remember(record.platform) { getPlatformBrandBadge(record.platform) }

    val cardContent = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_item_${record.id}")
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Área contraída
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge da plataforma na lateral esquerda: ~60dp x 54dp
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeInfo.backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = badgeInfo.line1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = badgeInfo.textColor,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                            Text(
                                text = badgeInfo.line2,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = badgeInfo.textColor,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Coluna de detalhes operacionais (Modifier.weight(1f))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Linha 1: [📍] KM rodados: "X,X KM"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = stringResource(R.string.cd_record_km),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            val kmFormatted = String.format(Locale.GERMAN, "%.1f KM", record.kmRodados)
                            Text(
                                text = kmFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Linha 2: [🕒] Xh Ym Online
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = stringResource(R.string.cd_record_time_online),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            val workMins = record.totalWorkMinutes
                            val durH = workMins / 60
                            val durM = workMins % 60
                            val timeOnlineStr = if (durH > 0 && durM > 0) {
                                "${durH}h ${durM}m Online"
                            } else if (durH > 0) {
                                "${durH}h Online"
                            } else {
                                "${durM}m Online"
                            }
                            Text(
                                text = timeOnlineStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Lado direito: Valor bruto + Chevron
                    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val grossTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color.Black
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val grossVal = CurrencyUtils.centsToDouble(record.grossEarnings)
                        Text(
                            text = formatHistoryCurrency(grossVal, isPrivacyEnabled),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = grossTextColor
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Seção expandida (AnimatedVisibility)
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Faixa de horário e KM
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                val hasTimes = record.startTime.isNotBlank() && record.endTime.isNotBlank()
                                val shiftRange = if (hasTimes) "${record.startTime} - ${record.endTime}" else "-- : --"
                                Text(
                                    text = shiftRange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NearMe,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = String.format(Locale.GERMAN, "%.1f km", record.kmRodados),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Grade 2x3 de custos
                        // Linha 1: Ganhos Brutos | Km Rodados | Entregas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_gross_earnings),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.grossEarnings), isPrivacyEnabled),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_km_traveled),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.GERMAN, "%.1f km", record.kmRodados),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_deliveries),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${record.deliveriesCount} ent.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Linha 2: "Custo Comb." | "Desgaste/Peças" | "Alimentação"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_fuel_cost),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.fuelCost), isPrivacyEnabled),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_wear_cost),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.wearCost), isPrivacyEnabled),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.history_card_food_expense),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.foodExpense), isPrivacyEnabled),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Rodapé com: Lucro Líquido + Botões de Ação Editar e Excluir agrupados e aproximados
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.history_card_net_profit),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                val netAmount = CurrencyUtils.centsToDouble(record.netProfit)
                                Text(
                                    text = formatHistoryCurrency(netAmount, isPrivacyEnabled),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (record.netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                        tint = MaterialTheme.colorScheme.primary,
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
                    }
                }
            }
        }
    }

    if (isInTimeline) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF334155))
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                cardContent()
            }
        }
    } else {
        cardContent()
    }
}
