package com.example.ui.screens.fuel

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.FuelRefill
import com.example.network.GasStationResult
import com.example.ui.components.FuelLocationRationaleDialog
import com.example.ui.GiroCustoViewModel
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillFormSection(
    refill: FuelRefill,
    isEdit: Boolean,
    existingGasStations: List<String>,
    viewModel: GiroCustoViewModel,
    onBack: () -> Unit,
    onSave: (FuelRefill) -> Unit,
    onDelete: () -> Unit,
    onRenameGasStation: (String, String) -> Unit
) {
    var gasStation by remember { mutableStateOf(if (isEdit) refill.gasStation else "") }
    var selectedFuelType by remember { mutableStateOf(if (isEdit) refill.fuelType else "Gasolina Comum") }
    var pricePerLiterStr by remember { mutableStateOf(if (isEdit) CurrencyUtils.fuelPriceMillisToDouble(refill.pricePerLiter).toString() else "") }
    var litersStr by remember { mutableStateOf(if (isEdit) refill.liters.toString() else "") }
    var discountStr by remember { mutableStateOf(if (isEdit) CurrencyUtils.centsToDouble(refill.discount).toString() else "0.0") }
    var totalPaidStr by remember { mutableStateOf(if (isEdit) CurrencyUtils.centsToDouble(refill.totalPaid).toString() else "") }
    var odometerStr by remember { mutableStateOf(if (isEdit) refill.odometer.toString() else "") }
    var isFullTank by remember { mutableStateOf(if (isEdit) refill.isFullTank else true) }
    var paymentMethod by remember { mutableStateOf(if (isEdit) refill.paymentMethod else "PIX") }
    var isInstallment by remember { mutableStateOf(if (isEdit) refill.isInstallment else false) }
    var installmentsCountStr by remember { mutableStateOf(if (isEdit) refill.installmentsCount.toString() else "1") }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val fuelTypes = listOf("Gasolina Comum", "Gasolina Aditivada", "Etanol Comum", "Etanol Aditivado", "Diesel", "GNV")
    val paymentMethods = listOf("PIX", "Cartão", "Dinheiro")

    val activeVehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val tankCapacity = activeVehicle?.tankCapacity ?: 0.0
    val currentLiters = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val exceedsTankCapacity = viewModel.checkExceedsTankCapacity(litersStr, tankCapacity)

    var showTankCapacityConfirmDialog by remember { mutableStateOf(false) }
    var pendingRefillToSave by remember { mutableStateOf<FuelRefill?>(null) }

    val nearbyStations by viewModel.nearbyGasStations.collectAsStateWithLifecycle()
    val isSearchingStations by viewModel.isSearchingStations.collectAsStateWithLifecycle()
    val stationSearchError by viewModel.stationSearchError.collectAsStateWithLifecycle()
    val lastSearchLocation by viewModel.lastSearchLocation.collectAsStateWithLifecycle()

    var showLocationRationaleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            viewModel.searchNearbyGasStations(context)
        } else {
            Toast.makeText(context, context.getString(R.string.fuel_location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStationSearch()
        }
    }

    var selectedStation by remember { mutableStateOf<GasStationResult?>(null) }

    var focusedField by remember { mutableStateOf<String?>(null) }
    var showAddStationDialog by remember { mutableStateOf(false) }
    var showEditStationDialog by remember { mutableStateOf(false) }
    var stationToEdit by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var newlyAddedStations by remember { mutableStateOf(listOf<String>()) }
    val stationsList = (existingGasStations + newlyAddedStations).distinct().filter { it.isNotBlank() }

    // Smart multi-directional calculation (T-16 [ARQ-03] delegado ao ViewModel)
    LaunchedEffect(pricePerLiterStr, litersStr, discountStr, totalPaidStr, focusedField) {
        val calc = viewModel.calculateRefillMultiDirectional(
            pricePerLiterStr = pricePerLiterStr,
            litersStr = litersStr,
            discountStr = discountStr,
            totalPaidStr = totalPaidStr,
            focusedField = focusedField
        )
        calc.updatedPricePerLiter?.let { pricePerLiterStr = it }
        calc.updatedLiters?.let { litersStr = it }
        calc.updatedTotalPaid?.let { totalPaidStr = it }
    }

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
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(if (isEdit) R.string.refill_title_edit else R.string.refill_title_new),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Posto
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = gasStation,
                    onValueChange = { 
                        gasStation = it 
                        dropdownExpanded = true
                    },
                    label = { Text(stringResource(R.string.refill_form_gas_station_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("refill_gas_station"),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = stringResource(R.string.cd_expand_options)
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    val filteredStations = if (gasStation.isBlank()) {
                        stationsList
                    } else {
                        stationsList.filter { it.contains(gasStation, ignoreCase = true) }
                    }

                    if (filteredStations.isNotEmpty()) {
                        filteredStations.forEach { station ->
                            DropdownMenuItem(
                                text = { Text(station) },
                                onClick = {
                                    gasStation = station
                                    dropdownExpanded = false
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            stationToEdit = station
                                            showEditStationDialog = true
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.cd_edit_gas_station),
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("station_option_${station}")
                            )
                        }
                    }

                    val exactMatch = filteredStations.any { it.equals(gasStation, ignoreCase = true) }
                    if (!exactMatch || gasStation.isNotBlank() || filteredStations.isEmpty()) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = stringResource(R.string.refill_station_register_new_question), 
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            onClick = {
                                dropdownExpanded = false
                                showAddStationDialog = true
                            },
                            modifier = Modifier.testTag("station_option_register_new")
                        )
                    }
                }
            }

            FilledIconButton(
                onClick = { showAddStationDialog = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_new_station_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_new_gas_station),
                    modifier = Modifier.size(24.dp)
                )
            }

            FilledIconButton(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        viewModel.searchNearbyGasStations(context)
                    } else {
                        showLocationRationaleDialog = true
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("search_nearby_gas_stations_btn")
            ) {
                if (isSearchingStations) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.NearMe,
                        contentDescription = stringResource(R.string.cd_search_nearby_stations),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Bloco de resultados de postos próximos por GPS extraído em componente dedicado
        FuelStationSearchResultsCard(
            nearbyStations = nearbyStations,
            isSearchingStations = isSearchingStations,
            stationSearchError = stationSearchError,
            lastSearchLocation = lastSearchLocation,
            selectedStation = selectedStation,
            onStationSelected = { station, displayName ->
                selectedStation = station
                gasStation = displayName
            },
            onClose = {
                viewModel.clearStationSearch()
                selectedStation = null
            }
        )

        if (showAddStationDialog) {
            FuelAddStationDialog(
                initialGasStation = gasStation,
                onDismiss = { showAddStationDialog = false },
                onConfirm = { nameWithBrand ->
                    gasStation = nameWithBrand
                    newlyAddedStations = newlyAddedStations + nameWithBrand
                    showAddStationDialog = false
                }
            )
        }

        if (showEditStationDialog) {
            FuelEditStationDialog(
                stationToEdit = stationToEdit,
                onDismiss = { showEditStationDialog = false },
                onConfirm = { trimmedNew ->
                    onRenameGasStation(stationToEdit, trimmedNew)
                    if (gasStation == stationToEdit) {
                        gasStation = trimmedNew
                    }
                    newlyAddedStations = newlyAddedStations.map { if (it == stationToEdit) trimmedNew else it }
                    Toast.makeText(context, context.getString(R.string.refill_toast_station_renamed), Toast.LENGTH_SHORT).show()
                    showEditStationDialog = false
                }
            )
        }

        // Tipo de Combustivel Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                TextField(
                    value = selectedFuelType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.refill_form_fuel_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("refill_fuel_type"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    fuelTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedFuelType = type
                                isDropdownExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Preço por litro
            TextField(
                value = pricePerLiterStr,
                onValueChange = { pricePerLiterStr = it },
                label = { Text(stringResource(R.string.refill_form_price_per_liter_label)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("refill_price_per_liter")
                    .onFocusChanged { if (it.isFocused) focusedField = "price" },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                shape = RoundedCornerShape(12.dp)
            )

            // Litros
            TextField(
                value = litersStr,
                onValueChange = { litersStr = it },
                label = { Text(stringResource(R.string.refill_form_liters_label)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("refill_liters")
                    .onFocusChanged { if (it.isFocused) focusedField = "liters" },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (exceedsTankCapacity) {
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth().testTag("tank_capacity_alert")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.cd_fuel_tank_alert),
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.err_exceeds_tank_capacity,
                            currentLiters,
                            tankCapacity,
                            activeVehicle?.model ?: stringResource(R.string.fuel_default_vehicle_name)
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Desconto
            TextField(
                value = discountStr,
                onValueChange = { discountStr = it },
                label = { Text(stringResource(R.string.refill_form_discount_label)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("refill_discount")
                    .onFocusChanged { if (it.isFocused) focusedField = "discount" },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                shape = RoundedCornerShape(12.dp)
            )

            // Total Pago (Calculado/Editável)
            TextField(
                value = totalPaidStr,
                onValueChange = { totalPaidStr = it },
                label = { Text(stringResource(R.string.refill_form_total_paid_label)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("refill_total_paid")
                    .onFocusChanged { if (it.isFocused) focusedField = "total" },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Odômetro
        TextField(
            value = odometerStr,
            onValueChange = { odometerStr = it },
            label = { Text(stringResource(R.string.refill_form_odometer_label)) },
            modifier = Modifier.fillMaxWidth().testTag("refill_odometer"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(12.dp)
        )

        // Tanque cheio / Parcial (Switch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(R.string.refill_form_full_tank_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = stringResource(if (isFullTank) R.string.refill_form_full_tank_desc_on else R.string.refill_form_full_tank_desc_off),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isFullTank,
                onCheckedChange = { isFullTank = it },
                modifier = Modifier.testTag("refill_is_full_tank")
            )
        }

        // Forma de Pagamento
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(stringResource(R.string.refill_form_payment_method_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = paymentMethod == method
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            paymentMethod = method
                            if (method != "Cartão") {
                                isInstallment = false
                            }
                        },
                        label = { Text(method) },
                        modifier = Modifier.weight(1f).testTag("payment_method_$method")
                    )
                }
            }
        }

        // Se Cartão, Parcelado?
        if (paymentMethod == "Cartão") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.refill_form_installment_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(R.string.refill_form_installment_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isInstallment,
                    onCheckedChange = { isInstallment = it },
                    modifier = Modifier.testTag("refill_is_installment")
                )
            }

            if (isInstallment) {
                TextField(
                    value = installmentsCountStr,
                    onValueChange = { installmentsCountStr = it },
                    label = { Text(stringResource(R.string.refill_form_installments_count_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("refill_installments_count"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isEdit) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).height(50.dp).testTag("delete_refill_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_delete_confirm))
                }
            }

            Button(
                onClick = {
                    val total = viewModel.computeRefillTotal(pricePerLiterStr, litersStr, discountStr, totalPaidStr)
                    val price = pricePerLiterStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val lts = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val odo = odometerStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val disc = discountStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                    if (gasStation.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.refill_error_empty_station), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (price <= 0 || lts <= 0 || odo <= 0) {
                        Toast.makeText(context, context.getString(R.string.refill_error_invalid_inputs), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cal = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dateStr = dateFormat.format(cal.time)

                    val newRefill = FuelRefill(
                        id = if (isEdit) refill.id else 0L,
                        vehicleId = refill.vehicleId,
                        dateTimestamp = refill.dateTimestamp,
                        dateString = dateStr,
                        gasStation = gasStation,
                        fuelType = selectedFuelType,
                        pricePerLiter = CurrencyUtils.toFuelPriceMillis(price),
                        liters = lts,
                        discount = CurrencyUtils.toCents(disc),
                        totalPaid = CurrencyUtils.toCents(total),
                        odometer = odo,
                        isFullTank = isFullTank,
                        paymentMethod = paymentMethod,
                        isInstallment = isInstallment,
                        installmentsCount = installmentsCountStr.toIntOrNull() ?: 1
                    )

                    if (exceedsTankCapacity) {
                        pendingRefillToSave = newRefill
                        showTankCapacityConfirmDialog = true
                    } else {
                        onSave(newRefill)
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp).testTag("save_refill_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_save))
            }
        }

        if (showTankCapacityConfirmDialog && pendingRefillToSave != null) {
            FuelTankCapacityConfirmDialog(
                liters = pendingRefillToSave!!.liters,
                tankCapacity = tankCapacity,
                onDismiss = {
                    showTankCapacityConfirmDialog = false
                    pendingRefillToSave = null
                },
                onConfirm = {
                    val toSave = pendingRefillToSave!!
                    showTankCapacityConfirmDialog = false
                    pendingRefillToSave = null
                    onSave(toSave)
                }
            )
        }

        if (showLocationRationaleDialog) {
            FuelLocationRationaleDialog(
                onConfirm = {
                    showLocationRationaleDialog = false
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onDismiss = {
                    showLocationRationaleDialog = false
                }
            )
        }
    }
}
