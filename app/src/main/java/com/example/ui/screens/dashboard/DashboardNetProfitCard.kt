package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.Period
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardNetProfitCard(
    selectedPeriod: Period,
    customStart: Long,
    customEnd: Long,
    realNetProfit: Double,
    totalMaintenance: Double,
    totalGross: Double,
    totalExpenses: Double,
    totalFuel: Double,
    totalFood: Double,
    totalWear: Double,
    totalFixed: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)), // Deep dark green/emerald
        shape = RoundedCornerShape(24.dp), // Rounded-3xl
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), // Emerald soft border glow
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
                val labelPeriod = when (selectedPeriod) {
                    Period.SEMANA -> stringResource(R.string.dashboard_net_profit_week)
                    Period.QUINZENA -> stringResource(R.string.dashboard_net_profit_fortnight)
                    Period.MENSAL -> stringResource(R.string.dashboard_net_profit_month)
                    Period.ULTIMOS_30_DIAS -> stringResource(R.string.dashboard_net_profit_30_days)
                    Period.PERSONALIZADO -> {
                        val startStr = displayFormat.format(Date(customStart))
                        val endStr = displayFormat.format(Date(customEnd))
                        stringResource(R.string.dashboard_net_profit_custom, startStr, endStr)
                    }
                }
                Text(
                    text = labelPeriod,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399), // Soft vibrant emerald
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_badge_money_in_pocket),
                        color = Color(0xFF34D399),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "R$ ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981), // Emerald
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = String.format(Locale.GERMAN, "%,.2f", realNetProfit),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White // High contrast White
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.dashboard_maintenance_included_notice,
                    String.format(Locale.GERMAN, "%,.2f", totalMaintenance)
                ),
                fontSize = 11.sp,
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_gross_revenue), fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %,.2f", totalGross),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                )

                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(stringResource(R.string.dashboard_operational_cost), fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                    Text(
                        text = String.format(Locale.GERMAN, "- R$ %,.2f", totalExpenses),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFF43F5E) // Bright light rose/red for cost in dark mode
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.dashboard_expenses_summary_title),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.dashboard_expense_fuel), fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    Text(String.format(Locale.GERMAN, "R$ %,.2f", totalFuel), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.dashboard_expense_food), fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    Text(String.format(Locale.GERMAN, "R$ %,.2f", totalFood), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.dashboard_expense_wear), fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    Text(String.format(Locale.GERMAN, "R$ %,.2f", totalWear), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.dashboard_expense_fixed), fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    Text(String.format(Locale.GERMAN, "R$ %,.2f", totalFixed), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.dashboard_expense_maintenance), fontSize = 12.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
                    Text(String.format(Locale.GERMAN, "R$ %,.2f", totalMaintenance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                }
            }
        }
    }
}
