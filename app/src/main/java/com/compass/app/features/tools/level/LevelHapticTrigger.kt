// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

/** After a buzz, the level has to tilt at least this far (degrees) from level before it can buzz again. */
const val REARM_TILT_DEGREES = 2f

/**
 * Decides when to buzz because the level has just become level.
 *
 * It fires once when the level turns level, then stays quiet until the phone has clearly tilted
 * away again, so a reading that wobbles right at the edge does not buzz over and over. If the
 * level is already level when it starts, it does not buzz for that.
 */
class LevelHapticTrigger(initiallyLevel: Boolean) {
    private var armed = !initiallyLevel

    /** Returns true when a buzz should play for this reading. */
    fun update(isLevel: Boolean, tiltDegrees: Float): Boolean {
        if (isLevel) {
            if (!armed) return false
            armed = false
            return true
        }
        if (tiltDegrees >= REARM_TILT_DEGREES) armed = true
        return false
    }
}
