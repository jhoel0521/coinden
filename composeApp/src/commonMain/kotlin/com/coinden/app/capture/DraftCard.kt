package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.SuggestionReason
import com.coinden.core.domain.model.draft.CaptureCoach
import com.coinden.core.domain.model.draft.CaptureTip
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.SameMovementReason
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.draft.WalletSuggestion
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.Wallet

/** Tipo de movimiento visto por la pantalla (la pantalla lo traduce). */
enum class MovementKindView { EXPENSE, INCOME, TRANSFER, ADJUSTMENT, NOT_A_MOVEMENT }

/** Estado del borrador visto por la pantalla. */
enum class DraftStatusView { CAPTURED, INCOMPLETE, READY, UNSUPPORTED }

/** Categoría sugerida y por qué se sugirió. */
data class CategoryView(
    val ref: CategoryRef,
    /** null: la eligió la persona en el formulario. */
    val reason: SuggestionReason?,
    /** Nombre guardado si es categoría propia; null si es base o ya no está disponible. */
    val customName: String? = null,
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
    /** Medio de pago dicho ("qr", "efectivo"); null si no se dijo. */
    val paymentMethod: PaymentMethod? = null,
    /** Por qué se sugirió la billetera; null si la dijo el usuario. */
    val walletSuggestion: WalletSuggestion? = null,
    val evidenceSource: EvidenceSource = EvidenceSource.TEXT,
    /** Archivo cifrado de la evidencia (foto, PDF o audio), si lo hay. */
    val attachmentRef: String? = null,
    /** Número de transacción del comprobante, si lo hay. */
    val reference: String? = null,
    /** Texto leído o transcrito en el teléfono de la foto, el PDF o el audio (null si no se leyó). */
    val perceivedText: String? = null,
    /** Cuántas evidencias más tiene por haberse unido con otras capturas. */
    val extraEvidence: Int = 0,
    /** "¿Es el mismo gasto que...?", si puede ser el mismo movimiento que otra captura. */
    val question: SameMovementView? = null,
    /** La nota del movimiento (lo que la persona dijo o el comercio), para la fila compacta. */
    val note: String? = null,
    /** Qué conviene decir la próxima vez (vacío si no faltó nada). */
    val tips: Set<CaptureTip> = emptySet(),
    /** Frase de ejemplo con las palabras de la persona («power 10 bnb»), si el consejo es decir la billetera. */
    val tipExample: String? = null,
)

/** La otra captura de la pregunta, en datos: la pantalla arma el texto. */
data class SameMovementView(
    val reason: SameMovementReason,
    val otherDraftId: String,
    val otherNote: String?,
    val otherAmount: Money?,
    val otherWalletName: String?,
    val otherCapturedAtEpochMs: Long,
    val otherConfirmed: Boolean,
)

/** Arma las tarjetas de los borradores con la sugerencia de categoría vigente. */
class DraftCards(
    private val container: AppContainer,
) {
    suspend fun of(drafts: List<Draft>): List<DraftCard> {
        val directory = container.drafts.directory(container.ownerId)
        val customNames =
            container.storage.categorization
                .customCategoriesOf(container.ownerId)
                .associate { it.id to it.name }
        val questions =
            container.findSameMovementQuestions(container.ownerId, container.utcOffsetMinutes).associateBy { it.draftId }
        return drafts.map { draft ->
            val card = card(draft, directory, customNames)
            val question = questions[draft.id] ?: return@map card
            val other = container.drafts.load(question.otherDraftId) ?: return@map card
            val otherCard = card(other, directory, customNames)
            card.copy(
                question =
                    SameMovementView(
                        reason = question.reason,
                        otherDraftId = other.id,
                        otherNote = noteOf(other),
                        otherAmount = otherCard.amount,
                        otherWalletName = otherCard.walletName,
                        otherCapturedAtEpochMs = other.header.capturedAtEpochMs,
                        otherConfirmed = other is Draft.Confirmed,
                    ),
            )
        }
    }

    private fun noteOf(draft: Draft): String? =
        when (draft) {
            is Draft.Ready -> draft.movement.note
            is Draft.Confirmed -> draft.movement.note
            is Draft.Incomplete -> (draft.proposal as? MovementProposal.Expense)?.note ?: (draft.proposal as? MovementProposal.Income)?.note
            else -> null
        }

    private suspend fun card(
        draft: Draft,
        directory: WalletDirectory,
        customNames: Map<String, String>,
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
        val evidence = draft.header.evidence
        val tips = CaptureCoach.tipsFor(draft)
        return DraftCard(
            draftId = draft.id,
            evidenceText = evidence.text,
            status = statusOf(draft),
            kind = proposal?.let(::kindOf) ?: (draft as? Draft.Unsupported)?.let { MovementKindView.NOT_A_MOVEMENT },
            amount = amountOf(draft, proposal, wallet),
            walletName = wallet?.name,
            walletSuggested = proposal?.origins?.get(DraftField.WALLET) == FieldOrigin.SUGGESTED,
            confidence = draft.confidence,
            paymentMethod = proposal?.let(::paymentMethodOf),
            category =
                suggestion?.let {
                    CategoryView(it.category, it.reason, (it.category as? CategoryRef.Custom)?.let { c -> customNames[c.id] })
                },
            walletSuggestion = proposal?.let(::walletSuggestionOf),
            evidenceSource = evidence.source,
            attachmentRef = evidence.attachmentRef,
            reference = referenceOf(draft, proposal),
            extraEvidence = draft.header.extraEvidence.size,
            perceivedText = draft.header.perceivedText,
            note = noteOf(draft),
            tips = tips,
            tipExample = if (CaptureTip.SAY_WALLET in tips) exampleOf(draft, proposal, directory) else null,
        )
    }

    /** «nota monto palabra-de-billetera», con la billetera sugerida o la por defecto; null si no se puede armar. */
    private suspend fun exampleOf(
        draft: Draft,
        proposal: MovementProposal?,
        directory: WalletDirectory,
    ): String? {
        val amount =
            when (proposal) {
                is MovementProposal.Expense -> proposal.amount
                is MovementProposal.Income -> proposal.amount
                else -> null
            } ?: return null
        val walletId = CaptureCoach.exampleWalletId(draft, directory) ?: return null
        val word = CaptureCoach.walletWord(walletId, directory, container.interpreter.walletNameHints(directory.wallets)) ?: return null
        val note = (proposal as? MovementProposal.Expense)?.note ?: (proposal as? MovementProposal.Income)?.note
        return listOfNotNull(note, amount.value.removeSuffix(".00"), word).joinToString(" ")
    }

    private fun paymentMethodOf(proposal: MovementProposal): PaymentMethod? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.paymentMethod
            is MovementProposal.Income -> proposal.paymentMethod
            is MovementProposal.Transfer -> proposal.paymentMethod
            else -> null
        }

    private fun referenceOf(
        draft: Draft,
        proposal: MovementProposal?,
    ): String? {
        val movement =
            when (draft) {
                is Draft.Ready -> draft.movement
                is Draft.Confirmed -> draft.movement
                else -> null
            }
        return when (movement) {
            is ReadyMovement.Expense -> movement.reference
            is ReadyMovement.Income -> movement.reference
            is ReadyMovement.Transfer -> movement.reference
            else ->
                when (proposal) {
                    is MovementProposal.Expense -> proposal.reference
                    is MovementProposal.Income -> proposal.reference
                    is MovementProposal.Transfer -> proposal.reference
                    else -> null
                }
        }
    }

    private fun walletSuggestionOf(proposal: MovementProposal): WalletSuggestion? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.walletSuggestion
            is MovementProposal.Income -> proposal.walletSuggestion
            else -> null
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
        val ready = (draft as? Draft.Ready)?.movement ?: (draft as? Draft.Confirmed)?.movement
        if (ready != null) {
            return when (val m = ready) {
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
        val ready = (draft as? Draft.Ready)?.movement ?: (draft as? Draft.Confirmed)?.movement
        if (ready != null) {
            return when (val m = ready) {
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
