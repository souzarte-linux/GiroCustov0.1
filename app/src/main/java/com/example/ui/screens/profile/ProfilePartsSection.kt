package com.example.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.util.CurrencyUtils
import java.util.Locale

@Composable
internal fun PartsWearSection(
    activeVehicle: Vehicle?,
    parts: List<VehiclePart>,
    onAddPartClick: () -> Unit,
    onEditPartClick: (VehiclePart) -> Unit,
    onResetWear: (Long) -> Unit,
    onDeletePart: (VehiclePart) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header com Botão "+ Adicionar Peça"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val vehicleName = activeVehicle?.model?.takeIf { it.isNotBlank() }
                val headerTitle = if (vehicleName != null) {
                    stringResource(R.string.profile_parts_title_with_vehicle, vehicleName)
                } else {
                    stringResource(R.string.profile_parts_title)
                }
                Text(
                    text = headerTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorBrandEmerald,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (vehicleName != null) {
                        stringResource(R.string.profile_parts_subtitle_with_vehicle, vehicleName)
                    } else {
                        stringResource(R.string.profile_parts_subtitle)
                    },
                    fontSize = 11.sp,
                    color = ColorTextMuted
                )
            }

            IconButton(
                onClick = onAddPartClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ColorSurfaceCard)
                    .testTag("profile_add_part_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_part), tint = ColorBrandEmerald)
            }
        }

        if (parts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val targetVehicleDesc = activeVehicle?.model?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_parts_empty_fallback_vehicle)
                    Text(
                        text = stringResource(R.string.profile_parts_empty_message, targetVehicleDesc),
                        color = ColorTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            parts.forEach { part ->
                val health = part.healthPercentage
                val barColor = when {
                    health <= 15.0 -> ColorRose
                    health <= 35.0 -> ColorAmber
                    else -> ColorBrandEmerald
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_part_item_${part.id}"),
                    colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorBorder),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Linha Superior: Nome, Valores e Ações
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = part.name,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorTextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = stringResource(R.string.profile_parts_item_meta, CurrencyUtils.formatCents(part.price), part.lifespanKm.toInt()),
                                    fontSize = 11.sp,
                                    color = ColorTextMuted
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEditPartClick(part) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("profile_part_edit_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_part), tint = ColorSky, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = { onResetWear(part.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("profile_part_reset_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = stringResource(R.string.cd_reset_part_wear), tint = ColorBrandEmerald, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = { onDeletePart(part) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("profile_part_delete_btn_${part.id}")
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.cd_delete_part),
                                        tint = ColorRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Indicadores de Saúde & Uso
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.profile_parts_item_usage, part.runKmSinceChange),
                                fontSize = 11.sp,
                                color = ColorTextMuted
                            )
                            Text(
                                text = stringResource(R.string.profile_parts_item_health, health),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            )
                        }

                        // Barra de Progresso Colorida
                        LinearProgressIndicator(
                            progress = { (health / 100.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .testTag("profile_part_progress_${part.id}"),
                            color = barColor,
                            trackColor = ColorBorder
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileAddPartDialog(
    activeVehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var lifespan by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_part_dialog"),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceElevated),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.profile_parts_dialog_add_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    val vehicleModel = activeVehicle?.model?.takeIf { it.isNotBlank() }
                    if (vehicleModel != null) {
                        Text(
                            text = stringResource(R.string.profile_parts_dialog_associated_vehicle, vehicleModel),
                            fontSize = 12.sp,
                            color = ColorBrandEmerald,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                HorizontalDivider(color = ColorBorder)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_parts_dialog_name_label_hint)) },
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text(stringResource(R.string.profile_parts_dialog_price_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lifespan,
                    onValueChange = { lifespan = it },
                    label = { Text(stringResource(R.string.profile_parts_dialog_lifespan_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel), color = ColorTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1000.0
                            if (name.isNotBlank() && p > 0.0) {
                                onAdd(name.trim(), p, l)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorBrandEmerald,
                            contentColor = ColorEmeraldDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.profile_parts_dialog_add_btn), fontWeight = FontWeight.Bold, color = ColorEmeraldDark)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileEditPartDialog(
    part: VehiclePart,
    activeVehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(part.name) }
    var price by remember { mutableStateOf(CurrencyUtils.centsToDouble(part.price).toString()) }
    var lifespan by remember { mutableStateOf(part.lifespanKm.toString()) }
    var runKm by remember { mutableStateOf(part.runKmSinceChange.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("edit_part_dialog"),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceElevated),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.profile_parts_dialog_edit_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    val vehicleModel = activeVehicle?.model?.takeIf { it.isNotBlank() }
                    if (vehicleModel != null) {
                        Text(
                            text = stringResource(R.string.profile_parts_dialog_associated_vehicle, vehicleModel),
                            fontSize = 12.sp,
                            color = ColorBrandEmerald,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                HorizontalDivider(color = ColorBorder)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_parts_dialog_name_label)) },
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text(stringResource(R.string.profile_parts_dialog_price_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lifespan,
                    onValueChange = { lifespan = it },
                    label = { Text(stringResource(R.string.profile_parts_dialog_lifespan_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = runKm,
                    onValueChange = { runKm = it },
                    label = { Text(stringResource(R.string.profile_parts_dialog_run_km_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel), color = ColorTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1000.0
                            val r = runKm.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && p > 0.0) {
                                onSave(name.trim(), p, l, r)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorBrandEmerald,
                            contentColor = ColorEmeraldDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold, color = ColorEmeraldDark)
                    }
                }
            }
        }
    }
}
