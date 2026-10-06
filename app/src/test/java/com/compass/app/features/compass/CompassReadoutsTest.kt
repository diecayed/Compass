// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.compass

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CompassReadoutsTest {

    private val originalLocale = Locale.getDefault()

    @Before
    fun useCommaLocale() {
        Locale.setDefault(Locale.GERMANY)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun mapCoordinates_areDecimalDegrees() {
        assertEquals("41.008200, 28.978400", formatCoordinatesForMaps(41.0082, 28.9784))
    }

    @Test
    fun mapCoordinates_keepTheSignInTheSouthAndWest() {
        assertEquals("-33.868800, -151.209300", formatCoordinatesForMaps(-33.8688, -151.2093))
    }

    @Test
    fun mapCoordinates_ignoreTheLanguagesDecimalComma() {
        assertEquals("0.500000, 0.250000", formatCoordinatesForMaps(0.5, 0.25))
    }
}
