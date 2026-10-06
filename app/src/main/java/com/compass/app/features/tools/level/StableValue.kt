// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.abs

/** How often a precise reading is allowed to change on screen. */
const val PRECISE_INTERVAL_MS = 250L

/** How often a whole-degree reading is allowed to change on screen. */
const val NORMAL_INTERVAL_MS = 150L

/**
 * The value to show for a reading that changes constantly.
 *
 * The shown value only updates at a calm rhythm, and only when the new reading has moved clearly
 * away from it. That keeps the last decimal (or a number sitting on a rounding boundary) from
 * flickering with every bit of sensor noise.
 */
@Composable
fun rememberStableValue(target: Float, precise: Boolean): Float {
    val latest by rememberUpdatedState(target)
    var shown by remember { mutableFloatStateOf(target) }

    LaunchedEffect(precise) {
        val interval = if (precise) PRECISE_INTERVAL_MS else NORMAL_INTERVAL_MS
        while (true) {
            delay(interval)
            if (isClearChange(shown, latest, precise)) shown = latest
        }
    }
    return shown
}

/** A change is clear once it is larger than most of one displayed step. */
internal fun isClearChange(shown: Float, latest: Float, precise: Boolean): Boolean {
    val step = if (precise) 0.1f else 1f
    return abs(latest - shown) >= step * 0.6f
}
