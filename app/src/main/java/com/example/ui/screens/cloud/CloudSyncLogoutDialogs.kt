package com.example.ui.screens.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PendingLogoutWarningDialog(
    onDismiss: () -> Unit,
    onSyncAndLogout: () -> Unit,
    onForceLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Lançamentos Pendentes Detectados",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Você possui lançamentos salvos neste celular que ainda não foram sincronizados com a nuvem. Se sair agora e outro motorista acessar o aparelho, seus lançamentos poderão ser descartados. Deseja sincronizar antes de sair?",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(onClick = onSyncAndLogout) {
                Text("Sincronizar Agora")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onForceLogout,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sair Mesmo Assim")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
fun CloudSyncLogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Desconectar Conta?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Seus dados locais continuarão salvos com segurança neste dispositivo. Ao sair, a sincronização em nuvem ficará pausada até você entrar novamente.",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Desconectar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
