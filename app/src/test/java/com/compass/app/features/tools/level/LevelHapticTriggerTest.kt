// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelHapticTriggerTest {
    @Test
    fun buzzesOnce_whenTheLevelBecomesLevel() {
        val trigger = LevelHapticTrigger(initiallyLevel = false)
        assertFalse(trigger.update(isLevel = false, tiltDegrees = 10f))
        assertTrue(trigger.update(isLevel = true, tiltDegrees = 0.2f))
        assertFalse(trigger.update(isLevel = true, tiltDegrees = 0.1f))
        assertFalse(trigger.update(isLevel = true, tiltDegrees = 0.3f))
    }

    @Test
    fun doesNotBuzzAgain_whenItWobblesAtTheEdge() {
        val trigger = LevelHapticTrigger(initiallyLevel = false)
        assertTrue(trigger.update(true, 0.4f))
        // drifts just past the green threshold and back, never clearly away
        assertFalse(trigger.update(false, 0.7f))
        assertFalse(trigger.update(true, 0.4f))
        assertFalse(trigger.update(false, 1.0f))
        assertFalse(trigger.update(true, 0.5f))
    }

    @Test
    fun buzzesAgain_afterTiltingClearlyAway() {
        val trigger = LevelHapticTrigger(initiallyLevel = false)
        assertTrue(trigger.update(true, 0.2f))
        assertFalse(trigger.update(false, 5f))
        assertTrue(trigger.update(true, 0.2f))
    }

    @Test
    fun doesNotBuzz_whenItStartsAlreadyLevel() {
        val trigger = LevelHapticTrigger(initiallyLevel = true)
        assertFalse(trigger.update(true, 0.1f))
        assertFalse(trigger.update(true, 0.2f))
        // but it does once it has been tilted away and comes back
        assertFalse(trigger.update(false, 4f))
        assertTrue(trigger.update(true, 0.1f))
    }
}
