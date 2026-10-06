// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.navigation

import androidx.annotation.StringRes
import com.compass.app.R
import kotlinx.serialization.Serializable

@Serializable
data object CompassRoute

@Serializable
data object ToolsRoute

@Serializable
data object SettingsRoute

data class TopLevelRoute<T : Any>(
    val route: T,
    @StringRes val label: Int,
    val icon: Int
)

val TopLevelDestination = listOf(
    TopLevelRoute(CompassRoute, R.string.tab_compass, R.drawable.ic_compass),
    TopLevelRoute(ToolsRoute, R.string.tools, R.drawable.ic_ruler),
    TopLevelRoute(SettingsRoute, R.string.settings, R.drawable.ic_settings),
)
