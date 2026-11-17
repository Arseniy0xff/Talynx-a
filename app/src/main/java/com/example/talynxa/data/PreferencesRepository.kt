package com.example.talynxa.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val OLED_THEME = booleanPreferencesKey("oled_theme")
        val DARK_THEME_OVERRIDE = stringPreferencesKey("dark_theme_override") // "system", "dark", "light"
    }

    val oledThemeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.OLED_THEME] ?: false
        }

    val darkThemeOverrideFlow: Flow<Boolean?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            when (preferences[PreferencesKeys.DARK_THEME_OVERRIDE]) {
                "dark" -> true
                "light" -> false
                else -> null
            }
        }

    suspend fun setOledTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OLED_THEME] = enabled
        }
    }

    suspend fun setDarkThemeOverride(override: Boolean?) {
        context.dataStore.edit { preferences ->
            val value = when (override) {
                true -> "dark"
                false -> "light"
                null -> "system"
            }
            preferences[PreferencesKeys.DARK_THEME_OVERRIDE] = value
        }
    }
}
