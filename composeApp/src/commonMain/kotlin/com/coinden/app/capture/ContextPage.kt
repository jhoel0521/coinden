package com.coinden.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.context_add_photo
import coinden.composeapp.generated.resources.context_done
import coinden.composeapp.generated.resources.context_empty
import coinden.composeapp.generated.resources.context_item_audio
import coinden.composeapp.generated.resources.context_item_note
import coinden.composeapp.generated.resources.context_item_photo
import coinden.composeapp.generated.resources.context_item_receipt
import coinden.composeapp.generated.resources.context_no_microphone
import coinden.composeapp.generated.resources.context_note_hint
import coinden.composeapp.generated.resources.context_record
import coinden.composeapp.generated.resources.context_remove
import coinden.composeapp.generated.resources.context_removed
import coinden.composeapp.generated.resources.context_stop
import coinden.composeapp.generated.resources.context_subtitle
import coinden.composeapp.generated.resources.context_title
import coinden.composeapp.generated.resources.context_write_note
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdField
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * La página de contexto rápido: todo lo que la persona mandó junto, con la opción de sumarle un
 * audio, una nota o una foto, y de quitar lo que no es de ese pago.
 *
 * No muestra montos ni saldos: no interpreta nada, así que no pide desbloquear la app. Salir de
 * la página, por el motivo que sea, cierra el paquete y lo manda a la cola.
 */
@Composable
fun ContextPage(
    presenter: ContextPagePresenter,
    recorder: ContextRecorder?,
    onPickPhoto: () -> Unit,
    onClosed: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { presenter.load() }

    // Salir por cualquier motivo cierra el paquete. Sin temporizadores.
    DisposableEffect(Unit) {
        onDispose {
            recorder?.cancel()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.context_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.context_subtitle), style = MaterialTheme.typography.bodyMedium)

        if (state.items.isEmpty()) {
            Text(stringResource(Res.string.context_empty), style = MaterialTheme.typography.bodyMedium)
        }

        state.items.forEach { item ->
            ContextItemRow(item) { scope.launch { presenter.remove(item.evidenceId) } }
        }

        CdField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(Res.string.context_note_hint),
            singleLine = false,
        )

        if (!state.canRecord) {
            Text(stringResource(Res.string.context_no_microphone), style = MaterialTheme.typography.bodySmall)
        }

        CdActions(
            listOfNotNull(
                CdActionSpec(
                    text = stringResource(Res.string.context_write_note),
                    enabled = note.isNotBlank(),
                    onClick = {
                        scope.launch {
                            presenter.addNote(note)
                            note = ""
                        }
                    },
                ),
                recorder?.takeIf { state.canRecord }?.let { grabador ->
                    CdActionSpec(
                        // Grabar tiene dos momentos: el mismo botón abre y cierra el micrófono.
                        text = stringResource(if (recording) Res.string.context_stop else Res.string.context_record),
                        onClick = {
                            scope.launch {
                                if (recording) {
                                    grabador.stop()?.let { presenter.addAudio(it) }
                                    recording = false
                                } else {
                                    recording = grabador.start() is RecordingStart.Started
                                }
                            }
                        },
                    )
                },
                CdActionSpec(text = stringResource(Res.string.context_add_photo), onClick = { onPickPhoto() }),
                CdActionSpec(
                    text = stringResource(Res.string.context_done),
                    onClick = {
                        scope.launch {
                            presenter.done()
                            onClosed()
                        }
                    },
                ),
            ),
        )
    }
}

/** Una evidencia de la lista: qué es, qué dice, y Quitar si todavía cuenta. */
@Composable
private fun ContextItemRow(
    item: ContextItem,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.75f)) {
            Text(
                text = stringResource(item.label()),
                style = MaterialTheme.typography.labelLarge,
                textDecoration = if (item.excluded) TextDecoration.LineThrough else null,
            )
            item.text?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
            if (item.excluded) {
                Text(stringResource(Res.string.context_removed), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (!item.excluded) {
            CdActions(listOf(CdActionSpec(text = stringResource(Res.string.context_remove), onClick = onRemove)))
        }
    }
}

/** El nombre de lo que es, sin interpretar nada: un comprobante, una foto, un audio o una nota. */
private fun ContextItem.label() =
    when {
        role == EvidenceRole.RECEIPT -> Res.string.context_item_receipt
        source == EvidenceSource.VOICE -> Res.string.context_item_audio
        source == EvidenceSource.PHOTO -> Res.string.context_item_photo
        else -> Res.string.context_item_note
    }
