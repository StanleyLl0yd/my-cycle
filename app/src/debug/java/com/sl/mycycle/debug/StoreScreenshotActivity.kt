package com.sl.mycycle.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.FlowIntensity
import com.sl.mycycle.domain.model.Mood
import com.sl.mycycle.domain.model.Symptom
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
        val today = LocalDate.now()
        runBlocking { seedDemoData(today) }
        val targetRoute = when (intent.getStringExtra(EXTRA_SCREEN)) {
            "calendar" -> Screen.Calendar.route
            "statistics" -> Screen.Statistics.route
            "settings" -> Screen.Settings.route
            else -> Screen.Today.route
        }

        enableEdgeToEdge()
        hideSystemBars()
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

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private suspend fun seedDemoData(today: LocalDate) {
        val koin = GlobalContext.get()
        val cycleDayRepository = koin.get<CycleDayRepository>()
        val preferencesRepository = koin.get<UserPreferencesRepository>()
        val periodStarts = PERIOD_START_OFFSETS.map(today::minusDays)
        val lastPeriodStart = periodStarts.last()
        val intensities = listOf(
            FlowIntensity.MEDIUM,
            FlowIntensity.HEAVY,
            FlowIntensity.MEDIUM,
            FlowIntensity.LIGHT,
            FlowIntensity.LIGHT,
        )
        val periodDays = periodStarts.flatMap { start ->
            intensities.mapIndexed { index, intensity ->
                CycleDay(
                    date = start.plusDays(index.toLong()),
                    hasPeriod = true,
                    flowIntensity = intensity,
                    mood = if (index == 1) Mood.OKAY else null,
                    symptoms = if (index == 1) {
                        setOf(Symptom.CRAMPS)
                    } else {
                        emptySet()
                    },
                )
            }
        }
        val prePeriodDays = periodStarts.drop(1).flatMap { start ->
            listOf(
                CycleDay(
                    date = start.minusDays(2),
                    mood = Mood.OKAY,
                    symptoms = setOf(Symptom.HEADACHE, Symptom.FATIGUE),
                ),
                CycleDay(
                    date = start.minusDays(1),
                    symptoms = setOf(Symptom.FATIGUE),
                ),
            )
        }

        cycleDayRepository.deleteAll()
        preferencesRepository.clearAll()
        cycleDayRepository.saveAll(periodDays + prePeriodDays)
        preferencesRepository.completeOnboarding(
            lastPeriodDate = lastPeriodStart,
            cycleLength = 29,
            cycleStage = CycleStage.ESTABLISHED,
            periodLength = 5,
        )
    }

    private companion object {
        const val EXTRA_SCREEN = "store_screen"
        const val LAST_PERIOD_DAYS_AGO = 24L
        val PERIOD_START_OFFSETS = listOf(171L, 141L, 112L, 82L, 53L, LAST_PERIOD_DAYS_AGO)
    }
}
