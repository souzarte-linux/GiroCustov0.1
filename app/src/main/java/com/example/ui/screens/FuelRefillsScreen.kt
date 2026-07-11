package com.example.ui.screens

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
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FuelRefill
import com.example.ui.GiroCustoViewModel
import com.example.network.GasStationResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelRefillsScreen(viewModel: GiroCustoViewModel) {
    val refills by viewModel.fuelRefills.collectAsStateWithLifecycle()
    val realConsumption by viewModel.realAverageConsumption.collectAsStateWithLifecycle()
    val avgFuelPrice by viewModel.averageFuelPrice.collectAsStateWithLifecycle()
    val avgLiters by viewModel.averageLitersPerRefill.collectAsStateWithLifecycle()
    val activeVehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // null -> listing, -1L -> adding, value -> editing
    var editingRefillId by remember { mutableStateOf<Long?>(null) }

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
                        text = "Você precisa de um veículo ativo para cadastrar abastecimentos.",
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
        targetState = editingRefillId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "refills_screen_transition"
    ) { currentId ->
        if (currentId == null) {
            RefillsListSection(
                refills = refills,
                realConsumption = realConsumption,
                avgFuelPrice = avgFuelPrice,
                avgLiters = avgLiters,
                onAddClick = { editingRefillId = -1L },
                onEditClick = { id -> editingRefillId = id }
            )
        } else {
            val isEdit = currentId != -1L
            val refillToEdit = if (isEdit) {
                refills.find { it.id == currentId } ?: FuelRefill(
                    vehicleId = activeVehicle!!.id,
                    dateTimestamp = System.currentTimeMillis(),
                    dateString = "",
                    gasStation = "",
                    fuelType = "Gasolina Comum",
                    pricePerLiter = 0.0,
                    liters = 0.0,
                    totalPaid = 0.0,
                    odometer = 0.0,
                    isFullTank = true,
                    paymentMethod = "PIX"
                )
            } else {
                FuelRefill(
                    vehicleId = activeVehicle!!.id,
                    dateTimestamp = System.currentTimeMillis(),
                    dateString = "",
                    gasStation = "",
                    fuelType = "Gasolina Comum",
                    pricePerLiter = 0.0,
                    liters = 0.0,
                    totalPaid = 0.0,
                    odometer = 0.0,
                    isFullTank = true,
                    paymentMethod = "PIX"
                )
            }

            val existingGasStations = remember(refills) {
                refills.map { it.gasStation }.distinct().filter { it.isNotBlank() }
            }

            RefillFormSection(
                refill = refillToEdit,
                isEdit = isEdit,
                existingGasStations = existingGasStations,
                viewModel = viewModel,
                onBack = { editingRefillId = null },
                onSave = { updatedRefill ->
                    viewModel.saveFuelRefill(updatedRefill)
                    Toast.makeText(
                        context,
                        if (isEdit) "Abastecimento atualizado com sucesso!" else "Abastecimento registrado!",
                        Toast.LENGTH_SHORT
                    ).show()
                    editingRefillId = null
                },
                onDelete = {
                    viewModel.deleteFuelRefill(refillToEdit)
                    Toast.makeText(context, "Abastecimento excluído.", Toast.LENGTH_SHORT).show()
                    editingRefillId = null
                },
                onRenameGasStation = { oldName, newName ->
                    viewModel.renameGasStation(oldName, newName)
                }
            )
        }
    }
}

@Composable
fun RefillsListSection(
    refills: List<FuelRefill>,
    realConsumption: Double?,
    avgFuelPrice: Double,
    avgLiters: Double,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
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
                    text = "Abastecimentos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_refill_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF064E3B)
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
                modifier = Modifier.fillMaxWidth().testTag("fuel_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Consumo Médio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (realConsumption != null) String.format(Locale.getDefault(), "%.2f km/L", realConsumption) else "--",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Preço Médio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.getDefault(), "R$ %.2f", avgFuelPrice),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Qtd. Média", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f L", avgLiters),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (refills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum abastecimento cadastrado para este veículo.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("refills_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(refills) { refill ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditClick(refill.id) }
                                .testTag("refill_item_${refill.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(refill.gasStation, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${refill.fuelType} • ${refill.dateString}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (refill.isFullTank) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Tanque Cheio", fontSize = 11.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    } else {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Parcial", fontSize = 11.sp) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(String.format(Locale.getDefault(), "%.2f L", refill.liters), fontSize = 14.sp)
                                    Text(String.format(Locale.getDefault(), "R$ %.2f", refill.totalPaid), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Odômetro: ${refill.odometer} km", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "Pago no ${refill.paymentMethod}${if (refill.isInstallment) " (${refill.installmentsCount}x)" else ""}",
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
    var pricePerLiterStr by remember { mutableStateOf(if (isEdit) refill.pricePerLiter.toString() else "") }
    var litersStr by remember { mutableStateOf(if (isEdit) refill.liters.toString() else "") }
    var discountStr by remember { mutableStateOf(if (isEdit) refill.discount.toString() else "0.0") }
    var totalPaidStr by remember { mutableStateOf(if (isEdit) refill.totalPaid.toString() else "") }
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

    val nearbyStations by viewModel.nearbyGasStations.collectAsStateWithLifecycle()
    val isSearchingStations by viewModel.isSearchingStations.collectAsStateWithLifecycle()
    val stationSearchError by viewModel.stationSearchError.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.searchNearbyGasStations(context)
        } else {
            Toast.makeText(context, "Permissão de localização necessária para buscar postos próximos.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStationSearch()
        }
    }

    var focusedField by remember { mutableStateOf<String?>(null) }
    var showAddStationDialog by remember { mutableStateOf(false) }
    var showEditStationDialog by remember { mutableStateOf(false) }
    var stationToEdit by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var newlyAddedStations by remember { mutableStateOf(listOf<String>()) }
    val stationsList = (existingGasStations + newlyAddedStations).distinct().filter { it.isNotBlank() }

    // Smart multi-directional calculation
    LaunchedEffect(pricePerLiterStr, litersStr, discountStr, totalPaidStr, focusedField) {
        val price = pricePerLiterStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val lts = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val disc = discountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val total = totalPaidStr.replace(",", ".").toDoubleOrNull() ?: 0.0

        when (focusedField) {
            "price" -> {
                if (price > 0.0 && lts > 0.0) {
                    val autoTotal = (price * lts - disc).coerceAtLeast(0.0)
                    val autoTotalStr = String.format(Locale.US, "%.2f", autoTotal).replace(",", ".")
                    if (totalPaidStr != autoTotalStr) {
                        totalPaidStr = autoTotalStr
                    }
                } else if (price > 0.0 && total > 0.0) {
                    val autoLts = ((total + disc) / price).coerceAtLeast(0.0)
                    if (autoLts.isFinite() && autoLts > 0.0) {
                        val autoLtsStr = String.format(Locale.US, "%.2f", autoLts).replace(",", ".")
                        if (litersStr != autoLtsStr) {
                            litersStr = autoLtsStr
                        }
                    }
                }
            }
            "liters" -> {
                if (lts > 0.0 && price > 0.0) {
                    val autoTotal = (price * lts - disc).coerceAtLeast(0.0)
                    val autoTotalStr = String.format(Locale.US, "%.2f", autoTotal).replace(",", ".")
                    if (totalPaidStr != autoTotalStr) {
                        totalPaidStr = autoTotalStr
                    }
                } else if (lts > 0.0 && total > 0.0) {
                    val autoPrice = ((total + disc) / lts).coerceAtLeast(0.0)
                    if (autoPrice.isFinite() && autoPrice > 0.0) {
                        val autoPriceStr = String.format(Locale.US, "%.2f", autoPrice).replace(",", ".")
                        if (pricePerLiterStr != autoPriceStr) {
                            pricePerLiterStr = autoPriceStr
                        }
                    }
                }
            }
            "total" -> {
                if (total > 0.0 && lts > 0.0) {
                    val autoPrice = ((total + disc) / lts).coerceAtLeast(0.0)
                    if (autoPrice.isFinite() && autoPrice > 0.0) {
                        val autoPriceStr = String.format(Locale.US, "%.2f", autoPrice).replace(",", ".")
                        if (pricePerLiterStr != autoPriceStr) {
                            pricePerLiterStr = autoPriceStr
                        }
                    }
                } else if (total > 0.0 && price > 0.0) {
                    val autoLts = ((total + disc) / price).coerceAtLeast(0.0)
                    if (autoLts.isFinite() && autoLts > 0.0) {
                        val autoLtsStr = String.format(Locale.US, "%.2f", autoLts).replace(",", ".")
                        if (litersStr != autoLtsStr) {
                            litersStr = autoLtsStr
                        }
                    }
                }
            }
            "discount" -> {
                if (price > 0.0 && lts > 0.0) {
                    val autoTotal = (price * lts - disc).coerceAtLeast(0.0)
                    val autoTotalStr = String.format(Locale.US, "%.2f", autoTotal).replace(",", ".")
                    if (totalPaidStr != autoTotalStr) {
                        totalPaidStr = autoTotalStr
                    }
                }
            }
        }
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
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEdit) "Editar Abastecimento" else "Lançar Abastecimento",
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
                    label = { Text("Posto de Combustível") },
                    modifier = Modifier.fillMaxWidth().testTag("refill_gas_station"),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = "Expandir opções"
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
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Editar Posto",
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
                                    text = "Cadastrar Novo Posto?", 
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
                    contentDescription = "Adicionar Novo Posto",
                    modifier = Modifier.size(24.dp)
                )
            }

            FilledIconButton(
                onClick = {
                    val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        viewModel.searchNearbyGasStations(context)
                    } else {
                        permissionLauncher.launch(permission)
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
                        contentDescription = "Buscar posto próximo",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Nearby Gas Stations Search Results
        AnimatedVisibility(
            visible = nearbyStations.isNotEmpty() || stationSearchError != null || isSearchingStations,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Postos Próximos (GPS)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { viewModel.clearStationSearch() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Fechar busca por GPS",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSearchingStations) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Buscando postos próximos...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (stationSearchError != null) {
                        Text(
                            text = stationSearchError ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (nearbyStations.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            nearbyStations.take(5).forEach { station ->
                                val distanceText = if (station.distanceMeters >= 1000) {
                                    String.format(Locale.getDefault(), "%.1f km", station.distanceMeters / 1000.0)
                                } else {
                                    "${station.distanceMeters.toInt()} m"
                                }
                                val displayName = if (!station.brand.isNullOrBlank()) {
                                    "${station.name} (${station.brand})"
                                } else {
                                    station.name
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            gasStation = displayName
                                            viewModel.clearStationSearch()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
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
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = station.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!station.brand.isNullOrBlank()) {
                                                Text(
                                                    text = station.brand,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = distanceText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }

        if (showAddStationDialog) {
            var newStationName by remember { mutableStateOf(gasStation) }
            var selectedBrand by remember { mutableStateOf("Petrobras") }
            var brandExpanded by remember { mutableStateOf(false) }
            var locationStr by remember { mutableStateOf("") }
            val brands = listOf("Petrobras", "Ipiranga", "Shell", "ALE", "Outra")

            AlertDialog(
                onDismissRequest = { showAddStationDialog = false },
                title = {
                    Text(
                        text = "Novo Posto de Combustível",
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
                            label = { Text("Nome do Posto") },
                            modifier = Modifier.fillMaxWidth().testTag("new_station_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Column {
                            Text("Bandeira", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .clickable { brandExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 14.dp)
                                        .testTag("new_station_brand_selector"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedBrand, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            label = { Text("Localização / Cidade (Opcional)") },
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
                                gasStation = nameWithBrand
                                newlyAddedStations = newlyAddedStations + nameWithBrand
                                showAddStationDialog = false
                            }
                        },
                        modifier = Modifier.testTag("save_new_station_btn")
                    ) {
                        Text("Salvar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddStationDialog = false },
                        modifier = Modifier.testTag("cancel_new_station_btn")
                    ) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        if (showEditStationDialog) {
            var editedStationName by remember { mutableStateOf(stationToEdit) }

            AlertDialog(
                onDismissRequest = { showEditStationDialog = false },
                title = {
                    Text(
                        text = "Editar Posto de Combustível",
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
                            text = "Renomear o posto mudará o nome em todos os abastecimentos passados.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextField(
                            value = editedStationName,
                            onValueChange = { editedStationName = it },
                            label = { Text("Nome do Posto") },
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
                                val trimmedNew = editedStationName.trim()
                                onRenameGasStation(stationToEdit, trimmedNew)
                                if (gasStation == stationToEdit) {
                                    gasStation = trimmedNew
                                }
                                newlyAddedStations = newlyAddedStations.map { if (it == stationToEdit) trimmedNew else it }
                                Toast.makeText(context, "Posto renomeado com sucesso!", Toast.LENGTH_SHORT).show()
                            }
                            showEditStationDialog = false
                        },
                        modifier = Modifier.testTag("save_edit_station_btn")
                    ) {
                        Text("Salvar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEditStationDialog = false },
                        modifier = Modifier.testTag("cancel_edit_station_btn")
                    ) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                    label = { Text("Tipo de Combustível") },
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
                label = { Text("Preço / Litro") },
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
                label = { Text("Quantidade (L)") },
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Desconto
            TextField(
                value = discountStr,
                onValueChange = { discountStr = it },
                label = { Text("Desconto (R$)") },
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
                label = { Text("Total Pago (R$)") },
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
            label = { Text("Quilometragem (Odômetro)") },
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
                Text("Tanque Cheio?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = if (isFullTank) "Usado para cálculo de consumo real" else "Soma litros mas não inicia/fecha trecho",
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
            Text("Forma de Pagamento", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    Text("Pagamento Parcelado?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Marque se dividiu o valor na fatura", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    label = { Text("Quantidade de Parcelas") },
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
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir")
                }
            }

            Button(
                onClick = {
                    val price = pricePerLiterStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val lts = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val odo = odometerStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val disc = discountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val total = totalPaidStr.replace(",", ".").toDoubleOrNull() ?: (price * lts - disc)

                    if (gasStation.isBlank()) {
                        Toast.makeText(context, "Por favor, digite o Posto.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (price <= 0 || lts <= 0 || odo <= 0) {
                        Toast.makeText(context, "Preencha preço, litros e odômetro com valores válidos.", Toast.LENGTH_SHORT).show()
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
                        pricePerLiter = price,
                        liters = lts,
                        discount = disc,
                        totalPaid = total,
                        odometer = odo,
                        isFullTank = isFullTank,
                        paymentMethod = paymentMethod,
                        isInstallment = isInstallment,
                        installmentsCount = installmentsCountStr.toIntOrNull() ?: 1
                    )
                    onSave(newRefill)
                },
                modifier = Modifier.weight(1f).height(50.dp).testTag("save_refill_btn"),
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
