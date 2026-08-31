package com.silverlightning.mycycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverlightning.mycycle.data.preferences.UserPreferencesRepository
import com.silverlightning.mycycle.data.repository.CycleDayRepository
import com.silverlightning.mycycle.domain.engine.CycleDetector
import com.silverlightning.mycycle.domain.engine.PredictionEngine
import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.DayState
import com.silverlightning.mycycle.domain.model.Prediction
import com.silverlightning.mycycle.domain.model.UserPreferences
import com.silverlightning.mycycle.util.ClockProvider
import com.silverlightning.mycycle.util.currentDateFlow
import com.silverlightning.mycycle.util.runSuspendCatching
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_CYCLE_LENGTH = 28
private const val DEFAULT_PERIOD_LENGTH = 5

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val dayStates: Map<LocalDate, DayState> = emptyMap(),
    val prediction: Prediction? = null,
    val isLoading: Boolean = true
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

            val dates = historicalPeriodDates(current.startDate, current.endDate)
            val boundaryDate = historicalPeriodBoundaryDate(current.endDate, today)
            val previousDays = mutableMapOf<LocalDate, CycleDay?>()
            val changedDates = mutableListOf<LocalDate>()

            val result = withContext(NonCancellable) {
                val saveResult = runSuspendCatching {
                    dates.forEach { date ->
                        val previous = cycleDayRepository.getByDate(date)
                        previousDays[date] = previous
                        cycleDayRepository.save(mergeHistoricalPeriodDay(date, previous))
                        changedDates += date
                    }

                    boundaryDate?.let { date ->
                        if (cycleDayRepository.getByDate(date) == null) {
                            previousDays[date] = null
                            cycleDayRepository.save(CycleDay(date = date))
                            changedDates += date
                        }
                    }
                }

                if (saveResult.isFailure) {
                    val originalError = saveResult.exceptionOrNull()
                    changedDates.asReversed().forEach { date ->
                        val rollbackResult = runSuspendCatching {
                            previousDays[date]?.let { cycleDayRepository.save(it) }
                                ?: cycleDayRepository.delete(date)
                        }
                        rollbackResult.exceptionOrNull()?.let { rollbackError ->
                            originalError?.addSuppressed(rollbackError)
                        }
                    }
                }

                saveResult
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
                    lastPeriodStart = lastPeriodStart,
                    prediction = prediction
                )

                _state.update {
                    CalendarState(
                        currentMonth = input.month,
                        today = input.today,
                        dayStates = dayStatesMap,
                        prediction = prediction,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun buildDayStates(
        month: YearMonth,
        today: LocalDate,
        daysMap: Map<LocalDate, CycleDay>,
        lastPeriodStart: LocalDate?,
        prediction: Prediction?
    ): Map<LocalDate, DayState> {
        val result = mutableMapOf<LocalDate, DayState>()

        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            val cycleDay = lastPeriodStart
                ?.let { ChronoUnit.DAYS.between(it, date).toInt() + 1 }
                ?.takeIf { it > 0 }
            val existingDay = daysMap[date]

            result[date] = DayState(
                date = date,
                cycleDay = cycleDay,
                periodState = resolvePeriodState(date, existingDay, prediction),
                fertilityState = resolveFertilityState(date, existingDay, prediction),
                symptoms = existingDay?.symptoms ?: emptySet(),
                mood = existingDay?.mood,
                hasNotes = !existingDay?.notes.isNullOrBlank(),
                isToday = date == today,
                isCurrentMonth = true
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
