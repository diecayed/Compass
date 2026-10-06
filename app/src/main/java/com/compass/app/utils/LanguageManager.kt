// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** A language the user can pick, shown in its own language. */
data class AppLanguage(val tag: String, val nativeName: String)

/**
 * In-app language selection. The choice is stored in plain SharedPreferences so it can be read
 * synchronously when an activity starts, and applied by wrapping the activity's context.
 */
object LanguageManager {
    /** Follow the phone's language. */
    const val SYSTEM = "system"

    val supportedLanguages = listOf(
        AppLanguage("en", "English"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("nl", "Nederlands"),
        AppLanguage("tr", "Türkçe"),
        AppLanguage("ja", "日本語"),
    )

    private const val PREFS = "language_prefs"
    private const val KEY_LANGUAGE = "language"

    fun savedTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM) ?: SYSTEM

    fun save(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_LANGUAGE, tag) }
    }

    /** Returns [base] with the saved language applied, or [base] itself when following the system. */
    fun wrap(base: Context): Context {
        val tag = savedTag(base)
        if (tag == SYSTEM) return base

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        }
        return base.createConfigurationContext(configuration)
    }
}
