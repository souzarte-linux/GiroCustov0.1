package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.util.BiometricHelper

@Composable
fun AuthBiometricLockedCard(
    userEmail: String?,
    isLoading: Boolean,
    onTriggerBiometric: () -> Unit,
    onDismissBiometricLock: () -> Unit
) {
    val context = LocalContext.current
    val savedEmail = remember(context, userEmail) {
        BiometricHelper.getBiometricEmail(context)
            ?: userEmail
            ?: "Motorista"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_biometric_locked_card"),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(ColorBrandEmerald.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Bloqueio Biométrico",
                    tint = ColorBrandEmerald,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Olá, $savedEmail",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Confirme sua digital ou biometria facial para acessar o GiroCusto com total segurança.",
                fontSize = 13.sp,
                color = ColorTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (BiometricHelper.getProtectionLevel(context) == com.example.util.BiometricProtectionLevel.WEAK) {
                Text(
                    text = stringResource(R.string.auth_biometric_weak_notice),
                    fontSize = 12.sp,
                    color = ColorBrandEmerald,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão largo de digital "Desbloquear com Biometria"
            Button(
                onClick = onTriggerBiometric,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("auth_biometric_button")
                    .semantics { testTag = "auth_gate_biometric_btn" },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorBrandEmerald,
                    contentColor = ColorOnPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("auth_loading_indicator"),
                        color = ColorOnPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = "Digital",
                            modifier = Modifier.size(26.dp),
                            tint = ColorOnPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Desbloquear com Biometria",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Botão secundário "Entrar com Google, e-mail ou senha"
            TextButton(
                onClick = onDismissBiometricLock,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_switch_account_btn")
            ) {
                Text(
                    text = stringResource(R.string.auth_biometric_switch_method),
                    color = ColorBrandEmerald,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
