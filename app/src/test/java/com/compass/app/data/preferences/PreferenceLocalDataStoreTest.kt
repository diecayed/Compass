// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.compass.app.ui.theme.ThemeConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceLocalDataStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = {
                File(tempFolder.root, "test.preferences_pb")
            }
        )
    }

    private val dataSource by lazy {
        PreferenceLocalDataStore(dataStore)
    }

    @Test
    fun defaultValues_areCorrect() = testScope.runTest {
        val prefs = dataSource.preferenceFlow.first()

        assertEquals(ThemeConfig.FOLLOW_SYSTEM.prefName, prefs.theme)
        assertEquals(false, prefs.isTrueDarkThemeEnabled)
        assertEquals(false, prefs.isTrueNorthEnabled)
    }

    @Test
    fun theme_roundTrip() = testScope.runTest {
        dataSource.setTheme(UserPreferences.KEY_THEME, "dark")

        val prefs = dataSource.preferenceFlow.first()

        assertEquals("dark", prefs.theme)
    }

    @Test
    fun trueNorth_roundTrip() = testScope.runTest {
        dataSource.setTrueNorthValue(UserPreferences.TRUE_NORTH, true)

        val prefs = dataSource.preferenceFlow.first()
        assertEquals(true, prefs.isTrueNorthEnabled)
    }

    @Test
    fun trueDark_roundTrip() = testScope.runTest {
        dataSource.setTrueDarkValue(UserPreferences.TRUE_DARK, true)

        val prefs = dataSource.preferenceFlow.first()
        assertEquals(true, prefs.isTrueDarkThemeEnabled)
    }
}
