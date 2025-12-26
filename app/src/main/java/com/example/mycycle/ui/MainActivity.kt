package com.example.mycycle.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.ui.daydetails.DayDetailsSheet
import com.example.mycycle.ui.navigation.MainNavHost
import com.example.mycycle.ui.navigation.Screen
import com.example.mycycle.ui.onboarding.OnboardingScreen
import com.example.mycycle.ui.theme.MyCycleTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferencesRepository: UserPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by preferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = null
            )

            preferences?.let { prefs ->
                MyCycleTheme(
                    themeMode = prefs.themeMode,
                    dynamicColor = prefs.useDynamicColors
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MyCycleApp(
                            showOnboarding = !prefs.onboardingCompleted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyCycleApp(showOnboarding: Boolean) {
    val navController = rememberNavController()
    var selectedDayForDetails by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) Screen.Onboarding.route else Screen.Main.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainNavHost(
                onDayClick = { dateString ->
                    selectedDayForDetails = dateString
                }
            )
        }
    }

    // Day details bottom sheet
    selectedDayForDetails?.let { dateString ->
        DayDetailsSheet(
            dateString = dateString,
            onDismiss = { selectedDayForDetails = null }
        )
    }
}
