package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.port.output.BundleText

/**
 * Por qué una propuesta no se puede dar por buena. Son códigos sin texto: la presentación los
 * traduce, el dominio no sabe de idiomas.
 */
sealed interface ValidationIssue {
    /** El monto no aparece en ninguna evidencia del paquete: nadie lo dijo ni lo trae un comprobante. */
    data class AmountNotInText(
        val amount: String,
    ) : ValidationIssue

    /** La billetera propuesta no está entre las de la persona. */
    data class UnknownWallet(
        val wallet: String,
    ) : ValidationIssue

    /** La categoría propuesta no está entre las de la persona. */
    data class UnknownCategory(
        val category: String,
    ) : ValidationIssue

    /** Un hecho que el comprobante sí trae se cambió: nunca se acepta en silencio. */
    data class ChangedEvidenceFact(
        val field: DraftField,
        val fromEvidence: String,
        val proposed: String,
    ) : ValidationIssue

    /**
     * La referencia del comprobante se cambió. Va aparte porque el número de transacción no es un
     * [DraftField]: no se propone ni se corrige campo a campo, viene del comprobante y punto.
     */
    data class ChangedReference(
        val fromEvidence: String,
        val proposed: String,
    ) : ValidationIssue
}

/**
 * Verifica que una propuesta esté respaldada por lo que hay, antes de que llegue a un borrador.
 *
 * Es puro y determinista, y **trata igual a quien proponga**: las reglas o un modelo de lenguaje.
 * No corrige nada ni elige por la persona; solo dice qué no cuadra, y con eso el borrador queda
 * para revisar en lugar de registrarse como si fuera un hecho.
 */
object ProposalValidator {
    fun validate(
        proposal: MovementProposal,
        texts: List<BundleText>,
        wallets: WalletDirectory,
        categories: Set<String> = emptySet(),
        proposedCategory: String? = null,
        receiptFacts: MovementProposal? = null,
    ): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val enLosTextos = texts.flatMapTo(mutableSetOf()) { numbersIn(it.text) }

        amountOf(proposal)?.let { monto ->
            if (canonicalNumber(monto.value) !in enLosTextos) issues += ValidationIssue.AmountNotInText(monto.value)
        }

        walletsOf(proposal).forEach { ref ->
            if (!exists(ref, wallets)) issues += ValidationIssue.UnknownWallet(nameOf(ref))
        }

        if (proposedCategory != null && categories.isNotEmpty() && proposedCategory !in categories) {
            issues += ValidationIssue.UnknownCategory(proposedCategory)
        }

        if (receiptFacts != null) issues += protectedFacts(receiptFacts, proposal)

        return issues
    }

    /**
     * Monto, moneda, hora y referencia que trae un comprobante no cambian en silencio. La nota, la
     * categoría y la billetera que el comprobante no identifica sí se pueden completar o mejorar:
     * son lo que el contexto aporta.
     */
    private fun protectedFacts(
        evidence: MovementProposal,
        proposal: MovementProposal,
    ): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val delComprobante = amountOf(evidence)
        val propuesto = amountOf(proposal)

        if (delComprobante != null && propuesto != null) {
            if (canonicalNumber(delComprobante.value) != canonicalNumber(propuesto.value)) {
                issues += ValidationIssue.ChangedEvidenceFact(DraftField.AMOUNT, delComprobante.value, propuesto.value)
            }
            val monedaLeida = delComprobante.currency
            val monedaPropuesta = propuesto.currency
            if (monedaLeida != null && monedaPropuesta != null && monedaLeida != monedaPropuesta) {
                issues += ValidationIssue.ChangedEvidenceFact(DraftField.CURRENCY, monedaLeida.code, monedaPropuesta.code)
            }
        }

        val referenciaLeida = referenceOfProposal(evidence)
        val referenciaPropuesta = referenceOfProposal(proposal)
        if (referenciaLeida != null && referenciaPropuesta != null && referenciaLeida != referenciaPropuesta) {
            issues += ValidationIssue.ChangedReference(referenciaLeida, referenciaPropuesta)
        }

        val horaLeida = occurredAtOf(evidence)
        val horaPropuesta = occurredAtOf(proposal)
        if (horaLeida != null && horaPropuesta != null && horaLeida != horaPropuesta) {
            issues += ValidationIssue.ChangedEvidenceFact(DraftField.OCCURRED_AT, horaLeida.toString(), horaPropuesta.toString())
        }

        return issues
    }

    private fun exists(
        ref: WalletRef,
        wallets: WalletDirectory,
    ): Boolean =
        when (ref) {
            is WalletRef.ById -> wallets.byId(ref.walletId) != null
            is WalletRef.ByHint -> wallets.byHint(ref.hint) != null
        }

    private fun nameOf(ref: WalletRef): String =
        when (ref) {
            is WalletRef.ById -> ref.walletId
            is WalletRef.ByHint -> ref.hint
        }

    private fun walletsOf(proposal: MovementProposal): List<WalletRef> =
        when (proposal) {
            is MovementProposal.Expense -> listOfNotNull(proposal.wallet)
            is MovementProposal.Income -> listOfNotNull(proposal.wallet)
            is MovementProposal.Transfer -> listOfNotNull(proposal.from, proposal.to)
            is MovementProposal.AdjustTo -> listOfNotNull(proposal.wallet)
            else -> emptyList()
        }

    private fun amountOf(proposal: MovementProposal): ProposedAmount? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.amount
            is MovementProposal.Income -> proposal.amount
            is MovementProposal.Transfer -> proposal.out
            is MovementProposal.AdjustTo -> proposal.target
            else -> null
        }

    private fun referenceOfProposal(proposal: MovementProposal): String? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.reference
            is MovementProposal.Income -> proposal.reference
            is MovementProposal.Transfer -> proposal.reference
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

    /** Los números que aparecen en un texto, en forma comparable ("Bs25.00" -> "25"). */
    private fun numbersIn(text: String): Set<String> = NUMBER.findAll(text).mapTo(mutableSetOf()) { canonicalNumber(it.value) }

    /** "25.00" y "25" son el mismo monto; "25,50" y "25.50" también. */
    private fun canonicalNumber(value: String): String {
        val limpio = value.replace(",", ".").trim()
        return if ("." in limpio) limpio.trimEnd('0').trimEnd('.') else limpio
    }

    private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")
}
