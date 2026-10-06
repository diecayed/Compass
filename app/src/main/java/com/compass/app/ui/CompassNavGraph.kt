// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.compass.app.core.sensors.SensorViewModel
import com.compass.app.features.compass.CompassScreen
import com.compass.app.features.settings.SettingsScreen
import com.compass.app.features.tools.Tool
import com.compass.app.features.tools.ToolsScreen
import com.compass.app.navigation.CompassRoute
import com.compass.app.navigation.SettingsRoute
import com.compass.app.navigation.ToolsRoute

@Composable
fun CompassNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sensorViewModel: SensorViewModel,
    selectedTool: Tool,
) {
    NavHost(
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        navController = navController,
        startDestination = CompassRoute
    ) {
        composable<CompassRoute> {
            CompassScreen(sensorViewModel = sensorViewModel)
        }

        composable<ToolsRoute>(
            enterTransition = {
                fadeThroughEnter()
            }, exitTransition = {
                fadeThroughExit()
            }, popEnterTransition = {
                fadeThroughEnter()
            }, popExitTransition = {
                fadeThroughExit()
            }) {
            ToolsScreen(tool = selectedTool)
        }

        composable<SettingsRoute>(
            enterTransition = {
                fadeThroughEnter()
            }, exitTransition = {
                fadeThroughExit()
            }, popEnterTransition = {
                fadeThroughEnter()
            }, popExitTransition = {
                fadeThroughExit()
            }) {
            SettingsScreen(sensorViewModel = sensorViewModel)
        }
    }
}

fun NavController.navigateWithBackStack(route: Any) {
    navigate(route) {
        popUpTo(this@navigateWithBackStack.graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when
        // reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

// `fadeThrough` transition is recommended for screens that aren't related:
// https://m3.material.io/styles/motion/transitions/transition-patterns#f852afd2-396f-49fd-a265-5f6d96680e16

fun fadeThroughEnter(): EnterTransition =
    fadeIn(
        initialAlpha = 0.4f,
        animationSpec = tween(durationMillis = 300)
    )

fun fadeThroughExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = 250
        )
    )
