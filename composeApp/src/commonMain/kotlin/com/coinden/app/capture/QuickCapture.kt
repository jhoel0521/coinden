package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.perception.ProcessingQueue
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.usecase.bundle.CloseBundleUseCase
import com.coinden.core.domain.usecase.bundle.StartBundleUseCase

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
    /**
     * Todo lo que la persona mandó junto entra a **un mismo paquete**: dos fotos elegidas a la vez
     * en WhatsApp son un pago con dos comprobantes, no dos capturas sueltas.
     *
     * El paquete queda **abierto** para que la página de contexto le sume un audio o una nota.
     * Quien la muestre lo cierra al salir; si no hay página, [closeNow] lo cierra en el acto y se
     * comporta como siempre: compartir y volver a la otra app.
     */
    suspend fun sharedBundle(
        contents: List<SharedContent>,
        closeNow: Boolean = false,
    ): String? {
        val evidences = contents.mapNotNull { evidenceOf(it) }
        if (evidences.isEmpty()) return null
        val bundleId = StartBundleUseCase(container.bundles)(container.ownerId, evidences)
        if (closeNow) {
            CloseBundleUseCase(container.bundles)(bundleId)
            queue.kick()
        }
        return bundleId
    }

    /**
     * Lo capturado dentro de la app (dictar, foto o escribir) como paquete abierto, para que la
     * página de contexto pueda sumarle un audio o una nota antes de cerrarlo.
     *
     * Un texto no tiene nada que leer, así que al cerrarlo se interpreta en el acto: capturar
     * sigue tomando segundos. Una foto o un audio quedan para la cola, como siempre.
     */
    suspend fun capturedBundle(evidence: Evidence): String = StartBundleUseCase(container.bundles)(container.ownerId, listOf(evidence))

    /** Un texto se guarda tal cual; un archivo se cifra y conserva su huella, que evita repetirlo. */
    private suspend fun evidenceOf(content: SharedContent): Evidence? =
        when (content) {
            is SharedContent.Text ->
                content.text
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { Evidence(EvidenceSource.TEXT, it) }
            is SharedContent.File -> {
                val source = if (content.mimeType.startsWith("audio/")) EvidenceSource.VOICE else EvidenceSource.PHOTO
                val fingerprint = sha256Hex(content.bytes)
                Evidence(source, attachmentRef = container.attachments.save(content.bytes, content.mimeType), fingerprint = fingerprint)
            }
        }

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
