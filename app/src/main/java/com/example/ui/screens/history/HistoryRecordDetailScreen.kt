package com.example.ui.screens.history

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DailyRecord
import com.example.ui.screens.formatHistoryCurrency
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRecordDetailScreen(
    record: DailyRecord,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isPrivacyEnabled: Boolean = false,
    routeName: String = stringResource(R.string.history_route_default_name)
) {
    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val sdfDisplay = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }
    val dateFormatted = remember(record) {
        try {
            if (record.dateTimestamp > 0L) sdfDisplay.format(Date(record.dateTimestamp)) else record.dateString
        } catch (_: Exception) {
            record.dateString
        }
    }

    val startOdoStr = remember(record.startOdometer) { HistoryRecordShareUtils.formatOdometer(record.startOdometer) }
    val endOdoStr = remember(record.endOdometer) { HistoryRecordShareUtils.formatOdometer(record.endOdometer) }
    val distStr = remember(record.kmRodados) { HistoryRecordShareUtils.formatOdometer(record.kmRodados) }

    val grossAmount = remember(record.grossEarnings) { CurrencyUtils.centsToDouble(record.grossEarnings) }
    val netAmount = remember(record.netProfit) { CurrencyUtils.centsToDouble(record.netProfit) }
    val isNetPositive = record.netProfit >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // TOPO DA TELA REESTRUTURADO (Requisitos 2.b e 2.c)
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("detail_screen_topbar"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // I) Na 1ª linha, centralizado o nome da empresa, seguido dos botões sem texto: Compartilhar e Editar
            // III) O nome da empresa centralizado deve estar envolto de um retângulo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botão Voltar (mínimo 48dp)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("detail_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back_from_detail),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Nome da empresa centralizado envolto em um retângulo
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("detail_company_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = record.platform.ifBlank { "GERAL" }.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Botões sem texto: Compartilhar e Editar agrupados à direita
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Botão Compartilhar (sem texto, no topo da tela)
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share_record),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Botão Editar (sem texto, no topo da tela)
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_record_view),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // II) Nome da rota (origem e destino)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = routeName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("detail_route_name")
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // ==========================================
        // CARD E: VALOR TOTAL DA ROTA (Requisito E)
        // Linha 1: ÍCONE + TÍTULO
        // Linha 2: LINHA
        // Linha 3: CONTEÚDO
        // ==========================================
        StandardDetailCard(
            icon = Icons.Filled.Payments,
            title = stringResource(R.string.history_card_route_total_value),
            testTag = "card_total_route_value"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatHistoryCurrency(grossAmount, isPrivacyEnabled),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("route_total_gross_value")
                )
                Text(
                    text = stringResource(R.string.history_gross_route_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ==========================================
        // CARD D: DISTÂNCIA PERCORRIDA (Requisito D)
        // Linha 1: ODÔMETRO INICIAL: XX.XXX "KM"
        // Linha 2: ODÔMETRO FINAL: XX.XXX "KM"
        // Linha 3: DIST. PERCORRIDO: XX.XXX "KM"
        // ==========================================
        StandardDetailCard(
            icon = Icons.Filled.DirectionsCar,
            title = stringResource(R.string.history_card_distance_traveled),
            testTag = "card_distance_traveled"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // I) Linha 1: ODÔMETRO INICIAL: XX.XXX "KM"
                Text(
                    text = stringResource(R.string.history_odo_initial_line, startOdoStr),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("odo_line_initial")
                )

                // II) Linha 2: ODÔMETRO FINAL: XX.XXX "KM"
                Text(
                    text = stringResource(R.string.history_odo_final_line, endOdoStr),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("odo_line_final")
                )

                // III) Linha 3: DIST. PERCORRIDO: XX.XXX "KM"
                Text(
                    text = stringResource(R.string.history_dist_traveled_line, distStr),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("odo_line_traveled")
                )
            }
        }

        // ==========================================
        // CARD: HORÁRIO DA JORNADA
        // ==========================================
        StandardDetailCard(
            icon = Icons.Filled.Schedule,
            title = stringResource(R.string.history_card_shift_time),
            testTag = "card_shift_time"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_shift_start_label),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.startTime.ifBlank { "--:--" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_shift_end_label),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.endTime.ifBlank { "--:--" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_shift_pause_label),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.pauseDuration.ifBlank { "00:00" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_shift_total_work_label),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.formattedWorkDuration,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ==========================================
        // CARD: ENTREGAS & PRODUTIVIDADE
        // ==========================================
        StandardDetailCard(
            icon = Icons.Filled.Inventory2,
            title = stringResource(R.string.history_card_deliveries_prod),
            testTag = "card_deliveries_prod"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_metric_total_deliveries),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${record.deliveriesCount} ent.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val hourlyRate = record.grossPerHour
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_metric_hourly_rate),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatHistoryCurrency(hourlyRate, isPrivacyEnabled)}/h",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val kmPerDel = if (record.deliveriesCount > 0) record.kmRodados / record.deliveriesCount else 0.0
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_metric_km_per_delivery),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "%.1f km", kmPerDel),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ==========================================
        // CARD: CUSTOS OPERACIONAIS
        // ==========================================
        StandardDetailCard(
            icon = Icons.Filled.Build,
            title = stringResource(R.string.history_card_operating_costs),
            testTag = "card_operating_costs"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.history_cost_fuel),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.fuelCost), isPrivacyEnabled),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.history_cost_wear),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.wearCost), isPrivacyEnabled),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.history_cost_food),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.foodExpense), isPrivacyEnabled),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.history_cost_fixed),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatHistoryCurrency(CurrencyUtils.centsToDouble(record.proportionalFixedCost), isPrivacyEnabled),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val totalCostsCents = record.fuelCost + record.wearCost + record.foodExpense + record.proportionalFixedCost
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.history_total_cost),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatHistoryCurrency(CurrencyUtils.centsToDouble(totalCostsCents), isPrivacyEnabled),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ==========================================
        // CARD: LUCRO LÍQUIDO
        // ==========================================
        StandardDetailCard(
            icon = Icons.Filled.Savings,
            title = stringResource(R.string.history_card_net_result),
            testTag = "card_net_result"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatHistoryCurrency(netAmount, isPrivacyEnabled),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isNetPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("route_net_value")
                )
                Text(
                    text = stringResource(R.string.history_net_route_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botão Excluir Registro na parte inferior
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("delete_record_btn_${record.id}"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_delete_confirm),
                fontWeight = FontWeight.Bold
            )
        }
    }

    // ==========================================
    // DIÁLOGO DE ESCOLHA DE FORMATO (IMAGEM OU PDF - Requisito 2.a)
    // ==========================================
    if (showShareDialog) {
        ShareFormatChoiceDialog(
            onDismiss = { showShareDialog = false },
            onSelectFormat = { format ->
                showShareDialog = false
                try {
                    val file = when (format) {
                        ShareFormat.IMAGE -> HistoryRecordShareUtils.generateRecordImage(
                            context = context,
                            record = record,
                            routeName = routeName,
                            isPrivacyEnabled = isPrivacyEnabled
                        )
                        ShareFormat.PDF -> HistoryRecordShareUtils.generateRecordPdf(
                            context = context,
                            record = record,
                            routeName = routeName,
                            isPrivacyEnabled = isPrivacyEnabled
                        )
                    }
                    val mime = if (format == ShareFormat.IMAGE) "image/png" else "application/pdf"
                    HistoryRecordShareUtils.shareFile(
                        context = context,
                        file = file,
                        mimeType = mime,
                        chooserTitle = context.getString(R.string.history_share_chooser_title)
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.history_share_error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * Componente padrão de card para a tela de visualização do registro
 * Padrão especificado:
 * Linha 1: ÍCONE + TÍTULO
 * Linha 2: LINHA
 * Linha 3: CONTEÚDO
 */
@Composable
fun StandardDetailCard(
    icon: ImageVector,
    title: String,
    testTag: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linha 1: ÍCONE + TÍTULO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Linha 2: LINHA
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Linha 3: CONTEÚDO
            content()
        }
    }
}

@Composable
fun ShareFormatChoiceDialog(
    onDismiss: () -> Unit,
    onSelectFormat: (ShareFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.history_share_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_share_dialog_subtitle),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Opção 1: Imagem (PNG)
                OutlinedButton(
                    onClick = { onSelectFormat(ShareFormat.IMAGE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_option_image"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.history_share_option_image))
                }

                // Opção 2: Documento (PDF)
                OutlinedButton(
                    onClick = { onSelectFormat(ShareFormat.PDF) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_option_pdf"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.history_share_option_pdf))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("share_cancel_btn")
            ) {
                Text(text = stringResource(R.string.btn_cancel))
            }
        },
        modifier = Modifier.testTag("share_format_dialog")
    )
}
