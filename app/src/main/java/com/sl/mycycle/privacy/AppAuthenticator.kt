package com.sl.mycycle.privacy

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.sl.mycycle.R
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object AppAuthenticator {
    private const val KEY_ALIAS = "my_cycle_app_lock"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val challenge = "my-cycle-app-lock".toByteArray(Charsets.UTF_8)

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
        val cipher = runCatching { createAuthenticationCipher() }.getOrElse {
            onError()
            return
        }
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
            BiometricPrompt.CryptoObject(cipher),
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    val authenticatedCipher = result?.cryptoObject?.cipher
                    if (authenticatedCipher == null) {
                        onError()
                        return
                    }
                    runCatching { authenticatedCipher.doFinal(challenge) }
                        .onSuccess { onSuccess() }
                        .onFailure { onError() }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    onError()
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createAuthenticationCipher(): Cipher {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: createSecretKey()
        return runCatching { initializeCipher(key) }.getOrElse {
            keyStore.deleteEntry(KEY_ALIAS)
            initializeCipher(createSecretKey())
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createSecretKey(): SecretKey {
        val specBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specBuilder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            specBuilder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        ).apply {
            init(specBuilder.build())
        }.generateKey()
    }

    private fun initializeCipher(key: SecretKey): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        init(Cipher.ENCRYPT_MODE, key)
    }
}
