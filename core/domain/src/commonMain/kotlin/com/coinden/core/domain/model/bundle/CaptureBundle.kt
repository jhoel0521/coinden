package com.coinden.core.domain.model.bundle

import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource

/** Qué papel cumple una evidencia en su paquete. */
enum class EvidenceRole {
    /** Un comprobante (foto, PDF o texto del banco): dice los hechos de un pago. */
    RECEIPT,

    /** Lo que la persona dice o escribe para explicar los pagos (audio o nota). */
    CONTEXT,
    ;

    companion object {
        /** El papel de una captura según por dónde llegó: la voz y lo escrito a mano son contexto. */
        fun of(evidence: Evidence): EvidenceRole =
            when (evidence.source) {
                EvidenceSource.VOICE, EvidenceSource.MANUAL -> CONTEXT
                EvidenceSource.PHOTO -> RECEIPT
                EvidenceSource.TEXT ->
                    if (evidence.text
                            .orEmpty()
                            .trim()
                            .lines()
                            .size > 1
                    ) {
                        RECEIPT
                    } else {
                        CONTEXT
                    }
            }
    }
}

/** Una evidencia dentro de su paquete, con lo que se leyó de ella. */
data class BundleEvidence(
    val id: String,
    val evidence: Evidence,
    val role: EvidenceRole,
    val attachedAtEpochMs: Long,
    /** Texto leído del archivo (foto, PDF o audio); null si todavía no se leyó. */
    val perceivedText: String? = null,
    /** Se intentó leer y no salió texto: no se vuelve a intentar. */
    val unreadable: Boolean = false,
    /** La persona la quitó: queda guardada, pero no se interpreta. */
    val excluded: Boolean = false,
) {
    /** El texto que entra a la interpretación: lo leído o lo escrito. */
    val text: String? get() = perceivedText ?: evidence.text?.takeIf { it.isNotBlank() }

    /** Tiene un archivo que la cola todavía tiene que leer. */
    val needsReading: Boolean get() = !excluded && evidence.attachmentRef != null && perceivedText == null && !unreadable
}

/** Paquete de captura: lo que la persona manda junto para explicar uno o varios pagos. */
data class CaptureBundle(
    val id: String,
    val ownerId: String,
    val evidences: List<BundleEvidence>,
    val open: Boolean,
    val openedAtEpochMs: Long,
) {
    /** Las evidencias que se interpretan, en el orden en que llegaron. */
    val active: List<BundleEvidence> get() = evidences.filterNot { it.excluded }

    /** Ya se puede interpretar: está cerrado y no queda ningún archivo por leer. */
    val readyToInterpret: Boolean get() = !open && active.none { it.needsReading }

    fun hasFingerprint(fingerprint: String?): Boolean = fingerprint != null && active.any { it.evidence.fingerprint == fingerprint }
}

/**
 * Motivo por el que el intérprete del paquete deja un pago para revisar. Son códigos: la
 * presentación los traduce.
 */
sealed interface ReviewReason {
    /** Varios comprobantes del mismo monto: se asignaron por el orden en que se dijeron. */
    data object AssignedByOrder : ReviewReason

    /** Una frase sin monto puede ser de cualquiera de los pagos del paquete. */
    data object ContextForSeveral : ReviewReason

    /** Lo dicho contradice un hecho del comprobante: [stated] lo dicho, [read] lo leído. */
    data class Contradiction(
        val field: DraftField,
        val stated: String,
        val read: String,
    ) : ReviewReason

    /** Un pago que solo sale de lo dicho, sin comprobante, en un paquete que tiene comprobantes. */
    data object PaymentWithoutReceipt : ReviewReason
}

/** Transición de un paquete, guardada como evento append-only (sync-ready). */
sealed interface BundleEvent {
    val id: String
    val bundleId: String
    val ownerId: String
    val deviceId: String
    val occurredAtEpochMs: Long
    val recordedAtEpochMs: Long

    data class Opened(
        override val id: String,
        override val bundleId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
    ) : BundleEvent

    data class EvidenceAttached(
        override val id: String,
        override val bundleId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val evidenceId: String,
        val evidence: Evidence,
        val role: EvidenceRole,
    ) : BundleEvent

    /** Quitar: la evidencia queda guardada, pero deja de interpretarse. */
    data class EvidenceExcluded(
        override val id: String,
        override val bundleId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val evidenceId: String,
    ) : BundleEvent

    /** Texto leído de un archivo con el lector [engine]; [text] null: no se pudo leer. */
    data class EvidencePerceived(
        override val id: String,
        override val bundleId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val evidenceId: String,
        val text: String?,
        val engine: String,
    ) : BundleEvent

    data class Closed(
        override val id: String,
        override val bundleId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
    ) : BundleEvent

    data class Reopened(
        override val id: String,
        override val bundleId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
    ) : BundleEvent

    /** Separar: la evidencia sale de este paquete hacia el paquete nuevo [newBundleId]. */
    data class EvidenceSplit(
        override val id: String,
        override val bundleId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val evidenceId: String,
        val newBundleId: String,
    ) : BundleEvent
}

/** Reproduce el historial de un paquete. Los eventos se aplican en orden cronológico. */
object BundleProjection {
    // Orden estable: con la misma hora se respeta el orden en que se guardaron (el de la base).
    private val CHRONOLOGICAL: Comparator<BundleEvent> =
        compareBy<BundleEvent>({ it.occurredAtEpochMs }, { it.recordedAtEpochMs })

    fun project(events: List<BundleEvent>): CaptureBundle? {
        val opened = events.filterIsInstance<BundleEvent.Opened>().minWithOrNull(CHRONOLOGICAL) ?: return null
        var bundle = CaptureBundle(opened.bundleId, opened.ownerId, emptyList(), open = true, opened.occurredAtEpochMs)
        for (event in events.sortedWith(CHRONOLOGICAL)) {
            bundle =
                when (event) {
                    is BundleEvent.Opened -> bundle
                    is BundleEvent.EvidenceAttached ->
                        if (bundle.evidences.any { it.id == event.evidenceId }) {
                            bundle
                        } else {
                            bundle.copy(
                                evidences =
                                    bundle.evidences +
                                        BundleEvidence(
                                            event.evidenceId,
                                            event.evidence,
                                            event.role,
                                            event.occurredAtEpochMs,
                                        ),
                            )
                        }
                    is BundleEvent.EvidenceExcluded -> bundle.update(event.evidenceId) { it.copy(excluded = true) }
                    is BundleEvent.EvidenceSplit -> bundle.update(event.evidenceId) { it.copy(excluded = true) }
                    is BundleEvent.EvidencePerceived ->
                        bundle.update(event.evidenceId) { it.copy(perceivedText = event.text, unreadable = event.text == null) }
                    is BundleEvent.Closed -> bundle.copy(open = false)
                    is BundleEvent.Reopened -> bundle.copy(open = true)
                }
        }
        return bundle
    }

    private fun CaptureBundle.update(
        evidenceId: String,
        change: (BundleEvidence) -> BundleEvidence,
    ): CaptureBundle = copy(evidences = evidences.map { if (it.id == evidenceId) change(it) else it })
}
