package com.example.mycycle.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mycycle.domain.model.ThemeMode
import com.example.mycycle.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val INITIAL_PERIOD_DATE = longPreferencesKey("initial_period_date")
        val ESTIMATED_CYCLE_LENGTH = intPreferencesKey("estimated_cycle_length")
        val ESTIMATED_PERIOD_LENGTH = intPreferencesKey("estimated_period_length")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            UserPreferences(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                initialPeriodDate = prefs[Keys.INITIAL_PERIOD_DATE]
                    ?.let { LocalDate.ofEpochDay(it) },
                estimatedCycleLength = prefs[Keys.ESTIMATED_CYCLE_LENGTH] ?: 28,
                estimatedPeriodLength = prefs[Keys.ESTIMATED_PERIOD_LENGTH] ?: 5,
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: true
            )
        }

    suspend fun completeOnboarding(
        lastPeriodDate: LocalDate,
        cycleLength: Int,
        periodLength: Int = 5
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
            prefs[Keys.INITIAL_PERIOD_DATE] = lastPeriodDate.toEpochDay()
            prefs[Keys.ESTIMATED_CYCLE_LENGTH] = cycleLength
            prefs[Keys.ESTIMATED_PERIOD_LENGTH] = periodLength
        }
    }

    suspend fun updateTheme(mode: ThemeMode, dynamicColors: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
            prefs[Keys.USE_DYNAMIC_COLORS] = dynamicColors
        }
    }

    suspend fun updateCycleLength(length: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.ESTIMATED_CYCLE_LENGTH] = length
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
