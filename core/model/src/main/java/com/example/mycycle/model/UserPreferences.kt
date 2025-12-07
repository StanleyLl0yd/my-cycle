package com.example.mycycle.model

public data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAM,
    val theme: ThemeSetting = ThemeSetting.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val analyticsOptIn: Boolean = false,
    val appLockEnabled: Boolean = false,
)

public enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;

    public companion object {
        public fun fromOrdinal(value: Int): TemperatureUnit = entries.getOrElse(value) { CELSIUS }
    }
}

public enum class WeightUnit {
    KILOGRAM,
    POUND;

    public companion object {
        public fun fromOrdinal(value: Int): WeightUnit = entries.getOrElse(value) { KILOGRAM }
    }
}

public enum class ThemeSetting {
    LIGHT,
    DARK,
    SYSTEM;

    public companion object {
        public fun fromOrdinal(value: Int): ThemeSetting = entries.getOrElse(value) { SYSTEM }
    }
}
