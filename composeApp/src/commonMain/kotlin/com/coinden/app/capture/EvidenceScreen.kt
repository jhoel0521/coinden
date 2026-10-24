package com.coinden.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.capture_add_context
import coinden.composeapp.generated.resources.context_item_audio
import coinden.composeapp.generated.resources.context_item_note
import coinden.composeapp.generated.resources.context_item_photo
import coinden.composeapp.generated.resources.context_item_receipt
import coinden.composeapp.generated.resources.context_remove
import coinden.composeapp.generated.resources.context_removed
import coinden.composeapp.generated.resources.evidence_bundle_title
import coinden.composeapp.generated.resources.evidence_counter
import coinden.composeapp.generated.resources.evidence_next
import coinden.composeapp.generated.resources.evidence_previous
import coinden.composeapp.generated.resources.evidence_quote
import coinden.composeapp.generated.resources.evidence_split
import coinden.composeapp.generated.resources.evidence_title
import coinden.composeapp.generated.resources.evidence_unavailable
import coinden.composeapp.generated.resources.evidence_unread
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdTopBar
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** La evidencia de un borrador o movimiento, una por vez: texto, foto, PDF o audio. */
@Composable
fun EvidenceScreen(
    presenter: EvidencePresenter,
    viewer: EvidenceViewerPort,
    onBack: () -> Unit,
    onAddContext: (() -> Unit)? = null,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(presenter) { presenter.load() }
    DisposableEffect(presenter) { onDispose { presenter.close() } }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CdTopBar(
            stringResource(Res.string.evidence_title),
            backLabel = stringResource(Res.string.back),
            onBack = onBack,
            trailing = if (state.items.size > 1) stringResource(Res.string.evidence_counter, state.index + 1, state.items.size) else null,
        )
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
            CdActions(
                listOf(
                    CdActionSpec(
                        stringResource(Res.string.evidence_previous),
                        { scope.launch { presenter.show(state.index - 1) } },
                        enabled =
                            state.index > 0,
                    ),
                    CdActionSpec(
                        stringResource(Res.string.evidence_next),
                        { scope.launch { presenter.show(state.index + 1) } },
                        enabled = state.index < state.items.size - 1,
                    ),
                ),
                Modifier.padding(16.dp),
            )
        }
        presenter.bundle?.let { bundle ->
            BundleSection(bundle, onChanged = { scope.launch { presenter.load() } }, onAddContext = onAddContext)
        }
    }
}

/**
 * Todo lo que se mandó junto con este pago: quitar lo que no es (queda guardado y a la vista) o
 * separar lo que era otro pago. Agregar contexto reabre el paquete.
 */
@Composable
private fun BundleSection(
    bundle: BundleEvidenceViewer,
    onChanged: () -> Unit,
    onAddContext: (() -> Unit)?,
) {
    val items by bundle.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(bundle) { bundle.load() }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(Res.string.evidence_bundle_title), style = MaterialTheme.typography.titleMedium)
        items.forEach { item ->
            Text(
                stringResource(item.label()),
                style = MaterialTheme.typography.labelLarge,
                textDecoration = if (item.excluded) TextDecoration.LineThrough else null,
            )
            when {
                item.text != null -> Text(item.text, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                item.pendingRead -> Text(stringResource(Res.string.evidence_unread), style = MaterialTheme.typography.bodySmall)
            }
            if (item.excluded) {
                Text(stringResource(Res.string.context_removed), style = MaterialTheme.typography.labelSmall)
            } else if (items.count { !it.excluded } > 1) {
                CdActions(
                    listOf(
                        CdActionSpec(stringResource(Res.string.context_remove), {
                            scope.launch {
                                bundle.remove(item.evidenceId)
                                onChanged()
                            }
                        }),
                        CdActionSpec(stringResource(Res.string.evidence_split), {
                            scope.launch {
                                bundle.split(item.evidenceId)
                                onChanged()
                            }
                        }),
                    ),
                )
            }
        }
        onAddContext?.let {
            CdActions(
                listOf(CdActionSpec(stringResource(Res.string.capture_add_context), it)),
                Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

/** El nombre de lo que es, sin interpretar nada. */
private fun BundleEvidenceItem.label() =
    when {
        role == EvidenceRole.RECEIPT -> Res.string.context_item_receipt
        source == EvidenceSource.VOICE -> Res.string.context_item_audio
        source == EvidenceSource.PHOTO -> Res.string.context_item_photo
        else -> Res.string.context_item_note
    }
