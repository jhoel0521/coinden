package com.coinden.app.capture

import androidx.compose.runtime.Composable

/** Intercepta Atrás del sistema sin acoplar el formulario compartido a Android. */
@Composable
expect fun FormBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
