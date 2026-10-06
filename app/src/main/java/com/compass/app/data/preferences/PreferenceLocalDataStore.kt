// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.compass.app.ui.theme.ThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class PreferenceLocalDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferenceDataSource {
    override val preferenceFlow: Flow<UserPreferences>
        get() = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                mapUserPreferences(preferences)
            }

    override suspend fun setTheme(key: String, value: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun setTrueDarkValue(key: String, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    override suspend fun setHapticStrengthValue(key: String, value: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun setHighPrecisionValue(key: String, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    override suspend fun setTrueNorthValue(key: String, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        return UserPreferences(
            theme = preferences[PreferencesKeys.THEME] ?: ThemeConfig.FOLLOW_SYSTEM.prefName,
            isTrueDarkThemeEnabled = preferences[PreferencesKeys.TRUE_DARK] ?: false,
            isTrueNorthEnabled = preferences[PreferencesKeys.TRUE_NORTH] ?: false,
            isHighPrecisionEnabled = preferences[PreferencesKeys.HIGH_PRECISION] ?: true,
            hapticStrength = preferences[PreferencesKeys.HAPTIC_STRENGTH] ?: "default",
        )
    }

    private object PreferencesKeys {
        val THEME = stringPreferencesKey(UserPreferences.KEY_THEME)
        val TRUE_DARK = booleanPreferencesKey(UserPreferences.TRUE_DARK)
        val TRUE_NORTH = booleanPreferencesKey(UserPreferences.TRUE_NORTH)
        val HIGH_PRECISION = booleanPreferencesKey(UserPreferences.HIGH_PRECISION)
        val HAPTIC_STRENGTH = stringPreferencesKey(UserPreferences.HAPTIC_STRENGTH)
    }
}
