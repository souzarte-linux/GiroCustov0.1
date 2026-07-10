package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
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
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.data.Platform
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
    val focusManager = LocalFocusManager.current

    val startOdo by viewModel.startOdometer.collectAsStateWithLifecycle()
    val endOdo by viewModel.endOdometer.collectAsStateWithLifecycle()
    val gross by viewModel.grossEarnings.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveriesCount.collectAsStateWithLifecycle()
    val fuelPr by viewModel.fuelPrice.collectAsStateWithLifecycle()
    val foodExp by viewModel.foodExpense.collectAsStateWithLifecycle()
    val platformVal by viewModel.platform.collectAsStateWithLifecycle()
    val platformsList by viewModel.allPlatforms.collectAsStateWithLifecycle()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showAddPlatformDialog by remember { mutableStateOf(false) }

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

    val inputCardContainerColor = if (isDark) Color(0xFF1E1E22) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val inputCardBorderColor = if (isDark) Color(0xFF2D2D34) else MaterialTheme.colorScheme.outlineVariant
    val inputFieldContainerColor = if (isDark) Color(0xFF2A2A30) else Color(0xFFF1F5F9)
    val inputFieldTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val inputFieldLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val dropdownMenuBgColor = if (isDark) Color(0xFF1E1E22) else Color(0xFFF1F5F9)
    val dropdownMenuBorderColor = if (isDark) Color(0xFF2D2D34) else MaterialTheme.colorScheme.outlineVariant
    val dropdownMenuItemTextColor = if (isDark) Color.White else Color(0xFF0F172A)

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
            colors = CardDefaults.cardColors(containerColor = inputCardContainerColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, inputCardBorderColor),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TextField(
                            value = platformVal,
                            onValueChange = { 
                                viewModel.platform.value = it 
                                dropdownExpanded = true
                            },
                            label = { Text("Plataforma", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("input_platform"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = "Plataforma",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                        contentDescription = "Expandir opções"
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
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

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            properties = PopupProperties(focusable = false),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(dropdownMenuBgColor)
                                .border(1.dp, dropdownMenuBorderColor, RoundedCornerShape(8.dp))
                        ) {
                            val filteredPlatforms = if (platformVal.isBlank()) {
                                platformsList
                            } else {
                                platformsList.filter { it.name.contains(platformVal, ignoreCase = true) }
                            }

                            if (filteredPlatforms.isNotEmpty()) {
                                filteredPlatforms.forEach { plat ->
                                    DropdownMenuItem(
                                        text = { Text(plat.name, color = dropdownMenuItemTextColor) },
                                        onClick = {
                                            viewModel.platform.value = plat.name
                                            dropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("platform_option_${plat.name}")
                                    )
                                }
                            }
                            
                            val exactMatch = filteredPlatforms.any { it.name.equals(platformVal, ignoreCase = true) }
                            if (!exactMatch || platformVal.isNotBlank() || filteredPlatforms.isEmpty()) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "Cadastrar Nova Plataforma?", 
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        ) 
                                    },
                                    onClick = {
                                        dropdownExpanded = false
                                        showAddPlatformDialog = true
                                    },
                                    modifier = Modifier.testTag("platform_option_register_new")
                                )
                            }
                        }
                    }

                    FilledIconButton(
                        onClick = { showAddPlatformDialog = true },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("add_new_platform_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Cadastrar Nova Plataforma",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = startOdo,
                        onValueChange = { viewModel.startOdometer.value = it },
                        label = { Text("Hodômetro Inicial", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                        onValueChange = { viewModel.endOdometer.value = it },
                        label = { Text("Hodômetro Final", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = gross,
                        onValueChange = { viewModel.grossEarnings.value = it },
                        label = { Text("Ganhos Brutos (R$)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("input_gross_earnings"),
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
                        value = deliveries,
                        onValueChange = { viewModel.deliveriesCount.value = it },
                        label = { Text("Nº Entregas", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("input_deliveries_count"),
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

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = fuelPr,
                        onValueChange = { viewModel.fuelPrice.value = it },
                        label = { Text("Preço do Litro (R$)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
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
                        value = foodExp,
                        onValueChange = { viewModel.foodExpense.value = it },
                        label = { Text("Alimentação / Outros", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

        if (showAddPlatformDialog) {
            var newPlatformName by remember { mutableStateOf(platformVal) }
            var selectedSegment by remember { mutableStateOf("delivery") }
            var selectedPaymentModel by remember { mutableStateOf("producao") }
            var selectedCycle by remember { mutableStateOf("semanal") }
            
            var segmentExpanded by remember { mutableStateOf(false) }
            var paymentExpanded by remember { mutableStateOf(false) }
            var cycleExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddPlatformDialog = false },
                title = {
                    Text(
                        text = "Cadastrar Nova Plataforma",
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
                            label = { Text("Nome da Plataforma", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                            Text("Segmento", fontSize = 12.sp, color = Color(0xFF94A3B8))
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
                                        "delivery" -> "Delivery (Entrega)"
                                        "transporte" -> "Transporte (Passageiros)"
                                        "misto" -> "Misto"
                                        else -> "Outro"
                                    }
                                    Text(segmentLabel, color = Color.White, fontSize = 14.sp)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = segmentExpanded,
                                    onDismissRequest = { segmentExpanded = false }
                                ) {
                                    listOf(
                                        "delivery" to "Delivery (Entrega)",
                                        "transporte" to "Transporte (Passageiros)",
                                        "misto" to "Misto",
                                        "outro" to "Outro"
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
                            Text("Modelo de Pagamento", fontSize = 12.sp, color = Color(0xFF94A3B8))
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
                                        "producao" -> "Produção (Por Corrida)"
                                        "fixo" -> "Fixo (Diária/Garantido)"
                                        else -> "Misto"
                                    }
                                    Text(modelLabel, color = Color.White, fontSize = 14.sp)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = paymentExpanded,
                                    onDismissRequest = { paymentExpanded = false }
                                ) {
                                    listOf(
                                        "producao" to "Produção (Por Corrida)",
                                        "fixo" to "Fixo (Diária/Garantido)",
                                        "misto" to "Misto"
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
                            Text("Ciclo de Pagamento", fontSize = 12.sp, color = Color(0xFF94A3B8))
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
                                        "diario" -> "Diário"
                                        "semanal" -> "Semanal"
                                        "quinzenal" -> "Quinzenal"
                                        "mensal" -> "Mensal"
                                        else -> "Variável / Misto"
                                    }
                                    Text(cycleLabel, color = Color.White, fontSize = 14.sp)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = cycleExpanded,
                                    onDismissRequest = { cycleExpanded = false }
                                ) {
                                    listOf(
                                        "diario" to "Diário",
                                        "semanal" to "Semanal",
                                        "quinzenal" to "Quinzenal",
                                        "mensal" to "Mensal",
                                        "misto" to "Variável / Misto"
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
                                val newPlatform = com.example.data.Platform(
                                    name = newPlatformName.trim(),
                                    segment = selectedSegment,
                                    paymentModel = selectedPaymentModel,
                                    cycle = selectedCycle,
                                    active = true
                                )
                                viewModel.savePlatform(newPlatform)
                                viewModel.platform.value = newPlatformName.trim()
                                showAddPlatformDialog = false
                            }
                        },
                        modifier = Modifier.testTag("save_new_platform_btn")
                    ) {
                        Text("Salvar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddPlatformDialog = false },
                        modifier = Modifier.testTag("cancel_new_platform_btn")
                    ) {
                        Text("Cancelar", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF1E1E22)
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
