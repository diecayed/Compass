// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Formats a level reading in degrees: `3.4°` with decimals, or `3°` without, using the locale's
 * digits and decimal separator.
 */
fun formatLevelAngle(
    value: Float,
    precise: Boolean,
    locale: Locale = Locale.getDefault(),
): String = if (precise) {
    String.format(locale, "%.1f", value) + "°"
} else {
    String.format(locale, "%d", value.roundToInt()) + "°"
}
