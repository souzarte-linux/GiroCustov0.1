package com.example.ui.screens.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.LaunchDistanceState

@Composable
fun LaunchOdometerSection(
    startOdo: String,
    onStartOdoChange: (String) -> Unit,
    endOdo: String,
    onEndOdoChange: (String) -> Unit,
    distanceState: LaunchDistanceState,
    isDark: Boolean,
    focusManager: FocusManager,
    inputFieldContainerColor: Color,
    inputFieldTextColor: Color,
    inputFieldLabelColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = startOdo,
                onValueChange = onStartOdoChange,
                label = { Text(stringResource(R.string.launch_start_odometer), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .testTag("input_start_odometer"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = inputFieldContainerColor,
                    unfocusedContainerColor = inputFieldContainerColor,
                    focusedTextColor = inputFieldTextColor,
                    unfocusedTextColor = inputFieldTextColor,
                    focusedLabelColor = inputFieldLabelColor,
                    unfocusedLabelColor = inputFieldLabelColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
            TextField(
                value = endOdo,
                onValueChange = onEndOdoChange,
                label = { Text(stringResource(R.string.launch_end_odometer), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .testTag("input_end_odometer"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = inputFieldContainerColor,
                    unfocusedContainerColor = inputFieldContainerColor,
                    focusedTextColor = inputFieldTextColor,
                    unfocusedTextColor = inputFieldTextColor,
                    focusedLabelColor = inputFieldLabelColor,
                    unfocusedLabelColor = inputFieldLabelColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }

        when (val dist = distanceState) {
            is LaunchDistanceState.Valid -> {
                Surface(
                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("dynamic_distance_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = stringResource(R.string.cd_launch_distance_travelled),
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.launch_distance_calculated_label),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = "${dist.formattedKm} KM",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
            is LaunchDistanceState.NegativeError -> {
                Surface(
                    color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("dynamic_distance_badge_error")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = stringResource(R.string.cd_launch_odometer_error),
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.launch_distance_odometer_error),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
            LaunchDistanceState.None -> {
                // Sem badge
            }
        }
    }
}
