// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.data.preferences

import kotlinx.coroutines.flow.Flow

class UserPreferencesRepositoryImpl(
    private val dataStore: PreferenceDataSource,
) : UserPreferenceRepository {
    override val getUserPreferenceStream: Flow<UserPreferences>
        get() = dataStore.preferenceFlow

    override suspend fun setTheme(theme: String) {
        dataStore.setTheme(UserPreferences.KEY_THEME, theme)
    }

    override suspend fun setTrueDarkState(boolean: Boolean) {
        dataStore.setTrueDarkValue(UserPreferences.TRUE_DARK, boolean)
    }

    override suspend fun setHapticStrength(strength: String) {
        dataStore.setHapticStrengthValue(UserPreferences.HAPTIC_STRENGTH, strength)
    }

    override suspend fun setHighPrecisionState(boolean: Boolean) {
        dataStore.setHighPrecisionValue(UserPreferences.HIGH_PRECISION, boolean)
    }

    override suspend fun setTrueNorthState(boolean: Boolean) {
        dataStore.setTrueNorthValue(UserPreferences.TRUE_NORTH, boolean)
    }
}
