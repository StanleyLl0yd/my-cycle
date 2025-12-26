package com.example.mycycle.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mycycle.R

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object Today : Screen("today")
    data object Calendar : Screen("calendar")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
    data object DayDetails : Screen("day/{date}") {
        fun createRoute(date: String) = "day/$date"
    }
}

enum class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector
) {
    TODAY(
        route = Screen.Today.route,
        labelRes = R.string.nav_today,
        iconOutlined = Icons.Outlined.Home,
        iconFilled = Icons.Rounded.Home
    ),
    CALENDAR(
        route = Screen.Calendar.route,
        labelRes = R.string.nav_calendar,
        iconOutlined = Icons.Outlined.CalendarMonth,
        iconFilled = Icons.Rounded.CalendarMonth
    ),
    STATISTICS(
        route = Screen.Statistics.route,
        labelRes = R.string.nav_statistics,
        iconOutlined = Icons.Outlined.ShowChart,
        iconFilled = Icons.Rounded.ShowChart
    ),
    SETTINGS(
        route = Screen.Settings.route,
        labelRes = R.string.nav_settings,
        iconOutlined = Icons.Outlined.Settings,
        iconFilled = Icons.Rounded.Settings
    )
}
