package com.example.mycycle.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun toInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)
}
