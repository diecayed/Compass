// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.compass

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.location.Location
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.compass.app.utils.HapticEvent
import com.compass.app.utils.HapticFeedbackPlayer
import com.compass.app.utils.HapticStrength
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/** Degrees between the strong marks on the dial; the phone ticks each time the heading crosses one. */
const val HAPTIC_INTERVAL_DEGREES = 30

/** Heading has to move this far past a mark before it counts, so wobble on a mark doesn't buzz. */
private const val HAPTIC_HYSTERESIS_DEGREES = 1f

/**
 * Gives a short haptic tick every time the heading passes a multiple of [HAPTIC_INTERVAL_DEGREES].
 */
@Composable
fun HeadingHaptics(headingDegrees: Float, strength: HapticStrength) {
    val view = LocalView.current
    val sectors = 360 / HAPTIC_INTERVAL_DEGREES
    val lastSector = remember { mutableIntStateOf(-1) }

    LaunchedEffect(headingDegrees) {
        val heading = (headingDegrees % 360f + 360f) % 360f
        val sector = floor(heading / HAPTIC_INTERVAL_DEGREES).toInt() % sectors
        val last = lastSector.intValue
        if (last == -1) {
            lastSector.intValue = sector
            return@LaunchedEffect
        }
        if (sector == last) return@LaunchedEffect

        // the mark sitting between the two sectors
        val boundary = if (sector == (last + 1) % sectors) {
            sector * HAPTIC_INTERVAL_DEGREES
        } else {
            last * HAPTIC_INTERVAL_DEGREES
        }.toFloat()
        val distance = abs(((heading - boundary + 540f) % 360f) - 180f)
        if (distance >= HAPTIC_HYSTERESIS_DEGREES) {
            lastSector.intValue = sector
            HapticFeedbackPlayer.play(view, strength, HapticEvent.TICK)
        }
    }
}

/** 48°51′30″ N  2°17′40″ E, with the hemisphere letters in the current language. */
// [cardinalLetters] is north, east, south, west.
fun formatCoordinates(
    location: Location,
    cardinalLetters: List<String>,
    hemisphereFirst: Boolean = false,
): String =
    "${dms(location.latitude, cardinalLetters[0], cardinalLetters[2], hemisphereFirst)}  " +
            dms(location.longitude, cardinalLetters[1], cardinalLetters[3], hemisphereFirst)

/** Puts [coordinates] on the clipboard. Returns false if the system refuses. */
fun copyCoordinates(context: Context, coordinates: String): Boolean {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return false
    return try {
        clipboard.setPrimaryClip(ClipData.newPlainText("coordinates", coordinates))
        true
    } catch (_: SecurityException) {
        false
    }
}

private fun dms(value: Double, positive: String, negative: String, hemisphereFirst: Boolean): String {
    val hemisphere = if (value >= 0) positive else negative
    val absolute = abs(value)
    var degrees = absolute.toInt()
    val minutesFloat = (absolute - degrees) * 60
    var minutes = minutesFloat.toInt()
    var seconds = ((minutesFloat - minutes) * 60).roundToInt()
    if (seconds == 60) {
        seconds = 0
        minutes += 1
    }
    if (minutes == 60) {
        minutes = 0
        degrees += 1
    }
    val value = "$degrees°$minutes′$seconds″"
    return if (hemisphereFirst) "$hemisphere$value" else "$value $hemisphere"
}

/** Elevation above sea level where the device can tell, otherwise the GPS altitude. */
fun formatElevation(location: Location): String? {
    val meters = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && location.hasMslAltitude() ->
            location.mslAltitudeMeters

        location.hasAltitude() -> location.altitude
        else -> return null
    }
    return if (usesImperialUnits()) {
        "${(meters * 3.28084).roundToInt()} ft"
    } else {
        "${meters.roundToInt()} m"
    }
}

private fun usesImperialUnits(): Boolean =
    Locale.getDefault().country in setOf("US", "LR", "MM")
