package com.example.mycycle.domain.model

import java.time.LocalDate

data class Cycle(
    val id: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val periodEndDate: LocalDate,
    val length: Int?,
    val periodLength: Int,
    val isComplete: Boolean
)
