package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.SuggestionReason
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.Wallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Tipo de movimiento visto por la pantalla (la pantalla lo traduce). */
enum class MovementKindView { EXPENSE, INCOME, TRANSFER, ADJUSTMENT, NOT_A_MOVEMENT }

/** Estado del borrador visto por la pantalla. */
enum class DraftStatusView { CAPTURED, INCOMPLETE, READY, UNSUPPORTED }

/** Categoría sugerida y por qué se sugirió. */
data class CategoryView(
    val ref: CategoryRef,
    val reason: SuggestionReason,
)

/** Un borrador capturado, en datos semánticos: sin textos ni formatos. */
data class DraftCard(
    val draftId: String,
    val evidenceText: String?,
    val status: DraftStatusView,
    val kind: MovementKindView?,
    val amount: Money?,
    val walletName: String?,
    val walletSuggested: Boolean,
    val confidence: Confidence,
    val category: CategoryView?,
)

data class CaptureDemoState(
    val text: String = "",
    /** Más reciente primero. */
    val cards: List<DraftCard> = emptyList(),
)

/**
 * Presentador de la demostración de captura: pasa la frase por el intérprete real y la
 * bandeja de borradores y expone el resultado. No usa Compose, así que se prueba en la JVM.
 */
class CaptureDemoPresenter(
    private val container: AppContainer,
) {
    private val mutableState = MutableStateFlow(CaptureDemoState())
    val state: StateFlow<CaptureDemoState> = mutableState.asStateFlow()

    fun onTextChange(text: String) {
        mutableState.update { it.copy(text = text) }
    }

    /** Al abrir: los borradores pendientes guardados, más recientes primero. */
    suspend fun load() {
        val owner = container.ownerId
        val directory = container.drafts.directory(owner)
        val pending = container.draftInbox(owner).pending.sortedByDescending { it.header.capturedAtEpochMs }
        val cards = pending.map { card(it, directory) }
        mutableState.update { it.copy(cards = cards) }
    }

    suspend fun onCapture() {
        val text = state.value.text.trim()
        if (text.isEmpty()) return
        val owner = container.ownerId
        val drafts = container.captureDraft(owner, Evidence(EvidenceSource.TEXT, text))
        val directory = container.drafts.directory(owner)
        val cards = drafts.map { card(it, directory) }
        mutableState.update { it.copy(text = "", cards = cards.reversed() + it.cards) }
    }

    private suspend fun card(
        draft: Draft,
        directory: WalletDirectory,
    ): DraftCard {
        val proposal =
            when (draft) {
                is Draft.Ready -> draft.proposal
                is Draft.Incomplete -> draft.proposal
                else -> null
            }
        val wallet = walletOf(draft, proposal, directory)
        val note = (proposal as? MovementProposal.Expense)?.note
        val suggestion = note?.let { container.categorization.suggest(container.ownerId, it) }
        return DraftCard(
            draftId = draft.id,
            evidenceText = draft.header.evidence.text,
            status = statusOf(draft),
            kind = proposal?.let(::kindOf) ?: (draft as? Draft.Unsupported)?.let { MovementKindView.NOT_A_MOVEMENT },
            amount = amountOf(draft, proposal, wallet),
            walletName = wallet?.name,
            walletSuggested = proposal?.origins?.get(DraftField.WALLET) == FieldOrigin.SUGGESTED,
            confidence = draft.confidence,
            category = suggestion?.let { CategoryView(it.category, it.reason) },
        )
    }

    private fun statusOf(draft: Draft): DraftStatusView =
        when (draft) {
            is Draft.Captured, is Draft.Confirmed, is Draft.Discarded -> DraftStatusView.CAPTURED
            is Draft.Incomplete -> DraftStatusView.INCOMPLETE
            is Draft.Ready -> DraftStatusView.READY
            is Draft.Unsupported -> DraftStatusView.UNSUPPORTED
        }

    private fun kindOf(proposal: MovementProposal): MovementKindView? =
        when (proposal) {
            is MovementProposal.Expense -> MovementKindView.EXPENSE
            is MovementProposal.Income -> MovementKindView.INCOME
            is MovementProposal.Transfer -> MovementKindView.TRANSFER
            is MovementProposal.AdjustTo -> MovementKindView.ADJUSTMENT
            is MovementProposal.NotAMovement -> MovementKindView.NOT_A_MOVEMENT
            is MovementProposal.Unknown -> null
        }

    private fun walletOf(
        draft: Draft,
        proposal: MovementProposal?,
        directory: WalletDirectory,
    ): Wallet? {
        if (draft is Draft.Ready) {
            return when (val m = draft.movement) {
                is ReadyMovement.Expense -> directory.byId(m.walletId)
                is ReadyMovement.Income -> directory.byId(m.walletId)
                is ReadyMovement.Transfer -> directory.byId(m.fromWalletId)
                is ReadyMovement.AdjustTo -> directory.byId(m.walletId)
            }
        }
        val ref =
            when (proposal) {
                is MovementProposal.Expense -> proposal.wallet
                is MovementProposal.Income -> proposal.wallet
                is MovementProposal.Transfer -> proposal.from
                is MovementProposal.AdjustTo -> proposal.wallet
                else -> null
            }
        return when (ref) {
            is WalletRef.ById -> directory.byId(ref.walletId)
            is WalletRef.ByHint -> directory.byHint(ref.hint)
            null -> null
        }
    }

    private fun amountOf(
        draft: Draft,
        proposal: MovementProposal?,
        wallet: Wallet?,
    ): Money? {
        if (draft is Draft.Ready) {
            return when (val m = draft.movement) {
                is ReadyMovement.Expense -> m.amount
                is ReadyMovement.Income -> m.amount
                is ReadyMovement.Transfer -> m.out
                is ReadyMovement.AdjustTo -> m.target
            }
        }
        val proposed: ProposedAmount =
            when (proposal) {
                is MovementProposal.Expense -> proposal.amount
                is MovementProposal.Income -> proposal.amount
                is MovementProposal.Transfer -> proposal.out
                is MovementProposal.AdjustTo -> proposal.target
                else -> null
            } ?: return null
        val currency = proposed.currency ?: wallet?.currency ?: return null
        return runCatching { Money.parseCanonical(proposed.value, currency) }.getOrNull()
    }
}
