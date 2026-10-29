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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReportMetricsState
import com.example.util.CurrencyUtils
import java.util.*

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
                    contentDescription = "Jornada e Horas",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Jornada & Eficiência por Hora",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "Tempo em rota, pausas e remuneração calculada por hora trabalhada",
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
                        text = "Lucro Líquido / Hora",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f /h", safeNetPerHour),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF10B981)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Faturamento Bruto / Hora",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f /h", safeGrossPerHour),
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
                    Text("Tempo Efetivo", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metrics.formattedTotalWork,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tempo em Pausa", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metrics.formattedTotalPause,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFBBF24)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Média / Turno", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.GERMAN, "%.1fh", safeAvgHours),
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "DESTAQUES E RECORDES",
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
                    val bestNet = CurrencyUtils.centsToDouble(metrics.bestDay.netProfit)
                    val safeBestNet = if (bestNet.isFinite()) bestNet else 0.0
                    val bestDate = metrics.bestDay.dateString.ifBlank { "Turno" }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF022C22)), // Dark emerald container
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), // Emerald soft border glow
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.ArrowUpward, "Melhor Dia", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Melhor Turno", fontSize = 11.sp, color = Color(0xFF34D399))
                            Text(
                                text = String.format(Locale.GERMAN, "R$ %.2f", safeBestNet),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = "no dia $bestDate",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (metrics.worstDay != null) {
                    val worstNet = CurrencyUtils.centsToDouble(metrics.worstDay.netProfit)
                    val safeWorstNet = if (worstNet.isFinite()) worstNet else 0.0
                    val worstDate = metrics.worstDay.dateString.ifBlank { "Turno" }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1018)), // deep ruby/dark charcoal
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)), // rose border glow
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.ArrowDownward, "Menor Turno", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Menor Turno", fontSize = 11.sp, color = Color(0xFFFDA4AF))
                            Text(
                                text = String.format(Locale.GERMAN, "R$ %.2f", safeWorstNet),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = "no dia $worstDate",
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
