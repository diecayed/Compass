// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StableValueTest {
    @Test
    fun smallSensorNoise_isIgnored() {
        assertFalse(isClearChange(shown = 3.4f, latest = 3.43f, precise = true))
        assertFalse(isClearChange(shown = 3.4f, latest = 3.36f, precise = true))
        assertFalse(isClearChange(shown = 3.4f, latest = 3.8f, precise = false))
    }

    @Test
    fun aRealMove_updatesTheValue() {
        assertTrue(isClearChange(shown = 3.4f, latest = 3.5f, precise = true))
        assertTrue(isClearChange(shown = 3.4f, latest = 4.2f, precise = false))
    }
}
