package com.example.ui.screens.launch

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Locale

@Composable
fun LaunchShiftHoursSection(
    context: Context,
    focusManager: FocusManager,
    startTimeVal: String,
    onStartTimeChange: (String) -> Unit,
    endTimeVal: String,
    onEndTimeChange: (String) -> Unit,
    pauseDurationVal: String,
    onPauseDurationChange: (String) -> Unit,
    inputFieldContainerColor: Color,
    inputFieldTextColor: Color,
    inputFieldLabelColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.launch_shift_hours_section),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Hora Início
            TextField(
                value = startTimeVal,
                onValueChange = onStartTimeChange,
                label = { Text(stringResource(R.string.launch_start_time), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("08:00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .testTag("input_start_time"),
                trailingIcon = {
                    IconButton(onClick = {
                        val sParts = startTimeVal.split(":")
                        val initialH = sParts.getOrNull(0)?.toIntOrNull() ?: 8
                        val initialM = sParts.getOrNull(1)?.toIntOrNull() ?: 0
                        android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                onStartTimeChange(String.format(Locale.US, "%02d:%02d", hourOfDay, minute))
                            },
                            initialH, initialM, true
                        ).show()
                    }) {
                        Icon(Icons.Filled.Schedule, contentDescription = stringResource(R.string.cd_launch_select_start_time), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                singleLine = true,
                maxLines = 1,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = inputFieldContainerColor,
                    unfocusedContainerColor = inputFieldContainerColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            // Hora Final
            TextField(
                value = endTimeVal,
                onValueChange = onEndTimeChange,
                label = { Text(stringResource(R.string.launch_end_time), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("18:00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .testTag("input_end_time"),
                trailingIcon = {
                    IconButton(onClick = {
                        val eParts = endTimeVal.split(":")
                        val initialH = eParts.getOrNull(0)?.toIntOrNull() ?: 18
                        val initialM = eParts.getOrNull(1)?.toIntOrNull() ?: 0
                        android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                onEndTimeChange(String.format(Locale.US, "%02d:%02d", hourOfDay, minute))
                            },
                            initialH, initialM, true
                        ).show()
                    }) {
                        Icon(Icons.Filled.Schedule, contentDescription = stringResource(R.string.cd_launch_select_end_time), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                singleLine = true,
                maxLines = 1,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = inputFieldContainerColor,
                    unfocusedContainerColor = inputFieldContainerColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }

        // Total Tempo Pause
        TextField(
            value = pauseDurationVal,
            onValueChange = onPauseDurationChange,
            label = { Text(stringResource(R.string.launch_pause_duration), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("01:00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("input_pause_duration"),
            visualTransformation = com.example.util.PauseDurationVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = {
                    val pParts = pauseDurationVal.split(":")
                    val initialH = pParts.getOrNull(0)?.toIntOrNull() ?: 1
                    val initialM = pParts.getOrNull(1)?.toIntOrNull() ?: 0
                    android.app.TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            onPauseDurationChange(String.format(Locale.US, "%02d:%02d", hourOfDay, minute))
                        },
                        initialH, initialM, true
                    ).show()
                }) {
                    Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.cd_launch_select_pause_time), tint = MaterialTheme.colorScheme.primary)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            maxLines = 1,
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = inputFieldContainerColor,
                unfocusedContainerColor = inputFieldContainerColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        // Resumo Dinâmico da Jornada
        val shiftSummary = remember(startTimeVal, endTimeVal, pauseDurationVal) {
            if (startTimeVal.contains(":") && endTimeVal.contains(":")) {
                val sParts = startTimeVal.split(":")
                val eParts = endTimeVal.split(":")
                val sH = sParts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                val sM = sParts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                val eH = eParts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                val eM = eParts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                val sTotal = sH * 60L + sM
                var eTotal = eH * 60L + eM
                if (eTotal < sTotal) eTotal += 24L * 60L
                val grossMin = eTotal - sTotal

                var pauseMin = 0L
                val pClean = pauseDurationVal.trim()
                if (pClean.contains(":")) {
                    val pParts = pClean.split(":")
                    pauseMin = (pParts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L) * 60L + (pParts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L)
                } else if (pClean.isNotBlank()) {
                    pauseMin = pClean.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
                }
                val netMin = (grossMin - pauseMin).coerceAtLeast(0L)
                val netH = netMin / 60
                val netM = netMin % 60
                context.getString(
                    R.string.launch_shift_summary,
                    netH,
                    if (netM > 0) "${netM}m" else "00m",
                    grossMin / 60,
                    grossMin % 60,
                    pauseMin / 60,
                    pauseMin % 60
                )
            } else {
                null
            }
        }

        if (shiftSummary != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(text = shiftSummary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}
