package com.sl.mycycle.privacy

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import com.sl.mycycle.R

object AppAuthenticator {
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun authenticate(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        if (!isSupported) {
            onError()
            return
        }
        authenticatePlatform(activity, onSuccess, onError)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun authenticatePlatform(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = activity.mainExecutor
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.app_lock_prompt_title))
            .setSubtitle(activity.getString(R.string.app_lock_prompt_subtitle))
            .setNegativeButton(
                activity.getString(R.string.dialog_cancel),
                executor
            ) { _, _ -> onError() }
            .build()

        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    onError()
                }
            }
        )
    }
}
