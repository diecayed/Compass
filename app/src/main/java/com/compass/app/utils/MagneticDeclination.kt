// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.utils
import android.hardware.GeomagneticField
import android.location.Location

/** Angle between magnetic north and true north at [location], in degrees. */
fun getMagneticDeclination(location: Location): Float {
    // Based on WGS84 geodetic coordinates
    val geomagneticField = GeomagneticField(
        location.latitude.toFloat(),
        location.longitude.toFloat(),
        location.altitude.toFloat(),
        location.time
    )
    return geomagneticField.declination
}
