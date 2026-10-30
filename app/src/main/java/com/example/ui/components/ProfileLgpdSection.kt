package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Design Tokens Dinâmicos para o Componente de Privacidade
private val ColorSurfaceCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface
private val ColorSurfaceElevated: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val ColorSurfaceInput: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val ColorBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outline
private val ColorBrandEmerald: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val ColorRose: Color
    @Composable get() = MaterialTheme.colorScheme.error
private val ColorTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
private val ColorTextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val ColorTextMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

/**
 * Seção de Privacidade e Gerenciamento de Dados (LGPD - T-10 [SEC-06]).
 * Oferece portabilidade de dados em JSON e exclusão definitiva de conta.
 */
@Composable
fun PrivacyAndDataSection(
    onExportClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("privacy_lgpd_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        border = BorderStroke(1.dp, ColorBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabeçalho da Seção com Ícone de Segurança
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ColorBrandEmerald.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = stringResource(R.string.cd_lgpd_export_icon),
                        tint = ColorBrandEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lgpd_section_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.lgpd_section_desc),
                        fontSize = 12.sp,
                        color = ColorTextMuted
                    )
                }
            }

            HorizontalDivider(color = ColorBorder.copy(alpha = 0.5f))

            // Ação 1: Portabilidade de Dados (Exportar JSON)
            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_export_data_lgpd"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ColorBrandEmerald),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ColorBrandEmerald
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.cd_lgpd_export_icon),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.lgpd_btn_export_data),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.lgpd_btn_export_desc),
                        fontSize = 11.sp,
                        color = ColorTextMuted
                    )
                }
            }

            // Ação 2: Exclusão Definitiva de Conta e Dados
            OutlinedButton(
                onClick = onDeleteAccountClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_delete_account_lgpd"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ColorRose.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ColorRose
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = stringResource(R.string.cd_lgpd_delete_icon),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.lgpd_btn_delete_account),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.lgpd_btn_delete_desc),
                        fontSize = 11.sp,
                        color = ColorRose.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de Exportação de Dados Operacionais (LGPD Art. 18, V).
 */
@Composable
fun LgpdExportDialog(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = ColorSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = ColorBrandEmerald,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.lgpd_export_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ColorTextPrimary
                )
            }
        },
        text = {
            Text(
                text = stringResource(R.string.lgpd_export_dialog_message),
                fontSize = 14.sp,
                color = ColorTextSecondary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onSaveToDevice,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("dialog_export_save_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorBrandEmerald)
            ) {
                Text(
                    text = stringResource(R.string.lgpd_export_save_btn),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("dialog_export_share_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ColorBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextPrimary)
                ) {
                    Text(text = stringResource(R.string.lgpd_export_share_btn))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        color = ColorTextMuted
                    )
                }
            }
        }
    )
}

/**
 * Diálogo de Confirmação Reforçada em Duas Etapas para Exclusão de Conta (LGPD Art. 18, VI).
 * Exige a digitação obrigatória da palavra de segurança "EXCLUIR" para habilitar a ação irreversível.
 */
@Composable
fun LgpdDeleteAccountDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    var confirmationInput by remember { mutableStateOf("") }
    val expectedWord = stringResource(R.string.lgpd_delete_confirmation_word)
    val isConfirmed = confirmationInput.trim().equals(expectedWord, ignoreCase = false)

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        shape = RoundedCornerShape(16.dp),
        containerColor = ColorSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = ColorRose,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.lgpd_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ColorRose
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.lgpd_delete_dialog_warning),
                    fontSize = 13.sp,
                    color = ColorTextPrimary,
                    lineHeight = 18.sp
                )

                Text(
                    text = stringResource(R.string.lgpd_delete_dialog_export_hint),
                    fontSize = 12.sp,
                    color = ColorBrandEmerald,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.lgpd_delete_confirmation_prompt),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTextSecondary
                )

                OutlinedTextField(
                    value = confirmationInput,
                    onValueChange = { confirmationInput = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.lgpd_delete_input_placeholder),
                            color = ColorTextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_confirm_delete_account")
                        .semantics { testTag = "input_confirm_delete_account" },
                    singleLine = true,
                    enabled = !isDeleting,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorSurfaceInput,
                        unfocusedContainerColor = ColorSurfaceInput,
                        focusedBorderColor = if (isConfirmed) ColorRose else ColorBorder,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary
                    )
                )

                if (isDeleting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = ColorRose,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.lgpd_deleting_progress),
                            fontSize = 12.sp,
                            color = ColorTextMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = isConfirmed && !isDeleting,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_confirm_delete_account_final"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorRose,
                    disabledContainerColor = ColorRose.copy(alpha = 0.3f),
                    disabledContentColor = ColorTextMuted
                )
            ) {
                Text(
                    text = stringResource(R.string.lgpd_delete_btn_confirm),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    color = ColorTextMuted
                )
            }
        }
    )
}
