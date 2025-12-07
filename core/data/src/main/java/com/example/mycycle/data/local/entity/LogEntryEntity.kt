package com.example.mycycle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "logs")
internal data class LogEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val bleedingLevel: String,
    val symptoms: List<String>,
    val mood: String,
    val temperatureCelsius: Float?,
    val weightKg: Float?,
    val notes: String?,
)
