package com.example.mycycle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mycycle.data.local.DatabaseProvider
import com.example.mycycle.data.local.MyCycleDatabase
import com.example.mycycle.data.local.dao.CycleDao
import com.example.mycycle.data.local.dao.LogEntryDao
import com.example.mycycle.data.local.dao.ReminderDao
import com.example.mycycle.data.local.entity.CycleEntity
import com.example.mycycle.data.local.entity.LogEntryEntity
import com.example.mycycle.data.local.entity.ReminderEntity
import com.example.mycycle.model.BleedingLevel
import com.example.mycycle.model.Cycle
import com.example.mycycle.model.LogEntry
import com.example.mycycle.model.Mood
import com.example.mycycle.model.Reminder
import com.example.mycycle.model.ReminderType
import com.example.mycycle.model.TemperatureUnit
import com.example.mycycle.model.ThemeSetting
import com.example.mycycle.model.UserPreferences
import com.example.mycycle.model.WeightUnit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

private class RoomCycleRepository(
    private val cycleDao: CycleDao,
) : CycleRepository {
    override val cycles: Flow<List<Cycle>> = cycleDao.observeCycles().map { entities ->
        entities.map { it.toModel() }
    }

    override val activeCycle: Flow<Cycle?> = cycleDao.observeActive().map { it?.toModel() }

    override suspend fun startCycle(startDate: LocalDate) {
        val existingActive = activeCycle.first()
        if (existingActive != null) return
        val allCycles = cycles.first()
        val average = allCycles.mapNotNull { cycle -> cycle.endDate?.toEpochDay()?.minus(cycle.startDate.toEpochDay()) }
            .average()
            .takeIf { !it.isNaN() }
            ?.toInt() ?: 28
        val entity = CycleEntity(
            id = UUID.randomUUID().toString(),
            startDate = startDate,
            endDate = null,
            averageLengthDays = average,
            lutealPhaseDays = 14,
            confidence = 0.7f,
        )
        cycleDao.insert(entity)
    }

    override suspend fun endCycle(endDate: LocalDate) {
        val active = cycleDao.observeActive().first() ?: return
        if (endDate.isAfter(active.startDate)) {
            cycleDao.update(active.copy(endDate = endDate, confidence = 0.9f))
        }
    }
}

private class RoomLogRepository(
    private val logEntryDao: LogEntryDao,
) : LogRepository {
    override val logs: Flow<List<LogEntry>> = logEntryDao.observeLogs().map { entities ->
        entities.map { it.toModel() }
    }

    override suspend fun addOrUpdate(entry: LogEntry) {
        val entity = entry.toEntity().copy(id = entry.id.ifEmpty { UUID.randomUUID().toString() })
        logEntryDao.insert(entity)
    }
}

private class RoomReminderRepository(
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler,
) : ReminderRepository {
    override val reminders: Flow<List<Reminder>> = reminderDao.observeReminders().map { items ->
        items.map { it.toModel() }
    }

    override suspend fun toggle(reminderId: String, enabled: Boolean) {
        val current = reminderDao.observeReminders().first().firstOrNull { it.id == reminderId } ?: return
        val updated = current.copy(enabled = enabled)
        reminderDao.update(updated)
        scheduler.updateSchedule(updated)
    }

    override suspend fun addReminder(type: ReminderType, time: LocalTime) {
        val entity = ReminderEntity(
            id = UUID.randomUUID().toString(),
            type = type.name,
            time = time,
            enabled = true,
        )
        reminderDao.insert(entity)
        scheduler.updateSchedule(entity)
    }
}

private class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private val themeKey = intPreferencesKey("theme")
    private val tempUnitKey = intPreferencesKey("temp_unit")
    private val weightUnitKey = intPreferencesKey("weight_unit")
    private val notificationsKey = booleanPreferencesKey("notifications")
    private val analyticsKey = booleanPreferencesKey("analytics")

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            theme = ThemeSetting.fromOrdinal(prefs[themeKey] ?: ThemeSetting.SYSTEM.ordinal),
            temperatureUnit = TemperatureUnit.fromOrdinal(prefs[tempUnitKey] ?: TemperatureUnit.CELSIUS.ordinal),
            weightUnit = WeightUnit.fromOrdinal(prefs[weightUnitKey] ?: WeightUnit.KILOGRAMS.ordinal),
            notificationsEnabled = prefs[notificationsKey] ?: true,
            analyticsOptIn = prefs[analyticsKey] ?: false,
        )
    }

    override suspend fun updateTheme(theme: ThemeSetting) {
        dataStore.edit { it[themeKey] = theme.ordinal }
    }

    override suspend fun updateUnits(temperatureUnit: TemperatureUnit, weightUnit: WeightUnit) {
        dataStore.edit {
            it[tempUnitKey] = temperatureUnit.ordinal
            it[weightUnitKey] = weightUnit.ordinal
        }
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        dataStore.edit { it[notificationsKey] = enabled }
    }

    override suspend fun updateAnalyticsOptIn(optIn: Boolean) {
        dataStore.edit { it[analyticsKey] = optIn }
    }
}

private fun CycleEntity.toModel(): Cycle = Cycle(
    id = id,
    startDate = startDate,
    endDate = endDate,
    averageLengthDays = averageLengthDays,
    lutealPhaseDays = lutealPhaseDays,
    confidence = confidence,
)

private fun LogEntryEntity.toModel(): LogEntry = LogEntry(
    id = id,
    date = date,
    bleedingLevel = BleedingLevel.valueOf(bleedingLevel),
    symptoms = symptoms,
    mood = Mood.valueOf(mood),
    temperatureCelsius = temperatureCelsius,
    weightKg = weightKg,
    notes = notes
)

private fun ReminderEntity.toModel(): Reminder = Reminder(
    id = id,
    type = ReminderType.valueOf(type),
    time = time,
    enabled = enabled
)

private fun LogEntry.toEntity(): LogEntryEntity = LogEntryEntity(
    id = id,
    date = date,
    bleedingLevel = bleedingLevel.name,
    symptoms = symptoms,
    mood = mood.name,
    temperatureCelsius = temperatureCelsius,
    weightKg = weightKg,
    notes = notes
)

internal val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(context: Context): MyCycleDatabase = DatabaseProvider.create(context)

    @Provides
    @Singleton
    fun provideCycleRepository(database: MyCycleDatabase): CycleRepository = RoomCycleRepository(database.cycleDao())

    @Provides
    @Singleton
    fun provideLogRepository(database: MyCycleDatabase): LogRepository = RoomLogRepository(database.logEntryDao())

    @Provides
    @Singleton
    fun provideReminderRepository(database: MyCycleDatabase, scheduler: ReminderScheduler): ReminderRepository =
        RoomReminderRepository(database.reminderDao(), scheduler)

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(context: Context): UserPreferencesRepository =
        DataStoreUserPreferencesRepository(context.userPrefsDataStore)

    @Provides
    @Singleton
    fun provideReminderScheduler(context: Context): ReminderScheduler = ReminderScheduler(context)
}
