package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.ui.EstimationDetail
import com.example.ui.GiroCustoViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LaunchScreen(
    viewModel: GiroCustoViewModel,
    vehicle: Vehicle?,
    parts: List<VehiclePart>
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val startOdo by viewModel.startOdometer.collectAsStateWithLifecycle()
    val endOdo by viewModel.endOdometer.collectAsStateWithLifecycle()
    val gross by viewModel.grossEarnings.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveriesCount.collectAsStateWithLifecycle()
    val fuelPr by viewModel.fuelPrice.collectAsStateWithLifecycle()
    val foodExp by viewModel.foodExpense.collectAsStateWithLifecycle()
    val platformVal by viewModel.platform.collectAsStateWithLifecycle()

    val estimate by viewModel.realTimeEstimation.collectAsStateWithLifecycle(initialValue = EstimationDetail())

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            android.widget.Toast.makeText(context, snackbarMessage, android.widget.Toast.LENGTH_LONG).show()
            snackbarMessage = null
        }
    }

    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

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
                text = "Lançar Turno do Dia",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, // Dynamic color
                fontFamily = FontFamily.SansSerif
            )

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.size(36.dp).testTag("theme_toggle_btn_launch")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = "Insira as informações de hodômetro e faturamento do seu fechamento de turno.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Dynamic color
        )

        // Formulário de Entradas (Polished Charcoal Input Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)), // Dark border
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val launchDate by viewModel.launchDateTimestamp.collectAsStateWithLifecycle()
                val dateDisplayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                val dateStr = remember(launchDate) { dateDisplayFormat.format(Date(launchDate)) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = launchDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    viewModel.setLaunchDate(newCal.timeInMillis)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("launch_date_picker_btn"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Selecionar Data",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Data do Lançamento",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar Data",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                TextField(
                    value = platformVal,
                    onValueChange = { viewModel.platform.value = it },
                    label = { Text("Plataforma (ex: iFood, Uber, Rappi)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_platform"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = "Plataforma",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = startOdo,
                        onValueChange = { viewModel.startOdometer.value = it },
                        label = { Text("Hodômetro Inicial") },
                        modifier = Modifier.weight(1f).testTag("input_start_odometer"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    TextField(
                        value = endOdo,
                        onValueChange = { viewModel.endOdometer.value = it },
                        label = { Text("Hodômetro Final") },
                        modifier = Modifier.weight(1f).testTag("input_end_odometer"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = gross,
                        onValueChange = { viewModel.grossEarnings.value = it },
                        label = { Text("Ganhos Brutos (R$)") },
                        modifier = Modifier.weight(1f).testTag("input_gross_earnings"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    TextField(
                        value = deliveries,
                        onValueChange = { viewModel.deliveriesCount.value = it },
                        label = { Text("Nº Entregas") },
                        modifier = Modifier.weight(1f).testTag("input_deliveries_count"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = fuelPr,
                        onValueChange = { viewModel.fuelPrice.value = it },
                        label = { Text("Preço do Litro (R$)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    TextField(
                        value = foodExp,
                        onValueChange = { viewModel.foodExpense.value = it },
                        label = { Text("Alimentação / Outros") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }

        // ESTIMADOR EM TEMPO REAL (Painel de Alta Tecnologia em tons de Slate e Verde Esmeralda)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), // Glowing emerald border
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTIMADOR EM TEMPO REAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981), // Emerald
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF064E3B)) // Soft emerald dark background
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.GERMAN, "%.1f", estimate.distance)} KM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF34D399) // Mint/soft emerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lucro Líquido Estimado com destaque grande
                Text("Lucro Líquido Projetado:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                    text = String.format(Locale.GERMAN, "R$ %.2f", estimate.netProfit),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (estimate.netProfit >= 0) Color(0xFF10B981) else Color(0xFFF43F5E) // Emerald or Rose
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF2D2D34))
                Spacer(modifier = Modifier.height(14.dp))

                // Detalhamento de Despesas Projetadas
                Text("Detalhamento de Custos Operacionais:", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Combustível
                EstimationRow(label = "Combustível (Gasolina/Álcool)", value = estimate.fuelCost)
                // Desgaste oculto
                EstimationRow(
                    label = "Desgaste Oculto (Manutenção/Peças)", 
                    value = estimate.wearCost, 
                    tooltip = "Calculado de forma dinâmica dividindo o preço das peças pela sua vida útil"
                )
                // Custos fixos
                EstimationRow(label = "Custos Fixos Mensais Proporcionais", value = estimate.fixedCost)
                // Alimentação
                EstimationRow(label = "Alimentação / Despesas do dia", value = estimate.foodCost)

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF2D2D34))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total de Gastos Diários:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", estimate.totalExpenses),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFBBF24) // Bright Amber
                    )
                }
            }
        }

        // Botão de Salvar Lançamento (Professional Emerald Style)
        Button(
            onClick = {
                isSaving = true
                viewModel.saveDailyRecord(
                    onSuccess = {
                        isSaving = false
                        snackbarMessage = "Turno de trabalho salvo com sucesso!"
                    },
                    onError = { err ->
                        isSaving = false
                        snackbarMessage = err
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                contentColor = Color(0xFF064E3B)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving
        ) {
            Icon(Icons.Filled.Save, "Salvar", tint = Color(0xFF064E3B))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Salvar Turno do Dia",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF064E3B)
            )
        }
    }
}

@Composable
fun EstimationRow(label: String, value: Double, tooltip: String? = null) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981))) // Emerald bullet
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label, 
                    fontSize = 11.sp, 
                    color = Color(0xFF94A3B8), // slate-400
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = String.format(Locale.GERMAN, "R$ %.2f", value),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (tooltip != null) {
            Text(tooltip, fontSize = 9.sp, color = Color(0xFF64748B), modifier = Modifier.padding(start = 12.dp))
        }
    }
}
