package com.coinden.app.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.coinden.app.AppContainer
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * La página de contexto con todo lo que necesita alrededor: el micrófono, la cámara para sumar
 * una foto y el cierre del paquete.
 *
 * Salir **por cualquier motivo** cierra el paquete: Listo, Atrás o que la app pase a segundo plano
 * (una llamada, otra app). El cierre corre en [closeScope], que sobrevive a la página, para que
 * irse no lo corte a la mitad.
 */
@Composable
fun ContextRoute(
    container: AppContainer,
    bundleId: String,
    platform: CapturePlatform,
    lifecycle: Lifecycle,
    closeScope: CoroutineScope,
    onClosed: suspend () -> Unit,
) {
    val presenter = remember(bundleId) { ContextPagePresenter(container, bundleId) }
    var camera by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val closed by rememberUpdatedState(onClosed)
    val askMicrophone = platform.rememberMicrophoneRequest { }
    val recorder = remember(platform) { platform.contextRecorder?.let { AskingRecorder(it) { askMicrophone() } } }

    val leave: () -> Unit = {
        if (!finished) {
            finished = true
            closeScope.launch {
                presenter.onLeave()
                closed()
            }
        }
    }

    FormBackHandler(enabled = !camera, onBack = leave)

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) leave() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (camera) {
        platform.Camera(
            onPhoto = { bytes, mimeType ->
                camera = false
                closeScope.launch {
                    val ref = container.attachments.save(bytes, mimeType)
                    presenter.addPhoto(Evidence(EvidenceSource.PHOTO, attachmentRef = ref, fingerprint = sha256Hex(bytes)))
                }
            },
            onDenied = { camera = false },
            onClose = { camera = false },
        )
    } else {
        ContextPage(presenter, recorder, onPickPhoto = { camera = true }, onClosed = leave)
    }
}

/** Si falta el permiso del micrófono, lo pide; la persona vuelve a tocar Grabar después de darlo. */
private class AskingRecorder(
    private val base: ContextRecorder,
    private val ask: () -> Unit,
) : ContextRecorder {
    override suspend fun start(): RecordingStart = base.start().also { if (it is RecordingStart.NoPermission) ask() }

    override suspend fun stop() = base.stop()

    override fun cancel() = base.cancel()
}
