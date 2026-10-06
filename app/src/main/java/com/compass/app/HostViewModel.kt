// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compass.app.data.preferences.UserPreferenceRepository
import com.compass.app.ui.theme.ThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HostViewModel @Inject constructor(
    userPreferencesRepository: UserPreferenceRepository,
) : ViewModel() {
    val uiStateFlow = userPreferencesRepository.getUserPreferenceStream
        .map { userPreferences ->
            UiState(
                isLoading = false,
                darkThemeConfig = when (userPreferences.theme) {
                    ThemeConfig.LIGHT.prefName -> ThemeConfig.LIGHT
                    ThemeConfig.DARK.prefName -> ThemeConfig.DARK
                    else -> ThemeConfig.FOLLOW_SYSTEM
                },
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UiState()
        )

    data class UiState(
        val isLoading: Boolean = true,
        val darkThemeConfig: ThemeConfig = ThemeConfig.FOLLOW_SYSTEM,
    )
}
