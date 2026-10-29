package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthSocialAndFooterSection(
    isRegisterMode: Boolean,
    isLoading: Boolean,
    onGoogleClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ==========================================
        // DIVISOR HORIZONTAL
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = ColorBorder
            )
            Text(
                text = if (isRegisterMode) "ou cadastre-se com" else "ou continue com",
                fontSize = 12.sp,
                color = ColorTextMuted,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = ColorBorder
            )
        }

        // ==========================================
        // BOTÃO GOOGLE SIGN-IN / SIGN-UP
        // ==========================================
        OutlinedButton(
            onClick = onGoogleClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(if (isRegisterMode) "auth_google_signup_btn" else "auth_google_signin_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ColorSurfaceCard,
                contentColor = ColorTextPrimary
            ),
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "G",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorGoogleBlue
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isRegisterMode) "Cadastrar com o Google" else "Continuar com o Google",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextPrimary
                )
            }
        }

        // ==========================================
        // RODAPÉ LEGAL
        // ==========================================
        Text(
            text = "Ao continuar, você concorda com nossos Termos de Uso e Política de Privacidade.",
            fontSize = 11.sp,
            color = ColorTextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
