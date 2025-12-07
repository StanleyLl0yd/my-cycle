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
                    ensureTable(database, "cycles") {
                        """
                        CREATE TABLE IF NOT EXISTS `cycles` (
                            `id` TEXT NOT NULL,
                            `startDate` INTEGER NOT NULL,
                            `endDate` INTEGER,
                            `averageLengthDays` INTEGER NOT NULL,
                            `lutealPhaseDays` INTEGER NOT NULL,
                            `confidence` REAL NOT NULL DEFAULT 0.8,
                            PRIMARY KEY(`id`)
                        )
                        """
                    }
                    ensureTable(database, "logs") {
                        """
                        CREATE TABLE IF NOT EXISTS `logs` (
                            `id` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `bleedingLevel` TEXT NOT NULL,
                            `symptoms` TEXT NOT NULL,
                            `mood` TEXT NOT NULL,
                            `temperatureCelsius` REAL,
                            `weightKg` REAL,
                            `notes` TEXT,
                            PRIMARY KEY(`id`)
                        )
                        """
                    }
                    ensureTable(database, "reminders") {
                        """
                        CREATE TABLE IF NOT EXISTS `reminders` (
                            `id` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `time` INTEGER NOT NULL,
                            `enabled` INTEGER NOT NULL DEFAULT 1,
                            PRIMARY KEY(`id`)
                        )
                        """
                    }

                    addColumnIfMissing(database, "cycles", "confidence", "REAL NOT NULL DEFAULT 0.8")
                    addColumnIfMissing(database, "logs", "notes", "TEXT")
                    addColumnIfMissing(database, "logs", "temperatureCelsius", "REAL")
                    addColumnIfMissing(database, "logs", "weightKg", "REAL")
                    addColumnIfMissing(database, "reminders", "enabled", "INTEGER NOT NULL DEFAULT 1")
                }
            }
        )
    }
}

private fun addColumnIfMissing(
    database: androidx.sqlite.db.SupportSQLiteDatabase,
    table: String,
    column: String,
    definition: String
) {
    val hasColumn = database.query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
            .any { it == column }
    }
    if (!hasColumn) {
        database.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
    }
}

private fun ensureTable(
    database: androidx.sqlite.db.SupportSQLiteDatabase,
    table: String,
    createStatement: () -> String
) {
    val exists = database.query(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        arrayOf(table)
    ).use { it.moveToFirst() }
    if (!exists) {
        database.execSQL(createStatement())
    }
}
