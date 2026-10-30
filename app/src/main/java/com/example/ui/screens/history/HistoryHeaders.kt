package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.util.Locale

// Paleta de Cores de Alta Precisão (Design System Nocturne Courier Logistics)
private val ColorSurfaceContainer = Color(0xFF171F33)
private val ColorSurfaceContainerLow = Color(0xFF131B2E)
private val ColorSurfaceContainerHigh = Color(0xFF222A3D)
private val ColorSurfaceVariant = Color(0xFF2D3449)
private val ColorPrimary = Color(0xFF38BDF8)
private val ColorSecondary = Color(0xFFBDC2FF)
private val ColorTertiary = Color(0xFF4EE6AA)
private val ColorOnSurface = Color(0xFFDAE2FD)
private val ColorOnSurfaceVariant = Color(0xFFBDC8D1)
private val ColorOutline = Color(0xFF87929A)

fun formatHistoryCurrency(amount: Double, isPrivacyEnabled: Boolean): String {
    return if (isPrivacyEnabled) {
        "R$ ••••••"
    } else {
        String.format(Locale.GERMAN, "R$ %,.2f", amount)
    }
}

/**
 * NÍVEL 0: Resumo Consolidado Anual (Sticky Glanceable Summary)
 */
@Composable
fun YearHeaderCard(
    year: Int,
    totalGross: Double,
    totalNet: Double,
    totalDeliveries: Int = 0,
    dailyAverage: Double = 0.0,
    growthPercentage: Double? = null,
    isPrivacyEnabled: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("year_header_$year"),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceContainer),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorSurfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fundo com brilho suave e sutil
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(ColorPrimary.copy(alpha = 0.05f))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: Badge verified + Título + Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = ColorPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.history_annual_consolidated_title, year),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (growthPercentage != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ColorTertiary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+${String.format(Locale.GERMAN, "%.1f", growthPercentage)}% vs ${year - 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorTertiary
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ColorTertiary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CONSOLIDADO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorTertiary
                            )
                        }
                    }
                }

                // Hero Currency & Subtítulo
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = formatHistoryCurrency(totalGross, isPrivacyEnabled),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = stringResource(R.string.history_annual_consolidated_subtitle),
                        fontSize = 12.sp,
                        color = ColorOnSurfaceVariant
                    )
                }

                // 2 Mini-cards: Entregas e Média / Dia
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mini-card 1: Entregas
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorSurfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorSurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = ColorPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.history_stat_deliveries),
                                fontSize = 10.sp,
                                color = ColorOnSurfaceVariant
                            )
                            Text(
                                text = if (totalDeliveries > 0) String.format(Locale.GERMAN, "%,d", totalDeliveries) else "--",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorOnSurface
                            )
                        }
                    }

                    // Mini-card 2: Média / Dia
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorSurfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorSurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = ColorTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.history_stat_daily_avg),
                                fontSize = 10.sp,
                                color = ColorOnSurfaceVariant
                            )
                            Text(
                                text = if (dailyAverage > 0) formatHistoryCurrency(dailyAverage, isPrivacyEnabled) else "--",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorOnSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Alias para YearHeaderCard conforme especificação do plano
 */
@Composable
fun ConsolidatedAnnualSummaryCard(
    year: Int,
    totalGross: Double,
    totalNet: Double,
    totalDeliveries: Int = 0,
    dailyAverage: Double = 0.0,
    growthPercentage: Double? = null,
    isPrivacyEnabled: Boolean = false
) = YearHeaderCard(
    year = year,
    totalGross = totalGross,
    totalNet = totalNet,
    totalDeliveries = totalDeliveries,
    dailyAverage = dailyAverage,
    growthPercentage = growthPercentage,
    isPrivacyEnabled = isPrivacyEnabled
)

/**
 * NÍVEL 1: Card de Mês (Acordeão Sanfonado com cantos 16dp e Sub-métricas)
 */
@Composable
fun MonthHeaderCard(
    monthLabel: String,
    totalGross: Double,
    totalNet: Double,
    activeDays: Int = 0,
    totalDeliveries: Int = 0,
    totalKm: Double = 0.0,
    isExpanded: Boolean = true,
    isPrivacyEnabled: Boolean = false,
    onToggleExpand: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("month_header_$monthLabel"),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorSurfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Ícone CalendarMonth + Nome e Dias + Faturamento + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = ColorPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = monthLabel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface
                        )
                        Text(
                            text = stringResource(R.string.history_month_active_days, activeDays),
                            fontSize = 12.sp,
                            color = ColorOnSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatHistoryCurrency(totalGross, isPrivacyEnabled),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(if (isExpanded) R.string.cd_collapse else R.string.cd_expand),
                        tint = ColorOnSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Faixa de Sub-métricas (3 blocos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bloco 1: Pacotes e KM
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorSurfaceContainerHigh.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.history_month_metric_packages),
                            fontSize = 10.sp,
                            color = ColorOnSurfaceVariant
                        )
                        Text(
                            text = totalDeliveries.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.history_month_metric_km),
                            fontSize = 10.sp,
                            color = ColorOnSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "%.0f km", totalKm),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface
                        )
                    }
                }

                // Bloco 2: Ganho Bruto
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorSurfaceContainerHigh.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_month_metric_gross),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorPrimary
                    )
                    Text(
                        text = formatHistoryCurrency(totalGross, isPrivacyEnabled),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary
                    )
                }

                // Bloco 3: Lucro Líquido
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorSurfaceContainerHigh.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_month_metric_net),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTertiary
                    )
                    Text(
                        text = formatHistoryCurrency(totalNet, isPrivacyEnabled),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTertiary
                    )
                }
            }

            // Dica de exportação quando colapsado
            if (!isExpanded) {
                Text(
                    text = stringResource(R.string.history_export_hint),
                    fontSize = 11.sp,
                    color = ColorOutline,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * NÍVEL 2: Card de Semana (Tarja vertical secondary, Lucro Líquido e Faixa de Resumo)
 */
@Composable
fun WeekHeaderCard(
    weekLabel: String,
    totalGross: Double,
    totalNet: Double,
    totalDeliveries: Int = 0,
    totalWorkMinutes: Long = 0L,
    hourlyRate: Double = 0.0,
    isPrivacyEnabled: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("week_header_$weekLabel"),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceContainer),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ColorSurfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Linha Principal: Tarja + Label + Lucro Líquido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ColorSecondary)
                    )
                    Text(
                        text = weekLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface
                    )
                }

                Text(
                    text = formatHistoryCurrency(totalNet, isPrivacyEnabled),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTertiary
                )
            }

            // Faixa de Resumo da Semana (Entregas, Horas Online, Média R$/h)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorSurfaceContainerHigh.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = null,
                        tint = ColorPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "$totalDeliveries entregas",
                        fontSize = 11.sp,
                        color = ColorOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = ColorTertiary,
                        modifier = Modifier.size(15.dp)
                    )
                    val onlineHours = totalWorkMinutes / 60
                    Text(
                        text = stringResource(R.string.history_week_online_hours, onlineHours),
                        fontSize = 11.sp,
                        color = ColorOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = null,
                        tint = ColorSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    val rateStr = if (isPrivacyEnabled) "••••" else String.format(Locale.GERMAN, "R$ %.2f", hourlyRate)
                    Text(
                        text = stringResource(R.string.history_week_hourly_rate, rateStr),
                        fontSize = 11.sp,
                        color = ColorOnSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * NÍVEL 3: DayHeaderView (Ponto indicador ciano, dia por extenso, linha divisora, Faturamento e Mini-card)
 */
@Composable
fun DayHeaderView(
    label: String,
    totalGross: Double = 0.0,
    totalDeliveries: Int = 0,
    totalWorkMinutes: Long = 0L,
    platformCount: Int = 0,
    isPrivacyEnabled: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("day_header_$label"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Linha 1: Ponto Ciano + Nome do Dia + Linha Divisora + Faturamento
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ColorPrimary)
            )

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorOnSurface
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = ColorSurfaceVariant
            )

            if (totalGross > 0.0) {
                Text(
                    text = formatHistoryCurrency(totalGross, isPrivacyEnabled),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPrimary
                )
            }
        }

        // Linha 2: Mini-card de Relance do Dia
        if (totalDeliveries > 0 || totalWorkMinutes > 0 || platformCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorSurfaceContainerHigh.copy(alpha = 0.8f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = null,
                        tint = ColorPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.history_day_glance_deliveries, totalDeliveries),
                        fontSize = 11.sp,
                        color = ColorOnSurfaceVariant
                    )
                }

                val h = totalWorkMinutes / 60
                val m = totalWorkMinutes % 60
                val durationStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = ColorOnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.history_day_glance_online, durationStr),
                        fontSize = 11.sp,
                        color = ColorOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = ColorTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.history_day_glance_platforms, platformCount.coerceAtLeast(1)),
                        fontSize = 11.sp,
                        color = ColorOnSurfaceVariant
                    )
                }
            }
        }
    }
}
