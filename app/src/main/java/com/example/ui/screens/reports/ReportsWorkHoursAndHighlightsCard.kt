package com.example.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.ReportMetricsState
import com.example.util.CurrencyUtils

@Composable
fun ReportsWorkHoursCard(metrics: ReportMetricsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2D2D34)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = stringResource(R.string.reports_workhours_icon_desc),
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reports_workhours_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Text(
                text = stringResource(R.string.reports_workhours_subtitle),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Métricas Principais de Lucro e Faturamento por Hora
            val safeNetPerHour = if (metrics.netPerHour.isFinite()) metrics.netPerHour else 0.0
            val safeGrossPerHour = if (metrics.grossPerHour.isFinite()) metrics.grossPerHour else 0.0
            val safeAvgHours = if (metrics.avgHoursPerShift.isFinite()) metrics.avgHoursPerShift else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.reports_workhours_net_per_hour_label),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = stringResource(
                            R.string.reports_workhours_rate_per_hour,
                            CurrencyUtils.formatCents(CurrencyUtils.toCents(safeNetPerHour))
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF10B981)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.reports_workhours_gross_per_hour_label),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = stringResource(
                            R.string.reports_workhours_rate_per_hour,
                            CurrencyUtils.formatCents(CurrencyUtils.toCents(safeGrossPerHour))
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2D2D34))
            Spacer(modifier = Modifier.height(14.dp))

            // Linha de Detalhes: Tempo Efetivo, Pausas e Média Diária
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.reports_workhours_effective_time_label),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metrics.formattedTotalWork,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.reports_workhours_pause_time_label),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metrics.formattedTotalPause,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFBBF24)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.reports_workhours_avg_shift_label),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.reports_workhours_avg_shift_value,
                            String.format(CurrencyUtils.ptBrLocale, "%.1f", safeAvgHours)
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
fun ReportsHighlightsSection(metrics: ReportMetricsState) {
    if (metrics.bestDay != null || metrics.worstDay != null) {
        val fallbackShift = stringResource(R.string.reports_highlights_shift_fallback)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.reports_highlights_section_title),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981), // Emerald accent
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (metrics.bestDay != null) {
                    val bestDate = metrics.bestDay.dateString.ifBlank { fallbackShift }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF022C22)), // Dark emerald container
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), // Emerald soft border glow
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = stringResource(R.string.reports_highlights_best_shift_icon_desc),
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.reports_highlights_best_shift_label),
                                fontSize = 11.sp,
                                color = Color(0xFF34D399)
                            )
                            Text(
                                text = CurrencyUtils.formatCents(metrics.bestDay.netProfit),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.reports_highlights_on_day, bestDate),
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (metrics.worstDay != null) {
                    val worstDate = metrics.worstDay.dateString.ifBlank { fallbackShift }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1018)), // deep ruby/dark charcoal
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)), // rose border glow
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = stringResource(R.string.reports_highlights_worst_shift_icon_desc),
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.reports_highlights_worst_shift_label),
                                fontSize = 11.sp,
                                color = Color(0xFFFDA4AF)
                            )
                            Text(
                                text = CurrencyUtils.formatCents(metrics.worstDay.netProfit),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.reports_highlights_on_day, worstDate),
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}
