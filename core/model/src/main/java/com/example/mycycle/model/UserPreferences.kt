package com.example.mycycle.model

public data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAM,
    val theme: ThemeSetting = ThemeSetting.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val analyticsOptIn: Boolean = false,
)

public enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
public enum class WeightUnit { KILOGRAM, POUND }
public enum class ThemeSetting { LIGHT, DARK, SYSTEM }
