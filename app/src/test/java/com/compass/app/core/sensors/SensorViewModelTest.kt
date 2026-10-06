// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.core.sensors

import android.hardware.SensorManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SensorViewModelTest {
    @Test
    fun defaultState_isCorrect() = runTest {
        val vm = SensorViewModel()

        assertFalse(vm.trueNorthEnabled.value) // default is false
        assertNull(vm.location.value)

        assertNull(vm.accuracy.value)
        assertFalse(vm.accuracyDialog.value.show)
    }

    @Test
    fun accuracy_isStored() = runTest {
        val vm = SensorViewModel()

        vm.updateSensorAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)

        assertEquals(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, vm.accuracy.value)
    }

    @Test
    fun lowAccuracy_triggersDialogOnce() = runTest {
        val vm = SensorViewModel()

        vm.updateSensorAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_LOW)

        val dialog = vm.accuracyDialog.value
        assertTrue(dialog.show)

        // Call again shouldn't  trigger again
        vm.updateSensorAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_LOW)

        val dialogAgain = vm.accuracyDialog.value
        assertTrue(dialogAgain.show) // still true, not duplicated
    }

    @Test
    fun accuracyRecovery_resetsAutoDialogFlag() = runTest {
        val vm = SensorViewModel()

        // low, show dialog
        vm.updateSensorAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_LOW)
        assertTrue(vm.accuracyDialog.value.show)

        // Dismiss dialog
        vm.dismissAccuracyDialog()

        // High, resets flag
        vm.updateSensorAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)

        // Low again, should trigger again
        vm.updateSensorAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_LOW)

        assertTrue(vm.accuracyDialog.value.show)
    }

    @Test
    fun showAccuracyDialog_usesCurrentAccuracy() = runTest {
        val vm = SensorViewModel()

        vm.updateSensorAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)

        vm.showAccuracyDialog()

        val dialog = vm.accuracyDialog.value

        assertTrue(dialog.show)
        assertEquals(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM, dialog.accuracyForDialog)
    }

    @Test
    fun dialogDismiss_hidesDialog() = runTest {
        val vm = SensorViewModel()

        vm.updateSensorAccuracy(SensorManager.SENSOR_STATUS_UNRELIABLE)

        vm.dismissAccuracyDialog()

        assertFalse(vm.accuracyDialog.value.show)
    }

    @Test
    fun trueNorth_updatesCorrectly() = runTest {
        val vm = SensorViewModel()

        vm.setTrueNorthState(true)

        assertTrue(vm.trueNorthEnabled.value)
    }
}
