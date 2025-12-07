package com.example.mycycle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "reminders")
internal data class ReminderEntity(
    @PrimaryKey val id: String,
    val type: String,
    val time: LocalTime,
    val enabled: Boolean
)
