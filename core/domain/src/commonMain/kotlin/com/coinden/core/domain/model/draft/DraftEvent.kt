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

    data class Discarded(
        override val id: String,
        override val draftId: String,
        override val ownerId: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
    ) : DraftEvent
}

/**
 * Reproduce el historial de un borrador en orden cronológico (ocurrencia, registro, id),
 * así el resultado es el mismo sin importar en qué orden llegaron los eventos de cada dispositivo.
 * Los estados terminales ignoran lo que llegue después.
 */
object DraftProjection {
    private val CHRONOLOGICAL: Comparator<DraftEvent> =
        compareBy<DraftEvent>({ it.occurredAtEpochMs }, { it.recordedAtEpochMs }, { it.id })

    fun project(
        events: List<DraftEvent>,
        directory: WalletDirectory,
    ): Draft? {
        val captured = events.filterIsInstance<DraftEvent.Captured>().minWithOrNull(CHRONOLOGICAL) ?: return null
        val header = DraftHeader(captured.draftId, captured.ownerId, captured.evidence, captured.occurredAtEpochMs)

        var state: Draft = Draft.Captured(header)
        for (event in events.sortedWith(CHRONOLOGICAL)) {
            if (state.isTerminal) break
            state =
                when (event) {
                    is DraftEvent.Captured -> state
                    is DraftEvent.Interpreted -> DraftEvaluator.evaluate(header, event.proposal, directory)
                    is DraftEvent.Revised -> DraftEvaluator.evaluate(header, event.proposal, directory)
                    is DraftEvent.Confirmed -> Draft.Confirmed(header, event.movement, event.ledgerEventId)
                    is DraftEvent.Discarded -> Draft.Discarded(header)
                }
        }
        return state
    }
}
