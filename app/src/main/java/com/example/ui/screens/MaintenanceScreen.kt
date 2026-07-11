package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MaintenanceRecord
import com.example.ui.GiroCustoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(viewModel: GiroCustoViewModel) {
    val records by viewModel.maintenanceRecords.collectAsStateWithLifecycle()
    val activeVehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingRecordId by remember { mutableStateOf<Long?>(null) }

    if (activeVehicle == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Alerta",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Selecione ou Cadastre um Veículo Ativo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Você precisa de um veículo ativo para gerenciar manutenções corretivas.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        return
    }

    AnimatedContent(
        targetState = editingRecordId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "maintenance_screen_transition"
    ) { currentId ->
        if (currentId == null) {
            MaintenanceListSection(
                records = records,
                onAddClick = { editingRecordId = -1L },
                onEditClick = { id -> editingRecordId = id }
            )
        } else {
            val isEdit = currentId != -1L
            val recordToEdit = if (isEdit) {
                records.find { it.id == currentId } ?: MaintenanceRecord(
                    vehicleId = activeVehicle!!.id,
                    dateTimestamp = System.currentTimeMillis(),
                    dateString = "",
                    description = "",
                    location = "",
                    value = 0.0,
                    odometer = 0.0
                )
            } else {
                MaintenanceRecord(
                    vehicleId = activeVehicle!!.id,
                    dateTimestamp = System.currentTimeMillis(),
                    dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                    description = "",
                    location = "",
                    value = 0.0,
                    odometer = activeVehicle!!.currentOdometer
                )
            }

            MaintenanceFormSection(
                record = recordToEdit,
                isEdit = isEdit,
                onBack = { editingRecordId = null },
                onSave = { updatedRecord ->
                    viewModel.saveMaintenanceRecord(updatedRecord)
                    Toast.makeText(
                        context,
                        if (isEdit) "Manutenção atualizada com sucesso!" else "Manutenção registrada com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    editingRecordId = null
                },
                onDelete = {
                    viewModel.deleteMaintenanceRecord(recordToEdit)
                    Toast.makeText(context, "Manutenção excluída.", Toast.LENGTH_SHORT).show()
                    editingRecordId = null
                }
            )
        }
    }
}

@Composable
fun MaintenanceListSection(
    records: List<MaintenanceRecord>,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val totalCost = remember(records) { records.sumOf { it.value } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manutenção Corretiva",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_maintenance_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B), // Warm Amber color
                        contentColor = Color(0xFF451A03)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lançar", fontWeight = FontWeight.Bold)
                }
            }

            // Card de resumo
            Card(
                modifier = Modifier.fillMaxWidth().testTag("maintenance_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Total Gasto em Reparos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(totalCost),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma manutenção corretiva registrada.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("maintenance_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(records) { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditClick(record.id) }
                                .testTag("maintenance_item_${record.id}"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = record.description,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currencyFormat.format(record.value),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = record.dateString,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Speed,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.getDefault(), "%,.1f km", record.odometer),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (record.location.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = record.location,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceFormSection(
    record: MaintenanceRecord,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: (MaintenanceRecord) -> Unit,
    onDelete: () -> Unit
) {
    var description by remember { mutableStateOf(if (isEdit) record.description else "") }
    var location by remember { mutableStateOf(if (isEdit) record.location else "") }
    var valueStr by remember { mutableStateOf(if (isEdit) record.value.toString() else "") }
    var odometerStr by remember { mutableStateOf(if (isEdit) record.odometer.toString() else "") }
    
    var dateTimestamp by remember { mutableStateOf(record.dateTimestamp) }
    var dateString by remember { mutableStateOf(record.dateString) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEdit) "Editar Registro" else "Lançar Reparo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Descrição
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descrição (ex: Troca de pastilhas)") },
            modifier = Modifier.fillMaxWidth().testTag("maintenance_desc_input"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        // Valor
        TextField(
            value = valueStr,
            onValueChange = { valueStr = it },
            label = { Text("Valor Pago (R$)") },
            modifier = Modifier.fillMaxWidth().testTag("maintenance_value_input"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        // Odômetro
        TextField(
            value = odometerStr,
            onValueChange = { odometerStr = it },
            label = { Text("Odômetro no Reparo (km)") },
            modifier = Modifier.fillMaxWidth().testTag("maintenance_odometer_input"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        // Local / Oficina
        TextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Local / Oficina (Opcional)") },
            modifier = Modifier.fillMaxWidth().testTag("maintenance_location_input"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        // Seletor de Data
        val calendar = Calendar.getInstance().apply { timeInMillis = dateTimestamp }
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                dateTimestamp = cal.timeInMillis
                dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        TextField(
            value = dateString,
            onValueChange = {},
            readOnly = true,
            label = { Text("Data do Reparo") },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() }
                .testTag("maintenance_date_input"),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            enabled = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEdit) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).height(50.dp).testTag("delete_maintenance_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir")
                }
            }

            Button(
                onClick = {
                    val value = valueStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val odo = odometerStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                    if (description.isBlank()) {
                        Toast.makeText(context, "Por favor, digite uma descrição.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (value <= 0) {
                        Toast.makeText(context, "Por favor, insira um valor pago válido.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (odo <= 0) {
                        Toast.makeText(context, "Por favor, insira um odômetro válido.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val newRecord = MaintenanceRecord(
                        id = if (isEdit) record.id else 0L,
                        vehicleId = record.vehicleId,
                        dateTimestamp = dateTimestamp,
                        dateString = dateString,
                        description = description,
                        location = location,
                        value = value,
                        odometer = odo
                    )
                    onSave(newRecord)
                },
                modifier = Modifier.weight(1f).height(50.dp).testTag("save_maintenance_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = "Salvar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Salvar")
            }
        }
    }
}
