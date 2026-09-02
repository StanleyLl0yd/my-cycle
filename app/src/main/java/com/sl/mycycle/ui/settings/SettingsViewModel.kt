package com.sl.mycycle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.data.transfer.BackupPreview
import com.sl.mycycle.data.transfer.CsvImportPreview
import com.sl.mycycle.data.transfer.DataPortabilityService
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.ThemeMode
import com.sl.mycycle.reminder.ReminderScheduler
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
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val appLockEnabled: Boolean = false,
    val protectScreenEnabled: Boolean = false,
    val isClearingData: Boolean = false,
    val hasOperationError: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val dataPortabilityService: DataPortabilityService,
    private val reminderScheduler: ReminderScheduler
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
                        useDynamicColors = prefs.useDynamicColors,
                        dailyReminderEnabled = prefs.dailyReminderEnabled,
                        reminderHour = prefs.reminderHour,
                        reminderMinute = prefs.reminderMinute,
                        appLockEnabled = prefs.appLockEnabled,
                        protectScreenEnabled = prefs.protectScreenEnabled
                    )
                }
            }
        }
    }

    fun setCycleStage(stage: CycleStage) {
        runPreferenceChange { preferencesRepository.updateCycleStage(stage) }
    }

    fun setThemeMode(mode: ThemeMode) {
        runPreferenceChange {
            preferencesRepository.updateTheme(mode, _state.value.useDynamicColors)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        runPreferenceChange {
            preferencesRepository.updateTheme(_state.value.themeMode, enabled)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        if (_state.value.isClearingData) return
        viewModelScope.launch {
            val state = _state.value
            val result = runSuspendCatching {
                preferencesRepository.updateReminder(
                    enabled,
                    state.reminderHour,
                    state.reminderMinute
                )
                reminderScheduler.sync(enabled, state.reminderHour, state.reminderMinute)
            }
            _state.update { it.copy(hasOperationError = result.isFailure) }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        if (_state.value.isClearingData) return
        viewModelScope.launch {
            val enabled = _state.value.dailyReminderEnabled
            val result = runSuspendCatching {
                preferencesRepository.updateReminder(enabled, hour, minute)
                reminderScheduler.sync(enabled, hour, minute)
            }
            _state.update { it.copy(hasOperationError = result.isFailure) }
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        runPreferenceChange { preferencesRepository.updateAppLock(enabled) }
    }

    fun setProtectScreenEnabled(enabled: Boolean) {
        runPreferenceChange { preferencesRepository.updateProtectScreen(enabled) }
    }

    suspend fun buildCsvExport(): String = dataPortabilityService.buildCsv()

    suspend fun previewCsvImport(csv: String): CsvImportPreview =
        dataPortabilityService.previewCsv(csv)

    suspend fun importCsv(csv: String) {
        dataPortabilityService.importCsv(csv)
    }

    suspend fun buildBackup(): String = dataPortabilityService.buildBackup()

    fun previewBackup(backup: String): BackupPreview =
        dataPortabilityService.previewBackup(backup)

    suspend fun restoreBackup(backup: String) {
        dataPortabilityService.restoreBackup(backup)
        val restored = preferencesRepository.preferences.first()
        reminderScheduler.sync(
            restored.dailyReminderEnabled,
            restored.reminderHour,
            restored.reminderMinute
        )
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
                    reminderScheduler.cancel()
                } catch (error: Throwable) {
                    if (daysDeleted && daysBeforeClear.isNotEmpty()) {
                        withContext(NonCancellable) {
                            try {
                                cycleDayRepository.saveAll(daysBeforeClear)
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

    private fun runPreferenceChange(action: suspend () -> Unit) {
        if (_state.value.isClearingData) return
        viewModelScope.launch {
            val result = runSuspendCatching { action() }
            _state.update { it.copy(hasOperationError = result.isFailure) }
        }
    }
}
