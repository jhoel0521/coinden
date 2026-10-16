package com.coinden.app.inbox

import com.coinden.app.AppContainer
import com.coinden.app.capture.ConfirmProblem
import com.coinden.app.capture.DraftCard
import com.coinden.app.capture.DraftCards
import com.coinden.app.capture.FormMode
import com.coinden.app.capture.MovementKindView
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.wallet.WalletError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Saldo de una billetera si se confirma lo pendiente. */
data class InboxBalance(
    val walletName: String,
    val current: Money,
    val projected: Money,
    val shortfall: Boolean,
)

/**
 * Un pendiente de la bandeja. [opensForm]: un gasto o ingreso se revisa en el formulario; una
 * transferencia o un ajuste se confirma o descarta desde la tarjeta. [correctionTarget]: es un
 * cambio pendiente sobre ese movimiento confirmado, con su nota [targetNote].
 */
data class InboxItem(
    val card: DraftCard,
    val opensForm: Boolean,
    val correctionTarget: String? = null,
    val targetNote: String? = null,
)

/** "¿Confirmar N movimientos?": cuántos y cuánto cambia cada billetera. */
data class BatchSummary(
    val count: Int,
    val byWallet: List<Pair<String, Money>>,
)

data class InboxState(
    val loaded: Boolean = false,
    val balances: List<InboxBalance> = emptyList(),
    val review: List<InboxItem> = emptyList(),
    val ready: List<InboxItem> = emptyList(),
    val summary: BatchSummary? = null,
    /** Por qué no se confirmó cada borrador (id -> motivo). */
    val problems: Map<String, ConfirmProblem> = emptyMap(),
) {
    val isEmpty: Boolean get() = loaded && review.isEmpty() && ready.isEmpty()
}

/**
 * La bandeja de la noche, sin Compose: saldos proyectados, "Revisar" (lo dudoso, las preguntas y
 * los cambios pendientes) y "Listos" (lo seguro, que se confirma de una vez tras un resumen).
 */
class ReviewInboxPresenter(
    private val container: AppContainer,
) {
    private val cards = DraftCards(container)
    private val mutableState = MutableStateFlow(InboxState())
    val state: StateFlow<InboxState> = mutableState.asStateFlow()

    private var readyDrafts: List<Draft> = emptyList()

    suspend fun load() {
        val owner = container.ownerId
        val inbox = container.draftInbox(owner, container.utcOffsetMinutes)
        val directory = container.drafts.directory(owner)
        val balances =
            inbox.projections.mapNotNull { p ->
                directory.byId(p.walletId)?.let { InboxBalance(it.name, p.current, p.projected, p.hasShortfall) }
            }
        val batch = inbox.batchReady.toSet()
        val items =
            cards.of(inbox.pending).zip(inbox.pending).map { (card, draft) ->
                val target = draft.header.correctionTarget
                val targetNote = target?.let { (container.drafts.load(it) as? Draft.Confirmed)?.movement?.note }
                InboxItem(card, opensForm = opensForm(card, draft), correctionTarget = target, targetNote = targetNote)
            }
        readyDrafts = inbox.pending.filter { it.id in batch }
        mutableState.update { s ->
            s.copy(
                loaded = true,
                balances = balances,
                review = items.filterNot { it.card.draftId in batch },
                ready = items.filter { it.card.draftId in batch },
                problems = s.problems.filterKeys { id -> inbox.pending.any { it.id == id } },
            )
        }
    }

    /** Dónde se revisa un pendiente: el formulario de revisión o el de corrección (cambio pendiente). */
    fun formFor(item: InboxItem): FormMode? =
        when {
            item.correctionTarget != null -> FormMode.Correct(item.correctionTarget, item.card.draftId)
            item.opensForm -> FormMode.Review(item.card.draftId)
            else -> null
        }

    /** Muestra el resumen antes de "Confirmar los N listos". */
    suspend fun askConfirmReady() {
        val directory = container.drafts.directory(container.ownerId)
        val totals = linkedMapOf<String, Money>()

        fun add(
            walletId: String,
            delta: Money,
        ) {
            totals[walletId] = totals[walletId]?.let { it + delta } ?: delta
        }
        readyDrafts.filterIsInstance<Draft.Ready>().forEach { draft ->
            when (val m = draft.movement) {
                is ReadyMovement.Expense -> add(m.walletId, -m.amount)
                is ReadyMovement.Income -> add(m.walletId, m.amount)
                is ReadyMovement.Transfer -> {
                    add(m.fromWalletId, -m.out)
                    add(m.toWalletId, m.into)
                }
                is ReadyMovement.AdjustTo -> Unit
            }
        }
        val byWallet = totals.map { (id, total) -> (directory.byId(id)?.name ?: id) to total }
        mutableState.update { it.copy(summary = BatchSummary(readyDrafts.size, byWallet)) }
    }

    fun cancelSummary() = mutableState.update { it.copy(summary = null) }

    /** Confirma los listos del resumen; los que el libro rechace quedan con su motivo. */
    suspend fun confirmReady() {
        val ids = readyDrafts.map { it.id }
        val results = container.confirmReadyDrafts(ids)
        val problems = results.mapNotNull { (id, r) -> (r as? DraftResult.Rejected)?.let { id to problemOf(it.error) } }.toMap()
        mutableState.update { it.copy(summary = null, problems = it.problems + problems) }
        load()
    }

    /** Confirma desde la tarjeta (transferencias y ajustes). */
    suspend fun confirm(draftId: String) {
        when (val r = container.confirmDraft(draftId)) {
            is DraftResult.Ok -> mutableState.update { it.copy(problems = it.problems - draftId) }
            is DraftResult.Rejected -> mutableState.update { it.copy(problems = it.problems + (draftId to problemOf(r.error))) }
        }
        load()
    }

    suspend fun discard(draftId: String) {
        container.discardDraft(draftId)
        load()
    }

    /**
     * Responde "¿es el mismo gasto?" en la bandeja. Devuelve el formulario de corrección si un
     * comprobante quedó como cambio pendiente de un confirmado (se revisa ahí antes de aplicar nada).
     */
    suspend fun resolve(
        draftId: String,
        otherDraftId: String,
        same: Boolean,
    ): FormMode? {
        val outcome = container.resolveSameMovement(draftId, otherDraftId, same)
        load()
        val draft = (outcome as? DraftResult.Ok)?.draft ?: return null
        return draft.header.correctionTarget?.let { FormMode.Correct(it, draft.id) }
    }

    private fun opensForm(
        card: DraftCard,
        draft: Draft,
    ): Boolean =
        when (card.kind) {
            MovementKindView.TRANSFER, MovementKindView.ADJUSTMENT -> false
            else -> draft !is Draft.Confirmed
        }

    private fun problemOf(error: DraftError): ConfirmProblem =
        when (error) {
            is DraftError.NotReady -> ConfirmProblem.NotReady
            is DraftError.Wallet ->
                (error.error as? WalletError.InsufficientFunds)?.let { ConfirmProblem.InsufficientFunds(it.shortfall) }
                    ?: ConfirmProblem.Rejected
            else -> ConfirmProblem.Rejected
        }
}
