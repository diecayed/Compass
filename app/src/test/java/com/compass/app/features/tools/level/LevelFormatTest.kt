// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools.level

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LevelFormatTest {
    @Test
    fun precise_showsOneDecimal() {
        assertEquals("3.4°", formatLevelAngle(3.44f, precise = true, locale = Locale.US))
    }

    @Test
    fun notPrecise_hidesTheFraction_andRounds() {
        assertEquals("3°", formatLevelAngle(3.44f, precise = false, locale = Locale.US))
        assertEquals("4°", formatLevelAngle(3.5f, precise = false, locale = Locale.US))
        assertEquals("19°", formatLevelAngle(18.9f, precise = false, locale = Locale.US))
    }

    @Test
    fun zero_isFormatted() {
        assertEquals("0.0°", formatLevelAngle(0f, precise = true, locale = Locale.US))
        assertEquals("0°", formatLevelAngle(0f, precise = false, locale = Locale.US))
    }

    @Test
    fun precise_usesTheLocalesDecimalSeparator() {
        assertEquals("3,4°", formatLevelAngle(3.4f, precise = true, locale = Locale.GERMANY))
    }
}
