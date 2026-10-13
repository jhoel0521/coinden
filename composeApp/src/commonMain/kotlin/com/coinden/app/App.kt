package com.coinden.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.coinden.app.capture.CaptureDemoPresenter
import com.coinden.app.capture.CaptureDemoScreen

/** Punto de entrada de la UI compartida (Android hoy; Desktop e iOS después). */
@Composable
fun App(container: AppContainer) {
    val presenter = remember(container) { CaptureDemoPresenter(container) }
    LaunchedEffect(presenter) { presenter.load() }
    MaterialTheme {
        Surface {
            CaptureDemoScreen(presenter, container.regional.settings().locale)
        }
    }
}
