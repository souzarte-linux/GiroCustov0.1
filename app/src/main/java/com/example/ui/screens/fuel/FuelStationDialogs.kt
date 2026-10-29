package com.example.ui.screens.fuel

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Locale

@Composable
fun FuelAddStationDialog(
    initialGasStation: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newStationName by remember { mutableStateOf(initialGasStation) }
    var selectedBrand by remember { mutableStateOf("Petrobras") }
    var brandExpanded by remember { mutableStateOf(false) }
    var locationStr by remember { mutableStateOf("") }
    val brands = listOf("Petrobras", "Ipiranga", "Shell", "ALE", "Outra")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.fuel_dialog_new_station_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = newStationName,
                    onValueChange = { newStationName = it },
                    label = { Text(stringResource(R.string.fuel_dialog_station_name_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("new_station_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Column {
                    Text(stringResource(R.string.fuel_dialog_station_brand_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { brandExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("new_station_brand_selector"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedBrand, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = stringResource(R.string.cd_select_brand),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            brands.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand) },
                                    onClick = {
                                        selectedBrand = brand
                                        brandExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                TextField(
                    value = locationStr,
                    onValueChange = { locationStr = it },
                    label = { Text(stringResource(R.string.fuel_dialog_station_location_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("new_station_location_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newStationName.isNotBlank()) {
                        val nameWithBrand = if (selectedBrand != "Outra") {
                            "${newStationName.trim()} ($selectedBrand)"
                        } else {
                            newStationName.trim()
                        }
                        onConfirm(nameWithBrand)
                    }
                },
                modifier = Modifier.testTag("save_new_station_btn")
            ) {
                Text(stringResource(R.string.btn_save), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_new_station_btn")
            ) {
                Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun FuelEditStationDialog(
    stationToEdit: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editedStationName by remember { mutableStateOf(stationToEdit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.fuel_dialog_edit_station_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.fuel_dialog_edit_station_warning),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = editedStationName,
                    onValueChange = { editedStationName = it },
                    label = { Text(stringResource(R.string.fuel_dialog_station_name_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_station_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editedStationName.isNotBlank() && editedStationName.trim() != stationToEdit) {
                        onConfirm(editedStationName.trim())
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_edit_station_btn")
            ) {
                Text(stringResource(R.string.btn_save), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_edit_station_btn")
            ) {
                Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun FuelTankCapacityConfirmDialog(
    liters: Double,
    tankCapacity: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF59E0B))
        },
        title = {
            Text(stringResource(R.string.fuel_dialog_tank_exceeded_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                stringResource(R.string.fuel_dialog_tank_exceeded_message, liters, tankCapacity)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_exceeded_tank_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.fuel_dialog_tank_exceeded_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_exceeded_tank_btn")
            ) {
                Text(stringResource(R.string.fuel_dialog_tank_exceeded_dismiss))
            }
        },
        modifier = Modifier.testTag("tank_capacity_confirm_dialog")
    )
}
