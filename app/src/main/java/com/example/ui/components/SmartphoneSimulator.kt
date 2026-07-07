package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmartphoneSimulator(
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0E)), // Ultra dark charcoal/black surrounding frame for premium feel
        contentAlignment = Alignment.Center
    ) {
        val maxWidth = maxWidth
        val isDesktop = maxWidth > 480.dp

        if (isDesktop) {
            val phoneBg = if (isDark) Color(0xFF121214) else Color(0xFFF8FAFC)
            val phoneBorder = if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)
            val phoneText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

            // Desenha um mock realista de smartphone centralizado na tela
            Box(
                modifier = Modifier
                    .width(390.dp)
                    .height(820.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .border(8.dp, phoneBorder, RoundedCornerShape(40.dp)) // Adaptive border
                    .background(phoneBg) // Adaptive background
                    .shadow(16.dp, RoundedCornerShape(40.dp))
            ) {
                // Layout interno do celular simulado
                Column(modifier = Modifier.fillMaxSize().background(phoneBg)) {
                    // Barra superior com Notch / Ilha Dinâmica simulada
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(phoneBg) // Seamless adaptive background
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Horário Simulado
                        Text(
                            text = "19:45",
                            color = phoneText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )

                        // Notch da Câmera (Ilha Dinâmica compacta)
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)) // Notch
                        )

                        // Ícones de Status (Bateria, Wifi, 5G)
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "5G",
                                color = phoneText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Filled.Wifi, "Wifi", tint = phoneText, modifier = Modifier.size(12.dp))
                            Icon(Icons.Filled.Battery5Bar, "Bateria", tint = phoneText, modifier = Modifier.size(12.dp))
                        }
                    }

                    // Conteúdo Principal do Applet
                    Box(modifier = Modifier.weight(1f)) {
                        content()
                    }

                    // Linha Inferior do Gesto do Smartphone
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .background(phoneBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)) // gesture bar
                        )
                    }
                }
            }
        } else {
            // Em smartphone real, roda em tela cheia direta
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
