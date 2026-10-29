package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Platform
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.ui.EstimationDetail
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.launch.*
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
    val distanceState by viewModel.launchCalculatedDistance.collectAsStateWithLifecycle()
    val gross by viewModel.grossEarnings.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveriesCount.collectAsStateWithLifecycle()
    val fuelPr by viewModel.fuelPrice.collectAsStateWithLifecycle()
    val foodExp by viewModel.foodExpense.collectAsStateWithLifecycle()
    val startTimeVal by viewModel.startTime.collectAsStateWithLifecycle()
    val endTimeVal by viewModel.endTime.collectAsStateWithLifecycle()
    val pauseDurationVal by viewModel.pauseDuration.collectAsStateWithLifecycle()
    val platformVal by viewModel.platform.collectAsStateWithLifecycle()
    val platformsList by viewModel.allPlatforms.collectAsStateWithLifecycle()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showAddPlatformDialog by remember { mutableStateOf(false) }

    val estimate by viewModel.realTimeEstimation.collectAsStateWithLifecycle(initialValue = EstimationDetail())
    val fuelEstimationInfo by viewModel.fuelCostEstimationInfo.collectAsStateWithLifecycle()

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
                text = stringResource(R.string.launch_screen_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.SansSerif
            )

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier.size(36.dp).testTag("theme_toggle_btn_launch")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = stringResource(R.string.cd_theme_toggle),
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.launch_screen_subtitle),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            DatePickerDialog(
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
                            contentDescription = stringResource(R.string.cd_select_date),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.launch_date_label),
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
                        contentDescription = stringResource(R.string.cd_edit_date),
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
                            label = { Text(if (platformVal.isBlank()) stringResource(R.string.launch_select_platform) else stringResource(R.string.launch_platform_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text(stringResource(R.string.launch_select_platform)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("input_platform"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = stringResource(R.string.cd_platform),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                        contentDescription = stringResource(R.string.cd_expand_platform_options)
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
                                            text = stringResource(R.string.launch_register_new_platform_question), 
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
                            contentDescription = stringResource(R.string.cd_register_new_platform),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Seção Modular de Odômetro e Distância Calculada
                LaunchOdometerSection(
                    startOdo = startOdo,
                    onStartOdoChange = { viewModel.startOdometer.value = it },
                    endOdo = endOdo,
                    onEndOdoChange = { viewModel.endOdometer.value = it },
                    distanceState = distanceState,
                    isDark = isDark,
                    focusManager = focusManager,
                    inputFieldContainerColor = inputFieldContainerColor,
                    inputFieldTextColor = inputFieldTextColor,
                    inputFieldLabelColor = inputFieldLabelColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = gross,
                        onValueChange = { viewModel.grossEarnings.value = it },
                        label = { Text(stringResource(R.string.launch_gross_earnings), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                        label = { Text(stringResource(R.string.launch_deliveries_count), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                        label = { Text(stringResource(R.string.launch_fuel_price), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                        label = { Text(stringResource(R.string.launch_food_expense), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Seção Modular: Horários do Turno e Pausas
                LaunchShiftHoursSection(
                    context = context,
                    focusManager = focusManager,
                    startTimeVal = startTimeVal,
                    onStartTimeChange = { viewModel.startTime.value = it },
                    endTimeVal = endTimeVal,
                    onEndTimeChange = { viewModel.endTime.value = it },
                    pauseDurationVal = pauseDurationVal,
                    onPauseDurationChange = { viewModel.pauseDuration.value = it },
                    inputFieldContainerColor = inputFieldContainerColor,
                    inputFieldTextColor = inputFieldTextColor,
                    inputFieldLabelColor = inputFieldLabelColor
                )
            }
        }

        // Subcomponente Modular: Estimativa de Combustível (Consumo Real vs Teórico)
        LaunchFuelEstimationCard(
            isDark = isDark,
            fuelEstimationInfo = fuelEstimationInfo
        )

        // Subcomponente Modular: Estimador em Tempo Real (Lucro Líquido e Custos Operacionais)
        LaunchRealTimeEstimationCard(
            estimate = estimate,
            fuelEstimationInfo = fuelEstimationInfo
        )

        // Botão de Salvar Lançamento (Professional Emerald Style)
        Button(
            onClick = {
                isSaving = true
                viewModel.saveDailyRecord(
                    onSuccess = {
                        isSaving = false
                        snackbarMessage = context.getString(R.string.launch_save_success)
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
            Icon(Icons.Filled.Save, stringResource(R.string.cd_save), tint = Color(0xFF064E3B))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.launch_button_save),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF064E3B)
            )
        }

        if (showAddPlatformDialog) {
            LaunchNewPlatformDialog(
                initialPlatformName = platformVal,
                onDismiss = { showAddPlatformDialog = false },
                onSavePlatform = { newPlatform ->
                    viewModel.savePlatform(newPlatform)
                    viewModel.platform.value = newPlatform.name
                    showAddPlatformDialog = false
                }
            )
        }
    }
}
