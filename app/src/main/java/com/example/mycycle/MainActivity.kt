package com.example.mycycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mycycle.calendar.CalendarScreen
import com.example.mycycle.designsystem.MyCycleTheme
import com.example.mycycle.insights.InsightsScreen
import com.example.mycycle.log.LogScreen
import com.example.mycycle.navigation.AppDestination
import com.example.mycycle.reminders.RemindersScreen
import com.example.mycycle.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyCycleRoot() }
    }
}

@Composable
private fun MyCycleRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination?.route

    MyCycleTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        val label = when (destination) {
                            AppDestination.Calendar -> stringResource(id = R.string.nav_calendar)
                            AppDestination.Log -> stringResource(id = R.string.nav_log)
                            AppDestination.Insights -> stringResource(id = R.string.nav_insights)
                            AppDestination.Reminders -> stringResource(id = R.string.nav_reminders)
                            AppDestination.Settings -> stringResource(id = R.string.nav_settings)
                        }
                        val icon = when (destination) {
                            AppDestination.Calendar -> Icons.Default.CalendarMonth
                            AppDestination.Log -> Icons.Default.Timeline
                            AppDestination.Insights -> Icons.Default.Assessment
                            AppDestination.Reminders -> Icons.Default.Notifications
                            AppDestination.Settings -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = currentDestination == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            label = { Text(text = label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Calendar.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestination.Calendar.route) { CalendarScreen(titleRes = R.string.calendar_title) }
                composable(AppDestination.Log.route) { LogScreen(titleRes = R.string.log_title) }
                composable(AppDestination.Insights.route) { InsightsScreen(titleRes = R.string.insights_title) }
                composable(AppDestination.Reminders.route) { RemindersScreen(titleRes = R.string.reminders_title) }
                composable(AppDestination.Settings.route) { SettingsScreen(titleRes = R.string.settings_title) }
            }
        }
    }
}
