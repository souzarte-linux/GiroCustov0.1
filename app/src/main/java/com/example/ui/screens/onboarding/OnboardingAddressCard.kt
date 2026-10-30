package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.ViaCepUiState
import com.example.util.BrazilianCepVisualTransformation

@Composable
internal fun OnboardingAddressCard(
    cep: String,
    onCepChange: (String) -> Unit,
    viaCepState: ViaCepUiState,
    onSearchCep: (String) -> Unit,
    onManualCepClick: () -> Unit,
    street: String,
    onStreetChange: (String) -> Unit,
    neighborhood: String,
    onNeighborhoodChange: (String) -> Unit,
    addressNumber: String,
    onAddressNumberChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    stateUf: String,
    onStateUfChange: (String) -> Unit,
    numberFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // SEÇÃO 2: LOCALIZAÇÃO & ENDEREÇO (VIACEP)
        // ==========================================
        Text(
            text = stringResource(R.string.onboarding_section_address),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ColorBrandEmerald
        )

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
                OutlinedTextField(
                    value = cep,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 8) {
                            onCepChange(digits)
                        }
                    },
                    label = { Text(stringResource(R.string.onboarding_cep_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_cep_placeholder), color = ColorTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.PinDrop,
                            contentDescription = null,
                            tint = ColorTextMuted
                        )
                    },
                    trailingIcon = {
                        when (viaCepState) {
                            is ViaCepUiState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .testTag("onboarding_cep_search_icon"),
                                    strokeWidth = 2.dp,
                                    color = ColorBrandEmerald
                                )
                            }
                            is ViaCepUiState.Success -> {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.onboarding_cd_cep_found),
                                    tint = ColorBrandEmerald,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .testTag("onboarding_cep_search_icon")
                                )
                            }
                            is ViaCepUiState.Error -> {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = stringResource(R.string.onboarding_cd_cep_error),
                                    tint = ColorError,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .testTag("onboarding_cep_search_icon")
                                )
                            }
                            else -> {
                                IconButton(
                                    onClick = { onSearchCep(cep) },
                                    modifier = Modifier.testTag("onboarding_cep_search_icon")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.onboarding_cd_cep_search),
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
                    colors = onboardingTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_cep_input")
                )

                // AÇÃO: NÃO SEI MEU CEP / PREENCHER MANUAL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onManualCepClick,
                        modifier = Modifier.testTag("onboarding_manual_cep_toggle")
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_cep_manual_action),
                            fontSize = 12.sp,
                            color = ColorBrandEmerald
                        )
                    }
                }

                // BANNER DE FEEDBACK DO CEP
                AnimatedVisibility(
                    visible = viaCepState is ViaCepUiState.Success || viaCepState is ViaCepUiState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    when (val state = viaCepState) {
                        is ViaCepUiState.Success -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_cep_feedback_banner"),
                                color = ColorEmeraldBadgeBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ColorBrandEmerald)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = ColorBrandEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.onboarding_cep_success_banner),
                                        color = ColorEmeraldBadgeText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        is ViaCepUiState.Error -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_cep_feedback_banner"),
                                color = ColorErrorBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ColorError)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = ColorError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        color = ColorError,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }

                // CAMPOS DE ENDEREÇO
                OutlinedTextField(
                    value = street,
                    onValueChange = onStreetChange,
                    label = { Text(stringResource(R.string.onboarding_street_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_street_placeholder), color = ColorTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Signpost,
                            contentDescription = null,
                            tint = ColorTextMuted
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    colors = onboardingTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_street_input")
                )

                OutlinedTextField(
                    value = neighborhood,
                    onValueChange = onNeighborhoodChange,
                    label = { Text(stringResource(R.string.onboarding_neighborhood_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_neighborhood_placeholder), color = ColorTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.HomeWork,
                            contentDescription = null,
                            tint = ColorTextMuted
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    colors = onboardingTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_neighborhood_input")
                )

                OutlinedTextField(
                    value = addressNumber,
                    onValueChange = onAddressNumberChange,
                    label = { Text(stringResource(R.string.onboarding_number_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_number_placeholder), color = ColorTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Numbers,
                            contentDescription = null,
                            tint = ColorTextMuted
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    colors = onboardingTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(numberFocusRequester)
                        .testTag("onboarding_number_input")
                )

                // LINHA CIDADE & UF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = onCityChange,
                        label = { Text(stringResource(R.string.onboarding_city_label)) },
                        placeholder = { Text(stringResource(R.string.onboarding_city_placeholder), color = ColorTextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = onboardingTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.72f)
                            .testTag("onboarding_city_input")
                    )

                    OutlinedTextField(
                        value = stateUf,
                        onValueChange = { input ->
                            if (input.length <= 2) {
                                onStateUfChange(input.uppercase())
                            }
                        },
                        label = { Text(stringResource(R.string.onboarding_uf_label)) },
                        placeholder = { Text(stringResource(R.string.onboarding_uf_placeholder), color = ColorTextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        colors = onboardingTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.28f)
                            .testTag("onboarding_uf_input")
                    )
                }
            }
        }
    }
}
