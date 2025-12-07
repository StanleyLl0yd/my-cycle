package com.example.mycycle.data

import com.example.mycycle.model.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

public interface CycleRepository {
    public val cycles: Flow<List<Cycle>>
    public val activeCycle: Flow<Cycle?>

    public suspend fun startCycle(startDate: LocalDate)
    public suspend fun endCycle(endDate: LocalDate)
}

public interface LogRepository {
    public val logs: Flow<List<LogEntry>>
    public suspend fun addOrUpdate(entry: LogEntry)
}

public interface ReminderRepository {
    public val reminders: Flow<List<Reminder>>
    public suspend fun toggle(reminderId: String, enabled: Boolean)
    public suspend fun addReminder(type: ReminderType, time: LocalTime)
}

public interface UserPreferencesRepository {
    public val preferences: Flow<UserPreferences>
    public suspend fun updateTheme(theme: ThemeSetting)
    public suspend fun updateUnits(temperatureUnit: TemperatureUnit, weightUnit: WeightUnit)
    public suspend fun updateNotifications(enabled: Boolean)
    public suspend fun updateAnalyticsOptIn(optIn: Boolean)
}

private class InMemoryCycleRepository : CycleRepository {
    private val state = MutableStateFlow(sampleCycles())

    override val cycles: Flow<List<Cycle>> = state
    override val activeCycle: Flow<Cycle?> = state.map { cycles -> cycles.firstOrNull { it.endDate == null } }

    override suspend fun startCycle(startDate: LocalDate) {
        val current = state.value.toMutableList()
        val active = current.indexOfFirst { it.endDate == null }
        if (active != -1) return
        val newCycle = Cycle(
            id = UUID.randomUUID().toString(),
            startDate = startDate,
            endDate = null,
            averageLengthDays = estimateAverageLength(current),
            lutealPhaseDays = 14,
            confidence = 0.6f,
        )
        current.add(0, newCycle)
        state.value = current.sortedByDescending { it.startDate }
    }

    override suspend fun endCycle(endDate: LocalDate) {
        val updated = state.value.map { cycle ->
            if (cycle.endDate == null && endDate.isAfter(cycle.startDate)) {
                cycle.copy(endDate = endDate, confidence = 0.9f)
            } else {
                cycle
            }
        }
        state.value = updated
    }

    private fun estimateAverageLength(cycles: List<Cycle>): Int {
        val lengths = cycles.mapNotNull { cycle ->
            cycle.endDate?.let { end -> end.toEpochDay() - cycle.startDate.toEpochDay() }
        }
        if (lengths.isEmpty()) return 28
        return lengths.average().toInt().coerceIn(21, 35)
    }
}

private class InMemoryLogRepository : LogRepository {
    private val state = MutableStateFlow(sampleLogs())
    override val logs: Flow<List<LogEntry>> = state.map { entries -> entries.sortedByDescending { it.date } }

    override suspend fun addOrUpdate(entry: LogEntry) {
        val current = state.value.toMutableList()
        val index = current.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            current[index] = entry
        } else {
            current.add(entry.copy(id = UUID.randomUUID().toString()))
        }
        state.value = current
    }
}

private class InMemoryReminderRepository : ReminderRepository {
    private val state = MutableStateFlow(sampleReminders())
    override val reminders: Flow<List<Reminder>> = state

    override suspend fun toggle(reminderId: String, enabled: Boolean) {
        state.value = state.value.map { reminder ->
            if (reminder.id == reminderId) reminder.copy(enabled = enabled) else reminder
        }
    }

    override suspend fun addReminder(type: ReminderType, time: LocalTime) {
        state.value = state.value + Reminder(
            id = UUID.randomUUID().toString(),
            type = type,
            time = time,
            enabled = true
        )
    }
}

private class InMemoryUserPreferencesRepository : UserPreferencesRepository {
    private val state = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = state

    override suspend fun updateTheme(theme: ThemeSetting) {
        state.value = state.value.copy(theme = theme)
    }

    override suspend fun updateUnits(temperatureUnit: TemperatureUnit, weightUnit: WeightUnit) {
        state.value = state.value.copy(
            temperatureUnit = temperatureUnit,
            weightUnit = weightUnit
        )
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        state.value = state.value.copy(notificationsEnabled = enabled)
    }

    override suspend fun updateAnalyticsOptIn(optIn: Boolean) {
        state.value = state.value.copy(analyticsOptIn = optIn)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    @Provides
    @Singleton
    fun provideCycleRepository(): CycleRepository = InMemoryCycleRepository()

    @Provides
    @Singleton
    fun provideLogRepository(): LogRepository = InMemoryLogRepository()

    @Provides
    @Singleton
    fun provideReminderRepository(): ReminderRepository = InMemoryReminderRepository()

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(): UserPreferencesRepository = InMemoryUserPreferencesRepository()
}

private fun sampleCycles(): List<Cycle> {
    val today = LocalDate.now()
    val lastStart = today.minusDays(27)
    val previousStart = lastStart.minusDays(30)
    return listOf(
        Cycle(
            id = UUID.randomUUID().toString(),
            startDate = lastStart,
            endDate = today.minusDays(1),
            averageLengthDays = 28,
            lutealPhaseDays = 14,
            confidence = 0.85f,
        ),
        Cycle(
            id = UUID.randomUUID().toString(),
            startDate = previousStart,
            endDate = lastStart.minusDays(1),
            averageLengthDays = 28,
            lutealPhaseDays = 14,
            confidence = 0.8f,
        )
    )
}

private fun sampleLogs(): List<LogEntry> {
    val today = LocalDate.now()
    return listOf(
        LogEntry(
            id = UUID.randomUUID().toString(),
            date = today.minusDays(1),
            bleedingLevel = BleedingLevel.LIGHT,
            symptoms = listOf("cramps", "fatigue"),
            mood = Mood.NEUTRAL,
            temperatureCelsius = 36.7f,
            weightKg = 62.5f,
            notes = "Лёгкие спазмы, помог тёплый чай",
        ),
        LogEntry(
            id = UUID.randomUUID().toString(),
            date = today.minusDays(3),
            bleedingLevel = BleedingLevel.MEDIUM,
            symptoms = listOf("headache"),
            mood = Mood.NEGATIVE,
            temperatureCelsius = null,
            weightKg = null,
            notes = "Головная боль ближе к вечеру",
        )
    )
}

private fun sampleReminders(): List<Reminder> = listOf(
    Reminder(
        id = UUID.randomUUID().toString(),
        type = ReminderType.CYCLE_START,
        time = LocalTime.of(8, 0),
        enabled = true
    ),
    Reminder(
        id = UUID.randomUUID().toString(),
        type = ReminderType.MEDICATION,
        time = LocalTime.of(21, 30),
        enabled = true
    )
)
