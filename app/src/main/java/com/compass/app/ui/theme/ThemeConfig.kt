// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.ui.theme

enum class ThemeConfig(val prefName: String) {
    FOLLOW_SYSTEM("default"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPref(value: String?): ThemeConfig {
            return entries.find { it.prefName == value } ?: FOLLOW_SYSTEM
        }
    }
}
