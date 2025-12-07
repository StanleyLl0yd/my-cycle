package com.example.mycycle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.example.mycycle.data.local.dao.CycleDao
import com.example.mycycle.data.local.dao.LogEntryDao
import com.example.mycycle.data.local.dao.ReminderDao
import com.example.mycycle.data.local.entity.CycleEntity
import com.example.mycycle.data.local.entity.LogEntryEntity
import com.example.mycycle.data.local.entity.ReminderEntity

@Database(
    entities = [CycleEntity::class, LogEntryEntity::class, ReminderEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(MyCycleTypeConverters::class)
internal abstract class MyCycleDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun reminderDao(): ReminderDao

    internal companion object {
        val migrations: Array<Migration> = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // No-op migration reserved for future schema changes.
                }
            }
        )
    }
}
