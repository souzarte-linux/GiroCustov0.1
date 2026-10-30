package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

enum class DrawerDestination {
    NONE,
    PERFIL,
    PLATAFORMAS,
    ABASTECIMENTOS,
    MANUTENCAO,
    NUVEM,
    TEMAS
}

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    selectedDestination: DrawerDestination,
    onDestinationSelected: (DrawerDestination) -> Unit,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "GiroCusto",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil & Veículos") },
                    label = { Text("Perfil & Veículos") },
                    selected = selectedDestination == DrawerDestination.PERFIL,
                    onClick = { onDestinationSelected(DrawerDestination.PERFIL) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_profile"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Layers, contentDescription = "Plataformas") },
                    label = { Text("Plataformas") },
                    selected = selectedDestination == DrawerDestination.PLATAFORMAS,
                    onClick = { onDestinationSelected(DrawerDestination.PLATAFORMAS) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_platforms"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.LocalGasStation, contentDescription = "Abastecimentos") },
                    label = { Text("Abastecimentos") },
                    selected = selectedDestination == DrawerDestination.ABASTECIMENTOS,
                    onClick = { onDestinationSelected(DrawerDestination.ABASTECIMENTOS) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_refills"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Build, contentDescription = "Manutenção") },
                    label = { Text("Manutenção") },
                    selected = selectedDestination == DrawerDestination.MANUTENCAO,
                    onClick = { onDestinationSelected(DrawerDestination.MANUTENCAO) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_maintenance"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)

                Text(
                    text = stringResource(R.string.drawer_section_settings_appearance),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                    letterSpacing = 1.sp
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Palette, contentDescription = stringResource(R.string.theme_drawer_item)) },
                    label = { Text(stringResource(R.string.theme_drawer_item)) },
                    selected = selectedDestination == DrawerDestination.TEMAS,
                    onClick = { onDestinationSelected(DrawerDestination.TEMAS) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_themes"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Cloud, contentDescription = "Nuvem & Backup") },
                    label = { Text("Nuvem & Backup") },
                    selected = selectedDestination == DrawerDestination.NUVEM,
                    onClick = { onDestinationSelected(DrawerDestination.NUVEM) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_cloud_sync"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        },
        content = content
    )
}
