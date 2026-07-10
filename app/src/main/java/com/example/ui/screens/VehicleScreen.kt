package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.ui.GiroCustoViewModel
import java.util.*

@Composable
fun VehicleScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>
) {
    val allVehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    var editingVehicleId by remember { mutableStateOf<Long?>(null) }

    AnimatedContent(
        targetState = editingVehicleId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "vehicle_screen_transition"
    ) { currentId ->
        if (currentId == null) {
            VehicleListAndPartsSection(
                allVehicles = allVehicles,
                parts = parts,
                viewModel = viewModel,
                isDark = isDark,
                onAddClick = { editingVehicleId = -1L },
                onEditClick = { id -> editingVehicleId = id }
            )
        } else {
            val isEdit = currentId != -1L
            val vehicleToEdit = if (isEdit) {
                allVehicles.find { it.id == currentId } ?: Vehicle()
            } else {
                Vehicle()
            }

            VehicleFormSection(
                vehicle = vehicleToEdit,
                isEdit = isEdit,
                onBack = { editingVehicleId = null },
                onSave = { updatedVehicle ->
                    viewModel.updateVehicle(updatedVehicle)
                    Toast.makeText(
                        context,
                        if (isEdit) "Veículo atualizado com sucesso!" else "Veículo cadastrado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    editingVehicleId = null
                },
                onDelete = {
                    viewModel.deleteVehicle(vehicleToEdit)
                    Toast.makeText(context, "Veículo excluído com sucesso.", Toast.LENGTH_SHORT).show()
                    editingVehicleId = null
                }
            )
        }
    }
}

@Composable
fun VehicleListAndPartsSection(
    allVehicles: List<Vehicle>,
    parts: List<VehiclePart>,
    viewModel: GiroCustoViewModel,
    isDark: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showAddPartDialog by remember { mutableStateOf(false) }
    var showEditPartDialog by remember { mutableStateOf(false) }
    var partToEdit by remember { mutableStateOf<VehiclePart?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestão de Veículos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleTheme() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("theme_toggle_btn_vehicle")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Alternar Tema",
                        tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_vehicle_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF064E3B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar", tint = Color(0xFF064E3B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Novo", fontWeight = FontWeight.Bold)
                }
            }
        }

        // VEHICLES LIST
        if (allVehicles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2D34))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TwoWheeler,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Nenhum veículo cadastrado",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Cadastre um veículo para poder calcular custos de combustível e desgaste.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                allVehicles.forEach { veh ->
                    val isActive = veh.active
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditClick(veh.id) }
                            .testTag("vehicle_item_${veh.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) Color(0xFF132D21) else Color(0xFF1E1E22)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isActive) Color(0xFF10B981) else Color(0xFF2D2D34)
                        )
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (veh.fuelType.lowercase().contains("flex") || veh.fuelType.lowercase().contains("álcool")) Icons.Filled.LocalGasStation else Icons.Filled.TwoWheeler,
                                        contentDescription = null,
                                        tint = if (isActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = veh.model,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isActive) {
                                        Badge(
                                            containerColor = Color(0xFF10B981)
                                        ) {
                                            Text(
                                                text = "ATIVO",
                                                color = Color(0xFF064E3B),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.setActiveVehicle(veh.id)
                                                Toast.makeText(context, "${veh.model} ativado!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2D2D34),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp).testTag("activate_vehicle_btn_${veh.id}")
                                        ) {
                                            Text("Ativar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    IconButton(
                                        onClick = { onEditClick(veh.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Editar veículo",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Consumo: ${veh.averageConsumption} km/L • ${veh.fuelType}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = String.format(Locale.GERMAN, "Custos Fixos: R$ %.2f • Dias: %d", veh.monthlyFixedCosts, veh.plannedWorkDays),
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }

                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Ativo",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color(0xFF2D2D34))
        Spacer(modifier = Modifier.height(4.dp))

        // WEAR MONITORING SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONITORAMENTO DE DESGASTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981),
                letterSpacing = 1.sp
            )
            
            IconButton(
                onClick = { showAddPartDialog = true },
                modifier = Modifier.testTag("add_part_icon_btn")
            ) {
                Icon(Icons.Filled.Add, "Adicionar Peça", tint = Color(0xFF10B981))
            }
        }

        if (parts.isNotEmpty()) {
            parts.forEach { part ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(part.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text(
                                    text = String.format(Locale.GERMAN, "Troca: R$ %.2f | Vida útil: %d km", part.price, part.lifespanKm.toInt()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        partToEdit = part
                                        showEditPartDialog = true
                                    },
                                    modifier = Modifier.size(28.dp).testTag("edit_part_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, "Editar Peça", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.resetPartWear(part.id) },
                                    modifier = Modifier.size(28.dp).testTag("reset_part_wear_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Build, "Zerar Desgaste", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.deletePart(part) },
                                    modifier = Modifier.size(28.dp).testTag("delete_part_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Delete, "Remover Peça", tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val health = part.healthPercentage
                        val barColor = when {
                            health <= 15.0 -> Color(0xFFF43F5E)
                            health <= 35.0 -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.GERMAN, "Uso: %.0f km rodados", part.runKmSinceChange),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.GERMAN, "Saúde: %.0f%%", health),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { (health / 100.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma peça ou componente monitorado.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }

    if (showAddPartDialog) {
        AddPartDialog(
            onDismiss = { showAddPartDialog = false },
            onAdd = { name, price, lifespan ->
                viewModel.addPart(name, price, lifespan)
                showAddPartDialog = false
                Toast.makeText(context, "Nova peça monitorada cadastrada!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEditPartDialog && partToEdit != null) {
        EditPartDialog(
            part = partToEdit!!,
            onDismiss = {
                showEditPartDialog = false
                partToEdit = null
            },
            onSave = { name, price, lifespan, runKm ->
                viewModel.updatePart(partToEdit!!.id, name, price, lifespan, runKm)
                showEditPartDialog = false
                partToEdit = null
                Toast.makeText(context, "Peça de desgaste atualizada!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun VehicleFormSection(
    vehicle: Vehicle,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: (Vehicle) -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()

    var model by remember { mutableStateOf(vehicle.model) }
    var consumption by remember { mutableStateOf(if (vehicle.id == 0L) "" else vehicle.averageConsumption.toString()) }
    var fuelType by remember { mutableStateOf(vehicle.fuelType) }
    var fixedCosts by remember { mutableStateOf(if (vehicle.id == 0L) "" else vehicle.monthlyFixedCosts.toString()) }
    var plannedDays by remember { mutableStateOf(if (vehicle.id == 0L) "" else vehicle.plannedWorkDays.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("vehicle_form_back_btn")
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text(
                    text = if (isEdit) "Editar Veículo" else "Novo Veículo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (isEdit) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .size(36.dp)
                        .testTag("delete_vehicle_btn")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444))
                }
            }
        }

        // FORM FIELDS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Modelo do Veículo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    TextField(
                        value = model,
                        onValueChange = { model = it },
                        placeholder = { Text("Ex: Honda CG 160 Titan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vehicle_model_input"),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Consumo Médio (km/L)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        TextField(
                            value = consumption,
                            onValueChange = { consumption = it },
                            placeholder = { Text("Ex: 40.0") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vehicle_consumption_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Tipo Combustível", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        TextField(
                            value = fuelType,
                            onValueChange = { fuelType = it },
                            placeholder = { Text("Ex: Gasolina, Flex") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vehicle_fueltype_input"),
                            singleLine = true
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Custos Fixos Mensais (R$)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        TextField(
                            value = fixedCosts,
                            onValueChange = { fixedCosts = it },
                            placeholder = { Text("Ex: 180.0") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vehicle_fixedcosts_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Dias Trabalhados / Mês", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        TextField(
                            value = plannedDays,
                            onValueChange = { plannedDays = it },
                            placeholder = { Text("Ex: 22") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vehicle_planneddays_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val consVal = consumption.toDoubleOrNull() ?: 40.0
                        val fCostsVal = fixedCosts.toDoubleOrNull() ?: 180.0
                        val pDaysVal = plannedDays.toIntOrNull() ?: 22

                        if (model.isNotBlank()) {
                            val updated = vehicle.copy(
                                model = model.trim(),
                                averageConsumption = consVal,
                                fuelType = fuelType.trim(),
                                monthlyFixedCosts = fCostsVal,
                                plannedWorkDays = pDaysVal
                            )
                            onSave(updated)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vehicle_save_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF064E3B)
                    )
                ) {
                    Icon(Icons.Filled.Save, "Salvar Veículo", tint = Color(0xFF064E3B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar Especificações", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                }
            }
        }
    }
}

@Composable
fun AddPartDialog(
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
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Adicionar Peça de Desgaste", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Peça (ex: Pneu Traseiro)") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço de Troca (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = lifespan,
                    onValueChange = { lifespan = it },
                    label = { Text("Durabilidade Estimada (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1000.0
                            if (name.isNotBlank() && p > 0.0) {
                                onAdd(name, p, l)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Adicionar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPartDialog(
    part: VehiclePart,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(part.name) }
    var price by remember { mutableStateOf(part.price.toString()) }
    var lifespan by remember { mutableStateOf(part.lifespanKm.toString()) }
    var runKm by remember { mutableStateOf(part.runKmSinceChange.toString()) }

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
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Editar Peça de Desgaste", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Peça") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço de Troca (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = lifespan,
                    onValueChange = { lifespan = it },
                    label = { Text("Durabilidade Estimada (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = runKm,
                    onValueChange = { runKm = it },
                    label = { Text("Km Rodados Desde a Troca") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1000.0
                            val r = runKm.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && p > 0.0) {
                                onSave(name, p, l, r)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
