package com.example.mycycle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mycycle.calendar.CalendarScreen
import com.example.mycycle.data.ReminderScheduler
import com.example.mycycle.data.UserPreferencesRepository
import com.example.mycycle.designsystem.MyCycleTheme
import com.example.mycycle.insights.InsightsScreen
import com.example.mycycle.log.LogScreen
import com.example.mycycle.model.UserPreferences
import com.example.mycycle.navigation.AppDestination
import com.example.mycycle.reminders.RemindersScreen
import com.example.mycycle.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinkRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkRoute.value = intent?.getStringExtra(ReminderScheduler.KEY_TARGET_DESTINATION)
        setContent { MyCycleRoot(deepLinkRoute = deepLinkRoute.value) { deepLinkRoute.value = it } }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRoute.value = intent?.getStringExtra(ReminderScheduler.KEY_TARGET_DESTINATION)
    }
}

@Composable
private fun MyCycleRoot(
    deepLinkRoute: String?,
    onDeepLinkConsumed: (String?) -> Unit,
    appLockViewModel: AppLockViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination?.route
    val prefs by appLockViewModel.preferences.collectAsStateWithLifecycle()
    var shouldLock by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val canAuthenticate = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(stringResource(id = R.string.app_lock_title))
            .setSubtitle(stringResource(id = R.string.app_lock_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }
    val biometricPrompt = remember {
        BiometricPrompt(context as ComponentActivity, context.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                shouldLock = false
            }

            override fun onAuthenticationFailed() {
                shouldLock = true
            }
        })
    }

    LaunchedEffect(prefs.appLockEnabled, canAuthenticate) {
        shouldLock = prefs.appLockEnabled && canAuthenticate
    }

    DisposableEffect(lifecycleOwner, prefs.appLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (prefs.appLockEnabled && event == Lifecycle.Event.ON_START) {
                shouldLock = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(shouldLock, prefs.appLockEnabled, canAuthenticate) {
        if (prefs.appLockEnabled && canAuthenticate && shouldLock) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    LaunchedEffect(deepLinkRoute) {
        deepLinkRoute?.let { target ->
            navController.navigate(target) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onDeepLinkConsumed(null)
        }
    }

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

@HiltViewModel
internal class AppLockViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val preferences = preferencesRepository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences()
    )
}
