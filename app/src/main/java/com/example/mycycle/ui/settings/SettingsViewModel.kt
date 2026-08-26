package com.example.mycycle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true
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
                _state.update {
                    SettingsState(
                        themeMode = prefs.themeMode,
                        useDynamicColors = prefs.useDynamicColors
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(mode, _state.value.useDynamicColors)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(_state.value.themeMode, enabled)
        }
    }

    suspend fun buildCsvExport(): String {
        val days = cycleDayRepository.observeAll().first().sortedBy { it.date }

        return buildString {
            // UTF-8 BOM improves non-Latin text detection in desktop spreadsheet apps.
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
        viewModelScope.launch {
            cycleDayRepository.deleteAll()
            preferencesRepository.clearAll()
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
