package com.sl.mycycle.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.ThemeMode
import com.sl.mycycle.domain.model.UserPreferences
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

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
        val CYCLE_STAGE = stringPreferencesKey("cycle_stage")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PROTECT_SCREEN_ENABLED = booleanPreferencesKey("protect_screen_enabled")
    }

    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map(::toDomain)

    suspend fun completeOnboarding(
        lastPeriodDate: LocalDate?,
        cycleLength: Int,
        cycleStage: CycleStage,
        periodLength: Int = 5
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
            if (lastPeriodDate != null) {
                prefs[Keys.INITIAL_PERIOD_DATE] = lastPeriodDate.toEpochDay()
            } else {
                prefs.remove(Keys.INITIAL_PERIOD_DATE)
            }
            prefs[Keys.ESTIMATED_CYCLE_LENGTH] = cycleLength
            prefs[Keys.ESTIMATED_PERIOD_LENGTH] = periodLength
            prefs[Keys.CYCLE_STAGE] = cycleStage.name
        }
    }

    suspend fun updateTheme(mode: ThemeMode, dynamicColors: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
            prefs[Keys.USE_DYNAMIC_COLORS] = dynamicColors
        }
    }

    suspend fun updateCycleStage(stage: CycleStage) {
        dataStore.edit { prefs ->
            prefs[Keys.CYCLE_STAGE] = stage.name
        }
    }

    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        require(hour in 0..23 && minute in 0..59)
        dataStore.edit { prefs ->
            prefs[Keys.DAILY_REMINDER_ENABLED] = enabled
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateAppLock(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun updateProtectScreen(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PROTECT_SCREEN_ENABLED] = enabled }
    }

    suspend fun replaceAll(snapshot: UserPreferences) {
        dataStore.edit { prefs ->
            prefs.clear()
            prefs.write(snapshot)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private fun toDomain(prefs: Preferences): UserPreferences = UserPreferences(
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        initialPeriodDate = prefs[Keys.INITIAL_PERIOD_DATE]?.let(LocalDate::ofEpochDay),
        estimatedCycleLength = prefs[Keys.ESTIMATED_CYCLE_LENGTH] ?: 28,
        estimatedPeriodLength = prefs[Keys.ESTIMATED_PERIOD_LENGTH] ?: 5,
        cycleStage = prefs[Keys.CYCLE_STAGE]
            ?.let { runCatching { CycleStage.valueOf(it) }.getOrNull() }
            ?: CycleStage.NOT_SET,
        themeMode = prefs[Keys.THEME_MODE]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: true,
        dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
        reminderHour = (prefs[Keys.REMINDER_HOUR] ?: 20).coerceIn(0, 23),
        reminderMinute = (prefs[Keys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
        appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
        protectScreenEnabled = prefs[Keys.PROTECT_SCREEN_ENABLED] ?: false
    )

    private fun MutablePreferences.write(snapshot: UserPreferences) {
        this[Keys.ONBOARDING_COMPLETED] = snapshot.onboardingCompleted
        snapshot.initialPeriodDate?.let {
            this[Keys.INITIAL_PERIOD_DATE] = it.toEpochDay()
        }
        this[Keys.ESTIMATED_CYCLE_LENGTH] = snapshot.estimatedCycleLength
        this[Keys.ESTIMATED_PERIOD_LENGTH] = snapshot.estimatedPeriodLength
        this[Keys.CYCLE_STAGE] = snapshot.cycleStage.name
        this[Keys.THEME_MODE] = snapshot.themeMode.name
        this[Keys.USE_DYNAMIC_COLORS] = snapshot.useDynamicColors
        this[Keys.DAILY_REMINDER_ENABLED] = snapshot.dailyReminderEnabled
        this[Keys.REMINDER_HOUR] = snapshot.reminderHour
        this[Keys.REMINDER_MINUTE] = snapshot.reminderMinute
        this[Keys.APP_LOCK_ENABLED] = snapshot.appLockEnabled
        this[Keys.PROTECT_SCREEN_ENABLED] = snapshot.protectScreenEnabled
    }
}
