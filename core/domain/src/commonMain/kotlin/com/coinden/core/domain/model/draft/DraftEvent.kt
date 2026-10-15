package com.coinden.core.domain.model.draft

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
    ) : DraftEvent

    data class Interpreted(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val proposal: MovementProposal,
    ) : DraftEvent

    /** El usuario reemplaza la propuesta entera (completar, corregir o convertir un No soportado). */
    data class Revised(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val proposal: MovementProposal,
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
}

/**
 * Reproduce el historial de un borrador en orden cronológico (ocurrencia, registro, id),
 * así el resultado es el mismo sin importar en qué orden llegaron los eventos de cada dispositivo.
 * Los estados terminales ignoran lo que llegue después, salvo la unión con un confirmado, que
 * agrega evidencia y referencia sin cambiar el movimiento.
 */
object DraftProjection {
    private val CHRONOLOGICAL: Comparator<DraftEvent> =
        compareBy<DraftEvent>({ it.occurredAtEpochMs }, { it.recordedAtEpochMs }, { it.id })

    fun project(
        events: List<DraftEvent>,
        directory: WalletDirectory,
    ): Draft? {
        val captured = events.filterIsInstance<DraftEvent.Captured>().minWithOrNull(CHRONOLOGICAL) ?: return null
        var header = DraftHeader(captured.draftId, captured.ownerId, captured.evidence, captured.occurredAtEpochMs)

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
            state =
                when (event) {
                    is DraftEvent.Captured, is DraftEvent.MarkedDistinct, is DraftEvent.Merged -> state
                    is DraftEvent.Interpreted -> DraftEvaluator.evaluate(header, event.proposal, directory)
                    is DraftEvent.Revised -> DraftEvaluator.evaluate(header, event.proposal, directory)
                    is DraftEvent.Confirmed -> Draft.Confirmed(header, event.movement, event.ledgerEventId)
                    is DraftEvent.Discarded -> Draft.Discarded(header, event.mergedInto)
                }
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
