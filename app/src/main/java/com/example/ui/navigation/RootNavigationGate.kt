package com.example.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.OnboardingProfileScreen

@Composable
fun RootNavigationGate(viewModel: GiroCustoViewModel) {
    val navState by viewModel.rootNavState.collectAsStateWithLifecycle()

    Crossfade(
        targetState = navState,
        animationSpec = tween(durationMillis = 350),
        label = "root_navigation_crossfade"
    ) { state ->
        when (state) {
            RootNavState.Initializing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF10B981),
                        strokeWidth = 3.dp
                    )
                }
            }
            RootNavState.Unauthenticated -> {
                AuthGateScreen(viewModel = viewModel)
            }
            RootNavState.OnboardingPending -> {
                OnboardingProfileScreen(viewModel = viewModel)
            }
            RootNavState.AuthenticatedReady -> {
                GiroCustoMainApp(viewModel = viewModel)
            }
        }
    }
}
