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
    viewModel: GiroCustoViewModel
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()

    val profile = userProfileState ?: UserProfile()

    // Form state variables reacting to profile database changes
    var name by remember(profile) { mutableStateOf(profile.name) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var city by remember(profile) { mutableStateOf(profile.city) }
    var platforms by remember(profile) { mutableStateOf(profile.platforms) }

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

                TextField(
                    value = platforms,
                    onValueChange = { platforms = it },
                    label = { Text("Plataformas Ativas (Separadas por vírgula)") },
                    placeholder = { Text("ex: iFood, Uber Flash, Rappi") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_platforms_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Work, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Save button
                Button(
                    onClick = {
                        viewModel.updateUserProfile(
                            name = name.trim(),
                            phone = phone.trim(),
                            city = city.trim(),
                            platforms = platforms.trim()
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
    }
}
