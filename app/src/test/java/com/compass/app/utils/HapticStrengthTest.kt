// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class HapticStrengthTest {
    @Test
    fun everyStrength_roundTripsThroughItsPrefName() {
        HapticStrength.entries.forEach {
            assertEquals(it, HapticStrength.fromPref(it.prefName))
        }
    }

    @Test
    fun unknownOrMissingValues_fallBackToDefault() {
        assertEquals(HapticStrength.DEFAULT, HapticStrength.fromPref(null))
        assertEquals(HapticStrength.DEFAULT, HapticStrength.fromPref(""))
        assertEquals(HapticStrength.DEFAULT, HapticStrength.fromPref("loud"))
    }

    @Test
    fun theFourOptionsAreInOrder() {
        assertEquals(
            listOf("off", "soft", "default", "powerful"),
            HapticStrength.entries.map { it.prefName }
        )
    }
}
