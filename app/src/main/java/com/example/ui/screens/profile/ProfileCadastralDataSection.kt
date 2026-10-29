package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserProfile
import com.example.ui.GiroCustoViewModel
import com.example.ui.ViaCepUiState
import com.example.util.BiometricHelper
import com.example.util.BrazilianCepVisualTransformation
import com.example.util.BrazilianPhoneVisualTransformation
import kotlinx.coroutines.delay

@Composable
internal fun CadastralDataCard(
    userProfile: UserProfile,
    viaCepState: ViaCepUiState,
    viewModel: GiroCustoViewModel
) {
    val context = LocalContext.current
    val streetFocusRequester = remember { FocusRequester() }
    val numberFocusRequester = remember { FocusRequester() }

    var isManualCep by remember { mutableStateOf(false) }
    var name by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var phoneDigits by remember(userProfile.phone) { mutableStateOf(userProfile.phone.filter { it.isDigit() }) }
    var isWhatsapp by remember(userProfile.isWhatsapp) { mutableStateOf(userProfile.isWhatsapp) }
    var cepDigits by remember(userProfile.cep) { mutableStateOf(userProfile.cep.filter { it.isDigit() }) }
    var street by remember(userProfile.street) { mutableStateOf(userProfile.street) }
    var addressNumber by remember(userProfile.addressNumber) { mutableStateOf(userProfile.addressNumber) }
    var neighborhood by remember(userProfile.neighborhood) { mutableStateOf(userProfile.neighborhood) }
    var city by remember(userProfile.city) { mutableStateOf(userProfile.city) }
    var stateUf by remember(userProfile.stateUf) { mutableStateOf(userProfile.stateUf) }

    // Dispara busca automática ao preencher 8 dígitos de CEP (caso não esteja em modo manual)
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

            // Switch Bloqueio por Biometria
            if (BiometricHelper.isBiometricHardwareAvailable(context)) {
                var isBiometricActive by remember(context) {
                    mutableStateOf(BiometricHelper.isBiometricEnabled(context))
                }

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
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = stringResource(R.string.cd_profile_biometric_switch),
                                tint = ColorBrandEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.profile_biometric_switch_title),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.profile_biometric_switch_desc),
                                    fontSize = 11.sp,
                                    color = ColorTextMuted
                                )
                            }
                        }

                        Switch(
                            checked = isBiometricActive,
                            onCheckedChange = { checked ->
                                isBiometricActive = checked
                                val email = viewModel.currentUser.value?.userEmail
                                BiometricHelper.setBiometricEnabled(context, checked, email)
                            },
                            modifier = Modifier.testTag("profile_biometric_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorBrandEmerald,
                                uncheckedThumbColor = ColorTextMuted,
                                uncheckedTrackColor = ColorSurfaceInput
                            )
                        )
                    }
                }
            }

            // Bloco CEP & ViaCEP
            OutlinedTextField(
                value = cepDigits,
                onValueChange = { input ->
                    cepDigits = input.filter { it.isDigit() }.take(8)
                    if (cepDigits.length == 8) {
                        isManualCep = false
                    }
                },
                label = { Text(if (isManualCep) "CEP Residencial (Opcional)" else "CEP Residencial") },
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
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("profile_cep_search_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.cd_search_cep),
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

            // Links/Ações de CEP: Preenchimento manual e Consulta nos Correios
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = {
                        isManualCep = true
                        viewModel.clearViaCepState()
                        try {
                            streetFocusRequester.requestFocus()
                        } catch (_: Exception) {
                            try {
                                numberFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("profile_manual_cep_toggle"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = stringResource(R.string.cd_profile_cep_manual),
                            tint = ColorBrandEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.profile_cep_manual_action),
                            fontSize = 12.sp,
                            color = ColorBrandEmerald
                        )
                    }
                }

                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://buscacepinter.correios.com.br/app/endereco/index.php")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("profile_search_cep_web_btn"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.cd_profile_search_correios),
                            tint = ColorTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.profile_cep_search_correios_action),
                            fontSize = 12.sp,
                            color = ColorTextMuted
                        )
                    }
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
                        .focusRequester(streetFocusRequester)
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
                        .testTag("profile_save_cadastrais_btn")
                        .semantics { testTag = "profile_save_btn" },
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
