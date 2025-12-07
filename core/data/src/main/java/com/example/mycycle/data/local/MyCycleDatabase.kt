package com.example.mycycle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mycycle.data.local.dao.CycleDao
import com.example.mycycle.data.local.dao.LogEntryDao
import com.example.mycycle.data.local.dao.ReminderDao
import com.example.mycycle.data.local.entity.CycleEntity
import com.example.mycycle.data.local.entity.LogEntryEntity
import com.example.mycycle.data.local.entity.ReminderEntity

@Database(
    entities = [CycleEntity::class, LogEntryEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(MyCycleTypeConverters::class)
internal abstract class MyCycleDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun reminderDao(): ReminderDao
}
