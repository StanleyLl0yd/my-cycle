package com.example.mycycle.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class MyCycleTypeConverters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromSeconds(value: Int?): LocalTime? = value?.let { LocalTime.ofSecondOfDay(it.toLong()) }

    @TypeConverter
    fun toSeconds(time: LocalTime?): Int? = time?.toSecondOfDay()

    @TypeConverter
    fun fromStringList(value: String?): List<String> =
        value?.takeIf { it.isNotEmpty() }?.let { Json.decodeFromString(it) } ?: emptyList()

    @TypeConverter
    fun toStringList(list: List<String>): String = Json.encodeToString(list)
}
