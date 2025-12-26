package com.example.mycycle.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mycycle.ui.calendar.CalendarScreen
import com.example.mycycle.ui.settings.SettingsScreen
import com.example.mycycle.ui.statistics.StatisticsScreen
import com.example.mycycle.ui.today.TodayScreen

@Composable
fun MainNavHost(
    navController: NavHostController = rememberNavController(),
    onDayClick: (String) -> Unit
) {
    Scaffold(
        bottomBar = {
            MyCycleBottomBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            }
        ) {
            composable(Screen.Today.route) {
                TodayScreen(
                    onLogClick = { onDayClick(java.time.LocalDate.now().toString()) }
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onDayClick = onDayClick
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun MyCycleBottomBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        BottomNavItem.entries.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.iconFilled else item.iconOutlined,
                        contentDescription = stringResource(item.labelRes)
                    )
                },
                label = { Text(stringResource(item.labelRes)) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
