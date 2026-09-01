package com.sl.mycycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.engine.CycleDetector
import com.sl.mycycle.domain.engine.PredictionEngine
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.DayState
import com.sl.mycycle.domain.model.Prediction
import com.sl.mycycle.domain.model.UserPreferences
import com.sl.mycycle.util.ClockProvider
import com.sl.mycycle.util.currentDateFlow
import com.sl.mycycle.util.runSuspendCatching
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_CYCLE_LENGTH = 28
private const val DEFAULT_PERIOD_LENGTH = 5

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val dayStates: Map<LocalDate, DayState> = emptyMap(),
    val prediction: Prediction? = null
)

data class HistoricalPeriodState(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isSaving: Boolean = false,
    val hasSaveError: Boolean = false,
    val isSaved: Boolean = false
)

class CalendarViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector,
    private val predictionEngine: PredictionEngine,
    private val clockProvider: ClockProvider
) : ViewModel() {

    private val initialToday = clockProvider.today()
    private val _state = MutableStateFlow(
        CalendarState(
            currentMonth = YearMonth.from(initialToday),
            today = initialToday
        )
    )
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val selectedMonth = MutableStateFlow(YearMonth.from(initialToday))
    private var latestCycleLength = DEFAULT_CYCLE_LENGTH
    private var latestPeriodLength = DEFAULT_PERIOD_LENGTH
    private var latestPeriodStart: LocalDate? = null

    private val initialHistoricalStart = initialToday.minusDays(DEFAULT_CYCLE_LENGTH.toLong())
    private val _historicalPeriodState = MutableStateFlow(
        HistoricalPeriodState(
            startDate = initialHistoricalStart,
            endDate = initialHistoricalStart.plusDays(DEFAULT_PERIOD_LENGTH - 1L)
        )
    )
    val historicalPeriodState: StateFlow<HistoricalPeriodState> =
        _historicalPeriodState.asStateFlow()

    init {
        loadData()
    }

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonth.update { it.plusMonths(1) }
    }

    fun goToToday() {
        selectedMonth.value = YearMonth.from(clockProvider.today())
    }

    fun resetHistoricalPeriodEntry() {
        if (_historicalPeriodState.value.isSaving) return

        val today = clockProvider.today()
        val referenceDate = latestPeriodStart ?: today
        val startDate = referenceDate.minusDays(latestCycleLength.toLong())
        val endDate = startDate
            .plusDays((latestPeriodLength - 1).coerceAtLeast(0).toLong())
            .coerceAtMost(today)

        _historicalPeriodState.value = HistoricalPeriodState(
            startDate = startDate,
            endDate = endDate
        )
    }

    fun setHistoricalPeriodStart(date: LocalDate) {
        if (_historicalPeriodState.value.isSaving) return

        val today = clockProvider.today()
        val safeDate = date.coerceAtMost(today)
        _historicalPeriodState.update { current ->
            val currentLength = ChronoUnit.DAYS
                .between(current.startDate, current.endDate)
                .coerceAtLeast(0)
            current.copy(
                startDate = safeDate,
                endDate = safeDate.plusDays(currentLength).coerceAtMost(today),
                hasSaveError = false,
                isSaved = false
            )
        }
    }

    fun setHistoricalPeriodEnd(date: LocalDate) {
        if (_historicalPeriodState.value.isSaving) return

        val today = clockProvider.today()
        _historicalPeriodState.update { current ->
            current.copy(
                endDate = date.coerceIn(current.startDate, today),
                hasSaveError = false,
                isSaved = false
            )
        }
    }

    fun prepareAnotherHistoricalPeriod() {
        if (_historicalPeriodState.value.isSaving) return

        _historicalPeriodState.update { current ->
            val periodLength = ChronoUnit.DAYS
                .between(current.startDate, current.endDate)
                .coerceAtLeast(0)
            val startDate = current.startDate.minusDays(latestCycleLength.toLong())
            current.copy(
                startDate = startDate,
                endDate = startDate.plusDays(periodLength),
                hasSaveError = false,
                isSaved = false
            )
        }
    }

    fun saveHistoricalPeriod() {
        if (_historicalPeriodState.value.isSaving) return

        viewModelScope.launch {
            val current = _historicalPeriodState.value
            val today = clockProvider.today()
            if (current.startDate.isAfter(current.endDate) || current.endDate.isAfter(today)) {
                _historicalPeriodState.update { it.copy(hasSaveError = true) }
                return@launch
            }

            _historicalPeriodState.update {
                it.copy(isSaving = true, hasSaveError = false, isSaved = false)
            }

            val result = runSuspendCatching {
                val daysToSave = historicalPeriodDates(current.startDate, current.endDate)
                    .map { date ->
                        val previous = cycleDayRepository.getByDate(date)
                        mergeHistoricalPeriodDay(date, previous)
                    }
                    .toMutableList()

                historicalPeriodBoundaryDate(current.endDate, today)?.let { date ->
                    if (cycleDayRepository.getByDate(date) == null) {
                        daysToSave += CycleDay(date = date)
                    }
                }

                cycleDayRepository.saveAll(daysToSave)
            }

            if (result.isSuccess) {
                selectedMonth.value = YearMonth.from(current.startDate)
                _historicalPeriodState.update {
                    it.copy(isSaving = false, hasSaveError = false, isSaved = true)
                }
            } else {
                _historicalPeriodState.update {
                    it.copy(isSaving = false, hasSaveError = true, isSaved = false)
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferences,
                cycleDayRepository.observeAll(),
                selectedMonth,
                currentDateFlow(clockProvider)
            ) { preferences, allDays, month, today ->
                CalendarInput(preferences, allDays, month, today)
            }.collect { input ->
                val previousToday = _state.value.today
                val previousCurrentMonth = YearMonth.from(previousToday)
                val actualCurrentMonth = YearMonth.from(input.today)
                if (
                    input.month == previousCurrentMonth &&
                    input.month != actualCurrentMonth
                ) {
                    selectedMonth.value = actualCurrentMonth
                    return@collect
                }

                val cycles = cycleDetector.detectCycles(input.allDays)

                val prediction = if (cycles.isNotEmpty()) {
                    predictionEngine.predictFromHistory(
                        cycles = cycles,
                        fallbackCycleLength = input.preferences.estimatedCycleLength,
                        fallbackPeriodLength = input.preferences.estimatedPeriodLength,
                        stage = input.preferences.cycleStage,
                        referenceDate = input.today
                    )
                } else if (input.preferences.initialPeriodDate != null) {
                    predictionEngine.predictFromOnboarding(
                        lastPeriodStart = input.preferences.initialPeriodDate,
                        cycleLength = input.preferences.estimatedCycleLength,
                        periodLength = input.preferences.estimatedPeriodLength,
                        stage = input.preferences.cycleStage
                    )
                } else {
                    null
                }

                val lastPeriodStart = cycles.lastOrNull()?.startDate
                    ?: input.preferences.initialPeriodDate

                latestCycleLength = input.preferences.estimatedCycleLength
                latestPeriodLength = input.preferences.estimatedPeriodLength
                latestPeriodStart = lastPeriodStart

                val daysMap = input.allDays.associateBy { it.date }
                val dayStatesMap = buildDayStates(
                    month = input.month,
                    today = input.today,
                    daysMap = daysMap,
                    prediction = prediction
                )

                _state.value = CalendarState(
                    currentMonth = input.month,
                    today = input.today,
                    dayStates = dayStatesMap,
                    prediction = prediction
                )
            }
        }
    }

    private fun buildDayStates(
        month: YearMonth,
        today: LocalDate,
        daysMap: Map<LocalDate, CycleDay>,
        prediction: Prediction?
    ): Map<LocalDate, DayState> {
        val result = mutableMapOf<LocalDate, DayState>()

        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            val existingDay = daysMap[date]

            result[date] = DayState(
                periodState = resolvePeriodState(date, existingDay, prediction),
                fertilityState = resolveFertilityState(date, existingDay, prediction),
                isToday = date == today
            )
        }

        return result
    }

    private data class CalendarInput(
        val preferences: UserPreferences,
        val allDays: List<CycleDay>,
        val month: YearMonth,
        val today: LocalDate
    )
}
