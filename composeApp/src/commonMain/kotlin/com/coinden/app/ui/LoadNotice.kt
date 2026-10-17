package com.coinden.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.load_error
import coinden.composeapp.generated.resources.load_loading
import coinden.composeapp.generated.resources.load_stale
import coinden.composeapp.generated.resources.settings_retry
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdNotice
import com.coinden.app.ui.components.NoticeKind
import org.jetbrains.compose.resources.stringResource

enum class ScreenLoad { LOADING, READY, ERROR }

@Composable
fun LoadNotice(
    load: ScreenLoad,
    hasPrevious: Boolean,
    onRetry: () -> Unit,
) {
    if (load == ScreenLoad.READY) return
    CdNotice(
        if (load == ScreenLoad.LOADING) NoticeKind.Info else NoticeKind.Error,
        stringResource(if (load == ScreenLoad.LOADING) Res.string.load_loading else Res.string.load_error),
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        detail = if (hasPrevious) stringResource(Res.string.load_stale) else null,
        action = if (load == ScreenLoad.ERROR) CdActionSpec(stringResource(Res.string.settings_retry), onRetry) else null,
    )
}
