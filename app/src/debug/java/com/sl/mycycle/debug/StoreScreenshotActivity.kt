package com.sl.mycycle.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.FlowIntensity
import com.sl.mycycle.domain.model.ThemeMode
import com.sl.mycycle.ui.navigation.MainNavHost
import com.sl.mycycle.ui.navigation.Screen
import com.sl.mycycle.ui.theme.MyCycleTheme
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

class StoreScreenshotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking { seedDemoData() }
        val targetRoute = when (intent.getStringExtra(EXTRA_SCREEN)) {
            "calendar" -> Screen.Calendar.route
            "statistics" -> Screen.Statistics.route
            "settings" -> Screen.Settings.route
            else -> Screen.Today.route
        }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            LaunchedEffect(targetRoute) {
                if (targetRoute != Screen.Today.route) {
                    navController.navigate(targetRoute) {
                        launchSingleTop = true
                    }
                }
            }
            MyCycleTheme(
                themeMode = ThemeMode.LIGHT,
                dynamicColor = false,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavHost(
                        navController = navController,
                        onDayClick = {},
                    )
                }
            }
        }
    }

    private suspend fun seedDemoData() {
        val koin = GlobalContext.get()
        val cycleDayRepository = koin.get<CycleDayRepository>()
        val preferencesRepository = koin.get<UserPreferencesRepository>()
        val today = LocalDate.now()
        val periodStarts = listOf(158L, 130L, 102L, 74L, 46L, 18L).map(today::minusDays)
        val intensities = listOf(
            FlowIntensity.MEDIUM,
            FlowIntensity.HEAVY,
            FlowIntensity.MEDIUM,
            FlowIntensity.LIGHT,
            FlowIntensity.LIGHT,
        )

        cycleDayRepository.deleteAll()
        preferencesRepository.clearAll()
        cycleDayRepository.saveAll(
            periodStarts.flatMap { start ->
                intensities.mapIndexed { index, intensity ->
                    CycleDay(
                        date = start.plusDays(index.toLong()),
                        hasPeriod = true,
                        flowIntensity = intensity,
                    )
                }
            }
        )

        preferencesRepository.completeOnboarding(
            lastPeriodDate = periodStarts.last(),
            cycleLength = 28,
            cycleStage = CycleStage.ESTABLISHED,
            periodLength = 5,
        )
    }

    private companion object {
        const val EXTRA_SCREEN = "store_screen"
    }
}
