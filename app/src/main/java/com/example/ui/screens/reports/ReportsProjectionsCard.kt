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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.ReportMetricsState
import com.example.util.CurrencyUtils

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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = stringResource(R.string.reports_projections_icon_desc),
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.reports_projections_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reports_projections_simulation_desc, metrics.monthlyWorkingDays),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(14.dp))

            val safeEarnings = if (metrics.projectedMonthlyEarnings.isFinite()) metrics.projectedMonthlyEarnings else 0.0
            val safeProfit = if (metrics.projectedMonthlyProfit.isFinite()) metrics.projectedMonthlyProfit else 0.0
            val safePctOfGoal = if (metrics.pctOfGoal.isFinite()) metrics.pctOfGoal.coerceIn(0.0, 1.5) else 1.0
            val safeGoal = if (monthlyNGoal.isFinite()) monthlyNGoal else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.reports_projections_gross_label),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = CurrencyUtils.formatCents(CurrencyUtils.toCents(safeEarnings)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8) // sky-400
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.reports_projections_net_label),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = CurrencyUtils.formatCents(CurrencyUtils.toCents(safeProfit)),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.reports_projections_goal_progress, (safePctOfGoal * 100).toInt()),
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = stringResource(R.string.reports_projections_goal_label, CurrencyUtils.formatCentsCompact(CurrencyUtils.toCents(safeGoal))),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { safePctOfGoal.coerceIn(0.0, 1.0).toFloat() },
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
