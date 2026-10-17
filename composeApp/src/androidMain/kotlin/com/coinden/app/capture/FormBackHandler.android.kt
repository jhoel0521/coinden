package com.coinden.app.capture

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun FormBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled = enabled, onBack = onBack)
}
