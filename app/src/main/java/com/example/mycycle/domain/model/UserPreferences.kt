package com.example.mycycle.domain.model

import androidx.annotation.StringRes
import com.example.mycycle.R
import java.time.LocalDate

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val initialPeriodDate: LocalDate? = null,
    val estimatedCycleLength: Int = 28,
    val estimatedPeriodLength: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true
)

enum class ThemeMode(@StringRes val labelRes: Int) {
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark),
    SYSTEM(R.string.settings_theme_system)
}
