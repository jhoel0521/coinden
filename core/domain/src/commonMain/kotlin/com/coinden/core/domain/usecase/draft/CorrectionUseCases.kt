package com.coinden.core.domain.usecase.draft

import com.coinden.core.domain.model.category.CategoryOrigin
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.Classification
import com.coinden.core.domain.model.draft.CorrectionOrigin
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftEvaluator
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.DraftHeader
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.DraftResult.Rejected
import com.coinden.core.domain.model.draft.FieldChange
import com.coinden.core.domain.model.draft.MovementDiff
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposalMerge
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.ReferenceKey
import com.coinden.core.domain.model.draft.TimelineEntry
import com.coinden.core.domain.model.draft.asProposal
import com.coinden.core.domain.model.draft.isReceipt
import com.coinden.core.domain.model.draft.markedCorrected
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.ReplaceMovementUseCase

/**
 * Caso de Uso: la persona aprueba la corrección de un movimiento confirmado con lo que dejó en el
 * formulario ([proposal] y [category]; null = Sin categoría). Se aplica todo junto o nada:
 *
 * - si cambia el dinero (tipo, monto, moneda, billetera o momento), anula el movimiento vigente y
 *   registra el nuevo en el mismo paso;
 * - si solo cambian la nota, la categoría o la referencia, el libro no se toca.
 *
 * [correctionId] permite reintentar la misma corrección (un corte a mitad) sin duplicar: los ids del
 * libro se derivan de él. Desde un comprobante, [sourceDraftId] es su borrador, que queda Descartado
 * enlazado y su evidencia pasa al movimiento.
 */
class CorrectMovementUseCase(
    private val ctx: DraftContext,
    private val ledger: LedgerContext,
) {
    private val replace = ReplaceMovementUseCase(ledger)

    suspend operator fun invoke(
        draftId: String,
        proposal: MovementProposal,
        category: CategoryRef?,
        origin: CorrectionOrigin = CorrectionOrigin.MANUAL,
        sourceDraftId: String? = null,
        correctionId: String? = null,
    ): DraftResult {
        val confirmed = ctx.load(draftId) as? Draft.Confirmed ?: return Rejected(DraftError.NotConfirmed)
        if (confirmed.movement is ReadyMovement.AdjustTo) return Rejected(DraftError.NotCorrectable)
        val id = correctionId ?: ctx.ids.newId()
        if (ctx.drafts.eventsFor(draftId).any { it.id == id }) return DraftResult.Ok(confirmed)

        val owner = confirmed.header.ownerId
        val evaluated = DraftEvaluator.evaluate(confirmed.header, proposal.markedCorrected(), ctx.directory(owner))
        val ready =
            when (evaluated) {
                is Draft.Ready -> evaluated
                is Draft.Incomplete -> return Rejected(DraftError.NotReady(evaluated.missing, evaluated.issues))
                else -> return Rejected(DraftError.NotReady(emptySet(), emptySet()))
            }
        if (ready.movement is ReadyMovement.AdjustTo) return Rejected(DraftError.NotCorrectable)
        val source = sourceDraftId?.let { ctx.load(it) ?: return Rejected(DraftError.NotFound) }
        if (source != null && source.isTerminal) return Rejected(DraftError.TerminalState)

        val categorization = ctx.categorization
        val before = categorization?.current(confirmed.ledgerEventId)
        val changes = MovementDiff.between(confirmed.movement, before?.category, ready.movement, category)
        if (changes.isEmpty() && source == null) return DraftResult.Ok(confirmed)

        val ledgerEventId =
            if (MovementDiff.touchesMoney(changes)) {
                val replacement = ledgerEventOf(ready.movement, "${ledgerIdBase(draftId)}:$id")
                val result = replace(confirmed.ledgerEventId, "annul:${confirmed.ledgerEventId}", replacement)
                if (result is WalletOperationResult.Rejected) return Rejected(DraftError.Wallet(result.error))
                replacement.id
            } else {
                confirmed.ledgerEventId
            }

        if (categorization != null && ready.movement is ReadyMovement.Expense) {
            val reclassify =
                ledgerEventId != confirmed.ledgerEventId || before?.category != category || before?.description != ready.movement.note
            if (reclassify) {
                categorization.classify(owner, ledgerEventId, ready.movement.note, category, category?.let { CategoryOrigin.USER })
            }
        }

        val corrected =
            ctx.record(confirmed, eventId = id) { eventId, device, now ->
                DraftEvent.Corrected(
                    eventId,
                    draftId,
                    owner,
                    device,
                    now,
                    now,
                    ready.movement,
                    ledgerEventId,
                    confirmed.ledgerEventId.takeIf { ledgerEventId != it },
                    origin,
                    source?.id,
                    source?.header?.evidence,
                )
            }
        if (source != null) {
            ctx.record(
                source,
            ) { eventId, device, now -> DraftEvent.Discarded(eventId, source.id, owner, device, now, now, mergedInto = draftId) }
        }
        return DraftResult.Ok(corrected)
    }

    private suspend fun ledgerEventOf(
        movement: ReadyMovement,
        eventId: String,
    ): LedgerEvent {
        val now = ledger.clock.nowEpochMs()
        val device = ledger.device.deviceId()
        return when (movement) {
            is ReadyMovement.Expense ->
                LedgerEvent.Expense(
                    eventId,
                    device,
                    movement.occurredAtEpochMs,
                    now,
                    movement.walletId,
                    movement.amount,
                )
            is ReadyMovement.Income ->
                LedgerEvent.Income(
                    eventId,
                    device,
                    movement.occurredAtEpochMs,
                    now,
                    movement.walletId,
                    movement.amount,
                )
            is ReadyMovement.Transfer ->
                LedgerEvent.Transfer(
                    eventId,
                    device,
                    movement.occurredAtEpochMs,
                    now,
                    movement.fromWalletId,
                    movement.toWalletId,
                    movement.out,
                    movement.into,
                )
            is ReadyMovement.AdjustTo -> error("Los ajustes no se corrigen")
        }
    }

    private fun ledgerIdBase(draftId: String) = ConfirmDraftUseCase.ledgerEventIdFor(draftId)
}

/** Un cambio pendiente: lo que diría el movimiento [target] si se une con el comprobante [source]. */
data class PendingCorrection(
    val target: Draft.Confirmed,
    val source: Draft,
    /** Propuesta combinada para precargar el formulario (el comprobante gana en lo leído). */
    val proposal: MovementProposal,
    /** Categoría vigente del movimiento (null = Sin categoría). */
    val category: CategoryRef?,
    val changes: List<FieldChange>,
)

/**
 * Caso de Uso: el cambio pendiente de un comprobante, calculado con los datos actuales (no se
 * guarda), o null si no tiene. Las diferencias son contra el movimiento confirmado vigente.
 */
class GetPendingCorrectionUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(sourceDraftId: String): PendingCorrection? {
        val source = ctx.load(sourceDraftId) ?: return null
        if (source.isTerminal) return null
        val target = source.header.correctionTarget?.let { ctx.load(it) } as? Draft.Confirmed ?: return null
        val sourceProposal =
            when (source) {
                is Draft.Ready -> source.proposal
                is Draft.Incomplete -> source.proposal
                else -> null
            }
        val proposal = ProposalMerge.combine(target.movement.asProposal(), sourceProposal) ?: return null
        val category = ctx.categorization?.current(target.ledgerEventId)?.category
        val evaluated = DraftEvaluator.evaluate(target.header, proposal, ctx.directory(target.header.ownerId))
        val changes = (evaluated as? Draft.Ready)?.let { MovementDiff.between(target.movement, category, it.movement, category) }.orEmpty()
        return PendingCorrection(target, source, proposal, category, changes)
    }
}

/**
 * Caso de Uso: descartar el cambio pendiente de un comprobante. El movimiento confirmado no cambia
 * y suma el comprobante como evidencia; el borrador del comprobante queda Descartado enlazado.
 */
class DismissCorrectionUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(sourceDraftId: String): DraftResult {
        val source = ctx.load(sourceDraftId) ?: return Rejected(DraftError.NotFound)
        if (source.isTerminal) return Rejected(DraftError.TerminalState)
        val target = source.header.correctionTarget?.let { ctx.load(it) } as? Draft.Confirmed ?: return Rejected(DraftError.NotConfirmed)
        val owner = target.header.ownerId
        val kept =
            ctx.record(target) { id, device, now ->
                DraftEvent.Merged(id, target.id, owner, device, now, now, source.id, source.header.evidence)
            }
        ctx.record(source) { id, device, now -> DraftEvent.Discarded(id, source.id, owner, device, now, now, mergedInto = target.id) }
        return DraftResult.Ok(kept)
    }
}

/**
 * Caso de Uso: después de interpretar una captura. Si es un comprobante real con la misma
 * referencia que un movimiento confirmado y no cambiaría nada, se guarda como su evidencia y no
 * queda nada para revisar. Devuelve el confirmado, o null si no era idéntico (sin referencia nunca
 * lo es: dos pagos iguales sin número no se distinguen).
 */
class SettleIdenticalReceiptUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(draftId: String): Draft.Confirmed? {
        val receipt = ctx.load(draftId) ?: return null
        if (receipt.isTerminal || !receipt.isReceipt) return null
        val proposal =
            when (receipt) {
                is Draft.Ready -> receipt.proposal
                is Draft.Incomplete -> receipt.proposal
                else -> return null
            }
        val key = referenceOf(proposal)?.let(ReferenceKey::of)?.takeIf { it.isNotEmpty() } ?: return null
        val owner = receipt.header.ownerId
        val directory = ctx.directory(owner)
        val target =
            ctx.drafts
                .draftIdsFor(owner)
                .filter { it != draftId }
                .mapNotNull { ctx.load(it) as? Draft.Confirmed }
                .firstOrNull { c -> referenceOf(c.movement)?.let(ReferenceKey::of) == key }
                ?: return null
        val combined = ProposalMerge.combine(target.movement.asProposal(), proposal) ?: return null
        val evaluated = DraftEvaluator.evaluate(target.header, combined, directory) as? Draft.Ready ?: return null
        if (MovementDiff.between(target.movement, null, evaluated.movement, null).isNotEmpty()) return null

        val kept =
            ctx.record(target) { id, device, now ->
                DraftEvent.Merged(id, target.id, owner, device, now, now, receipt.id, receipt.header.evidence)
            }
        ctx.record(receipt) { id, device, now -> DraftEvent.Discarded(id, receipt.id, owner, device, now, now, mergedInto = target.id) }
        return kept as Draft.Confirmed
    }

    private fun referenceOf(proposal: MovementProposal): String? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.reference
            is MovementProposal.Income -> proposal.reference
            is MovementProposal.Transfer -> proposal.reference
            else -> null
        }

    private fun referenceOf(movement: ReadyMovement): String? =
        when (movement) {
            is ReadyMovement.Expense -> movement.reference
            is ReadyMovement.Income -> movement.reference
            is ReadyMovement.Transfer -> movement.reference
            is ReadyMovement.AdjustTo -> null
        }
}

/** Un movimiento confirmado vigente con su clasificación (null si no es un gasto o no tiene). */
data class HistoryEntry(
    val draft: Draft.Confirmed,
    val classification: Classification?,
)

/**
 * Caso de Uso: el historial de movimientos confirmados, del más reciente al más antiguo. Solo lo
 * vigente: una corrección muestra el movimiento nuevo, no el anulado.
 */
class GetMovementHistoryUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(ownerId: String): List<HistoryEntry> =
        ctx.drafts
            .draftIdsFor(ownerId)
            .mapNotNull { ctx.load(it) as? Draft.Confirmed }
            .sortedWith(compareByDescending<Draft.Confirmed> { it.movement.occurredAtEpochMs }.thenByDescending { it.id })
            .map { HistoryEntry(it, ctx.categorization?.current(it.ledgerEventId)) }
}

/** Caso de Uso: la línea de tiempo de un movimiento confirmado (vacía si no está confirmado). */
class GetMovementTimelineUseCase(
    private val ctx: DraftContext,
) {
    private val chronological = compareBy<DraftEvent>({ it.occurredAtEpochMs }, { it.recordedAtEpochMs }, { it.id })

    suspend operator fun invoke(draftId: String): List<TimelineEntry> {
        val events = ctx.drafts.eventsFor(draftId)
        val confirmed = events.filterIsInstance<DraftEvent.Confirmed>().minWithOrNull(chronological) ?: return emptyList()
        val entries = mutableListOf<TimelineEntry>(TimelineEntry.Confirmed(confirmed.occurredAtEpochMs, confirmed.movement))
        var current = confirmed.movement
        for (event in events.sortedWith(chronological)) {
            when (event) {
                is DraftEvent.Corrected -> {
                    entries +=
                        TimelineEntry.Corrected(
                            event.occurredAtEpochMs,
                            event.origin,
                            MovementDiff.between(current, null, event.movement, null),
                            event.sourceDraftId?.let { readMovementOf(it) },
                            event.absorbedEvidence,
                        )
                    current = event.movement
                }
                is DraftEvent.Merged ->
                    if (event.occurredAtEpochMs >= confirmed.occurredAtEpochMs) {
                        entries += TimelineEntry.EvidenceAdded(event.occurredAtEpochMs, event.absorbedEvidence)
                    }
                else -> Unit
            }
        }
        return entries
    }

    /** Lo que propuso el borrador del comprobante (su última lectura o revisión), como movimiento. */
    private suspend fun readMovementOf(sourceDraftId: String): ReadyMovement? {
        val events = ctx.drafts.eventsFor(sourceDraftId)
        val header = events.filterIsInstance<DraftEvent.Captured>().firstOrNull() ?: return null
        val proposal =
            events
                .sortedWith(chronological)
                .mapNotNull {
                    when (it) {
                        is DraftEvent.Interpreted -> it.proposal
                        is DraftEvent.Revised -> it.proposal
                        else -> null
                    }
                }.lastOrNull() ?: return null
        val draftHeader = DraftHeader(header.draftId, header.ownerId, header.evidence, header.occurredAtEpochMs)
        return (DraftEvaluator.evaluate(draftHeader, proposal, ctx.directory(header.ownerId)) as? Draft.Ready)?.movement
    }
}
