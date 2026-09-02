package com.sl.mycycle.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sl.mycycle.R
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.privacy.AppAuthenticator
import com.sl.mycycle.ui.daydetails.DayDetailsSheet
import com.sl.mycycle.ui.navigation.MainNavHost
import com.sl.mycycle.ui.navigation.Screen
import com.sl.mycycle.ui.onboarding.OnboardingScreen
import com.sl.mycycle.ui.theme.MyCycleTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferencesRepository: UserPreferencesRepository by inject()
    private val requestedDay = MutableStateFlow<String?>(null)
    private val unlocked = MutableStateFlow(false)
    private var appLockEnabled = false
    private var authenticationRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val preferences by preferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = null
            )
            val requestedDayValue by requestedDay.collectAsStateWithLifecycle()
            val isUnlocked by unlocked.collectAsStateWithLifecycle()

            preferences?.let { prefs ->
                SideEffect {
                    appLockEnabled = prefs.appLockEnabled
                }
                LaunchedEffect(prefs.protectScreenEnabled) {
                    if (prefs.protectScreenEnabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                val lockRequired = prefs.appLockEnabled && AppAuthenticator.isSupported
                LaunchedEffect(lockRequired, isUnlocked) {
                    if (lockRequired && !isUnlocked) requestUnlock()
                }

                MyCycleTheme(
                    themeMode = prefs.themeMode,
                    dynamicColor = prefs.useDynamicColors
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (lockRequired && !isUnlocked) {
                            LockedScreen(onUnlock = ::requestUnlock)
                        } else {
                            MyCycleApp(
                                showOnboarding = !prefs.onboardingCompleted,
                                requestedDay = requestedDayValue,
                                onRequestedDayHandled = { requestedDay.value = null }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        if (appLockEnabled && !isChangingConfigurations) {
            unlocked.value = false
        }
    }

    fun markUnlocked() {
        unlocked.value = true
    }

    private fun requestUnlock() {
        if (authenticationRunning) return
        authenticationRunning = true
        AppAuthenticator.authenticate(
            activity = this,
            onSuccess = {
                authenticationRunning = false
                unlocked.value = true
            },
            onError = {
                authenticationRunning = false
            }
        )
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "mycycle" && data.host == "log" && data.path == "/today") {
            requestedDay.value = LocalDate.now().toString()
        }
    }
}

@Composable
private fun LockedScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.app_lock_locked_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.app_lock_locked_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onUnlock) {
            Text(stringResource(R.string.app_lock_unlock))
        }
    }
}

@Composable
fun MyCycleApp(
    showOnboarding: Boolean,
    requestedDay: String? = null,
    onRequestedDayHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    var selectedDayForDetails by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showOnboarding) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        when {
            showOnboarding && currentRoute == Screen.Main.route -> {
                selectedDayForDetails = null
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Main.route) { inclusive = true }
                    launchSingleTop = true
                }
            }

            !showOnboarding && currentRoute == Screen.Onboarding.route -> {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(showOnboarding, requestedDay) {
        if (!showOnboarding && requestedDay != null) {
            selectedDayForDetails = requestedDay
            onRequestedDayHandled()
        }
    }

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

    selectedDayForDetails?.let { dateString ->
        DayDetailsSheet(
            dateString = dateString,
            onDismiss = { selectedDayForDetails = null }
        )
    }
}
