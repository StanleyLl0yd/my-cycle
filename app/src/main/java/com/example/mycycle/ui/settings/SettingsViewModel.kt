package com.example.mycycle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
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
}
