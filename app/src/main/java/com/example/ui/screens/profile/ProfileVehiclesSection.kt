package com.example.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Vehicle
import com.example.util.CurrencyUtils

@Composable
internal fun MyVehiclesSection(
    allVehicles: List<Vehicle>,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (Long) -> Unit,
    onActivateVehicle: (Long) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header com Botão "+ Novo Veículo"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MEUS VEÍCULOS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorBrandEmerald,
                letterSpacing = 1.sp
            )

            Button(
                onClick = onAddVehicleClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorBrandEmerald,
                    contentColor = ColorEmeraldDark
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("profile_add_vehicle_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = ColorEmeraldDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Novo Veículo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (allVehicles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TwoWheeler,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Nenhum veículo cadastrado",
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Adicione sua moto ou carro com a Tabela FIPE para monitorar custos e desgastes.",
                            color = ColorTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            allVehicles.forEach { vehicle ->
                val isActive = vehicle.active
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_vehicle_item_${vehicle.id}")
                        .clickable { onEditVehicleClick(vehicle.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) ColorActiveVehicleCard else ColorSurfaceCard
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = if (isActive) ColorBorderActive else ColorBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Linha Superior: Ícone, Marca/Modelo/Ano, Badges, Botão Ativar / Badge Ativo e Ações
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isMoto = vehicle.vehicleType.equals("MOTO", ignoreCase = true)
                                Icon(
                                    imageVector = if (isMoto) Icons.Filled.TwoWheeler else Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (isActive) ColorBrandEmerald else ColorTextMuted,
                                    modifier = Modifier.size(22.dp)
                                )

                                Column {
                                    val displayName = buildString {
                                        if (vehicle.brand.isNotBlank()) append("${vehicle.brand} ")
                                        append(vehicle.model)
                                        if (vehicle.manufacturingYear.isNotBlank()) append(" (${vehicle.manufacturingYear})")
                                    }

                                    Text(
                                        text = displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = ColorTextPrimary
                                    )

                                    // Pílulas de Acessórios / Especificações
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (isMoto) {
                                            if (vehicle.hasTopBox) {
                                                Surface(
                                                    color = ColorSurfaceInput,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(10.dp), tint = ColorTextMuted)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Baú", fontSize = 10.sp, color = ColorTextMuted)
                                                    }
                                                }
                                            }
                                            if (vehicle.hasDeliveryBag) {
                                                Surface(
                                                    color = ColorSurfaceInput,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.Backpack, contentDescription = null, modifier = Modifier.size(10.dp), tint = ColorTextMuted)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Mochila", fontSize = 10.sp, color = ColorTextMuted)
                                                    }
                                                }
                                            }
                                        } else if (vehicle.carBodyType.isNotBlank()) {
                                            Surface(
                                                color = ColorSurfaceInput,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = vehicle.carBodyType,
                                                    fontSize = 10.sp,
                                                    color = ColorTextMuted,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Controle de Ativação e Ações
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isActive) {
                                    Surface(
                                        color = ColorBrandEmerald,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "ATIVO",
                                            color = ColorEmeraldDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onActivateVehicle(vehicle.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("profile_vehicle_activate_btn_${vehicle.id}")
                                    ) {
                                        Text("Ativar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorBrandEmerald)
                                    }
                                }

                                IconButton(
                                    onClick = { onEditVehicleClick(vehicle.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("profile_vehicle_edit_btn_${vehicle.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_vehicle), tint = ColorSky, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteVehicle(vehicle) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("profile_vehicle_delete_btn_${vehicle.id}")
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = stringResource(R.string.cd_delete_vehicle),
                                        tint = ColorRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Linha de Métricas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Consumo: ${vehicle.averageConsumption} km/L • ${vehicle.fuelType}",
                                    color = ColorTextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Tanque: ${vehicle.tankCapacity} L • Hodômetro: ${vehicle.currentOdometer.toLong()} km",
                                    color = ColorTextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = "${CurrencyUtils.formatCents(vehicle.monthlyFixedCosts)}/mês (${vehicle.plannedWorkDays}d)",
                                color = ColorTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
