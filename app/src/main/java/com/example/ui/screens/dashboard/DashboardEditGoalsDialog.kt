package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.GiroCustoViewModel
import java.util.*

fun parsePtBrDouble(input: String, fallback: Double): Double {
    val clean = input.trim()
        .replace("R$", "")
        .replace(" ", "")
        .replace("\u00A0", "")
    if (clean.isEmpty()) return fallback

    return try {
        if (clean.contains('.') && clean.contains(',')) {
            if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) {
                clean.replace(".", "").replace(',', '.').toDouble()
            } else {
                clean.replace(",", "").toDouble()
            }
        } else if (clean.contains(',')) {
            clean.replace(',', '.').toDouble()
        } else {
            clean.toDouble()
        }
    } catch (_: Exception) {
        fallback
    }
}

fun parsePtBrInt(input: String, fallback: Int): Int {
    val clean = input.trim()
        .replace(" ", "")
        .replace("\u00A0", "")
        .replace(".", "")
        .replace(",", "")
    return clean.toIntOrNull() ?: fallback
}

fun formatMoneyGoal(value: Double): String =
    String.format(Locale.GERMAN, "%.2f", value)

fun formatKmGoal(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.GERMAN, "%.1f", value)

@Composable
fun EditGoalsDialog(
    viewModel: GiroCustoViewModel,
    onDismiss: () -> Unit,
    onGoalsSaved: (() -> Unit)? = null
) {
    // Pegar valores atuais formatados no padrão pt-BR
    var dg by remember { mutableStateOf(formatMoneyGoal(viewModel.dailyGoalGross.value)) }
    var dn by remember { mutableStateOf(formatMoneyGoal(viewModel.dailyGoalNet.value)) }
    var dk by remember { mutableStateOf(formatKmGoal(viewModel.dailyGoalKm.value)) }
    var dd by remember { mutableStateOf(viewModel.dailyGoalDeliveries.value.toString()) }

    var wg by remember { mutableStateOf(formatMoneyGoal(viewModel.weeklyGoalGross.value)) }
    var wn by remember { mutableStateOf(formatMoneyGoal(viewModel.weeklyGoalNet.value)) }
    var wk by remember { mutableStateOf(formatKmGoal(viewModel.weeklyGoalKm.value)) }
    var wd by remember { mutableStateOf(viewModel.weeklyGoalDeliveries.value.toString()) }

    var mg by remember { mutableStateOf(formatMoneyGoal(viewModel.monthlyGoalGross.value)) }
    var mn by remember { mutableStateOf(formatMoneyGoal(viewModel.monthlyGoalNet.value)) }
    var mk by remember { mutableStateOf(formatKmGoal(viewModel.monthlyGoalKm.value)) }
    var md by remember { mutableStateOf(viewModel.monthlyGoalDeliveries.value.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(16.dp)
                .imePadding(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)), // Charcoal background
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)) // Dark border
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.goals_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                HorizontalDivider(color = Color(0xFF2D2D34))

                // Diária
                Text(
                    stringResource(R.string.goals_section_daily),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = dg,
                        onValueChange = { dg = it },
                        label = { Text(stringResource(R.string.goals_field_gross)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = dn,
                        onValueChange = { dn = it },
                        label = { Text(stringResource(R.string.goals_field_net)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = dk,
                        onValueChange = { dk = it },
                        label = { Text(stringResource(R.string.goals_field_km)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = dd,
                        onValueChange = { dd = it },
                        label = { Text(stringResource(R.string.goals_field_deliveries)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Semanal
                Text(
                    stringResource(R.string.goals_section_weekly),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = wg,
                        onValueChange = { wg = it },
                        label = { Text(stringResource(R.string.goals_field_gross)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = wn,
                        onValueChange = { wn = it },
                        label = { Text(stringResource(R.string.goals_field_net)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = wk,
                        onValueChange = { wk = it },
                        label = { Text(stringResource(R.string.goals_field_km)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = wd,
                        onValueChange = { wd = it },
                        label = { Text(stringResource(R.string.goals_field_deliveries)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Mensal
                Text(
                    stringResource(R.string.goals_section_monthly),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = mg,
                        onValueChange = { mg = it },
                        label = { Text(stringResource(R.string.goals_field_gross)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = mn,
                        onValueChange = { mn = it },
                        label = { Text(stringResource(R.string.goals_field_net)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = mk,
                        onValueChange = { mk = it },
                        label = { Text(stringResource(R.string.goals_field_km)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = md,
                        onValueChange = { md = it },
                        label = { Text(stringResource(R.string.goals_field_deliveries)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.goals_btn_cancel), color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateGoals(
                                dailyG = parsePtBrDouble(dg, viewModel.dailyGoalGross.value),
                                dailyN = parsePtBrDouble(dn, viewModel.dailyGoalNet.value),
                                dailyK = parsePtBrDouble(dk, viewModel.dailyGoalKm.value),
                                dailyD = parsePtBrInt(dd, viewModel.dailyGoalDeliveries.value),
                                weeklyG = parsePtBrDouble(wg, viewModel.weeklyGoalGross.value),
                                weeklyN = parsePtBrDouble(wn, viewModel.weeklyGoalNet.value),
                                weeklyK = parsePtBrDouble(wk, viewModel.weeklyGoalKm.value),
                                weeklyD = parsePtBrInt(wd, viewModel.weeklyGoalDeliveries.value),
                                monthlyG = parsePtBrDouble(mg, viewModel.monthlyGoalGross.value),
                                monthlyN = parsePtBrDouble(mn, viewModel.monthlyGoalNet.value),
                                monthlyK = parsePtBrDouble(mk, viewModel.monthlyGoalKm.value),
                                monthlyD = parsePtBrInt(md, viewModel.monthlyGoalDeliveries.value)
                            )
                            onGoalsSaved?.invoke()
                            onDismiss()
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF064E3B)
                        )
                    ) {
                        Text(stringResource(R.string.goals_btn_save), fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                    }
                }
            }
        }
    }
}
