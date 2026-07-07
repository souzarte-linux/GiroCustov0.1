package com.example.ui.screens

import android.widget.Toast
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
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    // Estado do veículo formulário
    var model by remember(vehicle) { mutableStateOf(vehicle?.model ?: "") }
    var consumption by remember(vehicle) { mutableStateOf(vehicle?.averageConsumption?.toString() ?: "") }
    var fuelType by remember(vehicle) { mutableStateOf(vehicle?.fuelType ?: "Gasolina") }
    var fixedCosts by remember(vehicle) { mutableStateOf(vehicle?.monthlyFixedCosts?.toString() ?: "") }
    var plannedDays by remember(vehicle) { mutableStateOf(vehicle?.plannedWorkDays?.toString() ?: "") }

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestão do Veículo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground // Dynamic color
            )

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.size(36.dp).testTag("theme_toggle_btn_vehicle")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // CARD: Cadastrar Veículo (Professional Polished Input Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)), // dark border
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Especificações do Veículo",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )

                TextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo do Veículo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = consumption,
                        onValueChange = { consumption = it },
                        label = { Text("Consumo Médio (km/L)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    // Combustível Tipo Dropdown simplificado
                    TextField(
                        value = fuelType,
                        onValueChange = { fuelType = it },
                        label = { Text("Tipo Combustível") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = fixedCosts,
                        onValueChange = { fixedCosts = it },
                        label = { Text("Custos Fixos Mensais (R$)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    TextField(
                        value = plannedDays,
                        onValueChange = { plannedDays = it },
                        label = { Text("Dias Trabalhados / Mês") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Salvar Configurações (Emerald Solid Button)
                Button(
                    onClick = {
                        val consVal = consumption.toDoubleOrNull() ?: 40.0
                        val fCostsVal = fixedCosts.toDoubleOrNull() ?: 180.0
                        val pDaysVal = plannedDays.toIntOrNull() ?: 22

                        viewModel.updateVehicle(model, consVal, fuelType, fCostsVal, pDaysVal)
                        android.widget.Toast.makeText(context, "Configurações do veículo atualizadas!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
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

        // SEÇÃO: Peças de Desgaste Oculto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONITORAMENTO DE DESGASTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981), // Emerald
                letterSpacing = 1.sp
            )
            
            IconButton(onClick = { showAddPartDialog = true }) {
                Icon(Icons.Filled.Add, "Adicionar Peça", tint = Color(0xFF10B981))
            }
        }

        // Listagem de Peças com Barras de Progresso Coloridas
        if (parts.isNotEmpty()) {
            parts.forEach { part ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // Dynamic border
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // Slate-400
                                )
                            }

                            // Botão rápido de reset, edição e exclusão
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
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Build, "Zerar Desgaste", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.deletePart(part) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, "Remover Peça", tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp)) // Rose/Red
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Barra de Progresso Colorida de Saúde
                        val health = part.healthPercentage
                        val barColor = when {
                            health <= 15.0 -> Color(0xFFF43F5E) // Vermelho crítico
                            health <= 35.0 -> Color(0xFFF59E0B) // Laranja atenção (Amber)
                            else -> Color(0xFF10B981) // Emerald
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.GERMAN, "Uso: %.0f km rodados", part.runKmSinceChange),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // slate-400
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
                            trackColor = MaterialTheme.colorScheme.outline // Dynamic track
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // Dynamic border
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

        if (showAddPartDialog) {
            AddPartDialog(
                onDismiss = { showAddPartDialog = false },
                onAdd = { name, price, lifespan ->
                    viewModel.addPart(name, price, lifespan)
                    showAddPartDialog = false
                    android.widget.Toast.makeText(context, "Nova peça monitorada cadastrada!", android.widget.Toast.LENGTH_SHORT).show()
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
                    android.widget.Toast.makeText(context, "Peça de desgaste atualizada!", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Dynamic background
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline) // Dynamic border
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
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) // Dynamic color
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1.000
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Dynamic background
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline) // Dynamic border
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
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) // Dynamic color
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1.000
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
