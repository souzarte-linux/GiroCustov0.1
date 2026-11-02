package com.example.ui.screens.profile

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserProfile
import com.example.util.BiometricHelper

internal fun formatPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.length == 11 -> "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7)}"
        digits.length == 10 -> "(${digits.substring(0, 2)}) ${digits.substring(2, 6)}-${digits.substring(6)}"
        digits.isNotEmpty() -> digits
        else -> ""
    }
}

internal fun formatCep(cep: String): String {
    val digits = cep.filter { it.isDigit() }
    return if (digits.length == 8) "${digits.substring(0, 5)}-${digits.substring(5)}" else digits
}

@Composable
internal fun CadastralDataViewMode(
    userProfile: UserProfile,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Nome Completo
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.profile_cadastral_name_label),
                fontSize = 11.sp,
                color = ColorTextMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (userProfile.name.isNotBlank()) userProfile.name else stringResource(R.string.profile_cadastral_name_not_set),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (userProfile.name.isNotBlank()) ColorTextPrimary else ColorTextMuted
            )
        }

        // Telefone & WhatsApp
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.profile_cadastral_phone_label),
                fontSize = 11.sp,
                color = ColorTextMuted,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedPhone = formatPhone(userProfile.phone)
                Text(
                    text = if (formattedPhone.isNotBlank()) formattedPhone else stringResource(R.string.profile_cadastral_phone_not_set),
                    fontSize = 14.sp,
                    color = if (formattedPhone.isNotBlank()) ColorTextPrimary else ColorTextMuted,
                    fontWeight = FontWeight.Medium
                )

                Surface(
                    color = if (userProfile.isWhatsapp) ColorBrandEmerald.copy(alpha = 0.15f) else ColorSurfaceInput,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (userProfile.isWhatsapp) ColorBrandEmerald.copy(alpha = 0.3f) else ColorBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = if (userProfile.isWhatsapp) ColorBrandEmerald else ColorTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (userProfile.isWhatsapp) {
                                stringResource(R.string.profile_cadastral_whatsapp_active)
                            } else {
                                stringResource(R.string.profile_cadastral_whatsapp_inactive)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (userProfile.isWhatsapp) ColorBrandEmerald else ColorTextMuted
                        )
                    }
                }
            }
        }

        // Endereço Completo
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.profile_cadastral_street_label),
                fontSize = 11.sp,
                color = ColorTextMuted,
                fontWeight = FontWeight.Medium
            )
            val addressParts = listOfNotNull(
                if (userProfile.street.isNotBlank()) {
                    if (userProfile.addressNumber.isNotBlank()) "${userProfile.street}, ${userProfile.addressNumber}" else userProfile.street
                } else null,
                userProfile.neighborhood.takeIf { it.isNotBlank() },
                if (userProfile.city.isNotBlank()) {
                    if (userProfile.stateUf.isNotBlank()) "${userProfile.city}/${userProfile.stateUf}" else userProfile.city
                } else null
            )
            val cepStr = formatCep(userProfile.cep)
            val fullAddress = if (addressParts.isNotEmpty()) {
                val base = addressParts.joinToString(" - ")
                if (cepStr.isNotBlank()) "$base • CEP $cepStr" else base
            } else if (cepStr.isNotBlank()) {
                "CEP $cepStr"
            } else {
                null
            }

            Text(
                text = fullAddress ?: stringResource(R.string.profile_cadastral_address_not_set),
                fontSize = 13.sp,
                color = if (fullAddress != null) ColorTextPrimary else ColorTextMuted,
                lineHeight = 18.sp
            )
        }

        // Proteção Biométrica (se disponível)
        if (BiometricHelper.isBiometricHardwareAvailable(context)) {
            val isBioEnabled = BiometricHelper.isBiometricEnabled(context)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ColorSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = if (isBioEnabled) ColorBrandEmerald else ColorTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isBioEnabled) {
                            stringResource(R.string.profile_cadastral_biometric_active)
                        } else {
                            stringResource(R.string.profile_cadastral_biometric_inactive)
                        },
                        fontSize = 12.sp,
                        color = if (isBioEnabled) ColorTextPrimary else ColorTextMuted
                    )
                }
            }
        }
    }
}
