package com.example.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReportMetricsState
import java.util.*

@Composable
fun ReportsProjectionsCard(metrics: ReportMetricsState, monthlyNGoal: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark charcoal border
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, "Projeções", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Economia Acumulada e Projeções",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simulação para o mês se trabalhar os ${metrics.monthlyWorkingDays} dias planejados no ritmo atual:",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Projeção de Faturamento", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", metrics.projectedMonthlyEarnings),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8) // sky-400
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Projeção de Lucro Limpo", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", metrics.projectedMonthlyProfit),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF10B981) // Emerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2D2D34))
            Spacer(modifier = Modifier.height(12.dp))

            // Progresso em relação ao Alvo Mensal
            val pctOfGoal = metrics.pctOfGoal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Alcançará ${(pctOfGoal * 100).toInt()}% da meta mensal de Lucro", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                    text = String.format(Locale.GERMAN, "Meta: R$ %.0f", monthlyNGoal),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { pctOfGoal.coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Color(0xFF10B981),
                trackColor = Color(0xFF2D2D34)
            )
        }
    }
}
