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
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.EstimationDetail
import com.example.ui.FuelCostEstimationInfo
import com.example.ui.GiroCustoViewModel
import com.example.ui.LaunchDistanceState
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
                color = MaterialTheme.colorScheme.onBackground, // Dynamic color
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

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = startOdo,
                        onValueChange = { viewModel.startOdometer.value = it },
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
                        onValueChange = { viewModel.endOdometer.value = it },
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

                // Exibição Dinâmica de Distância Calculada (KM rodados no dia) (T-16 [ARQ-03])
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
                        // Sem exibição quando hodômetros não preenchidos
                    }
                }

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

                // Seção: Horários do Turno e Pausas
                Text(
                    text = stringResource(R.string.launch_shift_hours_section),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Hora Início
                    TextField(
                        value = startTimeVal,
                        onValueChange = { viewModel.startTime.value = it },
                        label = { Text(stringResource(R.string.launch_start_time), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("08:00") },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("input_start_time"),
                        trailingIcon = {
                            IconButton(onClick = {
                                val sParts = startTimeVal.split(":")
                                val initialH = sParts.getOrNull(0)?.toIntOrNull() ?: 8
                                val initialM = sParts.getOrNull(1)?.toIntOrNull() ?: 0
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        viewModel.startTime.value = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                    },
                                    initialH, initialM, true
                                ).show()
                            }) {
                                Icon(Icons.Filled.Schedule, contentDescription = stringResource(R.string.cd_launch_select_start_time), tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
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

                    // Hora Final
                    TextField(
                        value = endTimeVal,
                        onValueChange = { viewModel.endTime.value = it },
                        label = { Text(stringResource(R.string.launch_end_time), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("18:00") },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("input_end_time"),
                        trailingIcon = {
                            IconButton(onClick = {
                                val eParts = endTimeVal.split(":")
                                val initialH = eParts.getOrNull(0)?.toIntOrNull() ?: 18
                                val initialM = eParts.getOrNull(1)?.toIntOrNull() ?: 0
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        viewModel.endTime.value = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                    },
                                    initialH, initialM, true
                                ).show()
                            }) {
                                Icon(Icons.Filled.Schedule, contentDescription = stringResource(R.string.cd_launch_select_end_time), tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
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

                // Total Tempo Pause
                TextField(
                    value = pauseDurationVal,
                    onValueChange = { viewModel.pauseDuration.value = it },
                    label = { Text(stringResource(R.string.launch_pause_duration), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("01:00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("input_pause_duration"),
                    trailingIcon = {
                        IconButton(onClick = {
                            val pParts = pauseDurationVal.split(":")
                            val initialH = pParts.getOrNull(0)?.toIntOrNull() ?: 1
                            val initialM = pParts.getOrNull(1)?.toIntOrNull() ?: 0
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    viewModel.pauseDuration.value = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                },
                                initialH, initialM, true
                            ).show()
                        }) {
                            Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.cd_launch_select_pause_time), tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
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

                // Resumo Dinâmico da Jornada
                val shiftSummary = remember(startTimeVal, endTimeVal, pauseDurationVal) {
                    if (startTimeVal.contains(":") && endTimeVal.contains(":")) {
                        val sParts = startTimeVal.split(":")
                        val eParts = endTimeVal.split(":")
                        val sH = sParts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                        val sM = sParts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                        val eH = eParts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                        val eM = eParts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                        val sTotal = sH * 60L + sM
                        var eTotal = eH * 60L + eM
                        if (eTotal < sTotal) eTotal += 24L * 60L
                        val grossMin = eTotal - sTotal

                        var pauseMin = 0L
                        val pClean = pauseDurationVal.trim()
                        if (pClean.contains(":")) {
                            val pParts = pClean.split(":")
                            pauseMin = (pParts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L) * 60L + (pParts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L)
                        } else if (pClean.isNotBlank()) {
                            pauseMin = pClean.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
                        }
                        val netMin = (grossMin - pauseMin).coerceAtLeast(0L)
                        val netH = netMin / 60
                        val netM = netMin % 60
                        context.getString(
                            R.string.launch_shift_summary,
                            netH,
                            if (netM > 0) "${netM}m" else "00m",
                            grossMin / 60,
                            grossMin % 60,
                            pauseMin / 60,
                            pauseMin % 60
                        )
                    } else {
                        null
                    }
                }

                if (shiftSummary != null) {
                    Surface(
                        color = Color(0xFF064E3B).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                            Text(text = shiftSummary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF34D399))
                        }
                    }
                }
            }
        }

        // Card Informativo de Estimativa de Combustível (Consumo Real vs Teórico Cadastrado)
        val fuelEstimationInfo by viewModel.fuelCostEstimationInfo.collectAsStateWithLifecycle()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fuel_cost_estimation_info_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E1E22) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(
                1.dp,
                if (fuelEstimationInfo.isUsingRealConsumption) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF3B82F6).copy(alpha = 0.4f)
            ),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalGasStation,
                            contentDescription = null,
                            tint = if (fuelEstimationInfo.isUsingRealConsumption) Color(0xFF10B981) else Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.launch_fuel_estimation_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = if (fuelEstimationInfo.isUsingRealConsumption) Color(0xFF064E3B) else Color(0xFF1E3A8A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (fuelEstimationInfo.isUsingRealConsumption) Color(0xFF10B981) else Color(0xFF60A5FA)
                        ),
                        modifier = Modifier.testTag("consumption_basis_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (fuelEstimationInfo.isUsingRealConsumption) Icons.Filled.CheckCircle else Icons.Filled.Info,
                                contentDescription = null,
                                tint = if (fuelEstimationInfo.isUsingRealConsumption) Color(0xFF34D399) else Color(0xFF93C5FD),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (fuelEstimationInfo.isUsingRealConsumption) {
                                    stringResource(R.string.launch_fuel_real_consumption, fuelEstimationInfo.effectiveConsumption)
                                } else {
                                    stringResource(R.string.launch_fuel_registered_consumption, fuelEstimationInfo.effectiveConsumption)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fuelEstimationInfo.isUsingRealConsumption) Color(0xFF34D399) else Color(0xFF93C5FD)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(
                        R.string.launch_fuel_estimation_message,
                        fuelEstimationInfo.distanceKm,
                        fuelEstimationInfo.vehicleModel.ifBlank { stringResource(R.string.launch_active_vehicle) }
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("fuel_cost_estimation_message")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (fuelEstimationInfo.isUsingRealConsumption) stringResource(R.string.launch_fuel_basis_real) else stringResource(R.string.launch_fuel_basis_registered),
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "R$ %.2f", fuelEstimationInfo.estimatedFuelCost),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (fuelEstimationInfo.isUsingRealConsumption) Color(0xFF10B981) else Color(0xFFFBBF24),
                        modifier = Modifier.testTag("fuel_cost_estimation_value")
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
                        text = stringResource(R.string.launch_realtime_estimator_title),
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
                Text(stringResource(R.string.launch_projected_net_profit), fontSize = 11.sp, color = Color(0xFF94A3B8))
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
                Text(stringResource(R.string.launch_operational_cost_details), fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Combustível
                EstimationRow(
                    label = stringResource(R.string.launch_cost_fuel),
                    value = estimate.fuelCost,
                    tooltip = if (fuelEstimationInfo.isUsingRealConsumption) {
                        stringResource(R.string.launch_cost_fuel_tooltip_real, fuelEstimationInfo.effectiveConsumption)
                    } else {
                        stringResource(R.string.launch_cost_fuel_tooltip_registered, fuelEstimationInfo.effectiveConsumption)
                    }
                )
                // Desgaste oculto
                EstimationRow(
                    label = stringResource(R.string.launch_cost_wear), 
                    value = estimate.wearCost, 
                    tooltip = stringResource(R.string.launch_cost_wear_tooltip)
                )
                // Custos fixos
                EstimationRow(label = stringResource(R.string.launch_cost_fixed), value = estimate.fixedCost)
                // Alimentação
                EstimationRow(label = stringResource(R.string.launch_cost_food), value = estimate.foodCost)

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF2D2D34))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.launch_total_daily_expenses), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.btn_save), color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddPlatformDialog = false },
                        modifier = Modifier.testTag("cancel_new_platform_btn")
                    ) {
                        Text(stringResource(R.string.btn_cancel), color = Color(0xFF94A3B8))
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
