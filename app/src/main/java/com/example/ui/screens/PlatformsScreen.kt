package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Platform
import com.example.ui.GiroCustoViewModel

data class LocalCycleEntry(val cut: String, val payDelay: String)

fun parseCycleEntries(json: String): List<LocalCycleEntry> {
    if (json.isBlank()) return listOf(LocalCycleEntry("1", "7"), LocalCycleEntry("16", "7"))
    return try {
        json.split(",").mapNotNull { entryStr ->
            val parts = entryStr.split(":")
            if (parts.size == 2) {
                val cut = parts[0]
                val payDelay = parts[1]
                LocalCycleEntry(cut, payDelay)
            } else null
        }
    } catch (e: Exception) {
        listOf(LocalCycleEntry("1", "7"), LocalCycleEntry("16", "7"))
    }
}

fun formatCycleEntries(entries: List<LocalCycleEntry>): String {
    return entries.joinToString(",") { 
        val cutVal = it.cut.toIntOrNull()?.coerceIn(1, 28) ?: 1
        val delayVal = it.payDelay.toIntOrNull()?.coerceAtLeast(1) ?: 7
        "$cutVal:$delayVal"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformsScreen(viewModel: GiroCustoViewModel) {
    val platforms by viewModel.allPlatforms.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigation state within the screen:
    // null -> listing platforms
    // -1L -> adding a new platform
    // platformId -> editing existing platform
    var editingPlatformId by remember { mutableStateOf<Long?>(null) }

    AnimatedContent(
        targetState = editingPlatformId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "platforms_screen_transition"
    ) { currentId ->
        if (currentId == null) {
            // LIST VIEW
            PlatformListSection(
                platforms = platforms,
                onAddClick = { editingPlatformId = -1L },
                onEditClick = { id -> editingPlatformId = id }
            )
        } else {
            // FORM VIEW (ADD OR EDIT)
            val isEdit = currentId != -1L
            val platformToEdit = if (isEdit) {
                platforms.find { it.id == currentId } ?: Platform(name = "")
            } else {
                Platform(name = "")
            }

            PlatformFormSection(
                platform = platformToEdit,
                isEdit = isEdit,
                onBack = { editingPlatformId = null },
                onSave = { updatedPlatform ->
                    viewModel.savePlatform(updatedPlatform)
                    Toast.makeText(
                        context,
                        if (isEdit) "Plataforma atualizada com sucesso!" else "Plataforma vinculada com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    editingPlatformId = null
                },
                onDelete = {
                    viewModel.deletePlatform(platformToEdit)
                    Toast.makeText(context, "Plataforma removida.", Toast.LENGTH_SHORT).show()
                    editingPlatformId = null
                }
            )
        }
    }
}

@Composable
fun PlatformListSection(
    platforms: List<Platform>,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Plataformas Ativas",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_platform_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF064E3B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar", tint = Color(0xFF064E3B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nova", fontWeight = FontWeight.Bold)
                }
            }

            if (platforms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Nenhuma plataforma cadastrada",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Cadastre as plataformas de delivery ou logística que você trabalha.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(platforms) { platform ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditClick(platform.id) }
                                .testTag("platform_item_${platform.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2D2D34))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = platform.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Badge(
                                            containerColor = if (platform.segment == "logistica") Color(0xFF3B82F6) else Color(0xFFEF4444)
                                        ) {
                                            Text(
                                                text = if (platform.segment == "logistica") "Logística" else "Delivery",
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Badge(
                                            containerColor = if (platform.paymentModel == "producao") Color(0xFF8B5CF6) else Color(0xFFF59E0B)
                                        ) {
                                            Text(
                                                text = if (platform.paymentModel == "producao") "Produção" else "Diária",
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Ciclo: ${platform.cycle.uppercase()}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        if (platform.cycle == "semanal") {
                                            Text(
                                                text = "Fechamento: ${platform.paymentDay} | Prazo: ${platform.fixedPayDelay} dias",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        } else if (platform.cycle == "quinzenal" || platform.cycle == "mensal") {
                                            Text(
                                                text = "Prazo: ${platform.fixedPayDelay} dias após fechamento",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        } else {
                                            val entriesCount = parseCycleEntries(platform.cycleEntriesJson).size
                                            Text(
                                                text = "$entriesCount ciclo(s) variável(is) cadastrado(s)",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Editar",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
    val weekDays = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")

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

    var pixExpanded by remember { mutableStateOf(false) }

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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text(
                    text = if (isEdit) "Editar Plataforma" else "Nova Plataforma",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (isEdit) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .size(36.dp)
                        .testTag("delete_platform_btn")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444))
                }
            }
        }

        // FORM FIELDS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // NAME
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nome da plataforma", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
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
                    Text("Segmento de Operação", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
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
                                containerColor = if (segment == "logistica") MaterialTheme.colorScheme.primary else Color(0xFF2D2D34),
                                contentColor = if (segment == "logistica") MaterialTheme.colorScheme.onPrimary else Color(0xFF94A3B8)
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
                                containerColor = if (segment == "delivery") MaterialTheme.colorScheme.primary else Color(0xFF2D2D34),
                                contentColor = if (segment == "delivery") MaterialTheme.colorScheme.onPrimary else Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delivery", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // PAYMENT MODEL
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Modelo de Pagamento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
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
                                containerColor = if (paymentModel == "producao") MaterialTheme.colorScheme.primary else Color(0xFF2D2D34),
                                contentColor = if (paymentModel == "producao") MaterialTheme.colorScheme.onPrimary else Color(0xFF94A3B8)
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
                                containerColor = if (paymentModel == "diaria") MaterialTheme.colorScheme.primary else Color(0xFF2D2D34),
                                contentColor = if (paymentModel == "diaria") MaterialTheme.colorScheme.onPrimary else Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Diária (Fixo)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // CYCLE
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ciclo de pagamento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val cycles = listOf("semanal", "quinzenal", "mensal", "misto")
                        cycles.forEach { c ->
                            Button(
                                onClick = { cycle = c },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cycle_${c}_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cycle == c) MaterialTheme.colorScheme.primary else Color(0xFF2D2D34),
                                    contentColor = if (cycle == c) MaterialTheme.colorScheme.onPrimary else Color(0xFF94A3B8)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (c == "misto") "VARIÁVEL" else c.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // CONDITIONAL VIEWS BASED ON CYCLE
                AnimatedVisibility(visible = cycle == "semanal") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Dia de fechamento semanal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                weekDays.forEach { d ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .background(
                                                if (paymentDay == d) MaterialTheme.colorScheme.primary else Color(0xFF2D2D34),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { paymentDay = d }
                                            .testTag("weekday_${d}_btn"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = d,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (paymentDay == d) MaterialTheme.colorScheme.onPrimary else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Prazo de pagamento (dias após fechamento)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                TextField(
                                    value = fixedPayDelay,
                                    onValueChange = { fixedPayDelay = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("fixed_pay_delay_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                Text("dias", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            }
                            Text(
                                text = "Ex: se fechar toda $paymentDay e o prazo for ${fixedPayDelay.ifBlank { "0" }} dias, o pagamento cai ${fixedPayDelay.ifBlank { "0" }} dia(s) após o fechamento.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = cycle == "quinzenal" || cycle == "mensal") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Prazo de pagamento (dias após fechamento do ciclo)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            TextField(
                                value = fixedPayDelay,
                                onValueChange = { fixedPayDelay = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fixed_pay_delay_input_other"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Text("dias", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        }
                        Text(
                            text = "Quantos dias após o fechamento do ciclo ($cycle) o pagamento é realizado.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                AnimatedVisibility(visible = cycle == "misto") {
                    Column(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF2D2D34), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ciclos de pagamento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Button(
                                onClick = addEntry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+ Adicionar ciclo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "Configure cada ciclo: o dia em que fecha e quantos dias depois o pagamento é realizado.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            cycleEntries.forEachIndexed { idx, entry ->
                                Column(
                                    modifier = Modifier
                                        .background(Color(0xFF131316), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF2D2D34), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Ciclo ${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        if (cycleEntries.size > 1) {
                                            IconButton(
                                                onClick = { removeEntry(idx) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Remover", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // FECHAMENTO
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Filled.ContentCut, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                                Text("Fechamento", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("Dia", fontSize = 11.sp, color = Color(0xFF64748B))
                                                TextField(
                                                    value = entry.cut,
                                                    onValueChange = { s ->
                                                        updateEntry(idx, "cut", s)
                                                    },
                                                    modifier = Modifier
                                                        .width(55.dp)
                                                        .testTag("cycle_cut_input_$idx"),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent
                                                    )
                                                )
                                                Text("do mês", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                        }

                                        // PAGAMENTO
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                                Text("Pagamento", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                TextField(
                                                    value = entry.payDelay,
                                                    onValueChange = { s ->
                                                        updateEntry(idx, "payDelay", s)
                                                    },
                                                    modifier = Modifier
                                                        .width(55.dp)
                                                        .testTag("cycle_pay_delay_input_$idx"),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent
                                                    )
                                                )
                                                Text("dias depois", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                    }

                                    // HELP TEXT FOR CYCLE
                                    val cutValInt = entry.cut.toIntOrNull() ?: 1
                                    val delayValInt = entry.payDelay.toIntOrNull() ?: 0
                                    val payDayText = if (cutValInt + delayValInt > 28) {
                                        "${cutValInt + delayValInt - 28} do mês seguinte"
                                    } else {
                                        "${cutValInt + delayValInt}"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Fecha dia ${entry.cut} → paga dia $payDayText",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BANKING CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Dados de recebimento", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Instituição financeira", fontSize = 13.sp, color = Color(0xFF94A3B8))
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
                        Text("Agência", fontSize = 13.sp, color = Color(0xFF94A3B8))
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
                        Text("Conta", fontSize = 13.sp, color = Color(0xFF94A3B8))
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

        // PIX CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Chave PIX", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PIX TYPE DROPDOWN
                    Box(modifier = Modifier.width(115.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D34), RoundedCornerShape(8.dp))
                                .clickable { pixExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("pix_type_selector"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pixKeyType, color = Color.White, fontSize = 14.sp)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
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

                    // PIX KEY INPUT
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
                containerColor = Color(0xFF10B981),
                contentColor = Color(0xFF064E3B)
            )
        ) {
            Icon(Icons.Filled.Save, contentDescription = null, tint = Color(0xFF064E3B))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isEdit) "SALVAR ALTERAÇÕES" else "VINCULAR PLATAFORMA",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
