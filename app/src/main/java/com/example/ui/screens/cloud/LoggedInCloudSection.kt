package com.example.ui.screens.cloud

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.supabase.AuthSession
import com.example.ui.SyncState

@Composable
fun LoggedInCloudSection(
    currentUser: AuthSession,
    syncState: SyncState,
    lastSyncTime: Long?,
    syncError: String?,
    recordsCount: Int,
    refillsCount: Int,
    maintenanceCount: Int,
    vehicleModel: String?,
    isDark: Boolean,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit,
    formatLastSyncTime: (Long?) -> String
) {
    val isSyncing = syncState == SyncState.SYNCING

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // CARD DE CONTA & STATUS
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1E22) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Perfil e Identificação
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF047857))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = currentUser.userEmail?.take(1)?.uppercase() ?: "U"
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conectado ao GiroCusto Cloud",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = currentUser.userEmail ?: "Usuário Autenticado",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)
            )

            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status da Conexão:",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (syncState) {
                        SyncState.SYNCING -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        SyncState.SUCCESS -> Color(0xFF10B981).copy(alpha = 0.15f)
                        SyncState.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        SyncState.OFFLINE -> Color(0xFF64748B).copy(alpha = 0.15f)
                        SyncState.IDLE -> Color(0xFF10B981).copy(alpha = 0.15f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (syncState) {
                            SyncState.SYNCING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFF59E0B)
                                )
                                Text("Sincronizando...", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                            }
                            SyncState.SUCCESS -> {
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Text("Sincronizado", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                            SyncState.ERROR -> {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                Text("Erro ao Sincronizar", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            SyncState.OFFLINE -> {
                                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                Text("Offline", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            }
                            SyncState.IDLE -> {
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Text("Conectado", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Última sincronização
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Última sincronização:",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Text(
                    text = formatLastSyncTime(lastSyncTime),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Mensagem de erro caso exista
            if (syncState == SyncState.ERROR && !syncError.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Aviso: ${translateAuthError(syncError)}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }

    // CARD DE RESUMO LOCAL PROTEGIDO NA NUVEM
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1E22) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2D34) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Resumo dos Dados em Nuvem",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatItem(label = "Diárias", value = recordsCount.toString(), isDark = isDark)
                SummaryStatItem(label = "Abastecimentos", value = refillsCount.toString(), isDark = isDark)
                SummaryStatItem(label = "Manutenções", value = maintenanceCount.toString(), isDark = isDark)
            }

            if (!vehicleModel.isNullOrBlank()) {
                Surface(
                    color = if (isDark) Color(0xFF2D2D34).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TwoWheeler,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Veículo vinculado: $vehicleModel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // BOTÃO SINCRONIZAR AGORA
    Button(
        onClick = onSyncClick,
        enabled = !isSyncing,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_sync_now"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF10B981),
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Sync,
            contentDescription = "Sincronizar",
            modifier = Modifier
                .size(20.dp)
                .then(if (isSyncing) Modifier.rotate(rotationAngle) else Modifier)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isSyncing) "Sincronizando com a Nuvem..." else "Sincronizar Agora",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }

    // BOTÃO DESCONECTAR / SAIR
    OutlinedButton(
        onClick = onLogoutClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_logout_supabase"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Sair",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Desconectar / Sair da Conta",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SummaryStatItem(label: String, value: String, isDark: Boolean) {
    Surface(
        color = if (isDark) Color(0xFF2D2D34).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46).copy(alpha = 0.5f) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}
