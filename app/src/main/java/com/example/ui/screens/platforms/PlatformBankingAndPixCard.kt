package com.example.ui.screens.platforms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlatformBankingCard(
    bankName: String,
    onBankNameChange: (String) -> Unit,
    bankAgency: String,
    onBankAgencyChange: (String) -> Unit,
    bankAccount: String,
    onBankAccountChange: (String) -> Unit
) {
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
                    onValueChange = onBankNameChange,
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
                        onValueChange = onBankAgencyChange,
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
                        onValueChange = onBankAccountChange,
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

@Composable
fun PlatformPixCard(
    pixKeyType: String,
    onPixKeyTypeChange: (String) -> Unit,
    pixKey: String,
    onPixKeyChange: (String) -> Unit
) {
    var pixExpanded by remember { mutableStateOf(false) }

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
                                    onPixKeyTypeChange(type)
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
                    onValueChange = onPixKeyChange,
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
