package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Tipos de movimiento del formulario. */
enum class FormKind { EXPENSE, INCOME }

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
)

/**
 * Escribir: el formulario completo del movimiento. Lo que se indica cuenta como dicho por el
 * usuario (confianza alta); lo que falte deja el borrador Incompleto. No usa Compose.
 */
class DraftFormPresenter(
    private val container: AppContainer,
) {
    private val cards = DraftCards(container)
    private val mutableState = MutableStateFlow(DraftFormState())
    val state: StateFlow<DraftFormState> = mutableState.asStateFlow()

    /** Las billeteras activas; la por defecto queda elegida. */
    suspend fun load() {
        val directory = container.drafts.directory(container.ownerId)
        val wallets = directory.wallets.filter { it.isActive }.map { FormWallet(it.id, it.name, it.currency, it.type) }
        mutableState.update { it.copy(wallets = wallets, walletId = it.walletId ?: directory.defaultWalletId) }
    }

    fun onPhraseChange(text: String) = mutableState.update { it.copy(phrase = text) }

    /** Pasa la frase por el intérprete y precarga los campos; la persona puede cambiarlos. */
    suspend fun fillFromPhrase() {
        val phrase = state.value.phrase.trim()
        if (phrase.isEmpty()) return
        val directory = container.drafts.directory(container.ownerId)
        val proposal = container.interpreter.interpret(Evidence(EvidenceSource.TEXT, phrase), directory).firstOrNull() ?: return
        val (kind, amount, wallet, method) =
            when (proposal) {
                is MovementProposal.Expense -> Fill(FormKind.EXPENSE, proposal.amount, proposal.wallet, proposal.paymentMethod)
                is MovementProposal.Income -> Fill(FormKind.INCOME, proposal.amount, proposal.wallet, proposal.paymentMethod)
                else -> return
            }
        val walletId =
            when (wallet) {
                is WalletRef.ById -> wallet.walletId
                is WalletRef.ByHint -> directory.byHint(wallet.hint)?.id
                null -> null
            }
        val note = proposal.noteOrNull()
        val category = note?.let { container.categorization.suggest(container.ownerId, it) }?.category
        mutableState.update { s ->
            s.copy(
                kind = kind,
                amount = amount?.value ?: s.amount,
                currency = amount?.currency ?: walletId?.let(directory::byId)?.currency ?: s.currency,
                walletId = walletId ?: s.walletId,
                method = method ?: s.method,
                category = category ?: s.category,
                occurredAtEpochMs = proposal.occurredAtOrNull() ?: s.occurredAtEpochMs,
                note = note ?: s.note,
                reference = proposal.referenceOrNull() ?: s.reference,
            )
        }
    }

    fun onKindChange(kind: FormKind) = mutableState.update { it.copy(kind = kind) }

    fun onAmountChange(amount: String) = mutableState.update { it.copy(amount = amount) }

    fun onCurrencyChange(currency: Currency?) = mutableState.update { it.copy(currency = currency) }

    /** Elegir billetera lleva su moneda: una billetera opera en una sola moneda. */
    fun onWalletChange(walletId: String?) =
        mutableState.update { s ->
            s.copy(walletId = walletId, currency = s.wallets.firstOrNull { it.id == walletId }?.currency ?: s.currency)
        }

    fun onMethodChange(method: PaymentMethod?) = mutableState.update { it.copy(method = method) }

    fun onCategoryChange(category: CategoryRef?) = mutableState.update { it.copy(category = category) }

    fun onDateChange(epochMs: Long?) = mutableState.update { it.copy(occurredAtEpochMs = epochMs) }

    fun onNoteChange(note: String) = mutableState.update { it.copy(note = note) }

    fun onReferenceChange(reference: String) = mutableState.update { it.copy(reference = reference) }

    /**
     * Guarda un borrador con la frase como evidencia y lo indicado como propuesta. La categoría
     * elegida viaja en la tarjeta del resultado para confirmarlo en el acto.
     */
    suspend fun save(): CaptureResult {
        val s = state.value
        val evidence = Evidence(EvidenceSource.MANUAL, s.phrase.trim().ifEmpty { null })
        val captured = container.captureManual(container.ownerId, evidence).single()
        val draft =
            when (val revised = container.reviseDraft(captured.id, proposalOf(s))) {
                is DraftResult.Ok -> revised.draft
                is DraftResult.Rejected -> captured
            }
        val card = cards.of(listOf(draft)).single()
        val chosen = s.category?.let { CategoryView(it, reason = null) } ?: card.category
        return CaptureResult(listOf(card.copy(category = chosen)))
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
