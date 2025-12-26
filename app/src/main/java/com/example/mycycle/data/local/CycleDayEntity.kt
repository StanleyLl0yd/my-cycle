package com.example.mycycle.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mycycle.domain.model.CycleDay
import com.example.mycycle.domain.model.FlowIntensity
import com.example.mycycle.domain.model.Mood
import com.example.mycycle.domain.model.Symptom
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "cycle_days")
data class CycleDayEntity(
    @PrimaryKey
    val date: LocalDate,
    val hasPeriod: Boolean = false,
    val flowIntensityLevel: Int? = null,
    val moodLevel: Int? = null,
    val symptomsMask: Int = 0,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain(): CycleDay = CycleDay(
        date = date,
        hasPeriod = hasPeriod,
        flowIntensity = flowIntensityLevel?.let { FlowIntensity.fromLevel(it) },
        mood = moodLevel?.let { Mood.fromLevel(it) },
        symptoms = Symptom.fromMask(symptomsMask),
        notes = notes
    )

    companion object {
        fun fromDomain(domain: CycleDay): CycleDayEntity = CycleDayEntity(
            date = domain.date,
            hasPeriod = domain.hasPeriod,
            flowIntensityLevel = domain.flowIntensity?.level,
            moodLevel = domain.mood?.level,
            symptomsMask = Symptom.toMask(domain.symptoms),
            notes = domain.notes
        )
    }
}
