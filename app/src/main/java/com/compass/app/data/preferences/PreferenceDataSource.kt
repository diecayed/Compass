// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.data.preferences

import kotlinx.coroutines.flow.Flow

interface PreferenceDataSource {
    val preferenceFlow: Flow<UserPreferences>

    suspend fun setTheme(key: String, value: String)
    suspend fun setTrueNorthValue(key: String, value: Boolean)
    suspend fun setTrueDarkValue(key: String, value: Boolean)
    suspend fun setHighPrecisionValue(key: String, value: Boolean)
    suspend fun setHapticStrengthValue(key: String, value: String)
}
