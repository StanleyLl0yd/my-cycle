package com.example.mycycle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "cycles")
internal data class CycleEntity(
    @PrimaryKey val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val averageLengthDays: Int,
    val lutealPhaseDays: Int,
    val confidence: Float
)
