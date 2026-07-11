package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DrawerDestination {
    NONE,
    VEICULO,
    PERFIL,
    PLATAFORMAS,
    ABASTECIMENTOS,
    MANUTENCAO
}

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    selectedDestination: DrawerDestination,
    onDestinationSelected: (DrawerDestination) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(280.dp)
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
                    icon = { Icon(Icons.Filled.TwoWheeler, contentDescription = "Veículo") },
                    label = { Text("Veículo") },
                    selected = selectedDestination == DrawerDestination.VEICULO,
                    onClick = { onDestinationSelected(DrawerDestination.VEICULO) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_vehicle"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
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
            }
        },
        content = content
    )
}
