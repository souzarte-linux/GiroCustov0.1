package com.example.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.util.BrazilianPhoneVisualTransformation

@Composable
internal fun OnboardingPersonalInfoCard(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    isWhatsapp: Boolean,
    onWhatsappChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // SEÇÃO 1: DADOS PESSOAIS & CONTATO
        // ==========================================
        Text(
            text = stringResource(R.string.onboarding_section_personal),
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.onboarding_name_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_name_placeholder), color = ColorTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
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
                        .testTag("onboarding_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 11) {
                            onPhoneChange(digits)
                        }
                    },
                    label = { Text(stringResource(R.string.onboarding_phone_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_phone_placeholder), color = ColorTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = ColorTextMuted
                        )
                    },
                    visualTransformation = BrazilianPhoneVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    colors = onboardingTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_phone_input")
                )

                // CARD INTEGRADO WHATSAPP
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
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
                                contentDescription = stringResource(R.string.onboarding_whatsapp_icon_desc),
                                tint = ColorBrandEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.onboarding_whatsapp_title),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_whatsapp_desc),
                                    fontSize = 11.sp,
                                    color = ColorTextMuted
                                )
                            }
                        }

                        Switch(
                            checked = isWhatsapp,
                            onCheckedChange = onWhatsappChange,
                            modifier = Modifier.testTag("onboarding_whatsapp_switch"),
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
        }
    }
}
