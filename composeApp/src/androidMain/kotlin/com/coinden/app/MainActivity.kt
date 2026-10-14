package com.coinden.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** Entrada Android: solo arma la UI compartida de commonMain. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 36 dibuja de borde a borde; la pantalla respeta las barras del sistema.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as CoinDenApplication
        setContent { App(app.container, app.deviceLanguage, app.deviceCountry) }
    }
}
