package com.coinden.core.domain.usecase.draft

import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.DraftProjection
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposalMerge
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.ReferenceKey
import com.coinden.core.domain.model.draft.SameMovementQuestion
import com.coinden.core.domain.model.draft.SameMovementReason
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.TextNormalizer

/**
 * Caso de Uso: las preguntas "¿es el mismo movimiento?" entre los borradores del usuario.
 *
 * Es una consulta sobre los eventos: no cambia nada y da lo mismo en cualquier dispositivo.
 * Cada borrador pregunta como mucho por uno capturado antes: el del motivo más seguro y, a igual
 * motivo, el más cercano en el tiempo. Los descartados no se comparan.
 */
class FindSameMovementQuestionsUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(
        ownerId: String,
        utcOffsetMinutes: Int,
    ): List<SameMovementQuestion> {
        val directory = ctx.directory(ownerId)
        val histories = ctx.drafts.draftIdsFor(ownerId).map { ctx.drafts.eventsFor(it) }
        // Pares que la persona ya dijo que son distintos (desde cualquier dispositivo).
        val distinct =
            histories
                .flatten()
                .filterIsInstance<DraftEvent.MarkedDistinct>()
                .map { setOf(it.draftId, it.otherDraftId) }
                .toSet()
        val candidates =
            histories
                .mapNotNull { DraftProjection.project(it, directory) }
                .filterNot { it is Draft.Discarded }
                .map { Candidate.of(it, directory, utcOffsetMinutes) }
                .sortedWith(compareBy({ it.capturedAt }, { it.id }))

        return candidates.mapIndexedNotNull { index, later ->
            candidates
                .take(index)
                .filterNot { earlier -> setOf(later.id, earlier.id) in distinct }
                .mapNotNull { earlier -> reasonBetween(later, earlier)?.let { it to earlier } }
                .minWithOrNull(compareBy({ it.first.ordinal }, { later.capturedAt - it.second.capturedAt }))
                ?.let { (reason, earlier) -> SameMovementQuestion(later.id, earlier.id, reason) }
        }
    }

    private fun reasonBetween(
        a: Candidate,
        b: Candidate,
    ): SameMovementReason? =
        when {
            a.fingerprint != null && a.fingerprint == b.fingerprint -> SameMovementReason.SAME_FILE
            a.referenceKey != null && a.referenceKey == b.referenceKey && a.amount != null && a.amount == b.amount ->
                SameMovementReason.SAME_REFERENCE
            a.receiptText != null && a.receiptText == b.receiptText -> SameMovementReason.SAME_TEXT
            a.kind != null && a.kind == b.kind && a.amount != null && a.amount == b.amount && a.day == b.day ->
                SameMovementReason.SAME_DAY_AMOUNT
            else -> null
        }

    /** Lo que se compara de un borrador. */
    private class Candidate(
        val id: String,
        val capturedAt: Long,
        val kind: String?,
        val amount: Money?,
        val day: Long,
        val referenceKey: String?,
        val receiptText: String?,
        val fingerprint: String?,
    ) {
        companion object {
            private const val DAY_MS = 86_400_000L

            fun of(
                draft: Draft,
                directory: WalletDirectory,
                utcOffsetMinutes: Int,
            ): Candidate {
                val movement =
                    when (draft) {
                        is Draft.Ready -> draft.movement
                        is Draft.Confirmed -> draft.movement
                        else -> null
                    }
                val proposal = (draft as? Draft.Incomplete)?.proposal
                val facts = movement?.let(::factsOf) ?: proposal?.let { factsOf(it, directory) }
                val occurredAt = facts?.occurredAt ?: draft.header.capturedAtEpochMs
                val text = draft.header.evidence.text
                return Candidate(
                    id = draft.id,
                    capturedAt = draft.header.capturedAtEpochMs,
                    kind = facts?.kind,
                    amount = facts?.amount,
                    day = Math.floorDiv(occurredAt + utcOffsetMinutes * 60_000L, DAY_MS),
                    referenceKey = facts?.reference?.let(ReferenceKey::of)?.takeIf { it.isNotEmpty() },
                    // Solo un comprobante (varias líneas): dos frases cortas iguales son dos gastos posibles.
                    receiptText =
                        text
                            ?.takeIf {
                                it.trim().lines().count { line ->
                                    line.isNotBlank()
                                } > 1
                            }?.let(TextNormalizer::normalize),
                    fingerprint = draft.header.evidence.fingerprint,
                )
            }

            private fun factsOf(movement: ReadyMovement): Facts? =
                when (movement) {
                    is ReadyMovement.Expense -> Facts("expense", movement.amount, movement.occurredAtEpochMs, movement.reference)
                    is ReadyMovement.Income -> Facts("income", movement.amount, movement.occurredAtEpochMs, movement.reference)
                    is ReadyMovement.Transfer -> Facts("transfer", movement.out, movement.occurredAtEpochMs, movement.reference)
                    is ReadyMovement.AdjustTo -> null
                }

            private fun factsOf(
                proposal: MovementProposal,
                directory: WalletDirectory,
            ): Facts? =
                when (proposal) {
                    is MovementProposal.Expense ->
                        Facts("expense", money(proposal.amount, proposal.wallet, directory), proposal.occurredAtEpochMs, proposal.reference)
                    is MovementProposal.Income ->
                        Facts("income", money(proposal.amount, proposal.wallet, directory), proposal.occurredAtEpochMs, proposal.reference)
                    is MovementProposal.Transfer ->
                        Facts("transfer", money(proposal.out, proposal.from, directory), proposal.occurredAtEpochMs, proposal.reference)
                    else -> null
                }

            /** Monto propuesto con su moneda o la de su billetera; null si no se puede evaluar. */
            private fun money(
                amount: ProposedAmount?,
                wallet: WalletRef?,
                directory: WalletDirectory,
            ): Money? {
                amount ?: return null
                val walletCurrency =
                    when (wallet) {
                        is WalletRef.ById -> directory.byId(wallet.walletId)?.currency
                        is WalletRef.ByHint -> directory.byHint(wallet.hint)?.currency
                        null -> null
                    }
                val currency = amount.currency ?: walletCurrency ?: return null
                return runCatching { Money.parseCanonical(amount.value, currency) }.getOrNull()
            }
        }
    }

    private class Facts(
        val kind: String,
        val amount: Money?,
        val occurredAt: Long?,
        val reference: String?,
    )
}

/**
 * Caso de Uso: responder "¿es el mismo movimiento?".
 *
 * Son distintos: queda registrado en [draftId] y no se vuelve a preguntar por el par.
 * Son el mismo: queda el confirmado o, si ninguno lo está, el capturado primero; absorbe la
 * evidencia del otro y este se descarta enlazado. Con un confirmado solo se agrega la referencia:
 * no se registra otro movimiento, y si el comprobante indica otra billetera no se unen.
 */
class ResolveSameMovementUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(
        draftId: String,
        otherDraftId: String,
        same: Boolean,
    ): DraftResult {
        val draft = ctx.load(draftId) ?: return DraftResult.Rejected(DraftError.NotFound)
        val other = ctx.load(otherDraftId) ?: return DraftResult.Rejected(DraftError.NotFound)
        if (draft is Draft.Discarded || other is Draft.Discarded) return DraftResult.Rejected(DraftError.TerminalState)
        val owner = draft.header.ownerId

        if (!same) {
            val marked =
                ctx.record(draft) { id, device, now -> DraftEvent.MarkedDistinct(id, draftId, owner, device, now, now, otherDraftId) }
            return DraftResult.Ok(marked)
        }

        val (kept, absorbed) =
            when {
                draft is Draft.Confirmed && other is Draft.Confirmed -> return DraftResult.Rejected(DraftError.TerminalState)
                other is Draft.Confirmed -> other to draft
                draft is Draft.Confirmed -> draft to other
                else -> listOf(draft, other).sortedWith(compareBy({ it.header.capturedAtEpochMs }, { it.id })).let { it[0] to it[1] }
            }
        val absorbedProposal = proposalOf(absorbed)
        val merged =
            if (kept is Draft.Confirmed) {
                val directory = ctx.directory(owner)
                val absorbedWallet = walletIdOf(absorbed, absorbedProposal, directory)
                if (absorbedWallet != null && absorbedWallet != walletIdOf(kept.movement)) {
                    return DraftResult.Rejected(DraftError.WalletMismatch)
                }
                ctx.record(kept) { id, device, now ->
                    DraftEvent.Merged(
                        id,
                        kept.id,
                        owner,
                        device,
                        now,
                        now,
                        absorbed.id,
                        absorbed.header.evidence,
                        reference = referenceOf(absorbedProposal),
                    )
                }
            } else {
                val proposal = ProposalMerge.combine(proposalOf(kept), absorbedProposal)
                ctx.record(kept) { id, device, now ->
                    DraftEvent.Merged(id, kept.id, owner, device, now, now, absorbed.id, absorbed.header.evidence, proposal = proposal)
                }
            }
        ctx.record(absorbed) { id, device, now -> DraftEvent.Discarded(id, absorbed.id, owner, device, now, now, mergedInto = kept.id) }
        return DraftResult.Ok(ctx.load(merged.id)!!)
    }

    private fun proposalOf(draft: Draft): MovementProposal? =
        when (draft) {
            is Draft.Ready -> draft.proposal
            is Draft.Incomplete -> draft.proposal
            else -> null
        }

    private fun referenceOf(proposal: MovementProposal?): String? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.reference
            is MovementProposal.Income -> proposal.reference
            is MovementProposal.Transfer -> proposal.reference
            else -> null
        }

    private fun walletIdOf(movement: ReadyMovement): String =
        when (movement) {
            is ReadyMovement.Expense -> movement.walletId
            is ReadyMovement.Income -> movement.walletId
            is ReadyMovement.Transfer -> movement.fromWalletId
            is ReadyMovement.AdjustTo -> movement.walletId
        }

    /** Billetera del borrador absorbido: la del movimiento listo o la que nombra su propuesta. */
    private fun walletIdOf(
        draft: Draft,
        proposal: MovementProposal?,
        directory: WalletDirectory,
    ): String? {
        if (draft is Draft.Ready) return walletIdOf(draft.movement)
        val ref =
            when (proposal) {
                is MovementProposal.Expense -> proposal.wallet
                is MovementProposal.Income -> proposal.wallet
                is MovementProposal.Transfer -> proposal.from
                else -> null
            }
        return when (ref) {
            is WalletRef.ById -> ref.walletId
            is WalletRef.ByHint -> directory.byHint(ref.hint)?.id
            null -> null
        }
    }
}
