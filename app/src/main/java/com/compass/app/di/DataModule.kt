// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.compass.app.data.preferences.PreferenceDataSource
import com.compass.app.data.preferences.PreferenceLocalDataStore
import com.compass.app.data.preferences.UserPreferenceRepository
import com.compass.app.data.preferences.UserPreferencesRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val PREF_NAME = "settings"

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREF_NAME)

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.preferencesDataStore
    }

    @Provides
    @Singleton
    fun provideLocalDataStore(
        dataStore: DataStore<Preferences>
    ): PreferenceDataSource {
        return PreferenceLocalDataStore(dataStore)
    }

    @Provides
    @Singleton
    fun provideUserPreferenceRepository(
        dataStore: PreferenceDataSource
    ): UserPreferenceRepository {
        return UserPreferencesRepositoryImpl(dataStore)
    }
}
