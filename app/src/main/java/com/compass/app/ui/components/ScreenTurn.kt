// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.ui.components

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * How far to turn something on screen so it reads upright for someone holding the phone in a
 * different orientation. The screen itself stays in portrait.
 *
 * @property angle clockwise rotation in degrees, animated; read it inside a layer or layout block
 * @property sideways true while the phone is held in landscape
 */
class ScreenTurn(private val angleState: State<Float>, val sideways: Boolean) {
    val angle: Float get() = angleState.value
}

/**
 * Follows how the phone is held, in quarter turns, even though the app stays in portrait. Camera
 * apps work the same way: the layout stays put and the controls turn. While [frozen] nothing turns.
 */
@Composable
fun rememberScreenTurn(frozen: Boolean = false): ScreenTurn {
    val context = LocalContext.current
    // quarter turns the phone has been rotated clockwise, counted without wrapping so the
    // animation always takes the short way round
    var turns by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        var quarter = 0
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN) return
                val nearest = ((degrees + 45) / 90) % 4
                if (nearest == quarter) return
                // only switch once the phone is clearly past the halfway mark
                val offCentre = abs(((degrees - nearest * 90 + 540) % 360) - 180)
                if (offCentre > SWITCH_MARGIN_DEGREES) return
                val step = (nearest - quarter + 4) % 4
                turns += if (step == 3) -1 else step
                quarter = nearest
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }

    val angle = animateFloatAsState(
        targetValue = if (frozen) 0f else -90f * turns,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "screenTurn",
    )
    val sideways = !frozen && turns % 2 != 0
    return remember(sideways) { ScreenTurn(angle, sideways) }
}

/** True while the phone's auto-rotate setting is on, which is when its rotation lock is off. */
@Composable
fun rememberAutoRotateEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isAutoRotateOn(context)) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = isAutoRotateOn(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}

private fun isAutoRotateOn(context: Context): Boolean =
    Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

private const val SWITCH_MARGIN_DEGREES = 30
