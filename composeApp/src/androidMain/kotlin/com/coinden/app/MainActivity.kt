package com.coinden.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.coinden.app.capture.AndroidCapturePlatform
import com.coinden.app.capture.SharedContent
import com.coinden.app.capture.SharedIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Entrada Android: arma la UI compartida y recibe lo que otras apps comparten con CoinDen. */
class MainActivity : ComponentActivity() {
    private val shared = MutableStateFlow<SharedContent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 36 dibuja de borde a borde; la pantalla respeta las barras del sistema.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as CoinDenApplication
        val platform = AndroidCapturePlatform(this)
        // Un "compartir" se procesa una vez: no se repite al rotar la pantalla.
        if (savedInstanceState == null) receive(intent)
        setContent {
            App(app.container, app.deviceLanguage, app.deviceCountry, platform, shared, onSharedTaken = { shared.value = null })
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
