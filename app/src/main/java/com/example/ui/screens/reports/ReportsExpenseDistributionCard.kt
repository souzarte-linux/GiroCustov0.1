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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReportMetrics
import java.util.*

@Composable
fun ExpenseLegendItem(label: String, color: Color, pct: Float, value: Double) {
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
                color = Color(0xFF94A3B8), // slate-400 for dark mode
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${String.format(Locale.GERMAN, "R$ %.2f", value)} (${(pct * 100).toInt()}%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun ReportsExpenseDistributionCard(metrics: ReportMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark charcoal border
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribuição de Despesas Operacionais",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "A fatia de cada custo no orçamento total rodado",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (metrics.sumAllExpenses > 0) {
                // Barra Segmentada
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    if (metrics.pctFuel > 0) Box(modifier = Modifier.fillMaxHeight().weight(metrics.pctFuel.coerceAtLeast(0.01f)).background(Color(0xFFFF9100)))
                    if (metrics.pctFood > 0) Box(modifier = Modifier.fillMaxHeight().weight(metrics.pctFood.coerceAtLeast(0.01f)).background(Color(0xFF00B0FF)))
                    if (metrics.pctWear > 0) Box(modifier = Modifier.fillMaxHeight().weight(metrics.pctWear.coerceAtLeast(0.01f)).background(Color(0xFFEF5350)))
                    if (metrics.pctFixed > 0) Box(modifier = Modifier.fillMaxHeight().weight(metrics.pctFixed.coerceAtLeast(0.01f)).background(Color(0xFFAB47BC)))
                    if (metrics.pctMaintenance > 0) Box(modifier = Modifier.fillMaxHeight().weight(metrics.pctMaintenance.coerceAtLeast(0.01f)).background(Color(0xFFFBBF24)))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Legendas com percentual e valor
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseLegendItem(label = "Combustível", color = Color(0xFFFF9100), pct = metrics.pctFuel, value = metrics.totalFuelCost)
                    ExpenseLegendItem(label = "Alimentação", color = Color(0xFF00B0FF), pct = metrics.pctFood, value = metrics.totalFoodCost)
                    ExpenseLegendItem(label = "Desgaste / Peças", color = Color(0xFFEF5350), pct = metrics.pctWear, value = metrics.totalWearCost)
                    ExpenseLegendItem(label = "Custos Fixos", color = Color(0xFFAB47BC), pct = metrics.pctFixed, value = metrics.totalFixedCost)
                    ExpenseLegendItem(label = "Manut. Corretiva", color = Color(0xFFFBBF24), pct = metrics.pctMaintenance, value = metrics.totalMaintenanceCost)
                }
            } else {
                Text("Nenhuma despesa registrada para cálculo.", fontSize = 12.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
