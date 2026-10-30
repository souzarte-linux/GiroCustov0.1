package com.example.ui.screens.platforms

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var name by remember { mutableStateOf(platform.name) }
    var segment by remember { mutableStateOf(platform.segment) }
    var paymentModel by remember { mutableStateOf(platform.paymentModel) }
    var cycle by remember { mutableStateOf(platform.cycle) }
    var paymentDay by remember { mutableStateOf(platform.paymentDay) }
    var fixedPayDelay by remember { mutableStateOf(platform.fixedPayDelay.toString()) }
    var cycleEntries by remember { mutableStateOf(parseCycleEntries(platform.cycleEntriesJson)) }

    var bankName by remember { mutableStateOf(platform.bankName) }
    var bankAgency by remember { mutableStateOf(platform.bankAgency) }
    var bankAccount by remember { mutableStateOf(platform.bankAccount) }
    var pixKeyType by remember { mutableStateOf(platform.pixKeyType) }
    var pixKey by remember { mutableStateOf(platform.pixKey) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("form_back_btn")
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = if (isEdit) androidx.compose.ui.res.stringResource(com.example.R.string.platforms_form_title_edit) else androidx.compose.ui.res.stringResource(com.example.R.string.platforms_form_title_new),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (isEdit) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .size(36.dp)
                        .testTag("delete_platform_btn")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // FORM FIELDS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // NAME
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

                // SEGMENT
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
                                .testTag("segment_logistica_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (segment == "logistica") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (segment == "logistica") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Logística", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { segment = "delivery" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("segment_delivery_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (segment == "delivery") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (segment == "delivery") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delivery", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // PAYMENT MODEL
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
                                .testTag("model_producao_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paymentModel == "producao") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (paymentModel == "producao") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Produção (Pacote)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { paymentModel = "diaria" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("model_diaria_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paymentModel == "diaria") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (paymentModel == "diaria") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Diária (Fixo)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // CYCLE CONFIGURATION
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

        // BANKING CARD
        PlatformBankingCard(
            bankName = bankName,
            onBankNameChange = { bankName = it },
            bankAgency = bankAgency,
            onBankAgencyChange = { bankAgency = it },
            bankAccount = bankAccount,
            onBankAccountChange = { bankAccount = it }
        )

        // PIX CARD
        PlatformPixCard(
            pixKeyType = pixKeyType,
            onPixKeyTypeChange = { pixKeyType = it },
            pixKey = pixKey,
            onPixKeyChange = { pixKey = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // SAVE / VINCULAR BUTTON
        Button(
            onClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "Por favor, insira o nome da plataforma.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val savedPlatform = Platform(
                    id = if (isEdit) platform.id else 0,
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
                .fillMaxWidth()
                .height(50.dp)
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
                text = if (isEdit) androidx.compose.ui.res.stringResource(com.example.R.string.platforms_btn_save_edit) else androidx.compose.ui.res.stringResource(com.example.R.string.platforms_btn_save_new),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
