// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.compass.app.core.sensors.SensorViewModel
import com.compass.app.features.settings.SettingsViewModel
import com.compass.app.features.tools.Tool
import com.compass.app.navigation.CompassRoute
import com.compass.app.navigation.ToolsRoute
import com.compass.app.navigation.TopLevelDestination
import com.compass.app.ui.components.CompassBottomBar
import com.compass.app.ui.components.FloatingBarMetrics
import com.compass.app.ui.components.LocalBottomBarInset

@Composable
fun CompassApp(
    navHostController: NavHostController = rememberNavController(),
    sensorViewModel: SensorViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()

    // Which tool the Tools tab is showing. Long-pressing the tab switches it.
    var selectedTool by rememberSaveable { mutableStateOf(Tool.LEVEL) }

    val navBackStackEntry by navHostController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute: Any = TopLevelDestination
        .firstOrNull { destination ->
            currentDestination?.hierarchy?.any {
                it.hasRoute(destination.route::class)
            } == true
        }?.route ?: CompassRoute

    // Leaving the Tools tab resets it: it always opens on the level.
    LaunchedEffect(currentRoute) {
        if (currentRoute != ToolsRoute) selectedTool = Tool.LEVEL
    }

    // The Tools tab is named after, and shows the icon of, the tool that is on screen.
    val destinations = remember(selectedTool) {
        TopLevelDestination.map { destination ->
            if (destination.route == ToolsRoute) {
                destination.copy(label = selectedTool.label, icon = selectedTool.icon)
            } else {
                destination
            }
        }
    }

    // Screens reserve this much room at the bottom so nothing hides behind the floating bar.
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = FloatingBarMetrics.ReservedHeight + navigationBarHeight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompositionLocalProvider(LocalBottomBarInset provides bottomInset) {
            CompassNavGraph(
                modifier = Modifier.fillMaxSize(),
                navController = navHostController,
                sensorViewModel = sensorViewModel,
                selectedTool = selectedTool
            )
        }

        // The menu floats above the content as an island.
        CompassBottomBar(
            destinations = destinations,
            currentRoute = currentRoute,
            navigateToRoute = { navHostController.navigateWithBackStack(it) },
            hapticStrength = settings.hapticStrength,
            onItemLongClick = { route ->
                // Long-pressing the Tools tab switches between the level and the ruler;
                // long-pressing the Compass tab opens the compass calibration dialog.
                if (route == ToolsRoute) {
                    selectedTool = selectedTool.next()
                    navHostController.navigateWithBackStack(ToolsRoute)
                }
                if (route == CompassRoute) {
                    navHostController.navigateWithBackStack(CompassRoute)
                    sensorViewModel.showAccuracyDialog()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = FloatingBarMetrics.BottomMargin),
        )
    }
}
