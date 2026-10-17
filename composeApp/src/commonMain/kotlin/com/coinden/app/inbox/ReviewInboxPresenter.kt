package com.coinden.app.inbox

import com.coinden.app.AppContainer
import com.coinden.app.capture.ConfirmProblem
import com.coinden.app.capture.DraftCard
import com.coinden.app.capture.DraftCards
import com.coinden.app.capture.FormMode
import com.coinden.app.capture.MovementKindView
import com.coinden.app.ui.ScreenLoad
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.CaptureCoach
import com.coinden.core.domain.model.draft.CaptureStats
import com.coinden.core.domain.model.draft.CaptureTip
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.wallet.WalletError
import kotlinx.coroutines.CancellationException
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
    /** Por qué hay que revisarlo (null si está listo): la tarjeta compacta lo muestra en lugar del estado. */
    val reason: InboxReason? = null,
)

/** Por qué un pendiente está en "Para revisar", como dato (la pantalla lo traduce). */
enum class InboxReason {
    PENDING_CHANGE,
    SAME_QUESTION,
    UNREAD,
    NOT_A_MOVEMENT,
    MISSING_AMOUNT,
    MISSING_WALLET,
    SUGGESTED_WALLET,
    CHECK_DATA,
}

/** El motivo de un pendiente, del más urgente al más leve; null si nada lo detiene. */
internal fun reasonOf(
    draft: Draft,
    card: DraftCard,
): InboxReason? {
    val tips = CaptureCoach.tipsFor(draft)
    return when {
        draft.header.correctionTarget != null -> InboxReason.PENDING_CHANGE
        card.question != null -> InboxReason.SAME_QUESTION
        draft is Draft.Captured && draft.header.evidence.attachmentRef != null -> InboxReason.UNREAD
        draft is Draft.Unsupported -> InboxReason.NOT_A_MOVEMENT
        CaptureTip.SAY_AMOUNT in tips || draft is Draft.Captured -> InboxReason.MISSING_AMOUNT
        CaptureTip.SAY_WALLET in tips && card.walletName == null -> InboxReason.MISSING_WALLET
        CaptureTip.SAY_WALLET in tips -> InboxReason.SUGGESTED_WALLET
        draft !is Draft.Ready || draft.confidence != Confidence.HIGH -> InboxReason.CHECK_DATA
        else -> null
    }
}

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
    /** Capturas de los últimos 7 días y cuántas quedaron listas sin revisar (null si no hubo). */
    val week: CaptureStats? = null,
    val load: ScreenLoad = ScreenLoad.LOADING,
) {
    val isEmpty: Boolean get() = loaded && load == ScreenLoad.READY && review.isEmpty() && ready.isEmpty()
}

data class InboxSnapshot(
    val state: InboxState,
    val readyDrafts: List<Draft> = emptyList(),
)

/**
 * La bandeja de la noche, sin Compose: saldos proyectados, "Revisar" (lo dudoso, las preguntas y
 * los cambios pendientes) y "Listos" (lo seguro, que se confirma de una vez tras un resumen).
 */
class ReviewInboxPresenter(
    private val container: AppContainer,
    private val query: (suspend () -> InboxSnapshot)? = null,
) {
    private val cards = DraftCards(container)
    private val mutableState = MutableStateFlow(InboxState())
    val state: StateFlow<InboxState> = mutableState.asStateFlow()

    private var readyDrafts: List<Draft> = emptyList()
    private var generation = 0L

    suspend fun load() {
        val request = ++generation
        mutableState.update { it.copy(load = ScreenLoad.LOADING, summary = null) }
        try {
            val snapshot = query?.invoke() ?: readSnapshot()
            if (request == generation) {
                readyDrafts = snapshot.readyDrafts
                mutableState.value = snapshot.state.copy(load = ScreenLoad.READY, loaded = true)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (request == generation) mutableState.update { it.copy(load = ScreenLoad.ERROR) }
        }
    }

    private suspend fun readSnapshot(): InboxSnapshot {
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
                InboxItem(card, opensForm(card, draft), target, targetNote, reason = if (draft.id in batch) null else reasonOf(draft, card))
            }
        val drafts = inbox.pending.filter { it.id in batch }
        val week = container.captureStats(owner, container.clock.nowEpochMs() - WEEK_MS).takeIf { it.total > 0 }
        return InboxSnapshot(
            state.value.copy(
                loaded = true,
                balances = balances,
                review = items.filterNot { it.card.draftId in batch },
                ready = items.filter { it.card.draftId in batch },
                problems = state.value.problems.filterKeys { id -> inbox.pending.any { it.id == id } },
                week = week,
            ),
            drafts,
        )
    }

    /** Dónde se revisa un pendiente: el formulario de revisión o el de corrección (cambio pendiente). */
    fun formFor(item: InboxItem): FormMode? =
        when {
            state.value.load != ScreenLoad.READY -> null
            item.correctionTarget != null -> FormMode.Correct(item.correctionTarget, item.card.draftId)
            item.opensForm -> FormMode.Review(item.card.draftId)
            else -> null
        }

    /** Muestra el resumen antes de "Confirmar los N listos". */
    suspend fun askConfirmReady() {
        if (state.value.load != ScreenLoad.READY) return
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
        if (state.value.load != ScreenLoad.READY) return
        val ids = readyDrafts.map { it.id }
        val results = container.confirmReadyDrafts(ids)
        val problems = results.mapNotNull { (id, r) -> (r as? DraftResult.Rejected)?.let { id to problemOf(it.error) } }.toMap()
        mutableState.update { it.copy(summary = null, problems = it.problems + problems) }
        load()
    }

    /** Confirma desde la tarjeta (transferencias y ajustes). */
    suspend fun confirm(draftId: String) {
        if (state.value.load != ScreenLoad.READY) return
        when (val r = container.confirmDraft(draftId)) {
            is DraftResult.Ok -> mutableState.update { it.copy(problems = it.problems - draftId) }
            is DraftResult.Rejected -> mutableState.update { it.copy(problems = it.problems + (draftId to problemOf(r.error))) }
        }
        load()
    }

    suspend fun discard(draftId: String) {
        if (state.value.load != ScreenLoad.READY) return
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
        if (state.value.load != ScreenLoad.READY) return null
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

    private companion object {
        const val WEEK_MS = 7 * 86_400_000L
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
