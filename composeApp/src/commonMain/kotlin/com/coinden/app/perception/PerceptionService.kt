package com.coinden.app.perception

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.usecase.bundle.PendingBundlesUseCase
import com.coinden.core.domain.usecase.bundle.PerceiveBundleEvidenceUseCase

/** Qué pasó al leer la evidencia de un borrador. */
sealed interface PerceptionOutcome {
    /** Se leyó y el intérprete actualizó los borradores (este primero). */
    data class Read(
        val drafts: List<Draft>,
    ) : PerceptionOutcome

    /** Ningún lector sacó texto: el borrador sigue Capturado. */
    data object NothingRead : PerceptionOutcome

    /** Es un audio y no hay modelo de voz ni reconocimiento local: se ofrece la descarga. */
    data object NeedsVoiceModel : PerceptionOutcome

    data object TooLong : PerceptionOutcome

    /** No hay nada que leer: ya se leyó, no está Capturado o no tiene archivo. */
    data object Skipped : PerceptionOutcome
}

/**
 * Lee en el teléfono la foto, el PDF o el audio de un borrador Capturado y pasa el texto al mismo
 * intérprete de siempre. Elige qué lectores usar (el de Google solo con consentimiento) y, con dos
 * lecturas de una foto, se queda con la que el intérprete entiende mejor. No usa Compose ni red.
 */
class PerceptionService(
    private val container: AppContainer,
    private val readers: PerceptionReaders,
) {
    suspend fun perceive(draftId: String): PerceptionOutcome {
        val draft = container.drafts.load(draftId) ?: return PerceptionOutcome.Skipped
        if (!isPending(draft)) return PerceptionOutcome.Skipped
        val evidence = draft.header.evidence
        val file = container.attachments.read(evidence.attachmentRef!!) ?: return PerceptionOutcome.Skipped

        val (text, engine) =
            when {
                file.mimeType == "application/pdf" -> readPdf(file.bytes)
                file.mimeType.startsWith("audio/") -> return transcribe(draftId, file.bytes, file.mimeType)
                else -> readImage(file.bytes)
            } ?: return PerceptionOutcome.NothingRead
        return interpret(draftId, text, engine)
    }

    /**
     * Lee **una** evidencia del paquete y guarda lo leído. Cuando el paquete cerrado ya no tiene
     * nada por leer, el dominio lo interpreta solo, en una sola pasada.
     *
     * Un archivo que no se puede leer se registra igual con texto vacío: así no se reintenta para
     * siempre y el paquete no queda colgado esperándolo.
     */
    suspend fun perceiveBundle(bundleId: String): PerceptionOutcome {
        val bundle = container.bundles.load(bundleId) ?: return PerceptionOutcome.Skipped
        val item = bundle.active.firstOrNull { it.needsReading } ?: return PerceptionOutcome.Skipped
        val file = container.attachments.read(item.evidence.attachmentRef!!) ?: return PerceptionOutcome.Skipped

        val read =
            when {
                file.mimeType.startsWith("audio/") -> {
                    val transcriber = readers.transcribers().firstOrNull() ?: return PerceptionOutcome.NeedsVoiceModel
                    val pcm = readers.audioDecoder.decode(file.bytes, file.mimeType)
                    val tooLong = pcm != null && pcm.seconds > MAX_AUDIO_SECONDS
                    if (tooLong) {
                        null
                    } else {
                        pcm
                            ?.let { transcriber.transcribe(it, container.languageCode) }
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { it to transcriber.engine }
                    }
                }
                file.mimeType == "application/pdf" -> readPdf(file.bytes)
                else -> readImage(file.bytes)
            }

        // Se registra aunque no se haya leído nada: así no se reintenta para siempre ni el paquete
        // queda colgado esperando un archivo que no se puede leer.
        val save = PerceiveBundleEvidenceUseCase(container.bundles)
        // Devuelve los borradores del paquete: vacío mientras quede algo por leer, y los N pagos
        // cuando ya no queda nada y el dominio lo interpreta.
        val drafts = save(bundleId, item.id, read?.first, read?.second ?: "sin lector")
        return if (read == null) PerceptionOutcome.NothingRead else PerceptionOutcome.Read(drafts)
    }

    /** Paquetes cerrados con archivos sin leer, los más viejos primero. */
    suspend fun pendingBundleIds(): List<String> = PendingBundlesUseCase(container.bundles)(container.ownerId).map { it.id }

    /** Si hay con qué transcribir audios (modelo de voz descargado o reconocimiento local). */
    suspend fun canTranscribe(): Boolean = readers.transcribers().isNotEmpty()

    /** Borradores Capturados con archivo que todavía no se leyeron. */
    suspend fun pendingDraftIds(): List<String> =
        container
            .draftInbox(container.ownerId)
            .pending
            .filter(::isPending)
            .sortedBy { it.header.capturedAtEpochMs }
            .map { it.id }

    /** Lee de a uno todos los pendientes. */
    suspend fun perceiveAll(): Map<String, PerceptionOutcome> = pendingDraftIds().associateWith { perceive(it) }

    private fun isPending(draft: Draft): Boolean =
        draft is Draft.Captured && draft.header.perceivedText == null && draft.header.evidence.attachmentRef != null

    private suspend fun interpret(
        draftId: String,
        text: String,
        engine: String,
    ): PerceptionOutcome {
        val drafts = container.settleIdentical(container.interpretPerceived(draftId, text, engine))
        return PerceptionOutcome.Read(drafts)
    }

    private suspend fun transcribe(
        draftId: String,
        audio: ByteArray,
        mimeType: String,
    ): PerceptionOutcome {
        val transcriber = readers.transcribers().firstOrNull() ?: return PerceptionOutcome.NeedsVoiceModel
        val pcm = readers.audioDecoder.decode(audio, mimeType) ?: return PerceptionOutcome.NothingRead
        if (pcm.seconds > MAX_AUDIO_SECONDS) return PerceptionOutcome.TooLong
        val text = transcriber.transcribe(pcm, container.languageCode)?.trim()?.takeIf { it.isNotEmpty() }
        return text?.let { interpret(draftId, it, transcriber.engine) } ?: PerceptionOutcome.NothingRead
    }

    private suspend fun readPdf(pdf: ByteArray): Pair<String, String>? =
        when (val reading = readers.pdfReader.read(pdf, MAX_PDF_PAGES)) {
            is PdfReading.Text ->
                reading.text
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { it to Engines.PDF_TEXT }
            is PdfReading.Pages -> {
                val pages = reading.images.mapNotNull { readImage(it) }
                if (pages.isEmpty()) null else pages.joinToString("\n") { it.first } to pages.first().second
            }
            PdfReading.Unreadable -> null
        }

    /** Lee con el lector libre y, si se aceptó, con el de Google; gana la mejor lectura. */
    private suspend fun readImage(image: ByteArray): Pair<String, String>? {
        val readings = mutableListOf<Pair<String, String>>()
        readers.freeImageReader
            .read(image)
            ?.clean()
            ?.let { readings += it to readers.freeImageReader.engine }
        if (container.storage.preferences.googleReaderEnabled()) {
            val google = readers.googleImageReader()
            google.read(image)?.clean()?.let { readings += it to google.engine }
        }
        // maxBy se queda con el primero entre iguales: el lector libre, que va primero.
        return readings.maxByOrNull { score(it.first) }
    }

    /** 3 si coincide una plantilla, 2 si trae referencia, 1 si trae monto, 0 si nada. */
    private suspend fun score(text: String): Int {
        val directory = container.drafts.directory(container.ownerId)
        val proposal =
            runCatching { container.interpreter.interpret(Evidence(EvidenceSource.PHOTO, text), directory) }
                .getOrDefault(emptyList())
                .firstOrNull() ?: return 0
        val (amount, reference) =
            when (proposal) {
                is MovementProposal.Expense -> proposal.amount to proposal.reference
                is MovementProposal.Income -> proposal.amount to proposal.reference
                is MovementProposal.Transfer -> proposal.out to proposal.reference
                else -> null to null
            }
        return when {
            proposal.origins[DraftField.KIND] == FieldOrigin.EVIDENCE -> 3
            reference != null -> 2
            amount != null -> 1
            else -> 0
        }
    }

    private fun String.clean(): String? = trim().takeIf { it.isNotEmpty() }

    companion object {
        const val MAX_AUDIO_SECONDS = 5 * 60L
        const val MAX_PDF_PAGES = 5
    }
}
