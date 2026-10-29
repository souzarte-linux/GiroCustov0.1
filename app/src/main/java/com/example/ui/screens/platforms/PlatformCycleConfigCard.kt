package com.example.ui.screens.platforms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlatformCycleConfigCard(
    cycle: String,
    onCycleChange: (String) -> Unit,
    paymentDay: String,
    onPaymentDayChange: (String) -> Unit,
    fixedPayDelay: String,
    onFixedPayDelayChange: (String) -> Unit,
    cycleEntries: List<LocalCycleEntry>,
    onAddEntry: () -> Unit,
    onRemoveEntry: (Int) -> Unit,
    onUpdateEntry: (Int, String, String) -> Unit
) {
    val weekDays = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // CYCLE SELECTOR
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Ciclo de pagamento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val cycles = listOf("semanal", "quinzenal", "mensal", "misto")
                cycles.forEach { c ->
                    Button(
                        onClick = { onCycleChange(c) },
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
                                    .clickable { onPaymentDayChange(d) }
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
                            onValueChange = onFixedPayDelayChange,
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
                        onValueChange = onFixedPayDelayChange,
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
                        onClick = onAddEntry,
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
                                        onClick = { onRemoveEntry(idx) },
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
                                            onValueChange = { s -> onUpdateEntry(idx, "cut", s) },
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
                                            onValueChange = { s -> onUpdateEntry(idx, "payDelay", s) },
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
