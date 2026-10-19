package com.coinden.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.coinden.app.capture.AndroidCapturePlatform
import com.coinden.app.capture.SharedContent
import com.coinden.app.capture.SharedIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Entrada Android: arma la UI compartida y recibe lo que otras apps comparten con CoinDen. */
class MainActivity : FragmentActivity() {
    private val shared = MutableStateFlow<SharedContent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash de la marca (#1C140F con el ícono) en todas las versiones; luego el tema de la app.
        installSplashScreen()
        // targetSdk 36 dibuja de borde a borde; la pantalla respeta las barras del sistema.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as CoinDenApplication
        val platform = AndroidCapturePlatform(this, app.perception)
        app.deviceAuth.attach(this)
        // Con el bloqueo activo, ni la vista de apps recientes ni una captura de pantalla muestran saldos.
        lifecycleScope.launch {
            app.lock.state.collect { state ->
                if (state.enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        // Un "compartir" se procesa una vez: no se repite al rotar la pantalla.
        if (savedInstanceState == null) receive(intent)
        setContent {
            App(
                app.container,
                app.deviceLanguage,
                app.deviceCountry,
                platform,
                shared,
                onSharedTaken = { shared.value = null },
                lock = app.lock,
                appVersion = version,
                foreground = app.foreground,
                forms = app.forms,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receive(intent)
    }

    /** Copia lo compartido fuera del hilo principal (un PDF o un audio puede pesar varios MB). */
    private fun receive(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { SharedIntents.read(applicationContext, intent) }?.let { shared.value = it }
        }
    }
}
