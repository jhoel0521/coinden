package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.CorrectionOrigin
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftEvaluator
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.DraftHeader
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldChange
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementDiff
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.draft.asProposal
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Tipos de movimiento del formulario. */
enum class FormKind { EXPENSE, INCOME }

enum class FormExit { LEAVE, ASK, BLOCKED }

/**
 * Para qué se abre el formulario: escribir uno nuevo, revisar un borrador pendiente o corregir un
 * movimiento confirmado (a mano, o con el cambio pendiente que trajo el comprobante [sourceDraftId]).
 */
sealed interface FormMode {
    data object New : FormMode

    data class Review(
        val draftId: String,
    ) : FormMode

    data class Correct(
        val draftId: String,
        val sourceDraftId: String? = null,
    ) : FormMode
}

/** Una billetera elegible en el formulario: nombre tal como lo escribió la persona. */
data class FormWallet(
    val id: String,
    val name: String,
    val currency: Currency,
    val type: WalletType,
)

data class DraftFormState(
    /** "Escríbelo como te salga": llena los campos con el intérprete. */
    val phrase: String = "",
    val kind: FormKind = FormKind.EXPENSE,
    /** Monto tal como se escribe; se guarda como decimal exacto. */
    val amount: String = "",
    /** null: la de la billetera. */
    val currency: Currency? = null,
    val walletId: String? = null,
    val method: PaymentMethod? = null,
    val category: CategoryRef? = null,
    /** null: la hora de la captura. */
    val occurredAtEpochMs: Long? = null,
    val note: String = "",
    /** Número de transacción (opcional). */
    val reference: String = "",
    val wallets: List<FormWallet> = emptyList(),
    val mode: FormMode = FormMode.New,
    /** Al corregir: lo que cambia respecto del movimiento confirmado, con su valor anterior. */
    val changes: List<FieldChange> = emptyList(),
    /** El borrador o movimiento tiene evidencia para ver (foto, PDF, audio o texto). */
    val evidenceDraftId: String? = null,
    /** Por qué no se pudo confirmar (el formulario sigue abierto). */
    val problem: ConfirmProblem? = null,
    val busy: Boolean = false,
    val storageError: Boolean = false,
    val completed: Boolean = false,
    val dirty: Boolean = false,
) {
    /** El monto nuevo es el doble o más, o la mitad o menos, del anterior. */
    val largeAmount: Boolean get() = changes.any { it is FieldChange.Amount && it.large }
}

/**
 * El formulario completo del movimiento, sin Compose. Lo que se indica cuenta como dicho por el
 * usuario (confianza alta); lo que falte deja el borrador Incompleto. Al revisar o corregir se
 * precarga con lo propuesto y nada se aplica hasta que la persona confirma.
 */
class DraftFormPresenter(
    private val container: AppContainer,
    private val mode: FormMode = FormMode.New,
) {
    private val cards = DraftCards(container)
    private val mutableState = MutableStateFlow(DraftFormState(mode = mode))
    val state: StateFlow<DraftFormState> = mutableState.asStateFlow()

    private var directory = WalletDirectory(emptyList())
    private var capturedId: String? = null
    private var loaded = false
    private var baseline: List<Any?> = emptyList()

    private fun DraftFormState.fields(): List<Any?> =
        listOf(phrase, kind, amount, currency, walletId, method, category, occurredAtEpochMs, note, reference)

    fun requestExit(): FormExit =
        when {
            state.value.busy -> FormExit.BLOCKED
            state.value.dirty && !state.value.completed -> FormExit.ASK
            else -> FormExit.LEAVE
        }

    /** Al corregir: el movimiento confirmado y su categoría, contra los que se comparan los cambios. */
    private var before: Pair<ReadyMovement, CategoryRef?>? = null

    /** Id de la corrección: reintentar Confirmar no duplica nada. */
    private val correctionId by lazy { container.newId() }

    /** Las billeteras activas; la por defecto queda elegida. Al revisar o corregir, precarga los datos. */
    suspend fun load() {
        if (loaded) return
        directory = container.drafts.directory(container.ownerId)
        val wallets = directory.wallets.filter { it.isActive }.map { FormWallet(it.id, it.name, it.currency, it.type) }
        mutableState.update { it.copy(wallets = wallets) }
        when (mode) {
            FormMode.New -> mutableState.update { it.copy(walletId = it.walletId ?: directory.defaultWalletId) }
            is FormMode.Review -> loadReview(mode.draftId)
            is FormMode.Correct -> loadCorrection(mode)
        }
        baseline = state.value.fields()
        loaded = true
    }

    private suspend fun loadReview(draftId: String) {
        val draft = container.drafts.load(draftId) ?: return
        val proposal =
            when (draft) {
                is Draft.Ready -> draft.proposal
                is Draft.Incomplete -> draft.proposal
                else -> null
            }
        val note = proposal?.noteOrNull()
        val category = note?.let { container.categorization.suggest(container.ownerId, it) }?.category
        mutableState.update { s ->
            (proposal?.let { s.filledWith(it, category) } ?: s.copy(walletId = directory.defaultWalletId))
                .copy(
                    evidenceDraftId = draft.id,
                    phrase =
                        draft.header.evidence.text
                            .orEmpty(),
                )
        }
    }

    private suspend fun loadCorrection(mode: FormMode.Correct) {
        val source = mode.sourceDraftId
        if (source != null) {
            val pending = container.pendingCorrection(source) ?: return
            before = pending.target.movement to pending.category
            mutableState.update { it.filledWith(pending.proposal, pending.category).copy(evidenceDraftId = source) }
        } else {
            val confirmed = container.drafts.load(mode.draftId) as? Draft.Confirmed ?: return
            val category = container.categorization.current(confirmed.ledgerEventId)?.category
            before = confirmed.movement to category
            mutableState.update { it.filledWith(confirmed.movement.asProposal(), category).copy(evidenceDraftId = confirmed.id) }
        }
        refreshChanges()
    }

    private fun DraftFormState.filledWith(
        proposal: MovementProposal,
        category: CategoryRef?,
    ): DraftFormState {
        val (kind, amount, wallet, method) =
            when (proposal) {
                is MovementProposal.Expense -> Fill(FormKind.EXPENSE, proposal.amount, proposal.wallet, proposal.paymentMethod)
                is MovementProposal.Income -> Fill(FormKind.INCOME, proposal.amount, proposal.wallet, proposal.paymentMethod)
                else -> return this
            }
        val walletId =
            when (wallet) {
                is WalletRef.ById -> wallet.walletId
                is WalletRef.ByHint -> directory.byHint(wallet.hint)?.id
                null -> null
            }
        return copy(
            kind = kind,
            amount = amount?.value ?: this.amount,
            currency = amount?.currency ?: walletId?.let(directory::byId)?.currency ?: currency,
            walletId = walletId ?: this.walletId,
            method = method ?: this.method,
            category = category ?: this.category,
            occurredAtEpochMs = proposal.occurredAtOrNull() ?: occurredAtEpochMs,
            note = proposal.noteOrNull() ?: note,
            reference = proposal.referenceOrNull() ?: reference,
        )
    }

    fun onPhraseChange(text: String) = edit { it.copy(phrase = text) }

    /** Pasa la frase por el intérprete y precarga los campos; la persona puede cambiarlos. */
    suspend fun fillFromPhrase() {
        if (state.value.busy || state.value.completed) return
        val phrase = state.value.phrase.trim()
        if (phrase.isEmpty()) return
        directory = container.drafts.directory(container.ownerId)
        val proposal = container.interpreter.interpret(Evidence(EvidenceSource.TEXT, phrase), directory).firstOrNull() ?: return
        val category = proposal.noteOrNull()?.let { container.categorization.suggest(container.ownerId, it) }?.category
        edit { it.filledWith(proposal, category) }
    }

    fun onKindChange(kind: FormKind) = edit { it.copy(kind = kind) }

    fun onAmountChange(amount: String) = edit { it.copy(amount = amount) }

    fun onCurrencyChange(currency: Currency?) = edit { it.copy(currency = currency) }

    /** Elegir billetera lleva su moneda: una billetera opera en una sola moneda. */
    fun onWalletChange(walletId: String?) =
        edit { s ->
            s.copy(walletId = walletId, currency = s.wallets.firstOrNull { it.id == walletId }?.currency ?: s.currency)
        }

    fun onMethodChange(method: PaymentMethod?) = edit { it.copy(method = method) }

    fun onCategoryChange(category: CategoryRef?) = edit { it.copy(category = category) }

    fun onDateChange(epochMs: Long?) = edit { it.copy(occurredAtEpochMs = epochMs) }

    fun onNoteChange(note: String) = edit { it.copy(note = note) }

    fun onReferenceChange(reference: String) = edit { it.copy(reference = reference) }

    private fun edit(change: (DraftFormState) -> DraftFormState) {
        if (state.value.busy || state.value.completed) return
        mutableState.update {
            change(it).let { next ->
                next.copy(problem = null, storageError = false, dirty = next.fields() != baseline)
            }
        }
        refreshChanges()
    }

    /** Al corregir, recalcula qué campos cambian con lo que hay ahora en el formulario. */
    private fun refreshChanges() {
        val (movement, category) = before ?: return
        val s = state.value
        // Sin fecha en el formulario se conserva la del movimiento.
        val header = DraftHeader("form", container.ownerId, Evidence(EvidenceSource.MANUAL), movement.occurredAtEpochMs)
        val ready = DraftEvaluator.evaluate(header, proposalOf(s), directory) as? Draft.Ready
        val changes = ready?.let { MovementDiff.between(movement, category, it.movement, s.category) }.orEmpty()
        mutableState.update { it.copy(changes = changes) }
    }

    /**
     * Nuevo: guarda un borrador con la frase como evidencia y lo indicado como propuesta. La
     * categoría elegida viaja en la tarjeta del resultado para confirmarlo en el acto.
     */
    suspend fun save(): CaptureResult? =
        write {
            val s = state.value
            val evidence = Evidence(EvidenceSource.MANUAL, s.phrase.trim().ifEmpty { null })
            val id =
                capturedId ?: container
                    .captureManual(container.ownerId, evidence)
                    .single()
                    .id
                    .also { capturedId = it }
            val draft =
                when (val revised = container.reviseDraft(id, proposalOf(s))) {
                    is DraftResult.Ok -> revised.draft
                    is DraftResult.Rejected -> {
                        mutableState.update { it.copy(problem = problemOf(revised.error)) }
                        return@write null
                    }
                }
            val card = cards.of(listOf(draft)).single()
            val chosen = s.category?.let { CategoryView(it, reason = null) } ?: card.category
            CaptureResult(listOf(card.copy(category = chosen)))
        }

    /** Revisar: guarda lo del formulario sin confirmar (el saldo no cambia). */
    suspend fun keep(): Boolean =
        write {
            val draftId = (mode as? FormMode.Review)?.draftId ?: return@write null
            accepted(container.reviseDraft(draftId, proposalOf(state.value)))
        } ?: false

    /**
     * Revisar: guarda y confirma. Corregir: aplica todo lo del formulario de una vez. Devuelve true
     * si se aplicó; si el libro lo rechaza, el formulario sigue abierto con el motivo.
     */
    suspend fun confirm(): Boolean =
        write {
            val s = state.value
            val result =
                when (mode) {
                    FormMode.New -> return@write null
                    is FormMode.Review -> {
                        val revised = container.reviseDraft(mode.draftId, proposalOf(s))
                        if (revised is DraftResult.Rejected) revised else container.confirmDraft(mode.draftId, s.category)
                    }
                    is FormMode.Correct ->
                        container.correctMovement(
                            mode.draftId,
                            proposalOf(s),
                            s.category,
                            if (mode.sourceDraftId != null) CorrectionOrigin.EVIDENCE else CorrectionOrigin.MANUAL,
                            mode.sourceDraftId,
                            correctionId,
                        )
                }
            accepted(result)
        } ?: false

    /** Revisar: descarta el borrador. Cambio pendiente: todo queda como estaba y el comprobante, como evidencia. */
    suspend fun discard(): Boolean =
        write {
            when (mode) {
                is FormMode.Review -> accepted(container.discardDraft(mode.draftId))
                is FormMode.Correct -> mode.sourceDraftId?.let { accepted(container.dismissCorrection(it)) }
                FormMode.New -> null
            }
        } ?: false

    /** Cambio pendiente: el comprobante es otro gasto; sigue como borrador propio. */
    suspend fun distinct(): Boolean =
        write {
            val correct = mode as? FormMode.Correct ?: return@write null
            val source = correct.sourceDraftId ?: return@write null
            accepted(container.resolveSameMovement(source, correct.draftId, false))
        } ?: false

    private fun accepted(result: DraftResult): Boolean? =
        when (result) {
            is DraftResult.Ok -> true
            is DraftResult.Rejected -> {
                mutableState.update { it.copy(problem = problemOf(result.error)) }
                null
            }
        }

    /** El resultado se entrega una vez: la vista solo navega cuando no es null. */
    private suspend fun <T : Any> write(action: suspend () -> T?): T? {
        if (state.value.busy || state.value.completed) return null
        mutableState.update { it.copy(busy = true, storageError = false, problem = null) }
        return try {
            action().also { result -> if (result != null) mutableState.update { it.copy(completed = true) } }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.update { it.copy(storageError = true) }
            null
        } finally {
            mutableState.update { it.copy(busy = false) }
        }
    }

    private fun problemOf(error: DraftError): ConfirmProblem =
        when (error) {
            is DraftError.NotReady -> ConfirmProblem.NotReady
            is DraftError.Wallet ->
                (error.error as? WalletError.InsufficientFunds)?.let { ConfirmProblem.InsufficientFunds(it.shortfall) }
                    ?: ConfirmProblem.Rejected
            else -> ConfirmProblem.Rejected
        }

    private fun proposalOf(s: DraftFormState): MovementProposal {
        val amount = normalizedAmount(s.amount)?.let { ProposedAmount(it, s.currency) }
        val wallet = s.walletId?.let { WalletRef.ById(it) }
        val note = s.note.trim().ifEmpty { null }
        val reference = s.reference.trim().ifEmpty { null }
        val origins =
            buildMap {
                put(DraftField.KIND, FieldOrigin.STATED)
                if (amount != null) put(DraftField.AMOUNT, FieldOrigin.STATED)
                if (s.currency != null) put(DraftField.CURRENCY, FieldOrigin.STATED)
                if (wallet != null) put(DraftField.WALLET, FieldOrigin.STATED)
                if (s.occurredAtEpochMs != null) put(DraftField.OCCURRED_AT, FieldOrigin.STATED)
            }
        return when (s.kind) {
            FormKind.EXPENSE ->
                MovementProposal.Expense(
                    amount,
                    wallet,
                    s.occurredAtEpochMs,
                    note,
                    origins,
                    s.method,
                    reference = reference,
                )
            FormKind.INCOME -> MovementProposal.Income(amount, wallet, s.occurredAtEpochMs, note, origins, s.method, reference = reference)
        }
    }

    /** "12,50" o "12.50" -> "12.50". El formulario no usa separador de miles. */
    private fun normalizedAmount(text: String): String? =
        text
            .trim()
            .replace(" ", "")
            .replace(',', '.')
            .ifEmpty { null }

    private fun MovementProposal.noteOrNull(): String? =
        when (this) {
            is MovementProposal.Expense -> note
            is MovementProposal.Income -> note
            else -> null
        }

    private fun MovementProposal.referenceOrNull(): String? =
        when (this) {
            is MovementProposal.Expense -> reference
            is MovementProposal.Income -> reference
            else -> null
        }

    private fun MovementProposal.occurredAtOrNull(): Long? =
        when (this) {
            is MovementProposal.Expense -> occurredAtEpochMs
            is MovementProposal.Income -> occurredAtEpochMs
            else -> null
        }

    private data class Fill(
        val kind: FormKind,
        val amount: ProposedAmount?,
        val wallet: WalletRef?,
        val method: PaymentMethod?,
    )
}
