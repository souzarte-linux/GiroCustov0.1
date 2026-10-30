package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.*

@Composable
fun GoalCard(
    title: String,
    grossAct: Double, grossGoal: Double,
    netAct: Double, netGoal: Double,
    kmAct: Double, kmGoal: Double,
    delAct: Int, delGoal: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Linhas de Progresso das sub-metas
            GoalRow(label = stringResource(R.string.dashboard_goal_label_gross), value = grossAct, target = grossGoal, format = "R$ %,.2f")
            GoalRow(label = stringResource(R.string.dashboard_goal_label_net), value = netAct, target = netGoal, format = "R$ %,.2f")
            GoalRow(label = stringResource(R.string.dashboard_goal_label_km), value = kmAct, target = kmGoal, format = "%,.1f km")
            GoalRowInt(label = stringResource(R.string.goals_field_deliveries), value = delAct, target = delGoal)
        }
    }
}

@Composable
fun GoalRow(label: String, value: Double, target: Double, format: String) {
    val pct = if (target > 0) (value / target).coerceIn(0.0, 1.0) else 1.0
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${String.format(Locale.GERMAN, format, value)} / ${String.format(Locale.GERMAN, format, target)} (${(pct * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (value >= target) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = if (value >= target) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun GoalRowInt(label: String, value: Int, target: Int) {
    val pct = if (target > 0) (value.toDouble() / target).coerceIn(0.0, 1.0) else 1.0
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = stringResource(R.string.dashboard_goal_deliveries_progress, value, target, (pct * 100).toInt()),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (value >= target) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = if (value >= target) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
