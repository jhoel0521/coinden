package com.coinden.app.i18n

import androidx.compose.runtime.Composable

/** Aplica el idioma de la app a los recursos de la interfaz, sin reiniciar la pantalla. */
@Composable
expect fun ProvideAppLocale(
    language: String,
    content: @Composable () -> Unit,
)
