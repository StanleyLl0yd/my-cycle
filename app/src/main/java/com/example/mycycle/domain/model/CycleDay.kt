package com.example.mycycle.domain.model

import java.time.LocalDate

data class CycleDay(
    val date: LocalDate,
    val hasPeriod: Boolean = false,
    val flowIntensity: FlowIntensity? = null,
    val mood: Mood? = null,
    val symptoms: Set<Symptom> = emptySet(),
    val notes: String? = null
)
