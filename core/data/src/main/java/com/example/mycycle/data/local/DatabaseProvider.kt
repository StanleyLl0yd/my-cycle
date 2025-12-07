package com.example.mycycle.data.local

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

public object DatabaseProvider {
    public fun create(context: Context): MyCycleDatabase {
        val database = Room.databaseBuilder(
            context,
            MyCycleDatabase::class.java,
            "mycycle.db"
        ).addMigrations(*MyCycleDatabase.migrations).build()

        runBlocking(Dispatchers.IO) { database.seedIfEmpty() }
        return database
    }
}

internal suspend fun MyCycleDatabase.seedIfEmpty() {
    withContext(Dispatchers.IO) {
        val existing = cycleDao().observeCycles().first()
        if (existing.isNotEmpty()) return@withContext

        val today = java.time.LocalDate.now()
        val lastStart = today.minusDays(27)
        val previousStart = lastStart.minusDays(30)
        cycleDao().insert(
            com.example.mycycle.data.local.entity.CycleEntity(
                id = java.util.UUID.randomUUID().toString(),
                startDate = lastStart,
                endDate = today.minusDays(1),
                averageLengthDays = 28,
                lutealPhaseDays = 14,
                confidence = 0.85f
            )
        )
        cycleDao().insert(
            com.example.mycycle.data.local.entity.CycleEntity(
                id = java.util.UUID.randomUUID().toString(),
                startDate = previousStart,
                endDate = lastStart.minusDays(1),
                averageLengthDays = 28,
                lutealPhaseDays = 14,
                confidence = 0.8f
            )
        )
        logEntryDao().insert(
            com.example.mycycle.data.local.entity.LogEntryEntity(
                id = java.util.UUID.randomUUID().toString(),
                date = today.minusDays(1),
                bleedingLevel = com.example.mycycle.model.BleedingLevel.LIGHT.name,
                symptoms = listOf("cramps", "fatigue"),
                mood = com.example.mycycle.model.Mood.NEUTRAL.name,
                temperatureCelsius = 36.7f,
                weightKg = 62.5f,
                notes = "Лёгкие спазмы, помог тёплый чай",
            )
        )
        logEntryDao().insert(
            com.example.mycycle.data.local.entity.LogEntryEntity(
                id = java.util.UUID.randomUUID().toString(),
                date = today.minusDays(3),
                bleedingLevel = com.example.mycycle.model.BleedingLevel.MEDIUM.name,
                symptoms = listOf("headache"),
                mood = com.example.mycycle.model.Mood.NEGATIVE.name,
                temperatureCelsius = null,
                weightKg = null,
                notes = "Головная боль ближе к вечеру",
            )
        )
        reminderDao().insert(
            com.example.mycycle.data.local.entity.ReminderEntity(
                id = java.util.UUID.randomUUID().toString(),
                type = com.example.mycycle.model.ReminderType.CYCLE_START.name,
                time = java.time.LocalTime.of(8, 0),
                enabled = true
            )
        )
        reminderDao().insert(
            com.example.mycycle.data.local.entity.ReminderEntity(
                id = java.util.UUID.randomUUID().toString(),
                type = com.example.mycycle.model.ReminderType.MEDICATION.name,
                time = java.time.LocalTime.of(21, 30),
                enabled = true
            )
        )
    }
}
