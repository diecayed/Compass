// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compass.app.data.preferences.UserPreferenceRepository
import com.compass.app.utils.HapticStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class LevelUiState(
    val supported: Boolean = true,
    val orientation: DeviceOrientation = DeviceOrientation.LANDING,
    /** Bubble target on the x axis, -1..1 */
    val bubbleX: Float = 0f,
    /** Bubble target on the y axis (only used when lying flat), -1..1 */
    val bubbleY: Float = 0f,
    /** Primary angle for display, in degrees */
    val angle1: Float = 0f,
    /** Total tilt away from level: the angle between the screen and the horizontal, in degrees */
    val tilt: Float = 0f,
    /** Secondary angle for display, only meaningful when lying flat */
    val angle2: Float = 0f,
    val isLevel: Boolean = false,
    val locked: Boolean = false,
    /** See [LevelReading.uprightRotation] */
    val uprightRotation: Float = 0f,
    /** Show decimals and use the extra-smooth readings (setting in Settings) */
    val highPrecision: Boolean = true,
    /** How strongly to buzz when the level turns level (setting in Settings) */
    val hapticStrength: HapticStrength = HapticStrength.DEFAULT,
)

@HiltViewModel
class LevelViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    userPreferences: UserPreferenceRepository,
) : ViewModel() {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Decimals and extra-smooth readings. Follows the setting in Settings. */
    private val highPrecision = userPreferences.getUserPreferenceStream
        .map { it.isHighPrecisionEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val hapticStrength = userPreferences.getUserPreferenceStream
        .map { HapticStrength.fromPref(it.hapticStrength) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HapticStrength.DEFAULT)

    private val _locked = MutableStateFlow(false)
    private val _calibrationEvents = MutableStateFlow<CalibrationEvent?>(null)
    val calibrationEvents: StateFlow<CalibrationEvent?> = _calibrationEvents.asStateFlow()

    init {
        prefs.edit { remove(KEY_INCLINATION) }
    }

    // Smoothing state, only touched from the sensor collector
    private var angle1Raw = 0f
    private var angle2Raw = 0f
    private var tiltSmoothed = 0f
    private var angleX = 0.0
    private var angleY = 0.0
    private var lockedOrientation: DeviceOrientation? = null
    private var lastRaw = LevelReading()

    private val calibration = DeviceOrientation.entries.associateWith { loadCalibration(it) }
        .toMutableMap()

    val uiState: StateFlow<LevelUiState> = readings()
        .map { process(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LevelUiState(supported = accelerometer != null)
        )

    fun toggleLock() {
        val nowLocked = !_locked.value
        _locked.value = nowLocked
        lockedOrientation = if (nowLocked) lastRaw.orientation else null
    }

    /** Stores the current angles as the zero point for the current orientation. */
    fun calibrate() {
        val raw = lastRaw
        calibration[raw.orientation] = Triple(raw.pitch, raw.roll, raw.balance)
        prefs.edit {
            putFloat("$KEY_PITCH${raw.orientation.name}", raw.pitch)
            putFloat("$KEY_ROLL${raw.orientation.name}", raw.roll)
            putFloat("$KEY_BALANCE${raw.orientation.name}", raw.balance)
        }
        _calibrationEvents.value = CalibrationEvent.Saved
    }

    fun resetCalibration() {
        calibration.keys.forEach { calibration[it] = Triple(0f, 0f, 0f) }
        prefs.edit {
            DeviceOrientation.entries.forEach {
                remove("$KEY_PITCH${it.name}")
                remove("$KEY_ROLL${it.name}")
                remove("$KEY_BALANCE${it.name}")
            }
        }
        _calibrationEvents.value = CalibrationEvent.Reset
    }

    fun calibrationEventConsumed() {
        _calibrationEvents.value = null
    }

    private fun loadCalibration(orientation: DeviceOrientation) = Triple(
        prefs.getFloat("$KEY_PITCH${orientation.name}", 0f),
        prefs.getFloat("$KEY_ROLL${orientation.name}", 0f),
        prefs.getFloat("$KEY_BALANCE${orientation.name}", 0f),
    )

    private fun process(raw: LevelReading): LevelUiState {
        lastRaw = raw
        val orientation = lockedOrientation ?: raw.orientation
        val (calPitch, calRoll, calBalance) = calibration.getValue(orientation)
        val pitch = raw.pitch - calPitch
        val roll = raw.roll - calRoll
        val balance = raw.balance - calBalance

        // How much of each new reading is mixed into the shown angle. The precise mode lets in less,
        // so the extra decimals do not flicker with every bit of sensor noise.
        val precise = highPrecision.value
        val smoothing = if (precise) SMOOTHING_PRECISE else SMOOTHING_NORMAL

        var angle1 = 0f
        var angle2 = 0f
        when (orientation) {
            DeviceOrientation.TOP, DeviceOrientation.BOTTOM -> {
                angle1Raw += (balance - angle1Raw) * smoothing
                angle1 = abs(angle1Raw)
                angleX = angleX * 0.7 + (sin(Math.toRadians(balance.toDouble())) / MAX_SINUS) * 0.3
            }

            DeviceOrientation.LANDING, DeviceOrientation.RIGHT, DeviceOrientation.LEFT -> {
                if (orientation == DeviceOrientation.LANDING) {
                    angle2Raw += (roll - angle2Raw) * smoothing
                    angle2 = abs(angle2Raw)
                    angleX = angleX * 0.7 + (sin(Math.toRadians(roll.toDouble())) / MAX_SINUS) * 0.3
                }
                angle1Raw += (pitch - angle1Raw) * smoothing
                angle1 = abs(angle1Raw)
                angleY = angleY * 0.7 + (sin(Math.toRadians(pitch.toDouble())) / MAX_SINUS) * 0.3
                if (angle1 > 90) angle1 = 180 - angle1
            }
        }

        // Total tilt: how far the screen normal is from vertical. For the upright / sideways modes it
        // is just the single angle.
        if (orientation == DeviceOrientation.LANDING) {
            val cosTilt = cos(Math.toRadians(pitch.toDouble())) * cos(Math.toRadians(roll.toDouble()))
            val rawTilt = Math.toDegrees(acos(cosTilt.coerceIn(-1.0, 1.0))).toFloat()
            tiltSmoothed += (rawTilt - tiltSmoothed) * smoothing
        } else {
            tiltSmoothed = angle1
        }
        val tilt = tiltSmoothed

        return LevelUiState(
            supported = true,
            orientation = orientation,
            bubbleX = angleX.coerceIn(-1.0, 1.0).toFloat(),
            bubbleY = angleY.coerceIn(-1.0, 1.0).toFloat(),
            angle1 = angle1.coerceAtMost(MAX_ANGLE),
            tilt = tilt.coerceAtMost(MAX_ANGLE),
            angle2 = angle2.coerceAtMost(MAX_ANGLE),
            isLevel = orientation.isLevel(pitch, roll, balance, LEVEL_TOLERANCE_DEG),
            locked = _locked.value,
            uprightRotation = raw.uprightRotation,
            highPrecision = precise,
            hapticStrength = hapticStrength.value,
        )
    }

    private fun readings() = callbackFlow {
        val sensor = accelerometer
        if (sensor == null) {
            awaitClose { }
            return@callbackFlow
        }

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val rotationMatrix = FloatArray(16)
        val remapped = FloatArray(16)
        val orientationAngles = FloatArray(3)
        val magnetic = floatArrayOf(1f, 1f, 1f)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!SensorManager.getRotationMatrix(rotationMatrix, null, event.values, magnetic)) {
                    return
                }
                val (axisX, axisY) = when (displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation) {
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)
                SensorManager.getOrientation(remapped, orientationAngles)

                // normalize z on ux, uy
                var tmp = sqrt(remapped[8] * remapped[8] + remapped[9] * remapped[9])
                tmp = if (tmp == 0f) 0f else remapped[8] / tmp

                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = -Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                val balance = Math.toDegrees(asin(tmp.toDouble())).toFloat()

                val orientation = when {
                    pitch < -45 && pitch > -135 -> DeviceOrientation.TOP
                    pitch > 45 && pitch < 135 -> DeviceOrientation.BOTTOM
                    roll > 45 -> DeviceOrientation.RIGHT
                    roll < -45 -> DeviceOrientation.LEFT
                    else -> DeviceOrientation.LANDING
                }
                // Where real-world "up" points inside the screen plane (third row of the matrix).
                val upX = remapped[8]
                val upY = remapped[9]
                val upright = if (hypot(upX, upY) < FLAT_THRESHOLD) 0f
                else Math.toDegrees(atan2(upX.toDouble(), upY.toDouble())).toFloat()

                trySend(LevelReading(orientation, pitch, roll, balance, upright))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    enum class CalibrationEvent { Saved, Reset }

    private companion object {
        const val PREFS_NAME = "level_prefs"
        /** Saved by earlier versions that had a percent mode; cleared on start. */
        const val KEY_INCLINATION = "use_inclination"
        const val KEY_PITCH = "pitch."
        const val KEY_ROLL = "roll."
        const val KEY_BALANCE = "balance."
        val MAX_SINUS = sin(Math.PI / 4)
        const val MAX_ANGLE = 99.9f
        const val LEVEL_TOLERANCE_DEG = 0.5f

        /** Below this in-plane gravity (about 6 degrees from flat) there is no usable "up" on the screen. */
        const val FLAT_THRESHOLD = 0.1f

        /** Share of a new reading that goes into the shown angle (0..1). Lower means calmer numbers. */
        const val SMOOTHING_NORMAL = 0.3f
        const val SMOOTHING_PRECISE = 0.12f
    }
}
