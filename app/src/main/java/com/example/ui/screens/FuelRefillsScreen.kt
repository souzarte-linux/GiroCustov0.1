package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.FuelRefill
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.fuel.RefillFormSection
import com.example.ui.screens.fuel.RefillsListSection

@Composable
fun FuelRefillsScreen(viewModel: GiroCustoViewModel) {
    val refills by viewModel.fuelRefills.collectAsStateWithLifecycle()
    val realConsumption by viewModel.realAverageConsumption.collectAsStateWithLifecycle()
    val avgFuelPrice by viewModel.averageFuelPrice.collectAsStateWithLifecycle()
    val avgLiters by viewModel.averageLitersPerRefill.collectAsStateWithLifecycle()
    val activeVehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val fuelComparison by viewModel.fuelComparisonState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // null -> listing, -1L -> adding, value -> editing
    var editingRefillId by remember { mutableStateOf<Long?>(null) }

    if (activeVehicle == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.cd_fuel_alert_icon),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = stringResource(R.string.fuel_no_active_vehicle_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.fuel_no_active_vehicle_desc),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        return
    }

    AnimatedContent(
        targetState = editingRefillId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "refills_screen_transition"
    ) { currentId ->
        if (currentId == null) {
            RefillsListSection(
                refills = refills,
                realConsumption = realConsumption,
                avgFuelPrice = avgFuelPrice,
                avgLiters = avgLiters,
                fuelComparison = fuelComparison,
                onAddClick = { editingRefillId = -1L },
                onEditClick = { id -> editingRefillId = id }
            )
        } else {
            val isEdit = currentId != -1L
            val refillToEdit = if (isEdit) {
                refills.find { it.id == currentId } ?: FuelRefill(
                    vehicleId = activeVehicle!!.id,
                    dateTimestamp = System.currentTimeMillis(),
                    dateString = "",
                    gasStation = "",
                    fuelType = "Gasolina Comum",
                    pricePerLiter = 0L,
                    liters = 0.0,
                    totalPaid = 0L,
                    odometer = 0.0,
                    isFullTank = true,
                    paymentMethod = "PIX"
                )
            } else {
                FuelRefill(
                    vehicleId = activeVehicle!!.id,
                    dateTimestamp = System.currentTimeMillis(),
                    dateString = "",
                    gasStation = "",
                    fuelType = "Gasolina Comum",
                    pricePerLiter = 0L,
                    liters = 0.0,
                    totalPaid = 0L,
                    odometer = 0.0,
                    isFullTank = true,
                    paymentMethod = "PIX"
                )
            }

            val existingGasStations = remember(refills) {
                refills.map { it.gasStation }.distinct().filter { it.isNotBlank() }
            }

            RefillFormSection(
                refill = refillToEdit,
                isEdit = isEdit,
                existingGasStations = existingGasStations,
                viewModel = viewModel,
                onBack = { editingRefillId = null },
                onSave = { updatedRefill ->
                    viewModel.saveFuelRefill(updatedRefill)
                    Toast.makeText(
                        context,
                        context.getString(if (isEdit) R.string.refill_toast_updated else R.string.refill_toast_registered),
                        Toast.LENGTH_SHORT
                    ).show()
                    editingRefillId = null
                },
                onDelete = {
                    viewModel.deleteFuelRefill(refillToEdit)
                    Toast.makeText(context, context.getString(R.string.refill_toast_deleted), Toast.LENGTH_SHORT).show()
                    editingRefillId = null
                },
                onRenameGasStation = { oldName, newName ->
                    viewModel.renameGasStation(oldName, newName)
                }
            )
        }
    }
}
