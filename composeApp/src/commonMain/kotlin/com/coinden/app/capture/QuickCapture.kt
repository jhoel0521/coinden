package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.perception.ProcessingQueue
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource

/**
 * Guardar en segundos, sin esperar a nada pesado: un texto se interpreta al instante (milisegundos)
 * y un archivo se guarda cifrado con su huella y queda para la cola, que lo lee después aunque se
 * cierre la app. Lo usan la pantalla de captura y "compartir" sin abrir la app. Un comprobante
 * idéntico a un gasto ya confirmado queda como su evidencia (se devuelve el confirmado).
 */
class QuickCapture(
    private val container: AppContainer,
    private val queue: ProcessingQueue,
) {
    suspend fun shared(content: SharedContent): List<Draft> =
        when (content) {
            is SharedContent.Text -> text(content.text, EvidenceSource.TEXT)
            is SharedContent.File -> listOf(file(content.bytes, content.mimeType))
        }

    suspend fun text(
        text: String,
        source: EvidenceSource,
    ): List<Draft> {
        val clean = text.trim().takeIf { it.isNotEmpty() } ?: return emptyList()
        return container.settleIdentical(container.captureDraft(container.ownerId, Evidence(source, clean)))
    }

    suspend fun file(
        bytes: ByteArray,
        mimeType: String,
    ): Draft {
        val source = if (mimeType.startsWith("audio/")) EvidenceSource.VOICE else EvidenceSource.PHOTO
        // La huella se calcula sobre el archivo original, antes de cifrarlo.
        val fingerprint = sha256Hex(bytes)
        val ref = container.attachments.save(bytes, mimeType)
        val draft = container.captureDraft(container.ownerId, Evidence(source, attachmentRef = ref, fingerprint = fingerprint)).single()
        queue.kick()
        return draft
    }
}
