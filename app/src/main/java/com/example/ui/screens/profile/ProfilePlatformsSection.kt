package com.example.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Platform

@Composable
internal fun ActivePlatformsCard(
    allPlatforms: List<Platform>,
    onPlatformCheckChange: (Platform, Boolean) -> Unit,
    onNavigateToPlatforms: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header do Card
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
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = ColorBrandEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_platforms_card_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorBrandEmerald,
                        letterSpacing = 1.sp
                    )
                }

                TextButton(
                    onClick = onNavigateToPlatforms,
                    modifier = Modifier.testTag("profile_manage_platforms_btn")
                ) {
                    Text(
                        text = stringResource(R.string.profile_platforms_manage_btn),
                        color = ColorSky,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            if (allPlatforms.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_platforms_empty_message),
                        color = ColorTextMuted,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onNavigateToPlatforms,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorEmeraldDark,
                            contentColor = ColorEmeraldLight
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("profile_go_to_platforms_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.profile_platforms_register_btn), fontSize = 13.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allPlatforms.forEach { platform ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ColorSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ColorBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = platform.active,
                                    onCheckedChange = { checked ->
                                        onPlatformCheckChange(platform, checked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ColorBrandEmerald,
                                        uncheckedColor = Color(0xFF64748B),
                                        checkmarkColor = ColorEmeraldDark
                                    ),
                                    modifier = Modifier
                                        .testTag("profile_platform_checkbox_${platform.id}")
                                        .semantics {
                                            testTag = "profile_platform_checkbox_${platform.name.lowercase().replace(" ", "_")}"
                                        }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = platform.name,
                                        color = ColorTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = ColorSurfaceInput,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = platform.segment.replaceFirstChar { it.uppercase() },
                                                color = ColorTextMuted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            color = ColorSurfaceInput,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = platform.cycle.replaceFirstChar { it.uppercase() },
                                                color = ColorTextMuted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    }
}
