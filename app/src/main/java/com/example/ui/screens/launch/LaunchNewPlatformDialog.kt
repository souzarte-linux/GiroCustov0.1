package com.example.ui.screens.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Platform

@Composable
fun LaunchNewPlatformDialog(
    initialPlatformName: String,
    onDismiss: () -> Unit,
    onSavePlatform: (Platform) -> Unit
) {
    var newPlatformName by remember { mutableStateOf(initialPlatformName) }
    var selectedSegment by remember { mutableStateOf("delivery") }
    var selectedPaymentModel by remember { mutableStateOf("producao") }
    var selectedCycle by remember { mutableStateOf("semanal") }

    var segmentExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var cycleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.launch_dialog_new_platform_title),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = newPlatformName,
                    onValueChange = { newPlatformName = it },
                    label = { Text(stringResource(R.string.launch_dialog_platform_name), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("new_platform_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2D2D34),
                        unfocusedContainerColor = Color(0xFF2D2D34),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                // SEGMENT SELECTOR
                Column {
                    Text(stringResource(R.string.launch_dialog_segment), fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D34), RoundedCornerShape(8.dp))
                                .clickable { segmentExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("new_platform_segment_selector"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val segmentLabel = when (selectedSegment) {
                                "delivery" -> stringResource(R.string.launch_segment_delivery)
                                "transporte" -> stringResource(R.string.launch_segment_transport)
                                "misto" -> stringResource(R.string.launch_segment_mixed)
                                else -> stringResource(R.string.launch_segment_other)
                            }
                            Text(segmentLabel, color = Color.White, fontSize = 14.sp)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = segmentExpanded,
                            onDismissRequest = { segmentExpanded = false }
                        ) {
                            listOf(
                                "delivery" to stringResource(R.string.launch_segment_delivery),
                                "transporte" to stringResource(R.string.launch_segment_transport),
                                "misto" to stringResource(R.string.launch_segment_mixed),
                                "outro" to stringResource(R.string.launch_segment_other)
                            ).forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedSegment = id
                                        segmentExpanded = false
                                    },
                                    modifier = Modifier.testTag("segment_option_$id")
                                )
                            }
                        }
                    }
                }

                // PAYMENT MODEL SELECTOR
                Column {
                    Text(stringResource(R.string.launch_dialog_payment_model), fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D34), RoundedCornerShape(8.dp))
                                .clickable { paymentExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("new_platform_payment_selector"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val modelLabel = when (selectedPaymentModel) {
                                "producao" -> stringResource(R.string.launch_payment_production)
                                "fixo" -> stringResource(R.string.launch_payment_fixed)
                                else -> stringResource(R.string.launch_payment_mixed)
                            }
                            Text(modelLabel, color = Color.White, fontSize = 14.sp)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = paymentExpanded,
                            onDismissRequest = { paymentExpanded = false }
                        ) {
                            listOf(
                                "producao" to stringResource(R.string.launch_payment_production),
                                "fixo" to stringResource(R.string.launch_payment_fixed),
                                "misto" to stringResource(R.string.launch_payment_mixed)
                            ).forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedPaymentModel = id
                                        paymentExpanded = false
                                    },
                                    modifier = Modifier.testTag("payment_option_$id")
                                )
                            }
                        }
                    }
                }

                // CYCLE SELECTOR
                Column {
                    Text(stringResource(R.string.launch_dialog_payment_cycle), fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D34), RoundedCornerShape(8.dp))
                                .clickable { cycleExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("new_platform_cycle_selector"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cycleLabel = when (selectedCycle) {
                                "diario" -> stringResource(R.string.launch_cycle_daily)
                                "semanal" -> stringResource(R.string.launch_cycle_weekly)
                                "quinzenal" -> stringResource(R.string.launch_cycle_fortnightly)
                                "mensal" -> stringResource(R.string.launch_cycle_monthly)
                                else -> stringResource(R.string.launch_cycle_variable)
                            }
                            Text(cycleLabel, color = Color.White, fontSize = 14.sp)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = cycleExpanded,
                            onDismissRequest = { cycleExpanded = false }
                        ) {
                            listOf(
                                "diario" to stringResource(R.string.launch_cycle_daily),
                                "semanal" to stringResource(R.string.launch_cycle_weekly),
                                "quinzenal" to stringResource(R.string.launch_cycle_fortnightly),
                                "mensal" to stringResource(R.string.launch_cycle_monthly),
                                "misto" to stringResource(R.string.launch_cycle_variable)
                            ).forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedCycle = id
                                        cycleExpanded = false
                                    },
                                    modifier = Modifier.testTag("cycle_option_$id")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPlatformName.isNotBlank()) {
                        val newPlatform = Platform(
                            name = newPlatformName.trim(),
                            segment = selectedSegment,
                            paymentModel = selectedPaymentModel,
                            cycle = selectedCycle,
                            active = true
                        )
                        onSavePlatform(newPlatform)
                    }
                },
                modifier = Modifier.testTag("save_new_platform_btn")
            ) {
                Text(stringResource(R.string.btn_save), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_new_platform_btn")
            ) {
                Text(stringResource(R.string.btn_cancel), color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E1E22)
    )
}
