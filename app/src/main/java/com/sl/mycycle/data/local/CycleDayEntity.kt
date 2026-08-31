package com.sl.mycycle.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.FlowIntensity
import com.sl.mycycle.domain.model.Mood
import com.sl.mycycle.domain.model.Symptom
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
    fun toDomain(): CycleDay {
        val flow = flowIntensityLevel?.let { FlowIntensity.fromLevel(it) }
        val normalizedHasPeriod = when (flow) {
            FlowIntensity.SPOTTING -> false
            FlowIntensity.LIGHT,
            FlowIntensity.MEDIUM,
            FlowIntensity.HEAVY -> true
            null -> hasPeriod
        }

        return CycleDay(
            date = date,
            hasPeriod = normalizedHasPeriod,
            flowIntensity = flow,
            mood = moodLevel?.let { Mood.fromLevel(it) },
            symptoms = Symptom.fromMask(symptomsMask),
            notes = notes
        )
    }

    companion object {
        fun fromDomain(domain: CycleDay): CycleDayEntity = CycleDayEntity(
            date = domain.date,
            hasPeriod = domain.isPeriodBleeding,
            flowIntensityLevel = domain.flowIntensity?.level,
            moodLevel = domain.mood?.level,
            symptomsMask = Symptom.toMask(domain.symptoms),
            notes = domain.notes
        )
    }
}
