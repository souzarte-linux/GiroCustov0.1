package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import java.text.NumberFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Locale


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
    netGrowthPercentage: Double? = null,
    annualGoalNet: Double = 0.0,
    isPrivacyEnabled: Boolean = false
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
        animationSpec = tween(durationMillis = 300),
        label = "chevron_rotation"
    )

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val subCardBg = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val subCardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("year_header_$year"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // A) Cabeçalho do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = stringResource(R.string.cd_payments_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isExpanded) {
                            stringResource(R.string.annual_consolidated_title)
                        } else {
                            stringResource(
                                R.string.annual_consolidated_collapsed_title,
                                formatHistoryCurrency(totalGross, isPrivacyEnabled)
                            )
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_toggle_annual_consolidation")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(if (isExpanded) R.string.cd_collapse else R.string.cd_expand),
                        modifier = Modifier.rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // B) Dois quadrados lado a lado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quadrado Esquerdo: TOTAL BRUTO ANUAL
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(subCardBg)
                                .border(subCardBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.annual_stat_total_gross),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatHistoryCurrency(totalGross, isPrivacyEnabled),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (growthPercentage != null) {
                                    val isPos = growthPercentage >= 0
                                    val sign = if (isPos) "+" else ""
                                    val color = if (isPos) Color(0xFF10B981) else Color(0xFFEF4444)
                                    val arrow = if (isPos) "↑" else "↓"
                                    Text(
                                        text = "$arrow $sign${String.format(Locale.forLanguageTag("pt-BR"), "%.1f", growthPercentage)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = color,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "—",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Quadrado Direito: LUCRO LÍQUIDO ANUAL
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(subCardBg)
                                .border(subCardBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.annual_stat_net_profit),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatHistoryCurrency(totalNet, isPrivacyEnabled),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (netGrowthPercentage != null) {
                                    val isPos = netGrowthPercentage >= 0
                                    val sign = if (isPos) "+" else ""
                                    val color = if (isPos) Color(0xFF10B981) else Color(0xFFEF4444)
                                    val arrow = if (isPos) "↑" else "↓"
                                    Text(
                                        text = "$arrow $sign${String.format(Locale.forLanguageTag("pt-BR"), "%.1f", netGrowthPercentage)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = color,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "—",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // C) Dois retângulos de mesmo tamanho, lado a lado
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Retângulo Esquerdo: Entregas
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("stat_card_deliveries")
                                .clip(RoundedCornerShape(10.dp))
                                .background(subCardBg)
                                .border(subCardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.annual_stat_deliveries),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (totalDeliveries > 0) {
                                            NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(totalDeliveries)
                                        } else {
                                            "0"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Retângulo Direito: Média Dia
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("stat_card_daily_avg")
                                .clip(RoundedCornerShape(10.dp))
                                .background(subCardBg)
                                .border(subCardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.annual_stat_daily_avg),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatHistoryCurrency(dailyAverage, isPrivacyEnabled),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // D) Linha divisória fina abaixo da seção acima
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // E) Meta Anual
                    if (annualGoalNet > 0.0) {
                        val rawProgress = (totalNet / annualGoalNet).toFloat()
                        val progress = rawProgress.coerceIn(0f, 1f)
                        val percentInt = (rawProgress * 100).toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.annual_goal_reached_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Selo com porcentagem
                            val badgeBg = if (isDarkTheme) Color(0xFF1E3A8A).copy(alpha = 0.4f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            val badgeBorder = if (isDarkTheme) Color(0xFF38BDF8).copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            val badgeText = if (isDarkTheme) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(badgeBg)
                                    .border(BorderStroke(1.dp, badgeBorder), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$percentInt%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Barra de progresso (LinearProgressIndicator)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = if (isDarkTheme) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Texto pequeno: R$ X de R$ Y
                        Text(
                            text = stringResource(
                                R.string.annual_goal_progress_text,
                                formatHistoryCurrency(totalNet, isPrivacyEnabled),
                                formatHistoryCurrency(annualGoalNet, isPrivacyEnabled)
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Caso a meta anual seja 0, exibe aviso orientando o usuário
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.annual_goal_not_set),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal
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
    netGrowthPercentage: Double? = null,
    annualGoalNet: Double = 0.0,
    isPrivacyEnabled: Boolean = false
) = YearHeaderCard(
    year = year,
    totalGross = totalGross,
    totalNet = totalNet,
    totalDeliveries = totalDeliveries,
    dailyAverage = dailyAverage,
    growthPercentage = growthPercentage,
    netGrowthPercentage = netGrowthPercentage,
    annualGoalNet = annualGoalNet,
    isPrivacyEnabled = isPrivacyEnabled
)

@Preview(name = "YearHeaderCard - Expanded Dark", showBackground = true)
@Composable
fun YearHeaderCardExpandedDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Color(0xFF0B132B)).padding(16.dp)) {
            YearHeaderCard(
                year = 2026,
                totalGross = 68420.0,
                totalNet = 58120.0,
                totalDeliveries = 1840,
                dailyAverage = 195.40,
                growthPercentage = 14.2,
                netGrowthPercentage = 12.8,
                annualGoalNet = 68000.0,
                isPrivacyEnabled = false
            )
        }
    }
}

@Preview(name = "YearHeaderCard - Collapsed Dark", showBackground = true)
@Composable
fun YearHeaderCardCollapsedDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(modifier = Modifier.background(Color(0xFF0B132B)).padding(16.dp)) {
            YearHeaderCard(
                year = 2026,
                totalGross = 68420.0,
                totalNet = 58120.0,
                totalDeliveries = 1840,
                dailyAverage = 195.40,
                growthPercentage = 14.2,
                netGrowthPercentage = 12.8,
                annualGoalNet = 68000.0,
                isPrivacyEnabled = false
            )
        }
    }
}

@Preview(name = "YearHeaderCard - Expanded Light", showBackground = true)
@Composable
fun YearHeaderCardExpandedLightPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Box(modifier = Modifier.background(Color(0xFFF8FAFC)).padding(16.dp)) {
            YearHeaderCard(
                year = 2026,
                totalGross = 68420.0,
                totalNet = 58120.0,
                totalDeliveries = 1840,
                dailyAverage = 195.40,
                growthPercentage = 14.2,
                netGrowthPercentage = 12.8,
                annualGoalNet = 68000.0,
                isPrivacyEnabled = false
            )
        }
    }
}

