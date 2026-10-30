package com.example.ui.screens.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.FuelCostEstimationInfo
import java.util.Locale

@Composable
fun LaunchFuelEstimationCard(
    isDark: Boolean,
    fuelEstimationInfo: FuelCostEstimationInfo
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fuel_cost_estimation_info_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (fuelEstimationInfo.isUsingRealConsumption) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalGasStation,
                    contentDescription = null,
                    tint = if (fuelEstimationInfo.isUsingRealConsumption) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.launch_fuel_estimation_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = if (fuelEstimationInfo.isUsingRealConsumption) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (fuelEstimationInfo.isUsingRealConsumption) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.testTag("consumption_basis_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (fuelEstimationInfo.isUsingRealConsumption) Icons.Filled.CheckCircle else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (fuelEstimationInfo.isUsingRealConsumption) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (fuelEstimationInfo.isUsingRealConsumption) {
                            stringResource(R.string.launch_fuel_real_consumption, fuelEstimationInfo.effectiveConsumption)
                        } else {
                            stringResource(R.string.launch_fuel_registered_consumption, fuelEstimationInfo.effectiveConsumption)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (fuelEstimationInfo.isUsingRealConsumption) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.launch_fuel_estimation_message,
                    fuelEstimationInfo.distanceKm,
                    fuelEstimationInfo.vehicleModel.ifBlank { stringResource(R.string.launch_active_vehicle) }
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("fuel_cost_estimation_message")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (fuelEstimationInfo.isUsingRealConsumption) stringResource(R.string.launch_fuel_basis_real) else stringResource(R.string.launch_fuel_basis_registered),
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = String.format(Locale.GERMAN, "R$ %.2f", fuelEstimationInfo.estimatedFuelCost),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (fuelEstimationInfo.isUsingRealConsumption) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("fuel_cost_estimation_value")
                )
            }
        }
    }
}
