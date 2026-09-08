package com.sl.mycycle.reminder

import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.Prediction
import java.time.LocalDate

enum class ReminderKind {
    NONE,
    DIARY,
    PERIOD_WINDOW
}

class ReminderDecisionEngine {

    companion object {
        private const val DAYS_BEFORE_WINDOW = 2
    }

    fun decide(
        today: LocalDate,
        todayEntry: CycleDay?,
        prediction: Prediction?
    ): ReminderKind {
        if (todayEntry != null) return ReminderKind.NONE
        val window = prediction?.nextPeriodStartWindow ?: return ReminderKind.DIARY
        val reminderStart = window.start.minusDays(DAYS_BEFORE_WINDOW.toLong())
        return if (!today.isBefore(reminderStart) && !today.isAfter(window.end)) {
            ReminderKind.PERIOD_WINDOW
        } else {
            ReminderKind.DIARY
        }
    }
}
