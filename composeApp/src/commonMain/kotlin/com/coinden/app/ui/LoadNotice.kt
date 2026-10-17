package com.coinden.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.load_error
import coinden.composeapp.generated.resources.load_loading
import coinden.composeapp.generated.resources.load_stale
import coinden.composeapp.generated.resources.settings_retry
import org.jetbrains.compose.resources.stringResource

enum class ScreenLoad { LOADING, READY, ERROR }

@Composable
fun LoadNotice(
    load: ScreenLoad,
    hasPrevious: Boolean,
    onRetry: () -> Unit,
) {
    if (load == ScreenLoad.READY) return
    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(if (load == ScreenLoad.LOADING) Res.string.load_loading else Res.string.load_error))
        if (hasPrevious) Text(stringResource(Res.string.load_stale))
        if (load == ScreenLoad.ERROR) {
            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.settings_retry)) }
        }
    }
}
