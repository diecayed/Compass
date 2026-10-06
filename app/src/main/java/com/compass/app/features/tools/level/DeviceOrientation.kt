// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

import kotlin.math.abs

/**
 * How the device is currently held.
 *
 * - [LANDING]: lying flat, shows the 2D bubble.
 * - [TOP] / [BOTTOM]: held upright in portrait, shows the horizontal 1D level.
 * - [LEFT] / [RIGHT]: held on its side in landscape, shows the vertical 1D level.
 */
enum class DeviceOrientation(val reverse: Int, val rotation: Int) {
    LANDING(1, 0),
    TOP(1, 0),
    RIGHT(1, 90),
    BOTTOM(-1, 180),
    LEFT(-1, -90);

    fun isLevel(pitch: Float, roll: Float, balance: Float, tolerance: Float): Boolean =
        when (this) {
            TOP, BOTTOM -> abs(balance) <= tolerance
            LANDING -> abs(roll) <= tolerance &&
                    (abs(pitch) <= tolerance || abs(pitch) >= 180 - tolerance)

            LEFT, RIGHT -> abs(pitch) <= tolerance || abs(pitch) >= 180 - tolerance
        }
}

/** Raw (uncalibrated) angles in degrees plus the orientation they were classified as. */
data class LevelReading(
    val orientation: DeviceOrientation = DeviceOrientation.LANDING,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val balance: Float = 0f,
    /**
     * Degrees (clockwise positive) to turn screen content so its top points at real-world up,
     * which keeps text parallel to the ground whatever the phone's tilt. 0 when the phone lies flat.
     */
    val uprightRotation: Float = 0f,
)
