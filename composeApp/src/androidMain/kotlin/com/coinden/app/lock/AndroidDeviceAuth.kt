package com.coinden.app.lock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.lock_prompt_subtitle
import coinden.composeapp.generated.resources.lock_prompt_title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/**
 * La autenticación del propio teléfono con el aviso del sistema: huella o rostro, o el PIN, patrón
 * o contraseña de la pantalla de bloqueo. CoinDen solo recibe si la persona se autenticó.
 * El aviso necesita la pantalla visible: [attach] la registra al crearse.
 */
class AndroidDeviceAuth(
    private val context: Context,
) : DeviceAuth {
    private var activity: WeakReference<FragmentActivity>? = null

    fun attach(activity: FragmentActivity) {
        this.activity = WeakReference(activity)
    }

    /** Con bloqueo de pantalla siempre se puede (el PIN o patrón sirve aunque no haya huella). */
    override fun availability(): AuthAvailability {
        val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return AuthAvailability.Unsupported
        return if (keyguard.isDeviceSecure) AuthAvailability.Available else AuthAvailability.NoScreenLock
    }

    override fun openScreenLockSettings() {
        val from = activity?.get() ?: context
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        if (from !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { from.startActivity(intent) }
            .onFailure { from.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** Desde Android 11 la huella fuerte se combina con el bloqueo de pantalla; antes, la débil. */
    private fun authenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    override suspend fun authenticate(): AuthResult {
        val host = activity?.get() ?: return AuthResult.Failed
        val title = getString(Res.string.lock_prompt_title)
        val subtitle = getString(Res.string.lock_prompt_subtitle)
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { done ->
                val callback =
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (done.isActive) done.resume(AuthResult.Success)
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            val cancelled =
                                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                    errorCode == BiometricPrompt.ERROR_CANCELED
                            if (done.isActive) done.resume(if (cancelled) AuthResult.Cancelled else AuthResult.Failed)
                        }
                        // Un intento fallido (dedo no reconocido) no termina: el aviso sigue abierto.
                    }
                val prompt = BiometricPrompt(host, ContextCompat.getMainExecutor(host), callback)
                prompt.authenticate(
                    BiometricPrompt.PromptInfo
                        .Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setAllowedAuthenticators(authenticators())
                        .build(),
                )
                done.invokeOnCancellation { prompt.cancelAuthentication() }
            }
        }
    }
}
