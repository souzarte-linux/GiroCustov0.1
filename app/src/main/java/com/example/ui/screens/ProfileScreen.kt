package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.data.UserProfile
import com.example.ui.GiroCustoViewModel

@Composable
fun ProfileScreen(
    viewModel: GiroCustoViewModel,
    onNavigateToPlatforms: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val allPlatforms by viewModel.allPlatforms.collectAsStateWithLifecycle()

    val profile = userProfileState ?: UserProfile()

    // Form state variables reacting to profile database changes
    var name by remember(profile) { mutableStateOf(profile.name) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var city by remember(profile) { mutableStateOf(profile.city) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perfil do Usuário",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("theme_toggle_btn_profile")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Alternar Tema",
                    tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // PROFILE CARD: Edit details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dados Pessoais & Profissionais",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                )

                TextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                )

                TextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Cidade / Estado") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_city_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Plataformas Ativas",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )

                if (allPlatforms.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Nenhuma plataforma cadastrada.",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = onNavigateToPlatforms,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("profile_go_to_platforms_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cadastrar Plataformas", fontSize = 13.sp)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allPlatforms.forEach { platform ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = platform.active,
                                    onCheckedChange = { checked ->
                                        viewModel.savePlatform(platform.copy(active = checked))
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF10B981),
                                        uncheckedColor = Color(0xFF64748B),
                                        checkmarkColor = Color(0xFF064E3B)
                                    ),
                                    modifier = Modifier.testTag("profile_platform_checkbox_${platform.name.lowercase().replace(" ", "_")}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = platform.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${platform.segment.replaceFirstChar { it.uppercase() }} • ${platform.cycle.replaceFirstChar { it.uppercase() }}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save button
                Button(
                    onClick = {
                        viewModel.updateUserProfile(
                            name = name.trim(),
                            phone = phone.trim(),
                            city = city.trim()
                        )
                        Toast.makeText(context, "Perfil de usuário atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_save_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF064E3B)
                    )
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Salvar Perfil", tint = Color(0xFF064E3B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar Perfil", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                }
            }
        }

        // SYSTEM ACTIONS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D2D34)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ações do Sistema",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                
                Text(
                    text = "Gere dados fictícios para o mês de Junho de 2026 contendo exatamente 25 dias de trabalho (excluindo os Sábados). Ideal para validar relatórios, médias e o agrupamento hierárquico no histórico.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                Button(
                    onClick = {
                        viewModel.seedJune2026Data(
                            onSuccess = {
                                Toast.makeText(context, "Dados de Junho/2026 semeados com sucesso (25 dias sem Sábados)!", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Erro ao semear dados: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seed_june_data_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Semear Junho 2026")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Semear Dados de Junho/2026", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
