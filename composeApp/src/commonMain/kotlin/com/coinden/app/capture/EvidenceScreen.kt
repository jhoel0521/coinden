package com.coinden.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.evidence_counter
import coinden.composeapp.generated.resources.evidence_next
import coinden.composeapp.generated.resources.evidence_previous
import coinden.composeapp.generated.resources.evidence_quote
import coinden.composeapp.generated.resources.evidence_title
import coinden.composeapp.generated.resources.evidence_unavailable
import com.coinden.app.theme.CoinDenIcon
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** La evidencia de un borrador o movimiento, una por vez: texto, foto, PDF o audio. */
@Composable
fun EvidenceScreen(
    presenter: EvidencePresenter,
    viewer: EvidenceViewerPort,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(presenter) { presenter.load() }
    DisposableEffect(presenter) { onDispose { presenter.close() } }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(CoinDenIcon.CLOSE.vector, stringResource(Res.string.back)) }
            Text(stringResource(Res.string.evidence_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (state.items.size > 1) {
                Text(
                    stringResource(Res.string.evidence_counter, state.index + 1, state.items.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val text = state.current?.text
            val attachment = state.attachment
            when {
                state.unavailable -> Text(stringResource(Res.string.evidence_unavailable))
                attachment != null -> viewer.Show(attachment, Modifier.weight(1f).fillMaxWidth())
                text != null ->
                    Text(
                        stringResource(Res.string.evidence_quote, text),
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
            }
        }
        if (state.items.size > 1) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { scope.launch { presenter.show(state.index - 1) } },
                    enabled = state.index > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(stringResource(Res.string.evidence_previous)) }
                OutlinedButton(
                    onClick = { scope.launch { presenter.show(state.index + 1) } },
                    enabled = state.index < state.items.size - 1,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(stringResource(Res.string.evidence_next)) }
            }
        }
    }
}
