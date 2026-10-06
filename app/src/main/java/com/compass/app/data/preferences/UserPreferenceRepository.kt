// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.data.preferences

import kotlinx.coroutines.flow.Flow

interface UserPreferenceRepository {
    val getUserPreferenceStream: Flow<UserPreferences>

    suspend fun setTheme(theme: String)
    suspend fun setTrueDarkState(boolean: Boolean)
    suspend fun setTrueNorthState(boolean: Boolean)
    suspend fun setHighPrecisionState(boolean: Boolean)
    suspend fun setHapticStrength(strength: String)
}
