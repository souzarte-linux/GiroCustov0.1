package com.example.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.fipe.FipeDetailDto
import com.example.network.fipe.FipeItemDto
import com.example.ui.FipeUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileVehicleFipeSelector(
    selectedBrand: FipeItemDto?,
    selectedModel: FipeItemDto?,
    selectedYear: FipeItemDto?,
    manualBrand: String,
    manualModel: String,
    manualYear: String,
    fipeMarcas: List<FipeItemDto>,
    fipeModelos: List<FipeItemDto>,
    fipeAnos: List<FipeItemDto>,
    fipeDetail: FipeDetailDto?,
    fipeUiState: FipeUiState,
    onSelectBrand: (FipeItemDto) -> Unit,
    onSelectModel: (FipeItemDto) -> Unit,
    onSelectYear: (FipeItemDto) -> Unit
) {
    var brandExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Catálogo Tabela FIPE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ColorBrandEmerald,
            letterSpacing = 1.sp
        )

        // 1. Marca FIPE
        ExposedDropdownMenuBox(
            expanded = brandExpanded,
            onExpandedChange = { brandExpanded = !brandExpanded }
        ) {
            OutlinedTextField(
                value = selectedBrand?.nome.orEmpty().ifBlank { manualBrand },
                onValueChange = {},
                readOnly = true,
                label = { Text("Marca (FIPE)") },
                placeholder = { Text("Selecione a Marca", color = ColorTextMuted) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("vehicle_fipe_brand_select")
            )

            ExposedDropdownMenu(
                expanded = brandExpanded,
                onDismissRequest = { brandExpanded = false },
                modifier = Modifier.background(ColorSurfaceElevated)
            ) {
                if (fipeMarcas.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Carregando marcas...", color = ColorTextMuted) },
                        onClick = {}
                    )
                } else {
                    fipeMarcas.forEach { marca ->
                        DropdownMenuItem(
                            text = { Text(marca.nome, color = ColorTextPrimary) },
                            onClick = {
                                brandExpanded = false
                                onSelectBrand(marca)
                            }
                        )
                    }
                }
            }
        }

        // 2. Modelo FIPE
        ExposedDropdownMenuBox(
            expanded = modelExpanded,
            onExpandedChange = {
                if (selectedBrand != null || fipeModelos.isNotEmpty()) {
                    modelExpanded = !modelExpanded
                }
            }
        ) {
            OutlinedTextField(
                value = selectedModel?.nome.orEmpty().ifBlank { manualModel },
                onValueChange = {},
                readOnly = true,
                enabled = selectedBrand != null || fipeModelos.isNotEmpty(),
                label = { Text("Modelo (FIPE)") },
                placeholder = { Text("Selecione o Modelo", color = ColorTextMuted) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("vehicle_fipe_model_select")
            )

            ExposedDropdownMenu(
                expanded = modelExpanded,
                onDismissRequest = { modelExpanded = false },
                modifier = Modifier.background(ColorSurfaceElevated)
            ) {
                if (fipeModelos.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Carregando modelos...", color = ColorTextMuted) },
                        onClick = {}
                    )
                } else {
                    fipeModelos.forEach { modelo ->
                        DropdownMenuItem(
                            text = { Text(modelo.nome, color = ColorTextPrimary) },
                            onClick = {
                                modelExpanded = false
                                onSelectModel(modelo)
                            }
                        )
                    }
                }
            }
        }

        // 3. Ano / Versão FIPE
        ExposedDropdownMenuBox(
            expanded = yearExpanded,
            onExpandedChange = {
                if (selectedModel != null || fipeAnos.isNotEmpty()) {
                    yearExpanded = !yearExpanded
                }
            }
        ) {
            OutlinedTextField(
                value = selectedYear?.nome.orEmpty().ifBlank { manualYear },
                onValueChange = {},
                readOnly = true,
                enabled = selectedModel != null || fipeAnos.isNotEmpty(),
                label = { Text("Ano Modelo / Versão (FIPE)") },
                placeholder = { Text("Selecione o Ano", color = ColorTextMuted) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("vehicle_fipe_year_select")
            )

            ExposedDropdownMenu(
                expanded = yearExpanded,
                onDismissRequest = { yearExpanded = false },
                modifier = Modifier.background(ColorSurfaceElevated)
            ) {
                if (fipeAnos.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Carregando anos...", color = ColorTextMuted) },
                        onClick = {}
                    )
                } else {
                    fipeAnos.forEach { ano ->
                        DropdownMenuItem(
                            text = { Text(ano.nome, color = ColorTextPrimary) },
                            onClick = {
                                yearExpanded = false
                                onSelectYear(ano)
                            }
                        )
                    }
                }
            }
        }

        // Card de Resumo FIPE
        if (fipeDetail != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ColorSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ColorBrandEmerald.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Valor Tabela FIPE",
                            fontSize = 11.sp,
                            color = ColorBrandEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = fipeDetail.valor.orEmpty(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "FIPE: ${fipeDetail.codigoFipe}",
                            fontSize = 11.sp,
                            color = ColorTextMuted
                        )
                        Text(
                            text = "Ref: ${fipeDetail.mesReferencia}",
                            fontSize = 11.sp,
                            color = ColorTextMuted
                        )
                    }
                }
            }
        } else if (fipeUiState is FipeUiState.Loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ColorBrandEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Consultando base oficial FIPE...", fontSize = 12.sp, color = ColorTextMuted)
            }
        } else if (fipeUiState is FipeUiState.Error) {
            Text(
                text = (fipeUiState as FipeUiState.Error).message,
                fontSize = 12.sp,
                color = ColorRose
            )
        }
    }
}
