// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/** How strong the app's haptic feedback is. Chosen in Settings. */
enum class HapticStrength(val prefName: String) {
    OFF("off"),
    SOFT("soft"),
    DEFAULT("default"),
    POWERFUL("powerful");

    companion object {
        fun fromPref(value: String?): HapticStrength =
            entries.firstOrNull { it.prefName == value } ?: DEFAULT
    }
}

/** What the buzz is for. */
enum class HapticEvent {
    /** A short tick, such as the compass passing a mark. */
    TICK,

    /** A success, such as the level turning level. */
    SUCCESS,

    /** A confirmation, such as a long press being recognised. */
    CONFIRM,
}

/**
 * Plays haptic feedback at the chosen strength.
 *
 * - Off: nothing.
 * - Soft: the lightest tick the phone's vibration motor can make.
 * - Default: the system's own haptics, so it follows the phone's touch-feedback setting.
 * - Powerful: the strongest click the motor can make.
 */
object HapticFeedbackPlayer {
    fun play(view: View, strength: HapticStrength, event: HapticEvent = HapticEvent.TICK) {
        when (strength) {
            HapticStrength.OFF -> Unit
            HapticStrength.DEFAULT -> view.performHapticFeedback(systemConstant(event))
            HapticStrength.SOFT, HapticStrength.POWERFUL -> vibrate(view.context, strength)
        }
    }

    private fun systemConstant(event: HapticEvent): Int = when (event) {
        HapticEvent.CONFIRM -> HapticFeedbackConstants.LONG_PRESS
        HapticEvent.SUCCESS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS
        HapticEvent.TICK ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CLOCK_TICK
            else HapticFeedbackConstants.KEYBOARD_TAP
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context, strength: HapticStrength) {
        val vibrator = vibratorOf(context) ?: return
        if (!vibrator.hasVibrator()) return

        val soft = strength == HapticStrength.SOFT
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> vibrator.vibrate(
                VibrationEffect.createPredefined(
                    if (soft) VibrationEffect.EFFECT_TICK else VibrationEffect.EFFECT_HEAVY_CLICK
                )
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(
                VibrationEffect.createOneShot(
                    if (soft) SOFT_MS else POWERFUL_MS,
                    if (soft) SOFT_AMPLITUDE else VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

            else -> vibrator.vibrate(if (soft) SOFT_MS else POWERFUL_MS)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibratorOf(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private const val SOFT_MS = 12L
    private const val POWERFUL_MS = 45L
    private const val SOFT_AMPLITUDE = 60
}
