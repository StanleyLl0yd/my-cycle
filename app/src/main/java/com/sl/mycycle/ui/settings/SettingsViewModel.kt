package com.sl.mycycle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.ThemeMode
import com.sl.mycycle.util.runSuspendCatching
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsState(
    val cycleStage: CycleStage = CycleStage.NOT_SET,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true,
    val isClearingData: Boolean = false,
    val hasOperationError: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _state.update { current ->
                    current.copy(
                        cycleStage = prefs.cycleStage,
                        themeMode = prefs.themeMode,
                        useDynamicColors = prefs.useDynamicColors
                    )
                }
            }
        }
    }

    fun setCycleStage(stage: CycleStage) {
        if (_state.value.isClearingData) return
        viewModelScope.launch {
            val result = runSuspendCatching {
                preferencesRepository.updateCycleStage(stage)
            }
            _state.update { it.copy(hasOperationError = result.isFailure) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        if (_state.value.isClearingData) return
        viewModelScope.launch {
            val result = runSuspendCatching {
                preferencesRepository.updateTheme(mode, _state.value.useDynamicColors)
            }
            _state.update { it.copy(hasOperationError = result.isFailure) }
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        if (_state.value.isClearingData) return
        viewModelScope.launch {
            val result = runSuspendCatching {
                preferencesRepository.updateTheme(_state.value.themeMode, enabled)
            }
            _state.update { it.copy(hasOperationError = result.isFailure) }
        }
    }

    suspend fun buildCsvExport(): String {
        val days = cycleDayRepository.observeAll().first().sortedBy { it.date }

        return buildString {
            append('\uFEFF')
            appendLine("date,period,flow,mood,symptoms,notes")
            days.forEach { day ->
                appendLine(
                    listOf(
                        day.date.toString(),
                        day.hasPeriod.toString(),
                        day.flowIntensity?.name.orEmpty(),
                        day.mood?.name.orEmpty(),
                        day.symptoms.joinToString("|") { it.name },
                        day.notes.orEmpty()
                    ).joinToString(",") { csvEscape(it) }
                )
            }
        }
    }

    fun clearAllData() {
        if (_state.value.isClearingData) return

        viewModelScope.launch {
            _state.update { it.copy(isClearingData = true, hasOperationError = false) }
            val daysBeforeClear = cycleDayRepository.observeAll().first()
            var daysDeleted = false

            val result = runSuspendCatching {
                try {
                    cycleDayRepository.deleteAll()
                    daysDeleted = true
                    preferencesRepository.clearAll()
                } catch (error: Throwable) {
                    if (daysDeleted && daysBeforeClear.isNotEmpty()) {
                        withContext(NonCancellable) {
                            try {
                                daysBeforeClear.forEach { cycleDayRepository.save(it) }
                            } catch (rollbackError: Exception) {
                                error.addSuppressed(rollbackError)
                            }
                        }
                    }
                    throw error
                }
            }

            _state.update {
                it.copy(
                    isClearingData = false,
                    hasOperationError = result.isFailure
                )
            }
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '\n' || it == '\r' || it == '\"' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
