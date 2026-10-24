package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.bundle.ReviewReason
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposalMerge
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.port.output.BundleInterpreterPort
import com.coinden.core.domain.port.output.BundlePayment
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.port.output.DraftInterpreterPort

/**
 * Respaldo determinista del paquete, sin listas de palabras ni modelo: es lo que corre cuando no
 * hay modelo de lenguaje en el teléfono, y la vara con que se verifica al modelo cuando lo hay.
 *
 * Cada comprobante es un pago con sus hechos. Lo que la persona dijo o escribió (el contexto) se
 * empareja **solo por lo que el dominio sí puede comparar**: monto, moneda y medio de pago.
 *
 * - Una frase con monto que coincide con un comprobante lo explica (la nota dicha gana).
 * - Una frase con monto que no coincide con ninguno es otro pago, o una contradicción si hay un
 *   solo comprobante: en ambos casos va a revisar.
 * - Una frase sin monto es la nota del pago que quede sin explicar; si quedan varios, se pone en
 *   todos y van a revisar.
 *
 * Entender "un ambulante" o "somó" no es tarea de este respaldo: es del modelo local. Lo que aquí
 * no queda claro queda para revisar, nunca se decide en silencio.
 */
class BundleInterpreter(
    private val reader: DraftInterpreterPort,
) : BundleInterpreterPort {
    override suspend fun interpret(
        texts: List<BundleText>,
        wallets: WalletDirectory,
    ): List<BundlePayment> {
        val receipts = texts.filter { it.role == EvidenceRole.RECEIPT }.flatMap { read(it, wallets, keyed = true) }
        val contexts = texts.filter { it.role == EvidenceRole.CONTEXT }
        if (receipts.isEmpty()) return contexts.flatMap { read(it, wallets, keyed = false) }
        if (contexts.isEmpty()) return receipts

        val payments = receipts.toMutableList()
        val explained = mutableSetOf<String>()
        val extras = mutableListOf<BundlePayment>()
        val phrases = mutableListOf<BundleText>()

        for (context in contexts) {
            val said = read(context, wallets, keyed = false)
            val withAmount = said.filter { amountOf(it.proposal) != null }
            if (withAmount.isEmpty()) {
                if (context.text.isNotBlank()) phrases += context
                continue
            }
            for (statement in withAmount) {
                val same = payments.filter { sameFacts(it.proposal, statement.proposal) }
                val free = same.filter { it.paymentKey !in explained }
                when {
                    free.isNotEmpty() -> {
                        val target = free.sortedBy { occurredAtOf(it.proposal) ?: Long.MAX_VALUE }.first()
                        val reasons = if (free.size > 1) setOf(ReviewReason.AssignedByOrder) else emptySet()
                        explained += target.paymentKey
                        payments[payments.indexOf(target)] = target.explainedBy(statement, reasons)
                    }
                    // El mismo pago dicho otra vez: no es un pago nuevo.
                    same.isNotEmpty() -> {
                        val target = same.first()
                        payments[payments.indexOf(target)] = target.copy(evidenceIds = target.evidenceIds + statement.evidenceIds)
                    }
                    receipts.size == 1 && extras.isEmpty() -> {
                        val target = payments.single()
                        val stated = amountOf(statement.proposal)!!
                        val read = amountOf(target.proposal)
                        val reason = ReviewReason.Contradiction(DraftField.AMOUNT, label(stated), read?.let(::label).orEmpty())
                        payments[0] =
                            target.copy(
                                evidenceIds = target.evidenceIds + statement.evidenceIds,
                                reviewReasons =
                                    target.reviewReasons + reason,
                            )
                    }
                    else -> extras += statement.copy(reviewReasons = statement.reviewReasons + ReviewReason.PaymentWithoutReceipt)
                }
            }
        }

        if (phrases.isNotEmpty()) applyPhrases(payments, explained, phrases)
        return payments + extras
    }

    /** Las frases sin monto son la nota del pago que quede sin explicar; con varios, de cualquiera. */
    private fun applyPhrases(
        payments: MutableList<BundlePayment>,
        explained: Set<String>,
        phrases: List<BundleText>,
    ) {
        val note = phrases.joinToString(". ") { it.text.trim().trimEnd('.') }
        val ids = phrases.map { it.evidenceId }.toSet()
        val free = payments.indices.filter { payments[it].paymentKey !in explained && payments[it].isReceiptPayment() }
        when {
            free.size == 1 -> payments[free.single()] = payments[free.single()].withNote(note, ids, emptySet())
            free.size > 1 -> free.forEach { payments[it] = payments[it].withNote(note, ids, setOf(ReviewReason.ContextForSeveral)) }
            else -> {
                // Todo está explicado: la frase completa el último pago si todavía no tiene nota.
                val last = payments.lastIndex
                val target = payments[last]
                payments[last] =
                    if (noteOf(target.proposal) ==
                        null
                    ) {
                        target.withNote(note, ids, emptySet())
                    } else {
                        target.copy(evidenceIds = target.evidenceIds + ids)
                    }
            }
        }
    }

    private suspend fun read(
        text: BundleText,
        wallets: WalletDirectory,
        keyed: Boolean,
    ): List<BundlePayment> {
        val proposals =
            runCatching { reader.interpret(Evidence(text.source, text.text), wallets) }
                .getOrDefault(emptyList())
                .filterNot { it is MovementProposal.Unknown }
        return proposals.mapIndexed { index, proposal ->
            val suffix = if (index == 0) "" else "#$index"
            val key = if (keyed) text.evidenceId + suffix else "context:${text.evidenceId}$suffix"
            BundlePayment(key, proposal, setOf(text.evidenceId))
        }
    }

    private fun BundlePayment.isReceiptPayment(): Boolean = !paymentKey.startsWith("context:")

    private fun BundlePayment.explainedBy(
        statement: BundlePayment,
        reasons: Set<ReviewReason>,
    ): BundlePayment =
        copy(
            proposal = ProposalMerge.combine(proposal, statement.proposal) ?: proposal,
            evidenceIds = evidenceIds + statement.evidenceIds,
            reviewReasons = reviewReasons + reasons,
        )

    private fun BundlePayment.withNote(
        note: String,
        ids: Set<String>,
        reasons: Set<ReviewReason>,
    ): BundlePayment = copy(proposal = proposal.withNote(note), evidenceIds = evidenceIds + ids, reviewReasons = reviewReasons + reasons)

    private fun MovementProposal.withNote(note: String): MovementProposal =
        when (this) {
            is MovementProposal.Expense -> copy(note = note)
            is MovementProposal.Income -> copy(note = note)
            is MovementProposal.Transfer -> copy(note = note)
            is MovementProposal.AdjustTo -> copy(note = note)
            else -> this
        }

    private fun noteOf(proposal: MovementProposal): String? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.note
            is MovementProposal.Income -> proposal.note
            is MovementProposal.Transfer -> proposal.note
            is MovementProposal.AdjustTo -> proposal.note
            else -> null
        }

    /** Mismo monto y moneda; y si los dos dicen el medio de pago, el mismo: otro medio es otro pago. */
    private fun sameFacts(
        a: MovementProposal,
        b: MovementProposal,
    ): Boolean {
        val x = amountOf(a) ?: return false
        val y = amountOf(b) ?: return false
        if (canonical(x.value) != canonical(y.value)) return false
        if (x.currency != null && y.currency != null && x.currency != y.currency) return false
        val methodA = methodOf(a)
        val methodB = methodOf(b)
        return methodA == null || methodB == null || methodA == methodB
    }

    private fun canonical(value: String): String {
        val clean = value.replace(",", ".").trim()
        return if ("." in clean) clean.trimEnd('0').trimEnd('.') else clean
    }

    private fun label(amount: ProposedAmount): String = listOfNotNull(amount.value, amount.currency?.code).joinToString(" ")

    private fun amountOf(proposal: MovementProposal): ProposedAmount? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.amount
            is MovementProposal.Income -> proposal.amount
            is MovementProposal.Transfer -> proposal.out
            is MovementProposal.AdjustTo -> proposal.target
            else -> null
        }

    private fun methodOf(proposal: MovementProposal): PaymentMethod? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.paymentMethod
            is MovementProposal.Income -> proposal.paymentMethod
            is MovementProposal.Transfer -> proposal.paymentMethod
            else -> null
        }

    private fun occurredAtOf(proposal: MovementProposal): Long? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.occurredAtEpochMs
            is MovementProposal.Income -> proposal.occurredAtEpochMs
            is MovementProposal.Transfer -> proposal.occurredAtEpochMs
            is MovementProposal.AdjustTo -> proposal.occurredAtEpochMs
            else -> null
        }
}
