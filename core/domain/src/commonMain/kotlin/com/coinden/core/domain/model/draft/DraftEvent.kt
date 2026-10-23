package com.coinden.core.domain.model.draft

import com.coinden.core.domain.model.bundle.ReviewReason
import com.coinden.core.domain.model.category.CategoryRef

/**
 * Transición de un borrador, guardada como evento append-only (sync-ready).
 *
 * El estado actual no se guarda: se obtiene reproduciendo los eventos con [DraftProjection].
 */
sealed interface DraftEvent {
    val id: String
    val draftId: String
    val ownerId: String
    val deviceId: String
    val occurredAtEpochMs: Long
    val recordedAtEpochMs: Long

    data class Captured(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val evidence: Evidence,
        /** Paquete de captura del que salió el borrador; null en los borradores de antes de los paquetes. */
        val bundleId: String? = null,
        /** Qué pago del paquete es: el id del comprobante que lo funda o la clave de un pago dicho. */
        val paymentKey: String? = null,
    ) : DraftEvent

    data class Interpreted(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val proposal: MovementProposal,
        /** Por qué el intérprete del paquete deja este pago para revisar (vacío: nada lo detiene). */
        val reviewReasons: Set<ReviewReason> = emptySet(),
    ) : DraftEvent

    /**
     * El usuario reemplaza la propuesta entera (completar, corregir o convertir un No soportado).
     * [category] es la categoría que eligió al guardar; null si no eligió ninguna.
     */
    data class Revised(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val proposal: MovementProposal,
        val category: CategoryRef? = null,
    ) : DraftEvent

    data class Confirmed(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val ledgerEventId: String,
        val movement: ReadyMovement,
    ) : DraftEvent

    /** [mergedInto]: el borrador con el que se unió ("son el mismo"); su evidencia sigue aquí. */
    data class Discarded(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val mergedInto: String? = null,
    ) : DraftEvent

    /**
     * Texto leído en el teléfono de la foto, el PDF o el audio de la evidencia. [engine] es el código
     * del lector ("tesseract", "mlkit", "pdf-text", "whisper", "system-speech").
     */
    data class Perceived(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val text: String,
        val engine: String,
    ) : DraftEvent

    /** "Son distintos": no cambia el estado; evita volver a preguntar por el par. */
    data class MarkedDistinct(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val otherDraftId: String,
    ) : DraftEvent

    /**
     * "Son el mismo": este borrador absorbe a [absorbedDraftId] y suma su evidencia. Si estaba
     * pendiente, [proposal] es la propuesta combinada; si estaba confirmado, solo se agrega
     * [reference] al movimiento confirmado (el libro no cambia).
     */
    data class Merged(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val absorbedDraftId: String,
        val absorbedEvidence: Evidence,
        val proposal: MovementProposal? = null,
        val reference: String? = null,
    ) : DraftEvent

    /**
     * "Son el mismo" entre este comprobante y el movimiento confirmado [targetDraftId]: queda un
     * cambio pendiente que la persona aprueba o descarta. No cambia nada del confirmado.
     */
    data class CorrectionProposed(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val targetDraftId: String,
    ) : DraftEvent

    /**
     * La persona aprobó una corrección del movimiento confirmado: [movement] reemplaza al anterior.
     * Si cambió el dinero, [annulledLedgerEventId] es el movimiento anulado y [ledgerEventId] el nuevo;
     * si no, [ledgerEventId] sigue igual. Desde un comprobante, [sourceDraftId] es su borrador y
     * [absorbedEvidence] su evidencia.
     */
    data class Corrected(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val movement: ReadyMovement,
        val ledgerEventId: String,
        val annulledLedgerEventId: String?,
        val origin: CorrectionOrigin,
        val sourceDraftId: String? = null,
        val absorbedEvidence: Evidence? = null,
    ) : DraftEvent
}

/**
 * Reproduce el historial de un borrador en orden cronológico (ocurrencia, registro, id),
 * así el resultado es el mismo sin importar en qué orden llegaron los eventos de cada dispositivo.
 * Los estados terminales ignoran lo que llegue después, salvo la unión con un confirmado, que
 * agrega evidencia sin cambiar el movimiento, y la corrección aprobada de un confirmado.
 */
object DraftProjection {
    private val CHRONOLOGICAL: Comparator<DraftEvent> =
        compareBy<DraftEvent>({ it.occurredAtEpochMs }, { it.recordedAtEpochMs }, { it.id })

    fun project(
        events: List<DraftEvent>,
        directory: WalletDirectory,
    ): Draft? {
        val captured = events.filterIsInstance<DraftEvent.Captured>().minWithOrNull(CHRONOLOGICAL) ?: return null
        var header =
            DraftHeader(
                captured.draftId,
                captured.ownerId,
                captured.evidence,
                captured.occurredAtEpochMs,
                bundleId = captured.bundleId,
                paymentKey = captured.paymentKey,
            )

        var state: Draft = Draft.Captured(header)
        for (event in events.sortedWith(CHRONOLOGICAL)) {
            if (event is DraftEvent.Merged && state !is Draft.Discarded) {
                header = header.copy(extraEvidence = header.extraEvidence + event.absorbedEvidence)
                val current = state
                state =
                    when {
                        current is Draft.Confirmed ->
                            current.copy(
                                header = header,
                                movement =
                                    event.reference?.let(current.movement::withReference) ?: current.movement,
                            )
                        event.proposal != null -> DraftEvaluator.evaluate(header, event.proposal, directory)
                        else -> current.withHeader(header)
                    }
                continue
            }
            if (state.isTerminal) continue
            if (event is DraftEvent.CorrectionProposed) {
                header = header.copy(correctionTarget = event.targetDraftId)
                state = state.withHeader(header)
                continue
            }
            if (event is DraftEvent.MarkedDistinct && event.otherDraftId == header.correctionTarget) {
                header = header.copy(correctionTarget = null)
                state = state.withHeader(header)
                continue
            }
            if (event is DraftEvent.Perceived) {
                header = header.copy(perceivedText = event.text)
                state = state.withHeader(header)
                continue
            }
            state =
                when (event) {
                    is DraftEvent.Captured, is DraftEvent.MarkedDistinct, is DraftEvent.Merged, is DraftEvent.Perceived,
                    is DraftEvent.Corrected, is DraftEvent.CorrectionProposed,
                    -> state
                    is DraftEvent.Interpreted -> {
                        header = header.copy(reviewReasons = event.reviewReasons)
                        DraftEvaluator.evaluate(header, event.proposal, directory)
                    }
                    is DraftEvent.Revised -> {
                        // La revisión más reciente dice la categoría elegida (o que no hay). Lo que la
                        // persona corrige a mano resuelve los motivos de revisión del intérprete.
                        header = header.copy(chosenCategory = event.category, reviewReasons = emptySet())
                        DraftEvaluator.evaluate(header, event.proposal, directory)
                    }
                    is DraftEvent.Confirmed -> Draft.Confirmed(header, event.movement, event.ledgerEventId)
                    is DraftEvent.Discarded -> Draft.Discarded(header, event.mergedInto)
                }
        }
        // Las correcciones se aplican sobre el confirmado aunque un reloj atrasado las ponga antes.
        val corrections = events.filterIsInstance<DraftEvent.Corrected>().sortedWith(CHRONOLOGICAL)
        if (state is Draft.Confirmed && corrections.isNotEmpty()) {
            for (event in corrections) {
                event.absorbedEvidence?.let { header = header.copy(extraEvidence = header.extraEvidence + it) }
            }
            val last = corrections.last()
            state = Draft.Confirmed(header, last.movement, last.ledgerEventId)
        }
        return state
    }
}

private fun Draft.withHeader(header: DraftHeader): Draft =
    when (this) {
        is Draft.Captured -> copy(header = header)
        is Draft.Incomplete -> copy(header = header)
        is Draft.Ready -> copy(header = header)
        is Draft.Unsupported -> copy(header = header)
        is Draft.Confirmed -> copy(header = header)
        is Draft.Discarded -> copy(header = header)
    }
