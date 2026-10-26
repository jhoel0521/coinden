package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.bundle.ReviewReason
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.port.output.BundleInterpreterPort
import com.coinden.core.domain.port.output.BundlePayment
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.port.output.IntentInterpreterPort

/**
 * Interpreta un paquete con las dos cabezas: el modelo entiende, las reglas verifican.
 *
 * Es donde se cumple el principio 4 del Manifiesto. Las reglas corren **siempre**, porque cuestan
 * microsegundos y son la vara contra la que se mide lo que propone el modelo:
 *
 * - coinciden en los hechos -> vale la lectura del modelo, más rica en nota y categoría;
 * - no coinciden -> se conserva el hecho que leyeron las reglas del comprobante y el borrador va
 *   a revisar **con las dos versiones**, para que decida la persona;
 * - el modelo no contesta, o su propuesta no pasa la verificación -> queda el respaldo;
 * - un pago que solo vio uno de los dos no se pierde: queda, para revisar.
 *
 * Nada de esto toca saldos: son borradores hasta que la persona confirma.
 */
class BundleInterpretation(
    private val model: IntentInterpreterPort?,
    private val rules: BundleInterpreterPort,
    /** Las categorías de la persona: el modelo elige de ahí y no puede inventar otra. */
    private val categoriesOf: suspend () -> Set<String> = { emptySet() },
) : BundleInterpreterPort {
    /** Cumple el mismo contrato que las reglas, así que se enchufa donde ya iban ellas. */
    override suspend fun interpret(
        texts: List<BundleText>,
        wallets: WalletDirectory,
    ): List<BundlePayment> = interpret(texts, wallets, categoriesOf())

    suspend fun interpret(
        texts: List<BundleText>,
        wallets: WalletDirectory,
        categories: Set<String>,
    ): List<BundlePayment> {
        val deLasReglas = rules.interpret(texts, wallets)
        val delModelo = model?.interpret(texts, wallets, categories) ?: return deLasReglas

        val porClave = deLasReglas.associateBy { it.paymentKey }
        val vistos = mutableSetOf<String>()
        val salida = mutableListOf<BundlePayment>()

        delModelo.forEach { propuesto ->
            val problemas = ProposalValidator.validate(propuesto.proposal, texts, wallets, categories)
            val deLaRegla = porClave[propuesto.paymentKey]
            vistos += propuesto.paymentKey

            salida +=
                when {
                    // Lo que no se puede respaldar no se usa como hecho: queda lo que leyeron las reglas.
                    problemas.isNotEmpty() ->
                        (deLaRegla ?: propuesto).withReasons(setOf(ReviewReason.UnverifiedProposal))
                    // Un pago que las reglas no vieron se conserva, pero nadie lo confirmó todavía.
                    deLaRegla == null -> propuesto.withReasons(setOf(ReviewReason.PaymentWithoutReceipt))
                    else -> reconcile(propuesto, deLaRegla)
                }
        }

        // Lo que vieron las reglas y el modelo no mencionó tampoco se pierde.
        salida += deLasReglas.filterNot { it.paymentKey in vistos }
        return salida
    }

    /** Compara los hechos de las dos lecturas. Si difieren, manda el comprobante y se avisa. */
    private fun reconcile(
        delModelo: BundlePayment,
        deLasReglas: BundlePayment,
    ): BundlePayment {
        val desacuerdos = disagreements(delModelo.proposal, deLasReglas.proposal)
        return if (desacuerdos.isEmpty()) {
            delModelo.copy(reviewReasons = delModelo.reviewReasons + deLasReglas.reviewReasons)
        } else {
            deLasReglas.withReasons(desacuerdos)
        }
    }

    private fun disagreements(
        model: MovementProposal,
        rules: MovementProposal,
    ): Set<ReviewReason> {
        val reasons = mutableSetOf<ReviewReason>()
        val delModelo = amountOf(model)
        val deLasReglas = amountOf(rules)

        if (delModelo?.value != null && deLasReglas?.value != null && delModelo.value != deLasReglas.value) {
            reasons += ReviewReason.ModelDisagrees(DraftField.AMOUNT, delModelo.value, deLasReglas.value)
        }
        val monedaModelo = delModelo?.currency
        val monedaReglas = deLasReglas?.currency
        if (monedaModelo != null && monedaReglas != null && monedaModelo != monedaReglas) {
            reasons += ReviewReason.ModelDisagrees(DraftField.CURRENCY, monedaModelo.code, monedaReglas.code)
        }
        return reasons
    }

    private fun BundlePayment.withReasons(reasons: Set<ReviewReason>) = copy(reviewReasons = reviewReasons + reasons)

    private fun amountOf(proposal: MovementProposal): ProposedAmount? =
        when (proposal) {
            is MovementProposal.Expense -> proposal.amount
            is MovementProposal.Income -> proposal.amount
            is MovementProposal.Transfer -> proposal.out
            is MovementProposal.AdjustTo -> proposal.target
            else -> null
        }
}
