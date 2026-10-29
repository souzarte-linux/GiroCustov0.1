package com.example.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Vehicle
import com.example.network.fipe.FipeItemDto
import com.example.ui.GiroCustoViewModel
import com.example.util.CurrencyUtils

@Composable
internal fun VehicleFormSection(
    vehicle: Vehicle,
    isEdit: Boolean,
    viewModel: GiroCustoViewModel,
    onBack: () -> Unit,
    onSave: (Vehicle) -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()

    var selectedVehicleType by remember { mutableStateOf(vehicle.vehicleType.ifBlank { "MOTO" }) }
    var isManualInput by remember { mutableStateOf(vehicle.brand.isBlank() && vehicle.id != 0L) }

    // FIPE Estados
    val fipeMarcas by viewModel.fipeMarcas.collectAsStateWithLifecycle()
    val fipeModelos by viewModel.fipeModelos.collectAsStateWithLifecycle()
    val fipeAnos by viewModel.fipeAnos.collectAsStateWithLifecycle()
    val fipeDetail by viewModel.fipeDetail.collectAsStateWithLifecycle()
    val fipeUiState by viewModel.fipeUiState.collectAsStateWithLifecycle()

    var selectedBrand by remember { mutableStateOf<FipeItemDto?>(null) }
    var selectedModel by remember { mutableStateOf<FipeItemDto?>(null) }
    var selectedYear by remember { mutableStateOf<FipeItemDto?>(null) }

    // Campos Manuais
    var manualBrand by remember { mutableStateOf(vehicle.brand) }
    var manualModel by remember { mutableStateOf(vehicle.model) }
    var manualYear by remember { mutableStateOf(vehicle.manufacturingYear) }

    // Campos Técnicos
    var consumption by remember { mutableStateOf(if (vehicle.id == 0L) (if (selectedVehicleType == "MOTO") "40.0" else "12.0") else vehicle.averageConsumption.toString()) }
    var fuelType by remember { mutableStateOf(vehicle.fuelType.ifBlank { "Gasolina" }) }
    var tankCapacity by remember { mutableStateOf(if (vehicle.id == 0L) (if (selectedVehicleType == "MOTO") "16.0" else "50.0") else vehicle.tankCapacity.toString()) }
    var fixedCosts by remember { mutableStateOf(if (vehicle.id == 0L) "180.0" else CurrencyUtils.centsToDouble(vehicle.monthlyFixedCosts).toString()) }
    var plannedDays by remember { mutableStateOf(if (vehicle.id == 0L) "22" else vehicle.plannedWorkDays.toString()) }
    var currentOdometer by remember { mutableStateOf(if (vehicle.id == 0L) "0.0" else vehicle.currentOdometer.toString()) }

    // Condicionais Moto
    var hasTopBox by remember { mutableStateOf(vehicle.hasTopBox) }
    var hasDeliveryBag by remember { mutableStateOf(vehicle.hasDeliveryBag) }

    // Condicionais Carro
    var carBodyType by remember { mutableStateOf(vehicle.carBodyType.ifBlank { "Hatch" }) }

    // Carrega marcas FIPE automaticamente ao iniciar ou alternar tipo
    LaunchedEffect(selectedVehicleType, isManualInput) {
        if (!isManualInput && fipeMarcas.isEmpty()) {
            viewModel.loadFipeMarcas(selectedVehicleType)
        }
    }

    // Se carregar o detalhe FIPE, sugere tipo de combustível e valor
    LaunchedEffect(fipeDetail) {
        if (fipeDetail != null) {
            val fipeFuel = fipeDetail?.combustivel.orEmpty()
            if (fipeFuel.isNotBlank()) {
                fuelType = fipeFuel
            }
        }
    }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("vehicle_form_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = ColorTextPrimary
                    )
                }

                Text(
                    text = if (isEdit) "Editar Veículo" else "Novo Veículo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
            }

            if (isEdit) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorRose.copy(alpha = 0.15f))
                        .testTag("delete_vehicle_btn")
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.cd_delete_vehicle),
                        tint = ColorRose
                    )
                }
            }
        }

        // COMPONENTE 1: SELETOR SEGMENTADO MOTO / CARRO
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ColorSurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Aba Moto
                val isMotoSelected = selectedVehicleType.equals("MOTO", ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (!isMotoSelected) {
                                selectedVehicleType = "MOTO"
                                selectedBrand = null
                                selectedModel = null
                                selectedYear = null
                                if (!isManualInput) viewModel.loadFipeMarcas("MOTO")
                                if (vehicle.id == 0L) {
                                    consumption = "40.0"
                                    tankCapacity = "16.0"
                                }
                            }
                        }
                        .testTag("vehicle_form_type_moto"),
                    color = if (isMotoSelected) ColorBrandEmerald else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TwoWheeler,
                            contentDescription = null,
                            tint = if (isMotoSelected) ColorEmeraldDark else ColorTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Moto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isMotoSelected) ColorEmeraldDark else ColorTextMuted
                        )
                    }
                }

                // Aba Carro
                val isCarSelected = selectedVehicleType.equals("CARRO", ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (!isCarSelected) {
                                selectedVehicleType = "CARRO"
                                selectedBrand = null
                                selectedModel = null
                                selectedYear = null
                                if (!isManualInput) viewModel.loadFipeMarcas("CARRO")
                                if (vehicle.id == 0L) {
                                    consumption = "12.0"
                                    tankCapacity = "50.0"
                                }
                            }
                        }
                        .testTag("vehicle_form_type_car"),
                    color = if (isCarSelected) ColorBrandEmerald else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = if (isCarSelected) ColorEmeraldDark else ColorTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Carro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isCarSelected) ColorEmeraldDark else ColorTextMuted
                        )
                    }
                }
            }
        }

        // COMPONENTE 2: CATÁLOGO FIPE vs DIGITAÇÃO MANUAL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Switch de Entrada Manual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Digitar Manualmente",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTextPrimary
                        )
                        Text(
                            text = "Não encontrei meu veículo no catálogo FIPE",
                            fontSize = 11.sp,
                            color = ColorTextMuted
                        )
                    }

                    Switch(
                        checked = isManualInput,
                        onCheckedChange = { isManualInput = it },
                        modifier = Modifier.testTag("vehicle_manual_input_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ColorBrandEmerald,
                            uncheckedThumbColor = ColorTextMuted,
                            uncheckedTrackColor = ColorSurfaceInput
                        )
                    )
                }

                HorizontalDivider(color = ColorBorder)

                if (!isManualInput) {
                    // MODO FIPE (Assistido)
                    ProfileVehicleFipeSelector(
                        selectedBrand = selectedBrand,
                        selectedModel = selectedModel,
                        selectedYear = selectedYear,
                        manualBrand = manualBrand,
                        manualModel = manualModel,
                        manualYear = manualYear,
                        fipeMarcas = fipeMarcas,
                        fipeModelos = fipeModelos,
                        fipeAnos = fipeAnos,
                        fipeDetail = fipeDetail,
                        fipeUiState = fipeUiState,
                        onSelectBrand = { marca ->
                            selectedBrand = marca
                            manualBrand = marca.nome
                            selectedModel = null
                            manualModel = ""
                            selectedYear = null
                            manualYear = ""
                            viewModel.loadFipeModelos(selectedVehicleType, marca.codigo)
                        },
                        onSelectModel = { modelo ->
                            selectedModel = modelo
                            manualModel = modelo.nome
                            selectedYear = null
                            manualYear = ""
                            if (selectedBrand != null) {
                                viewModel.loadFipeAnos(selectedVehicleType, selectedBrand!!.codigo, modelo.codigo)
                            }
                        },
                        onSelectYear = { ano ->
                            selectedYear = ano
                            manualYear = ano.nome
                            if (selectedBrand != null && selectedModel != null) {
                                viewModel.loadFipeDetail(
                                    selectedVehicleType,
                                    selectedBrand!!.codigo,
                                    selectedModel!!.codigo,
                                    ano.codigo
                                )
                            }
                        }
                    )
                } else {
                    // MODO MANUAL (Digitação Livre)
                    Text(
                        text = "Especificação Manual",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorAmber,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = manualBrand,
                        onValueChange = { manualBrand = it },
                        label = { Text("Marca") },
                        placeholder = { Text("Ex: Shineray / Sundown / Dafra", color = ColorTextMuted) },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vehicle_brand_manual_input")
                    )

                    OutlinedTextField(
                        value = manualModel,
                        onValueChange = { manualModel = it },
                        label = { Text("Modelo") },
                        placeholder = { Text("Ex: Worker 125 Cross", color = ColorTextMuted) },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vehicle_model_manual_input")
                    )

                    OutlinedTextField(
                        value = manualYear,
                        onValueChange = { manualYear = it },
                        label = { Text("Ano de Fabricação") },
                        placeholder = { Text("Ex: 2023", color = ColorTextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vehicle_year_manual_input")
                    )
                }
            }
        }

        // COMPONENTE 3: CAMPOS TÉCNICOS OBRIGATÓRIOS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "PARÂMETROS TÉCNICOS & CUSTOS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorBrandEmerald,
                    letterSpacing = 1.sp
                )

                // Linha: Consumo Médio + Tipo de Combustível
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = consumption,
                        onValueChange = { consumption = it },
                        label = { Text("Consumo (km/L)") },
                        placeholder = { Text("40.0", color = ColorTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_consumption_input")
                    )

                    OutlinedTextField(
                        value = fuelType,
                        onValueChange = { fuelType = it },
                        label = { Text("Combustível") },
                        placeholder = { Text("Gasolina, Flex", color = ColorTextMuted) },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_fueltype_input")
                    )
                }

                // Linha: Tanque + Custos Fixos Mensais
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tankCapacity,
                        onValueChange = { tankCapacity = it },
                        label = { Text("Tanque (L)") },
                        placeholder = { Text("16.0", color = ColorTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_tank_capacity_input")
                    )

                    OutlinedTextField(
                        value = fixedCosts,
                        onValueChange = { fixedCosts = it },
                        label = { Text("Custo Fixo (R$)") },
                        placeholder = { Text("180.0", color = ColorTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_fixedcosts_input")
                    )
                }

                // Linha: Dias Trabalhados + Hodômetro Atual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = plannedDays,
                        onValueChange = { plannedDays = it },
                        label = { Text("Dias/Mês") },
                        placeholder = { Text("22", color = ColorTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_planneddays_input")
                    )

                    OutlinedTextField(
                        value = currentOdometer,
                        onValueChange = { currentOdometer = it },
                        label = { Text("Hodômetro (km)") },
                        placeholder = { Text("15000", color = ColorTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_odometer_input")
                    )
                }
            }
        }

        // COMPONENTE 4: CONDICIONAIS POR TIPO DE VEÍCULO
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedVehicleType.equals("MOTO", ignoreCase = true)) {
                    Text(
                        text = "ACESSÓRIOS DE CARGA (MOTO)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrandEmerald,
                        letterSpacing = 1.sp
                    )

                    // Switch Baú de Carga
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = ColorBrandEmerald, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Possui Baú de Carga", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary)
                                Text("Instalação de baú traseiro para volumes maiores", fontSize = 11.sp, color = ColorTextMuted)
                            }
                        }
                        Switch(
                            checked = hasTopBox,
                            onCheckedChange = { hasTopBox = it },
                            modifier = Modifier.testTag("vehicle_has_topbox_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorBrandEmerald,
                                uncheckedThumbColor = ColorTextMuted,
                                uncheckedTrackColor = ColorSurfaceInput
                            )
                        )
                    }

                    HorizontalDivider(color = ColorBorder)

                    // Switch Mochila / Bag Térmica
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Backpack, contentDescription = null, tint = ColorBrandEmerald, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Utiliza Mochila / Bag Térmica", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary)
                                Text("Transporte em bag nas costas para delivery", fontSize = 11.sp, color = ColorTextMuted)
                            }
                        }
                        Switch(
                            checked = hasDeliveryBag,
                            onCheckedChange = { hasDeliveryBag = it },
                            modifier = Modifier.testTag("vehicle_has_bag_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorBrandEmerald,
                                uncheckedThumbColor = ColorTextMuted,
                                uncheckedTrackColor = ColorSurfaceInput
                            )
                        )
                    }
                } else {
                    // Se Carro: Seletor de Carroceria
                    Text(
                        text = "TIPO DE CARROCERIA (CARRO)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrandEmerald,
                        letterSpacing = 1.sp
                    )

                    val bodyOptions = listOf("Hatch", "Sedan", "Furgão", "Picape", "SUV")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bodyOptions.forEach { type ->
                            val isSelected = carBodyType.equals(type, ignoreCase = true)
                            val tagKey = type.lowercase().replace("ã", "a")
                            FilterChip(
                                selected = isSelected,
                                onClick = { carBodyType = type },
                                label = { Text(type, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorBrandEmerald,
                                    selectedLabelColor = ColorEmeraldDark,
                                    containerColor = ColorSurfaceElevated,
                                    labelColor = ColorTextSecondary
                                ),
                                modifier = Modifier
                                    .testTag("vehicle_carbody_chip_$tagKey")
                                    .semantics {
                                        testTag = "vehicle_carbody_chip_${type.lowercase()}"
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // BOTÃO DE SALVAR VEÍCULO
        Button(
            onClick = {
                val finalBrand = if (isManualInput) manualBrand else (selectedBrand?.nome ?: manualBrand)
                val finalModel = if (isManualInput) manualModel else (selectedModel?.nome ?: manualModel)
                val finalYear = if (isManualInput) manualYear else (selectedYear?.nome ?: manualYear)

                val consVal = consumption.toDoubleOrNull() ?: (if (selectedVehicleType == "MOTO") 40.0 else 12.0)
                val tankVal = tankCapacity.toDoubleOrNull() ?: (if (selectedVehicleType == "MOTO") 16.0 else 50.0)
                val fCostsVal = CurrencyUtils.parseToCents(fixedCosts, 18000L)
                val pDaysVal = plannedDays.toIntOrNull() ?: 22
                val odoVal = currentOdometer.toDoubleOrNull() ?: 0.0

                if (finalModel.isNotBlank()) {
                    val toSave = vehicle.copy(
                        brand = finalBrand.trim(),
                        model = finalModel.trim(),
                        manufacturingYear = finalYear.trim(),
                        vehicleType = selectedVehicleType.trim().uppercase(),
                        averageConsumption = consVal,
                        fuelType = fuelType.trim(),
                        tankCapacity = tankVal,
                        monthlyFixedCosts = fCostsVal,
                        plannedWorkDays = pDaysVal,
                        currentOdometer = odoVal,
                        hasTopBox = hasTopBox,
                        hasDeliveryBag = hasDeliveryBag,
                        carBodyType = carBodyType.trim()
                    )
                    onSave(toSave)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vehicle_save_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorBrandEmerald,
                contentColor = ColorEmeraldDark
            )
        ) {
            Icon(Icons.Filled.Save, contentDescription = null, tint = ColorEmeraldDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEdit) "Salvar Alterações do Veículo" else "Cadastrar Veículo",
                fontWeight = FontWeight.Bold,
                color = ColorEmeraldDark,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
