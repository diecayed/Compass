// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.core.sensors

import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccuracyDialogState(
    val show: Boolean,
    val accuracyForDialog: Int? = null
)

class SensorViewModel : ViewModel() {
    private val _trueNorthEnabled = MutableStateFlow(false)
    val trueNorthEnabled: StateFlow<Boolean> = _trueNorthEnabled.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _accuracy = MutableStateFlow<Int?>(null)
    val accuracy: StateFlow<Int?> = _accuracy.asStateFlow()

    private val _accuracyDialog = MutableStateFlow(AccuracyDialogState(show = false))
    val accuracyDialog: StateFlow<AccuracyDialogState> = _accuracyDialog.asStateFlow()

    private var autoDialogShownForCurrentLowState = false

    fun updateSensorAccuracy(accuracy: Int) {
        _accuracy.value = accuracy

        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH ||
            accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        ) {
            autoDialogShownForCurrentLowState = false
        }

        val isLow = accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW ||
                accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
        if (isLow && !autoDialogShownForCurrentLowState && !_accuracyDialog.value.show) {
            _accuracyDialog.value = AccuracyDialogState(show = true, accuracyForDialog = accuracy)
            autoDialogShownForCurrentLowState = true
        }
    }

    fun provideLocation(location: Location?) {
        location?.let { _location.value = it }
    }

    fun setTrueNorthState(enabled: Boolean) {
        _trueNorthEnabled.value = enabled
    }

    fun showAccuracyDialog() {
        val current = _accuracy.value ?: return
        _accuracyDialog.value = AccuracyDialogState(show = true, accuracyForDialog = current)
    }

    fun dismissAccuracyDialog() {
        _accuracyDialog.value = AccuracyDialogState(show = false)
    }
}
