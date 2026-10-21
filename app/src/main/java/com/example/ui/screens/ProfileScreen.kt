package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Platform
import com.example.data.UserProfile
import com.example.data.Vehicle
import com.example.data.VehiclePart
import com.example.network.fipe.FipeItemDto
import com.example.ui.FipeUiState
import com.example.ui.GiroCustoViewModel
import com.example.ui.ViaCepUiState
import com.example.util.BrazilianCepVisualTransformation
import com.example.util.BrazilianPhoneVisualTransformation
import kotlinx.coroutines.delay
import java.util.Locale

// Design Tokens Dark Theme Pro
private val ColorDarkBg = Color(0xFF121214)
private val ColorSurfaceCard = Color(0xFF1E1E22)
private val ColorSurfaceElevated = Color(0xFF24242A)
private val ColorSurfaceInput = Color(0xFF2A2A30)
private val ColorBorder = Color(0xFF2D2D34)
private val ColorBorderActive = Color(0xFF10B981)
private val ColorBrandEmerald = Color(0xFF10B981)
private val ColorEmeraldDark = Color(0xFF064E3B)
private val ColorEmeraldLight = Color(0xFF34D399)
private val ColorActiveVehicleCard = Color(0xFF132D21)
private val ColorSky = Color(0xFF38BDF8)
private val ColorAmber = Color(0xFFF59E0B)
private val ColorRose = Color(0xFFF43F5E)
private val ColorTextPrimary = Color(0xFFF1F5F9)
private val ColorTextSecondary = Color(0xFFCBD5E1)
private val ColorTextMuted = Color(0xFF94A3B8)

@Composable
fun ProfileScreen(
    viewModel: GiroCustoViewModel,
    onNavigateToPlatforms: () -> Unit = {}
) {
    val allVehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val allPlatforms by viewModel.allPlatforms.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val viaCepState by viewModel.viaCepState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Estado do formulário de veículo (null: lista / seções, -1L: novo veículo, >0L: edição de veículo)
    var editingVehicleId by remember { mutableStateOf<Long?>(null) }

    AnimatedContent(
        targetState = editingVehicleId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "profile_vehicle_transition"
    ) { currentVehicleId ->
        if (currentVehicleId == null) {
            ProfileSectionsContent(
                viewModel = viewModel,
                userProfile = userProfileState ?: UserProfile(),
                allPlatforms = allPlatforms,
                allVehicles = allVehicles,
                parts = parts,
                viaCepState = viaCepState,
                isDark = isDark,
                onNavigateToPlatforms = onNavigateToPlatforms,
                onAddVehicleClick = { editingVehicleId = -1L },
                onEditVehicleClick = { id -> editingVehicleId = id }
            )
        } else {
            val isEdit = currentVehicleId != -1L
            val vehicleToEdit = if (isEdit) {
                allVehicles.find { it.id == currentVehicleId } ?: Vehicle(id = 0L)
            } else {
                Vehicle(id = 0L)
            }

            VehicleFormSection(
                vehicle = vehicleToEdit,
                isEdit = isEdit,
                viewModel = viewModel,
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

/**
 * Conteúdo principal contendo as 4 seções ordenadas da tela de perfil.
 */
@Composable
private fun ProfileSectionsContent(
    viewModel: GiroCustoViewModel,
    userProfile: UserProfile,
    allPlatforms: List<Platform>,
    allVehicles: List<Vehicle>,
    parts: List<VehiclePart>,
    viaCepState: ViaCepUiState,
    isDark: Boolean,
    onNavigateToPlatforms: () -> Unit,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (Long) -> Unit
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Meu Perfil & Veículos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Gestão de infraestrutura operacional",
                    fontSize = 12.sp,
                    color = ColorTextMuted
                )
            }

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ColorSurfaceCard)
                    .testTag("theme_toggle_btn_profile")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) ColorAmber else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // =========================================================================
        // SEÇÃO 1: CARD MEUS DADOS CADASTRAIS
        // =========================================================================
        CadastralDataCard(
            userProfile = userProfile,
            viaCepState = viaCepState,
            viewModel = viewModel
        )

        // =========================================================================
        // SEÇÃO 2: CARD PLATAFORMAS ATIVAS
        // =========================================================================
        ActivePlatformsCard(
            allPlatforms = allPlatforms,
            onPlatformCheckChange = { platform, checked ->
                viewModel.savePlatform(platform.copy(active = checked))
            },
            onNavigateToPlatforms = onNavigateToPlatforms
        )

        // =========================================================================
        // SEÇÃO 3: MEUS VEÍCULOS
        // =========================================================================
        MyVehiclesSection(
            allVehicles = allVehicles,
            onAddVehicleClick = onAddVehicleClick,
            onEditVehicleClick = onEditVehicleClick,
            onActivateVehicle = { vehicleId ->
                viewModel.setActiveVehicle(vehicleId)
                Toast.makeText(context, "Veículo operacional ativado!", Toast.LENGTH_SHORT).show()
            },
            onDeleteVehicle = { vehicle ->
                viewModel.deleteVehicle(vehicle)
                Toast.makeText(context, "Veículo excluído.", Toast.LENGTH_SHORT).show()
            }
        )

        // =========================================================================
        // SEÇÃO 4: MONITORAMENTO DE DESGASTE
        // =========================================================================
        PartsWearSection(
            parts = parts,
            onAddPartClick = { showAddPartDialog = true },
            onEditPartClick = { part ->
                partToEdit = part
                showEditPartDialog = true
            },
            onResetWear = { partId ->
                viewModel.resetPartWear(partId)
                Toast.makeText(context, "Desgaste da peça zerado!", Toast.LENGTH_SHORT).show()
            },
            onDeletePart = { part ->
                viewModel.deletePart(part)
                Toast.makeText(context, "Peça removida do monitoramento.", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Diálogos de Peças
    if (showAddPartDialog) {
        ProfileAddPartDialog(
            onDismiss = { showAddPartDialog = false },
            onAdd = { name: String, price: Double, lifespan: Double ->
                viewModel.addPart(name, price, lifespan)
                showAddPartDialog = false
                Toast.makeText(context, "Nova peça adicionada!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEditPartDialog && partToEdit != null) {
        ProfileEditPartDialog(
            part = partToEdit!!,
            onDismiss = {
                showEditPartDialog = false
                partToEdit = null
            },
            onSave = { name: String, price: Double, lifespan: Double, runKm: Double ->
                viewModel.updatePart(partToEdit!!.id, name, price, lifespan, runKm)
                showEditPartDialog = false
                partToEdit = null
                Toast.makeText(context, "Peça de desgaste atualizada!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// =============================================================================
// COMPONENTES DA SEÇÃO 1: MEUS DADOS CADASTRAIS
// =============================================================================

@Composable
private fun CadastralDataCard(
    userProfile: UserProfile,
    viaCepState: ViaCepUiState,
    viewModel: GiroCustoViewModel
) {
    val context = LocalContext.current
    val numberFocusRequester = remember { FocusRequester() }

    var name by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var phoneDigits by remember(userProfile.phone) { mutableStateOf(userProfile.phone.filter { it.isDigit() }) }
    var isWhatsapp by remember(userProfile.isWhatsapp) { mutableStateOf(userProfile.isWhatsapp) }
    var cepDigits by remember(userProfile.cep) { mutableStateOf(userProfile.cep.filter { it.isDigit() }) }
    var street by remember(userProfile.street) { mutableStateOf(userProfile.street) }
    var addressNumber by remember(userProfile.addressNumber) { mutableStateOf(userProfile.addressNumber) }
    var neighborhood by remember(userProfile.neighborhood) { mutableStateOf(userProfile.neighborhood) }
    var city by remember(userProfile.city) { mutableStateOf(userProfile.city) }
    var stateUf by remember(userProfile.stateUf) { mutableStateOf(userProfile.stateUf) }

    // Dispara busca automática ao preencher 8 dígitos de CEP
    LaunchedEffect(cepDigits) {
        if (cepDigits.length == 8) {
            viewModel.searchCep(cepDigits)
        }
    }

    // Ao encontrar o CEP com sucesso, autopreenche endereço e move o foco para o número
    LaunchedEffect(viaCepState) {
        if (viaCepState is ViaCepUiState.Success) {
            val addr = viaCepState.address
            if (!addr.logradouro.isNullOrBlank()) street = addr.logradouro
            if (!addr.bairro.isNullOrBlank()) neighborhood = addr.bairro
            if (!addr.localidade.isNullOrBlank()) city = addr.localidade
            if (!addr.uf.isNullOrBlank()) stateUf = addr.uf.uppercase()

            delay(100)
            try {
                numberFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header do Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Badge,
                    contentDescription = null,
                    tint = ColorBrandEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MEUS DADOS CADASTRAIS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorBrandEmerald,
                    letterSpacing = 1.sp
                )
            }

            // Campo Nome Completo
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome Completo") },
                placeholder = { Text("Ex: Carlos Silva", color = ColorTextMuted) },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = ColorTextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input")
            )

            // Campo Telefone com máscara
            OutlinedTextField(
                value = phoneDigits,
                onValueChange = { input ->
                    phoneDigits = input.filter { it.isDigit() }.take(11)
                },
                label = { Text("Telefone de Contato") },
                placeholder = { Text("(11) 98765-4321", color = ColorTextMuted) },
                leadingIcon = {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = ColorTextMuted)
                },
                visualTransformation = BrazilianPhoneVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_phone_input")
            )

            // Switch WhatsApp Integrado
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ColorSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp",
                            tint = ColorBrandEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Este número é WhatsApp?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorTextPrimary
                            )
                            Text(
                                text = "Para avisos operacionais e relatórios",
                                fontSize = 11.sp,
                                color = ColorTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isWhatsapp,
                        onCheckedChange = { isWhatsapp = it },
                        modifier = Modifier.testTag("profile_is_whatsapp_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ColorBrandEmerald,
                            uncheckedThumbColor = ColorTextMuted,
                            uncheckedTrackColor = ColorSurfaceInput
                        )
                    )
                }
            }

            // Bloco CEP & ViaCEP
            OutlinedTextField(
                value = cepDigits,
                onValueChange = { input ->
                    cepDigits = input.filter { it.isDigit() }.take(8)
                },
                label = { Text("CEP Residencial") },
                placeholder = { Text("01001-000", color = ColorTextMuted) },
                leadingIcon = {
                    Icon(Icons.Filled.PinDrop, contentDescription = null, tint = ColorTextMuted)
                },
                trailingIcon = {
                    when (viaCepState) {
                        is ViaCepUiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("profile_cep_search_icon"),
                                strokeWidth = 2.dp,
                                color = ColorBrandEmerald
                            )
                        }
                        is ViaCepUiState.Success -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "CEP Localizado",
                                tint = ColorBrandEmerald,
                                modifier = Modifier
                                    .size(22.dp)
                                    .testTag("profile_cep_search_icon")
                            )
                        }
                        is ViaCepUiState.Error -> {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Erro no CEP",
                                tint = ColorRose,
                                modifier = Modifier
                                    .size(22.dp)
                                    .testTag("profile_cep_search_icon")
                            )
                        }
                        else -> {
                            IconButton(
                                onClick = { viewModel.searchCep(cepDigits) },
                                modifier = Modifier.testTag("profile_cep_search_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Buscar CEP",
                                    tint = ColorTextMuted
                                )
                            }
                        }
                    }
                },
                visualTransformation = BrazilianCepVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_cep_input")
            )

            // Link "Não sei meu CEP" (Abre URL externa dos Correios)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://buscacepinter.correios.com.br/app/endereco/index.php")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.testTag("profile_search_cep_web_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = ColorBrandEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Não sei meu CEP (Consultar Correios)",
                        fontSize = 12.sp,
                        color = ColorBrandEmerald
                    )
                }
            }

            // Alerta se CEP deu erro
            if (viaCepState is ViaCepUiState.Error) {
                Text(
                    text = viaCepState.message,
                    color = ColorRose,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Linha: Logradouro/Rua + Número
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Logradouro / Rua") },
                    placeholder = { Text("Av. Paulista", color = ColorTextMuted) },
                    singleLine = true,
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("profile_street_input")
                )

                OutlinedTextField(
                    value = addressNumber,
                    onValueChange = { addressNumber = it },
                    label = { Text("Nº") },
                    placeholder = { Text("123", color = ColorTextMuted) },
                    singleLine = true,
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.3f)
                        .focusRequester(numberFocusRequester)
                        .testTag("profile_number_input")
                )
            }

            // Linha: Bairro
            OutlinedTextField(
                value = neighborhood,
                onValueChange = { neighborhood = it },
                label = { Text("Bairro") },
                placeholder = { Text("Bela Vista", color = ColorTextMuted) },
                singleLine = true,
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_neighborhood_input")
            )

            // Linha: Cidade + UF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Cidade") },
                    placeholder = { Text("São Paulo", color = ColorTextMuted) },
                    singleLine = true,
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("profile_city_input")
                )

                OutlinedTextField(
                    value = stateUf,
                    onValueChange = { stateUf = it.take(2).uppercase() },
                    label = { Text("UF") },
                    placeholder = { Text("SP", color = ColorTextMuted) },
                    singleLine = true,
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.3f)
                        .testTag("profile_uf_input")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botão de Salvar Perfil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = "profile_save_btn" }
            ) {
                Button(
                    onClick = {
                        viewModel.updateUserProfile(
                            name = name.trim(),
                            phone = phoneDigits.trim(),
                            city = city.trim(),
                            isWhatsapp = isWhatsapp,
                            cep = cepDigits.trim(),
                            street = street.trim(),
                            addressNumber = addressNumber.trim(),
                            neighborhood = neighborhood.trim(),
                            stateUf = stateUf.trim().uppercase()
                        )
                        Toast.makeText(context, "Dados cadastrais salvos com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_save_cadastrais_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorBrandEmerald,
                        contentColor = ColorEmeraldDark
                    )
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = ColorEmeraldDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salvar Dados Cadastrais",
                        fontWeight = FontWeight.Bold,
                        color = ColorEmeraldDark,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// COMPONENTES DA SEÇÃO 2: PLATAFORMAS ATIVAS
// =============================================================================

@Composable
private fun ActivePlatformsCard(
    allPlatforms: List<Platform>,
    onPlatformCheckChange: (Platform, Boolean) -> Unit,
    onNavigateToPlatforms: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header do Card
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
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = ColorBrandEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "PLATAFORMAS ATIVAS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrandEmerald,
                        letterSpacing = 1.sp
                    )
                }

                TextButton(
                    onClick = onNavigateToPlatforms,
                    modifier = Modifier.testTag("profile_manage_platforms_btn")
                ) {
                    Text(
                        text = "Gerenciar",
                        color = ColorSky,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            if (allPlatforms.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nenhuma plataforma cadastrada.",
                        color = ColorTextMuted,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onNavigateToPlatforms,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorEmeraldDark,
                            contentColor = ColorEmeraldLight
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("profile_go_to_platforms_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cadastrar Plataformas", fontSize = 13.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allPlatforms.forEach { platform ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ColorSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ColorBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = platform.active,
                                    onCheckedChange = { checked ->
                                        onPlatformCheckChange(platform, checked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ColorBrandEmerald,
                                        uncheckedColor = Color(0xFF64748B),
                                        checkmarkColor = ColorEmeraldDark
                                    ),
                                    modifier = Modifier
                                        .testTag("profile_platform_checkbox_${platform.id}")
                                        .semantics {
                                            testTag = "profile_platform_checkbox_${platform.name.lowercase().replace(" ", "_")}"
                                        }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = platform.name,
                                        color = ColorTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = ColorSurfaceInput,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = platform.segment.replaceFirstChar { it.uppercase() },
                                                color = ColorTextMuted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            color = ColorSurfaceInput,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = platform.cycle.replaceFirstChar { it.uppercase() },
                                                color = ColorTextMuted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
}

// =============================================================================
// COMPONENTES DA SEÇÃO 3: MEUS VEÍCULOS
// =============================================================================

@Composable
private fun MyVehiclesSection(
    allVehicles: List<Vehicle>,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (Long) -> Unit,
    onActivateVehicle: (Long) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header com Botão "+ Novo Veículo"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MEUS VEÍCULOS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorBrandEmerald,
                letterSpacing = 1.sp
            )

            Button(
                onClick = onAddVehicleClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorBrandEmerald,
                    contentColor = ColorEmeraldDark
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("profile_add_vehicle_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = ColorEmeraldDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Novo Veículo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (allVehicles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
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
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Nenhum veículo cadastrado",
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Adicione sua moto ou carro com a Tabela FIPE para monitorar custos e desgastes.",
                            color = ColorTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            allVehicles.forEach { vehicle ->
                val isActive = vehicle.active
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_vehicle_item_${vehicle.id}")
                        .clickable { onEditVehicleClick(vehicle.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) ColorActiveVehicleCard else ColorSurfaceCard
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = if (isActive) ColorBorderActive else ColorBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Linha Superior: Ícone, Marca/Modelo/Ano, Badges, Botão Ativar / Badge Ativo e Ações
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isMoto = vehicle.vehicleType.equals("MOTO", ignoreCase = true)
                                Icon(
                                    imageVector = if (isMoto) Icons.Filled.TwoWheeler else Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (isActive) ColorBrandEmerald else ColorTextMuted,
                                    modifier = Modifier.size(22.dp)
                                )

                                Column {
                                    val displayName = buildString {
                                        if (vehicle.brand.isNotBlank()) append("${vehicle.brand} ")
                                        append(vehicle.model)
                                        if (vehicle.manufacturingYear.isNotBlank()) append(" (${vehicle.manufacturingYear})")
                                    }

                                    Text(
                                        text = displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = ColorTextPrimary
                                    )

                                    // Pílulas de Acessórios / Especificações
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (isMoto) {
                                            if (vehicle.hasTopBox) {
                                                Surface(
                                                    color = ColorSurfaceInput,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(10.dp), tint = ColorTextMuted)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Baú", fontSize = 10.sp, color = ColorTextMuted)
                                                    }
                                                }
                                            }
                                            if (vehicle.hasDeliveryBag) {
                                                Surface(
                                                    color = ColorSurfaceInput,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.Backpack, contentDescription = null, modifier = Modifier.size(10.dp), tint = ColorTextMuted)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Mochila", fontSize = 10.sp, color = ColorTextMuted)
                                                    }
                                                }
                                            }
                                        } else if (vehicle.carBodyType.isNotBlank()) {
                                            Surface(
                                                color = ColorSurfaceInput,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = vehicle.carBodyType,
                                                    fontSize = 10.sp,
                                                    color = ColorTextMuted,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Controle de Ativação e Ações
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isActive) {
                                    Surface(
                                        color = ColorBrandEmerald,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "ATIVO",
                                            color = ColorEmeraldDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onActivateVehicle(vehicle.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("profile_vehicle_activate_btn_${vehicle.id}")
                                    ) {
                                        Text("Ativar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorBrandEmerald)
                                    }
                                }

                                IconButton(
                                    onClick = { onEditVehicleClick(vehicle.id) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("profile_vehicle_edit_btn_${vehicle.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = ColorSky, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteVehicle(vehicle) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("profile_vehicle_delete_btn_${vehicle.id}")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir", tint = ColorRose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Linha de Métricas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Consumo: ${vehicle.averageConsumption} km/L • ${vehicle.fuelType}",
                                    color = ColorTextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Tanque: ${vehicle.tankCapacity} L • Hodômetro: ${vehicle.currentOdometer.toLong()} km",
                                    color = ColorTextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = String.format(Locale.GERMAN, "R$ %.2f/mês (%dd)", vehicle.monthlyFixedCosts, vehicle.plannedWorkDays),
                                color = ColorTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// COMPONENTES DA SEÇÃO 4: MONITORAMENTO DE DESGASTE
// =============================================================================

@Composable
private fun PartsWearSection(
    parts: List<VehiclePart>,
    onAddPartClick: () -> Unit,
    onEditPartClick: (VehiclePart) -> Unit,
    onResetWear: (Long) -> Unit,
    onDeletePart: (VehiclePart) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header com Botão "+ Adicionar Peça"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MONITORAMENTO DE DESGASTE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorBrandEmerald,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Componentes do veículo em uso ativo",
                    fontSize = 11.sp,
                    color = ColorTextMuted
                )
            }

            IconButton(
                onClick = onAddPartClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ColorSurfaceCard)
                    .testTag("profile_add_part_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar Peça", tint = ColorBrandEmerald)
            }
        }

        if (parts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma peça monitorada para este veículo.",
                        color = ColorTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            parts.forEach { part ->
                val health = part.healthPercentage
                val barColor = when {
                    health <= 15.0 -> ColorRose
                    health <= 35.0 -> ColorAmber
                    else -> ColorBrandEmerald
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_part_item_${part.id}"),
                    colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorBorder),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Linha Superior: Nome, Valores e Ações
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = part.name,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorTextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = String.format(Locale.GERMAN, "Troca: R$ %.2f | Vida útil: %d km", part.price, part.lifespanKm.toInt()),
                                    fontSize = 11.sp,
                                    color = ColorTextMuted
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEditPartClick(part) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("profile_part_edit_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, "Editar Peça", tint = ColorSky, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { onResetWear(part.id) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("profile_part_reset_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Build, "Zerar Desgaste", tint = ColorBrandEmerald, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { onDeletePart(part) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("profile_part_delete_btn_${part.id}")
                                ) {
                                    Icon(Icons.Filled.Delete, "Remover Peça", tint = ColorRose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Indicadores de Saúde & Uso
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.GERMAN, "Uso: %.0f km rodados", part.runKmSinceChange),
                                fontSize = 11.sp,
                                color = ColorTextMuted
                            )
                            Text(
                                text = String.format(Locale.GERMAN, "Saúde: %.0f%%", health),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            )
                        }

                        // Barra de Progresso Colorida
                        LinearProgressIndicator(
                            progress = { (health / 100.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .testTag("profile_part_progress_${part.id}"),
                            color = barColor,
                            trackColor = ColorBorder
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-TELA / FORMULÁRIO DE VEÍCULO (CATÁLOGO FIPE & DIGITAÇÃO MANUAL)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleFormSection(
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

    var brandExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    // Campos Manuais
    var manualBrand by remember { mutableStateOf(vehicle.brand) }
    var manualModel by remember { mutableStateOf(vehicle.model) }
    var manualYear by remember { mutableStateOf(vehicle.manufacturingYear) }

    // Campos Técnicos
    var consumption by remember { mutableStateOf(if (vehicle.id == 0L) (if (selectedVehicleType == "MOTO") "40.0" else "12.0") else vehicle.averageConsumption.toString()) }
    var fuelType by remember { mutableStateOf(vehicle.fuelType.ifBlank { "Gasolina" }) }
    var tankCapacity by remember { mutableStateOf(if (vehicle.id == 0L) (if (selectedVehicleType == "MOTO") "16.0" else "50.0") else vehicle.tankCapacity.toString()) }
    var fixedCosts by remember { mutableStateOf(if (vehicle.id == 0L) "180.0" else vehicle.monthlyFixedCosts.toString()) }
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
                    modifier = Modifier.testTag("vehicle_form_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorRose.copy(alpha = 0.15f))
                        .testTag("delete_vehicle_btn")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir Veículo", tint = ColorRose)
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
                    Text(
                        text = "Catálogo Tabela FIPE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrandEmerald,
                        letterSpacing = 1.sp
                    )

                    // 1. Marca FIPE
                    ExposedDropdownMenuBox(
                        expanded = brandExpanded,
                        onExpandedChange = { brandExpanded = !brandExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBrand?.nome.orEmpty().ifBlank { manualBrand },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Marca (FIPE)") },
                            placeholder = { Text("Selecione a Marca", color = ColorTextMuted) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                            colors = outlinedTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("vehicle_fipe_brand_select")
                        )

                        ExposedDropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false },
                            modifier = Modifier.background(ColorSurfaceElevated)
                        ) {
                            if (fipeMarcas.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Carregando marcas...", color = ColorTextMuted) },
                                    onClick = {}
                                )
                            } else {
                                fipeMarcas.forEach { marca ->
                                    DropdownMenuItem(
                                        text = { Text(marca.nome, color = ColorTextPrimary) },
                                        onClick = {
                                            selectedBrand = marca
                                            manualBrand = marca.nome
                                            selectedModel = null
                                            manualModel = ""
                                            selectedYear = null
                                            manualYear = ""
                                            brandExpanded = false
                                            viewModel.loadFipeModelos(selectedVehicleType, marca.codigo)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Modelo FIPE
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = {
                            if (selectedBrand != null || fipeModelos.isNotEmpty()) {
                                modelExpanded = !modelExpanded
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedModel?.nome.orEmpty().ifBlank { manualModel },
                            onValueChange = {},
                            readOnly = true,
                            enabled = selectedBrand != null || fipeModelos.isNotEmpty(),
                            label = { Text("Modelo (FIPE)") },
                            placeholder = { Text("Selecione o Modelo", color = ColorTextMuted) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            colors = outlinedTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("vehicle_fipe_model_select")
                        )

                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false },
                            modifier = Modifier.background(ColorSurfaceElevated)
                        ) {
                            if (fipeModelos.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Carregando modelos...", color = ColorTextMuted) },
                                    onClick = {}
                                )
                            } else {
                                fipeModelos.forEach { modelo ->
                                    DropdownMenuItem(
                                        text = { Text(modelo.nome, color = ColorTextPrimary) },
                                        onClick = {
                                            selectedModel = modelo
                                            manualModel = modelo.nome
                                            selectedYear = null
                                            manualYear = ""
                                            modelExpanded = false
                                            if (selectedBrand != null) {
                                                viewModel.loadFipeAnos(selectedVehicleType, selectedBrand!!.codigo, modelo.codigo)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Ano / Versão FIPE
                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = {
                            if (selectedModel != null || fipeAnos.isNotEmpty()) {
                                yearExpanded = !yearExpanded
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedYear?.nome.orEmpty().ifBlank { manualYear },
                            onValueChange = {},
                            readOnly = true,
                            enabled = selectedModel != null || fipeAnos.isNotEmpty(),
                            label = { Text("Ano Modelo / Versão (FIPE)") },
                            placeholder = { Text("Selecione o Ano", color = ColorTextMuted) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                            colors = outlinedTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("vehicle_fipe_year_select")
                        )

                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false },
                            modifier = Modifier.background(ColorSurfaceElevated)
                        ) {
                            if (fipeAnos.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Carregando anos...", color = ColorTextMuted) },
                                    onClick = {}
                                )
                            } else {
                                fipeAnos.forEach { ano ->
                                    DropdownMenuItem(
                                        text = { Text(ano.nome, color = ColorTextPrimary) },
                                        onClick = {
                                            selectedYear = ano
                                            manualYear = ano.nome
                                            yearExpanded = false
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
                                }
                            }
                        }
                    }

                    // Card de Resumo FIPE
                    if (fipeDetail != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ColorSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ColorBrandEmerald.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Valor Tabela FIPE",
                                        fontSize = 11.sp,
                                        color = ColorBrandEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = fipeDetail?.valor.orEmpty(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorTextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "FIPE: ${fipeDetail?.codigoFipe}",
                                        fontSize = 11.sp,
                                        color = ColorTextMuted
                                    )
                                    Text(
                                        text = "Ref: ${fipeDetail?.mesReferencia}",
                                        fontSize = 11.sp,
                                        color = ColorTextMuted
                                    )
                                }
                            }
                        }
                    } else if (fipeUiState is FipeUiState.Loading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ColorBrandEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Consultando base oficial FIPE...", fontSize = 12.sp, color = ColorTextMuted)
                        }
                    } else if (fipeUiState is FipeUiState.Error) {
                        Text(
                            text = (fipeUiState as FipeUiState.Error).message,
                            fontSize = 12.sp,
                            color = ColorRose
                        )
                    }
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
                val fCostsVal = fixedCosts.toDoubleOrNull() ?: 180.0
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

// =============================================================================
// DIÁLOGOS DE ADICIONAR E EDITAR PEÇA DE DESGASTE
// =============================================================================

@Composable
private fun ProfileAddPartDialog(
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
                .padding(16.dp)
                .testTag("add_part_dialog"),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceElevated),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Adicionar Peça de Desgaste",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                HorizontalDivider(color = ColorBorder)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Peça (ex: Pneu Traseiro)") },
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço de Troca (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lifespan,
                    onValueChange = { lifespan = it },
                    label = { Text("Durabilidade Estimada (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = ColorTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1000.0
                            if (name.isNotBlank() && p > 0.0) {
                                onAdd(name.trim(), p, l)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorBrandEmerald,
                            contentColor = ColorEmeraldDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Adicionar", fontWeight = FontWeight.Bold, color = ColorEmeraldDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditPartDialog(
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
                .padding(16.dp)
                .testTag("edit_part_dialog"),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceElevated),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Editar Peça de Desgaste",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                HorizontalDivider(color = ColorBorder)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Peça") },
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço de Troca (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lifespan,
                    onValueChange = { lifespan = it },
                    label = { Text("Durabilidade Estimada (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = runKm,
                    onValueChange = { runKm = it },
                    label = { Text("Km Rodados Desde a Troca") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = ColorTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val l = lifespan.toDoubleOrNull() ?: 1000.0
                            val r = runKm.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && p > 0.0) {
                                onSave(name.trim(), p, l, r)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorBrandEmerald,
                            contentColor = ColorEmeraldDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold, color = ColorEmeraldDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ColorSurfaceInput,
    unfocusedContainerColor = ColorSurfaceInput,
    disabledContainerColor = ColorSurfaceInput.copy(alpha = 0.5f),
    focusedBorderColor = ColorBrandEmerald,
    unfocusedBorderColor = ColorBorder,
    focusedTextColor = ColorTextPrimary,
    unfocusedTextColor = ColorTextPrimary,
    focusedLabelColor = ColorBrandEmerald,
    unfocusedLabelColor = ColorTextMuted
)
