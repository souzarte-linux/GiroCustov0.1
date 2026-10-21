package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.network.supabase.AuthSession
import com.example.ui.SyncState

@Composable
fun CloudSyncTopBarIcon(
    currentUser: AuthSession?,
    syncState: SyncState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSyncing = syncState == SyncState.SYNCING

    val infiniteTransition = rememberInfiniteTransition(label = "topbar_sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.testTag("topbar_cloud_sync_btn")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp)
        ) {
            when {
                currentUser == null -> {
                    // Desconectado: Nuvem com corte / cinza
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = "Desconectado da Nuvem - Toque para entrar",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                isSyncing -> {
                    // Sincronizando: Ícone com loading girando em verde esmeralda
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Sincronizando com a Nuvem...",
                        tint = Color(0xFF10B981),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotationAngle)
                    )
                }

                syncState == SyncState.SUCCESS || syncState == SyncState.IDLE -> {
                    // Sincronizado / Conectado: Nuvem com check / verde
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = "Nuvem Sincronizada",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                }

                syncState == SyncState.ERROR -> {
                    // Erro: Nuvem com exclamação / alerta vermelho
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = "Erro na sincronização",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E))
                        )
                    }
                }

                syncState == SyncState.OFFLINE -> {
                    // Offline: Nuvem off em âmbar
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = "Modo Offline",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
