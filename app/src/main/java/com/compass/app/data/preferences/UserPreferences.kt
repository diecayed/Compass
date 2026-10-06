// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.data.preferences

data class UserPreferences(
    val theme: String,
    val isTrueDarkThemeEnabled: Boolean = false,
    val isTrueNorthEnabled: Boolean = false,
    /** Show decimals on the level readings, with smoother updates. On by default. */
    val isHighPrecisionEnabled: Boolean = true,
    /** One of the [com.compass.app.utils.HapticStrength] pref names. */
    val hapticStrength: String = "default",
) {
    companion object {
        const val KEY_THEME = "theme"
        const val TRUE_DARK = "true_dark"
        const val TRUE_NORTH = "true_north"
        const val HIGH_PRECISION = "high_precision"
        const val HAPTIC_STRENGTH = "haptic_strength"
    }
}
