package com.example.mycycle.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.UserPreferencesRepository
import com.example.mycycle.R
import com.example.mycycle.model.TemperatureUnit
import com.example.mycycle.model.ThemeSetting
import com.example.mycycle.model.UserPreferences
import com.example.mycycle.model.WeightUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val prefs = preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateNotifications(enabled) }
    }

    fun toggleAnalytics(optIn: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAnalyticsOptIn(optIn) }
    }

    fun updateUnits(temp: TemperatureUnit, weight: WeightUnit) {
        viewModelScope.launch { preferencesRepository.updateUnits(temp, weight) }
    }

    fun updateTheme(theme: ThemeSetting) {
        viewModelScope.launch { preferencesRepository.updateTheme(theme) }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAppLock(enabled) }
    }
}

@Composable
fun SettingsScreen(titleRes: Int, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsState()
    val context = LocalContext.current
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(id = titleRes)) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PreferenceCard(title = stringResource(id = R.string.settings_notifications)) {
                    Switch(checked = prefs.notificationsEnabled, onCheckedChange = viewModel::toggleNotifications)
                }
            }
            item {
                PreferenceCard(title = stringResource(id = R.string.settings_analytics)) {
                    Switch(checked = prefs.analyticsOptIn, onCheckedChange = viewModel::toggleAnalytics)
                }
            }
            item {
                PreferenceCard(title = stringResource(id = R.string.settings_app_lock)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Switch(
                            checked = prefs.appLockEnabled && biometricAvailable,
                            enabled = biometricAvailable,
                            onCheckedChange = viewModel::toggleAppLock
                        )
                        if (!biometricAvailable) {
                            Text(
                                text = stringResource(id = R.string.settings_app_lock_unavailable),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            item {
                PreferenceCard(title = stringResource(id = R.string.settings_storage_security)) {
                    Text(
                        text = stringResource(id = R.string.settings_storage_security),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item {
                UnitsRow(prefs = prefs, onUnitsChange = viewModel::updateUnits)
            }
            item {
                ThemeRow(theme = prefs.theme, onThemeSelected = viewModel::updateTheme)
            }
        }
    }
}

@Composable
private fun PreferenceCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun UnitsRow(prefs: UserPreferences, onUnitsChange: (TemperatureUnit, WeightUnit) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(id = R.string.settings_units_title), fontWeight = FontWeight.SemiBold)
            val expanded = remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded.value, onExpandedChange = { expanded.value = !expanded.value }) {
                OutlinedTextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    value = stringResource(id = R.string.settings_temperature_unit, prefs.temperatureUnit.name.lowercase()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.settings_temperature_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) }
                )
                DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                    TemperatureUnit.values().forEach { unit ->
                        DropdownMenuItem(text = { Text(unit.name) }, onClick = {
                            expanded.value = false
                            onUnitsChange(unit, prefs.weightUnit)
                        })
                    }
                }
            }
            val weightExpanded = remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = weightExpanded.value, onExpandedChange = { weightExpanded.value = !weightExpanded.value }) {
                OutlinedTextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    value = stringResource(id = R.string.settings_weight_unit, prefs.weightUnit.name.lowercase()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.settings_weight_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weightExpanded.value) }
                )
                DropdownMenu(expanded = weightExpanded.value, onDismissRequest = { weightExpanded.value = false }) {
                    WeightUnit.values().forEach { unit ->
                        DropdownMenuItem(text = { Text(unit.name) }, onClick = {
                            weightExpanded.value = false
                            onUnitsChange(prefs.temperatureUnit, unit)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(theme: ThemeSetting, onThemeSelected: (ThemeSetting) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(id = R.string.settings_theme_title), fontWeight = FontWeight.SemiBold)
            ThemeSetting.values().forEach { option ->
                Switch(
                    checked = option == theme,
                    onCheckedChange = { if (it) onThemeSelected(option) },
                    thumbContent = { Text(option.name.take(1)) }
                )
            }
        }
    }
}
