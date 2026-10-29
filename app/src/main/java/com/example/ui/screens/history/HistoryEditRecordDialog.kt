package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.DailyRecord
import com.example.util.CurrencyUtils
import java.util.Locale

@Composable
fun EditRecordDialog(
    record: DailyRecord,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Int, Double, Double, String, String, String) -> Unit
) {
    var platformState by remember { mutableStateOf(record.platform) }
    var startOdo by remember { mutableStateOf(record.startOdometer.toString()) }
    var endOdo by remember { mutableStateOf(record.endOdometer.toString()) }
    var gross by remember { mutableStateOf(CurrencyUtils.centsToDouble(record.grossEarnings).toString()) }
    var deliveries by remember { mutableStateOf(record.deliveriesCount.toString()) }
    var fuelPrice by remember { mutableStateOf(CurrencyUtils.fuelPriceMillisToDouble(record.fuelPrice).toString()) }
    var foodExpense by remember { mutableStateOf(CurrencyUtils.centsToDouble(record.foodExpense).toString()) }
    var startTimeState by remember { mutableStateOf(record.startTime) }
    var endTimeState by remember { mutableStateOf(record.endTime) }
    var pauseDurationState by remember { mutableStateOf(record.pauseDuration) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.history_edit_dialog_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                TextField(
                    value = platformState,
                    onValueChange = { platformState = it },
                    label = { Text(stringResource(R.string.history_edit_platform_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("edit_platform"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = stringResource(R.string.cd_platform),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = startOdo,
                        onValueChange = { startOdo = it },
                        label = { Text(stringResource(R.string.history_edit_start_odo_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_start_odo")
                    )
                    TextField(
                        value = endOdo,
                        onValueChange = { endOdo = it },
                        label = { Text(stringResource(R.string.history_edit_end_odo_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_end_odo")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = gross,
                        onValueChange = { gross = it },
                        label = { Text(stringResource(R.string.history_edit_gross_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_gross")
                    )
                    TextField(
                        value = deliveries,
                        onValueChange = { deliveries = it },
                        label = { Text(stringResource(R.string.history_edit_deliveries_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_deliveries")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = fuelPrice,
                        onValueChange = { fuelPrice = it },
                        label = { Text(stringResource(R.string.history_edit_fuel_price_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_fuel_price")
                    )
                    TextField(
                        value = foodExpense,
                        onValueChange = { foodExpense = it },
                        label = { Text(stringResource(R.string.history_edit_food_expense_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_food_expense")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = startTimeState,
                        onValueChange = { startTimeState = it },
                        label = { Text(stringResource(R.string.history_edit_start_time_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("08:00") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val sParts = startTimeState.split(":")
                                    val initialH = sParts.getOrNull(0)?.toIntOrNull() ?: 8
                                    val initialM = sParts.getOrNull(1)?.toIntOrNull() ?: 0
                                    android.app.TimePickerDialog(context, { _, h, m ->
                                        startTimeState = String.format(Locale.US, "%02d:%02d", h, m)
                                    }, initialH, initialM, true).show()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = stringResource(R.string.cd_select_start_time), tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_start_time")
                    )

                    TextField(
                        value = endTimeState,
                        onValueChange = { endTimeState = it },
                        label = { Text(stringResource(R.string.history_edit_end_time_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("18:00") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val eParts = endTimeState.split(":")
                                    val initialH = eParts.getOrNull(0)?.toIntOrNull() ?: 18
                                    val initialM = eParts.getOrNull(1)?.toIntOrNull() ?: 0
                                    android.app.TimePickerDialog(context, { _, h, m ->
                                        endTimeState = String.format(Locale.US, "%02d:%02d", h, m)
                                    }, initialH, initialM, true).show()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = stringResource(R.string.cd_select_end_time), tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp).testTag("edit_end_time")
                    )
                }

                TextField(
                    value = pauseDurationState,
                    onValueChange = { pauseDurationState = it },
                    label = { Text(stringResource(R.string.history_edit_pause_duration_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("01:00") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val pParts = pauseDurationState.split(":")
                                val initialH = pParts.getOrNull(0)?.toIntOrNull() ?: 1
                                val initialM = pParts.getOrNull(1)?.toIntOrNull() ?: 0
                                android.app.TimePickerDialog(context, { _, h, m ->
                                    pauseDurationState = String.format(Locale.US, "%02d:%02d", h, m)
                                }, initialH, initialM, true).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.cd_select_pause_duration), tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("edit_pause_duration")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val startOdoVal = startOdo.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                            val endOdoVal = endOdo.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                            val grossCents = CurrencyUtils.parseToCents(gross, 0L)
                            val deliveriesVal = deliveries.trim().toIntOrNull() ?: 0
                            val fuelPriceMillis = CurrencyUtils.parseToFuelPriceMillis(fuelPrice, 0L)
                            val foodExpenseCents = CurrencyUtils.parseToCents(foodExpense, 0L)

                            if (grossCents > 0L && fuelPriceMillis > 0L && platformState.isNotBlank()) {
                                onSave(
                                    platformState,
                                    startOdoVal,
                                    endOdoVal,
                                    CurrencyUtils.centsToDouble(grossCents),
                                    deliveriesVal,
                                    CurrencyUtils.fuelPriceMillisToDouble(fuelPriceMillis),
                                    CurrencyUtils.centsToDouble(foodExpenseCents),
                                    startTimeState,
                                    endTimeState,
                                    pauseDurationState
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
