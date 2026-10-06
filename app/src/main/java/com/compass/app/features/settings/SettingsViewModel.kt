// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compass.app.data.preferences.UserPreferenceRepository
import com.compass.app.ui.theme.ThemeConfig
import com.compass.app.utils.HapticStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferenceRepository,
) : ViewModel() {
    val uiState = userPreferencesRepository.getUserPreferenceStream
        .map { userPreferences ->
            SettingsUiState(
                theme = userPreferences.theme,
                isTrueDarkThemeEnabled = userPreferences.isTrueDarkThemeEnabled,
                isTrueNorthEnabled = userPreferences.isTrueNorthEnabled,
                isHighPrecisionEnabled = userPreferences.isHighPrecisionEnabled,
                hapticStrength = HapticStrength.fromPref(userPreferences.hapticStrength)
            )
        }.catch {
            Log.d("SettingsViewModel", "Error getting user preference", it)
            emit(SettingsUiState())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(theme)
        }
    }

    fun setTrueDarkState(isTrueDarkThemeEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTrueDarkState(isTrueDarkThemeEnabled)
        }
    }

    fun setHapticStrength(strength: HapticStrength) {
        viewModelScope.launch {
            userPreferencesRepository.setHapticStrength(strength.prefName)
        }
    }

    fun setHighPrecisionState(isHighPrecisionEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHighPrecisionState(isHighPrecisionEnabled)
        }
    }

    fun setTrueNorthState(isTrueNorthEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTrueNorthState(isTrueNorthEnabled)
        }
    }

    data class SettingsUiState(
        val theme: String = ThemeConfig.FOLLOW_SYSTEM.prefName,
        val isTrueNorthEnabled: Boolean = false,
        val isTrueDarkThemeEnabled: Boolean = false,
        val isHighPrecisionEnabled: Boolean = true,
        val hapticStrength: HapticStrength = HapticStrength.DEFAULT,
        val themeDialogOptions: List<String> = listOf(
            ThemeConfig.FOLLOW_SYSTEM.prefName,
            ThemeConfig.LIGHT.prefName,
            ThemeConfig.DARK.prefName,
        ),
    )
}
