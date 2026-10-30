package com.example.ui.screens.fuel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.ConsumptionAlertLevel
import com.example.ui.FuelConsumptionComparisonState
import java.util.Locale

@Composable
fun FuelComparisonCard(
    fuelComparison: FuelConsumptionComparisonState,
    modifier: Modifier = Modifier
) {
    val (statusColor, containerBg, badgeBg, badgeTextColor, statusLabel, icon) = when (fuelComparison.alertLevel) {
        ConsumptionAlertLevel.GREEN -> StatusStyle(
            borderColor = MaterialTheme.colorScheme.tertiary,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
            badgeTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
            label = stringResource(R.string.fuel_comparison_status_excellent),
            icon = Icons.Filled.CheckCircle
        )
        ConsumptionAlertLevel.AMBER -> StatusStyle(
            borderColor = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            badgeColor = MaterialTheme.colorScheme.secondaryContainer,
            badgeTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            label = stringResource(R.string.fuel_comparison_status_moderate),
            icon = Icons.Filled.Warning
        )
        ConsumptionAlertLevel.RED -> StatusStyle(
            borderColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            badgeColor = MaterialTheme.colorScheme.errorContainer,
            badgeTextColor = MaterialTheme.colorScheme.onErrorContainer,
            label = stringResource(R.string.fuel_comparison_status_high),
            icon = Icons.Filled.Error
        )
        ConsumptionAlertLevel.GRAY -> StatusStyle(
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            badgeColor = MaterialTheme.colorScheme.surfaceVariant,
            badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            label = stringResource(R.string.fuel_comparison_status_collecting),
            icon = Icons.Filled.Info
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fuel_consumption_comparison_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.fuel_comparison_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("efficiency_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }

            // Comparison row with two boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cadastrado (Teórico)
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.fuel_comparison_registered_label),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.fuel_format_km_l_one_decimal, fuelComparison.theoreticalConsumption),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("theoretical_consumption_text")
                        )
                    }
                }

                // Consumo Real
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.fuel_comparison_real_label),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val realText = if (fuelComparison.hasSufficientData && fuelComparison.realConsumption != null) {
                            stringResource(R.string.fuel_format_km_l_one_decimal, fuelComparison.realConsumption)
                        } else {
                            stringResource(R.string.fuel_comparison_no_data_km_l)
                        }
                        Text(
                            text = realText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fuelComparison.hasSufficientData) statusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("real_consumption_text")
                        )
                    }
                }
            }

            // Footer explanation & percentage difference
            if (fuelComparison.hasSufficientData && fuelComparison.differencePercentage != null) {
                val diff = fuelComparison.differencePercentage
                val diffFormatted = String.format(Locale.getDefault(), "%.1f", Math.abs(diff))
                val diffText = if (diff >= 0) {
                    stringResource(R.string.fuel_comparison_diff_more_efficient, diffFormatted)
                } else {
                    stringResource(R.string.fuel_comparison_diff_vs_registered, diffFormatted)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fuelComparison.message,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = diffText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.testTag("consumption_diff_text")
                    )
                }
            } else {
                Text(
                    text = fuelComparison.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class StatusStyle(
    val borderColor: Color,
    val containerColor: Color,
    val badgeColor: Color,
    val badgeTextColor: Color,
    val label: String,
    val icon: ImageVector
)
