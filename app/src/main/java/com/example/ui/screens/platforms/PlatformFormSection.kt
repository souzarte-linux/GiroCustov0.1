package com.example.ui.screens.platforms

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Platform

@Composable
fun PlatformFormSection(
    platform: Platform,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: (Platform) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isExisting = isEdit && platform.id > 0L
    var isEditing by rememberSaveable { mutableStateOf(!isExisting) }

    var isGeneralExpanded by rememberSaveable { mutableStateOf(true) }
    var isBankingExpanded by rememberSaveable { mutableStateOf(false) }
    var isPixExpanded by rememberSaveable { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var name by remember(platform.name) { mutableStateOf(platform.name) }
    var segment by remember(platform.segment) { mutableStateOf(platform.segment) }
    var paymentModel by remember(platform.paymentModel) { mutableStateOf(platform.paymentModel) }
    var cycle by remember(platform.cycle) { mutableStateOf(platform.cycle) }
    var paymentDay by remember(platform.paymentDay) { mutableStateOf(platform.paymentDay) }
    var fixedPayDelay by remember(platform.fixedPayDelay) { mutableStateOf(platform.fixedPayDelay.toString()) }
    var cycleEntries by remember(platform.cycleEntriesJson) { mutableStateOf(parseCycleEntries(platform.cycleEntriesJson)) }

    var bankName by remember(platform.bankName) { mutableStateOf(platform.bankName) }
    var bankAgency by remember(platform.bankAgency) { mutableStateOf(platform.bankAgency) }
    var bankAccount by remember(platform.bankAccount) { mutableStateOf(platform.bankAccount) }
    var pixKeyType by remember(platform.pixKeyType) { mutableStateOf(platform.pixKeyType) }
    var pixKey by remember(platform.pixKey) { mutableStateOf(platform.pixKey) }

    val onCancelEdit = {
        if (isExisting) {
            name = platform.name
            segment = platform.segment
            paymentModel = platform.paymentModel
            cycle = platform.cycle
            paymentDay = platform.paymentDay
            fixedPayDelay = platform.fixedPayDelay.toString()
            cycleEntries = parseCycleEntries(platform.cycleEntriesJson)
            bankName = platform.bankName
            bankAgency = platform.bankAgency
            bankAccount = platform.bankAccount
            pixKeyType = platform.pixKeyType
            pixKey = platform.pixKey
            isEditing = false
        } else {
            onBack()
        }
    }

    val addEntry = {
        cycleEntries = (cycleEntries + LocalCycleEntry("1", "7")).sortedBy { it.cut.toIntOrNull() ?: 1 }
    }
    val removeEntry = { idx: Int ->
        cycleEntries = cycleEntries.filterIndexed { i, _ -> i != idx }
    }
    val updateEntry = { idx: Int, field: String, value: String ->
        val cleanValue = value.filter { it.isDigit() }
        cycleEntries = cycleEntries.mapIndexed { i, e ->
            if (i == idx) {
                if (field == "cut") e.copy(cut = cleanValue)
                else e.copy(payDelay = cleanValue)
            } else e
        }
    }

    // DIÁLOGO DE CONFIRMAÇÃO DE EXCLUSÃO (Material 3)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_platform_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.dialog_delete_platform_message, platform.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("dialog_confirm_delete_platform_btn")
                ) {
                    Text(
                        text = stringResource(R.string.btn_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("dialog_cancel_delete_platform_btn")
                ) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // TOP HEADER: VIEW vs EDIT
        // =========================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = {
                        if (isEditing && isExisting) {
                            onCancelEdit()
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("form_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (!isEditing) {
                        name.ifBlank { stringResource(R.string.platforms_form_title_view) }
                    } else if (isExisting) {
                        stringResource(R.string.platforms_form_title_edit)
                    } else {
                        stringResource(R.string.platforms_form_title_new)
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isExisting) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isEditing) {
                        // Modo VIEW: Botão Lápis
                        IconButton(
                            onClick = { isEditing = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("platform_form_edit_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.cd_platform_edit_action),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    // Botão Lixeira
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .testTag("platform_form_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.cd_platform_delete_action),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SEÇÃO 1 RETRÁTIL: DADOS GERAIS
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PlatformSectionAccordionHeader(
                    title = stringResource(R.string.platform_section_general),
                    icon = Icons.Filled.Tune,
                    isExpanded = isGeneralExpanded,
                    onToggle = { isGeneralExpanded = !isGeneralExpanded },
                    modifier = Modifier.testTag("platform_accordion_header_general")
                )

                AnimatedVisibility(
                    visible = isGeneralExpanded,
                    enter = expandVertically(animationSpec = tween(250)) + fadeIn(tween(250)),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(200))
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!isEditing) {
                            PlatformGeneralViewContent(
                                name = name,
                                segment = segment,
                                paymentModel = paymentModel,
                                cycle = cycle,
                                paymentDay = paymentDay,
                                fixedPayDelay = fixedPayDelay,
                                cycleEntriesCount = cycleEntries.size
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Nome da plataforma", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("Digite o nome da plataforma") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("platform_name_input"),
                                    singleLine = true
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Segmento de Operação", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { segment = "logistica" },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("segment_logistica_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (segment == "logistica") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (segment == "logistica") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.platform_segment_logistics),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = { segment = "delivery" },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("segment_delivery_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (segment == "delivery") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (segment == "delivery") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.platform_segment_delivery),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Modelo de Pagamento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { paymentModel = "producao" },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("model_producao_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (paymentModel == "producao") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (paymentModel == "producao") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.platform_model_production_short),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = { paymentModel = "diaria" },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("model_diaria_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (paymentModel == "diaria") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (paymentModel == "diaria") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.platform_model_daily_short),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // CONFIGURAÇÃO DO CICLO DE PAGAMENTO
                            PlatformCycleConfigCard(
                                cycle = cycle,
                                onCycleChange = { cycle = it },
                                paymentDay = paymentDay,
                                onPaymentDayChange = { paymentDay = it },
                                fixedPayDelay = fixedPayDelay,
                                onFixedPayDelayChange = { fixedPayDelay = it },
                                cycleEntries = cycleEntries,
                                onAddEntry = addEntry,
                                onRemoveEntry = removeEntry,
                                onUpdateEntry = updateEntry
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SEÇÃO 2 RETRÁTIL: DADOS DE RECEBIMENTO
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PlatformSectionAccordionHeader(
                    title = stringResource(R.string.platform_section_banking),
                    icon = Icons.Filled.AccountBalance,
                    isExpanded = isBankingExpanded,
                    onToggle = { isBankingExpanded = !isBankingExpanded },
                    modifier = Modifier.testTag("platform_accordion_header_banking")
                )

                AnimatedVisibility(
                    visible = isBankingExpanded,
                    enter = expandVertically(animationSpec = tween(250)) + fadeIn(tween(250)),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(200))
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!isEditing) {
                            PlatformBankingViewContent(
                                bankName = bankName,
                                bankAgency = bankAgency,
                                bankAccount = bankAccount
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Instituição financeira", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextField(
                                    value = bankName,
                                    onValueChange = { bankName = it },
                                    placeholder = { Text("Ex: Nubank, Itaú…") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bank_name_input"),
                                    singleLine = true
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Agência", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextField(
                                        value = bankAgency,
                                        onValueChange = { bankAgency = it },
                                        placeholder = { Text("0001") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("bank_agency_input"),
                                        singleLine = true
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Conta", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextField(
                                        value = bankAccount,
                                        onValueChange = { bankAccount = it },
                                        placeholder = { Text("123456-7") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("bank_account_input"),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SEÇÃO 3 RETRÁTIL: CHAVE PIX
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PlatformSectionAccordionHeader(
                    title = stringResource(R.string.platform_section_pix),
                    icon = Icons.Filled.Key,
                    isExpanded = isPixExpanded,
                    onToggle = { isPixExpanded = !isPixExpanded },
                    modifier = Modifier.testTag("platform_accordion_header_pix")
                )

                AnimatedVisibility(
                    visible = isPixExpanded,
                    enter = expandVertically(animationSpec = tween(250)) + fadeIn(tween(250)),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(200))
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!isEditing) {
                            PlatformPixViewContent(
                                pixKeyType = pixKeyType,
                                pixKey = pixKey
                            )
                        } else {
                            var pixExpanded by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(115.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                            .clickable { pixExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 14.dp)
                                            .testTag("pix_type_selector"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(pixKeyType, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    DropdownMenu(
                                        expanded = pixExpanded,
                                        onDismissRequest = { pixExpanded = false }
                                    ) {
                                        listOf("CPF", "CNPJ", "E-mail", "Celular", "Aleatória").forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = {
                                                    pixKeyType = type
                                                    pixExpanded = false
                                                },
                                                modifier = Modifier.testTag("pix_option_$type")
                                            )
                                        }
                                    }
                                }

                                TextField(
                                    value = pixKey,
                                    onValueChange = { pixKey = it },
                                    placeholder = { Text("000.000.000-00") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pix_key_input"),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // =========================================================================
        // RODAPÉ: CANCELAR E SALVAR (Apenas no Modo EDIT)
        // =========================================================================
        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier
                        .weight(0.4f)
                        .heightIn(min = 48.dp)
                        .testTag("platform_form_cancel_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = stringResource(R.string.platforms_btn_cancel),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Por favor, insira o nome da plataforma.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val savedPlatform = Platform(
                            id = if (isExisting) platform.id else 0,
                            name = name.trim(),
                            segment = segment,
                            paymentModel = paymentModel,
                            cycle = cycle,
                            paymentDay = paymentDay,
                            fixedPayDelay = fixedPayDelay.toIntOrNull() ?: 7,
                            cycleEntriesJson = formatCycleEntries(cycleEntries),
                            bankName = bankName.trim(),
                            bankAgency = bankAgency.trim(),
                            bankAccount = bankAccount.trim(),
                            pixKeyType = pixKeyType,
                            pixKey = pixKey.trim(),
                            active = platform.active
                        )
                        onSave(savedPlatform)
                    },
                    modifier = Modifier
                        .weight(0.6f)
                        .heightIn(min = 48.dp)
                        .testTag("platform_save_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExisting) {
                            stringResource(R.string.platforms_btn_save_edit)
                        } else {
                            stringResource(R.string.platforms_btn_save_new)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
