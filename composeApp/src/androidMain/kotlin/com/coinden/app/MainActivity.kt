package com.coinden.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** Entrada Android: solo arma la UI compartida de commonMain. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CoinDenApplication).container
        setContent { App(container) }
    }
}
