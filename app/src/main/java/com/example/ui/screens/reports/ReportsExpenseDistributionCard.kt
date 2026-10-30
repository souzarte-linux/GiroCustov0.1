package com.example.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.ReportMetricsState
import com.example.util.CurrencyUtils

@Composable
fun ExpenseLegendItem(label: String, color: Color, pct: Float, value: Double) {
    val safePct = if (pct.isFinite()) pct.coerceIn(0f, 1f) else 0f
    val safeValue = if (value.isFinite()) value.coerceAtLeast(0.0) else 0.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label, 
                fontSize = 12.sp, 
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(
                R.string.reports_expense_legend_value,
                CurrencyUtils.formatCents(CurrencyUtils.toCents(safeValue)),
                (safePct * 100).toInt()
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun ReportsExpenseDistributionCard(metrics: ReportMetricsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.reports_expense_distribution_title),
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            Text(
                text = stringResource(R.string.reports_expense_distribution_subtitle),
                fontSize = 11.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val fuelWeight = if (metrics.pctFuel.isFinite() && metrics.pctFuel > 0f) metrics.pctFuel.coerceIn(0.001f, 1f) else 0f
            val foodWeight = if (metrics.pctFood.isFinite() && metrics.pctFood > 0f) metrics.pctFood.coerceIn(0.001f, 1f) else 0f
            val wearWeight = if (metrics.pctWear.isFinite() && metrics.pctWear > 0f) metrics.pctWear.coerceIn(0.001f, 1f) else 0f
            val fixedWeight = if (metrics.pctFixed.isFinite() && metrics.pctFixed > 0f) metrics.pctFixed.coerceIn(0.001f, 1f) else 0f
            val maintWeight = if (metrics.pctMaintenance.isFinite() && metrics.pctMaintenance > 0f) metrics.pctMaintenance.coerceIn(0.001f, 1f) else 0f
            val hasValidSegment = (fuelWeight + foodWeight + wearWeight + fixedWeight + maintWeight) > 0f

            if (metrics.sumAllExpenses > 0 && hasValidSegment) {
                // Barra Segmentada Segura
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    if (fuelWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(fuelWeight).background(Color(0xFFFF9100)))
                    if (foodWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(foodWeight).background(Color(0xFF00B0FF)))
                    if (wearWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(wearWeight).background(Color(0xFFEF5350)))
                    if (fixedWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(fixedWeight).background(Color(0xFFAB47BC)))
                    if (maintWeight > 0f) Box(modifier = Modifier.fillMaxHeight().weight(maintWeight).background(Color(0xFFFBBF24)))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Legendas com percentual e valor
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseLegendItem(label = stringResource(R.string.reports_expense_cat_fuel), color = Color(0xFFFF9100), pct = metrics.pctFuel, value = metrics.totalFuelCost)
                    ExpenseLegendItem(label = stringResource(R.string.reports_expense_cat_food), color = Color(0xFF00B0FF), pct = metrics.pctFood, value = metrics.totalFoodCost)
                    ExpenseLegendItem(label = stringResource(R.string.reports_expense_cat_wear), color = Color(0xFFEF5350), pct = metrics.pctWear, value = metrics.totalWearCost)
                    ExpenseLegendItem(label = stringResource(R.string.reports_expense_cat_fixed), color = Color(0xFFAB47BC), pct = metrics.pctFixed, value = metrics.totalFixedCost)
                    ExpenseLegendItem(label = stringResource(R.string.reports_expense_cat_maintenance), color = Color(0xFFFBBF24), pct = metrics.pctMaintenance, value = metrics.totalMaintenanceCost)
                }
            } else {
                Text(
                    text = stringResource(R.string.reports_expense_empty),
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
