package com.coinden.app.history

import com.coinden.app.AppContainer
import com.coinden.app.capture.FormMode
import com.coinden.app.capture.MovementKindView
import com.coinden.app.ui.ScreenLoad
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.ReadyMovement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Un movimiento confirmado vigente, en datos (la pantalla lo traduce y formatea). */
data class HistoryItem(
    val draftId: String,
    val kind: MovementKindView,
    val note: String?,
    val amount: Money,
    val walletName: String?,
    val occurredAtEpochMs: Long,
    val category: CategoryRef?,
    val reference: String?,
    /** Evidencias guardadas: la original más las unidas o de comprobantes. */
    val evidenceCount: Int,
    /** Tuvo al menos una corrección aprobada. */
    val corrected: Boolean,
) {
    /** Se corrige en el formulario (gastos e ingresos). */
    val correctable: Boolean get() = kind == MovementKindView.EXPENSE || kind == MovementKindView.INCOME
}

data class HistoryState(
    val loaded: Boolean = false,
    val items: List<HistoryItem> = emptyList(),
    val load: ScreenLoad = ScreenLoad.LOADING,
)

/** El historial de lo confirmado, del más reciente al más antiguo, con Corregir. Sin Compose. */
class HistoryPresenter(
    private val container: AppContainer,
    private val query: (suspend () -> HistoryState)? = null,
) {
    private val mutableState = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = mutableState.asStateFlow()
    private var generation = 0L

    suspend fun load() {
        val request = ++generation
        mutableState.value = state.value.copy(load = ScreenLoad.LOADING)
        try {
            val snapshot = query?.invoke() ?: readSnapshot()
            if (request == generation) mutableState.value = snapshot.copy(load = ScreenLoad.READY, loaded = true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (request == generation) mutableState.value = state.value.copy(load = ScreenLoad.ERROR)
        }
    }

    private suspend fun readSnapshot(): HistoryState {
        val owner = container.ownerId
        val directory = container.drafts.directory(owner)
        val items =
            container.movementHistory(owner).map { entry ->
                val draft = entry.draft
                val (kind, amount, walletId, reference) =
                    when (val m = draft.movement) {
                        is ReadyMovement.Expense -> Facts(MovementKindView.EXPENSE, m.amount, m.walletId, m.reference)
                        is ReadyMovement.Income -> Facts(MovementKindView.INCOME, m.amount, m.walletId, m.reference)
                        is ReadyMovement.Transfer -> Facts(MovementKindView.TRANSFER, m.out, m.fromWalletId, m.reference)
                        is ReadyMovement.AdjustTo -> Facts(MovementKindView.ADJUSTMENT, m.target, m.walletId, null)
                    }
                HistoryItem(
                    draftId = draft.id,
                    kind = kind,
                    note = draft.movement.note,
                    amount = amount,
                    walletName = directory.byId(walletId)?.name,
                    occurredAtEpochMs = draft.movement.occurredAtEpochMs,
                    category = entry.classification?.category,
                    reference = reference,
                    evidenceCount = 1 + draft.header.extraEvidence.size,
                    corrected =
                        container.drafts.drafts
                            .eventsFor(draft.id)
                            .any { it is DraftEvent.Corrected },
                )
            }
        return HistoryState(loaded = true, items = items)
    }

    /** Corregir abre el formulario con los datos del movimiento; nada cambia hasta confirmar. */
    fun correct(item: HistoryItem): FormMode? = if (item.correctable) FormMode.Correct(item.draftId) else null

    private data class Facts(
        val kind: MovementKindView,
        val amount: Money,
        val walletId: String,
        val reference: String?,
    )
}
