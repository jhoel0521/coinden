package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposalMerge
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.port.output.BundleInterpreterPort
import com.coinden.core.domain.port.output.BundlePayment
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.port.output.DraftInterpreterPort

/**
 * Intérprete del paquete con reglas, en una sola pasada.
 *
 * Cada comprobante es un pago con los hechos que leyó [texts] (plantillas o lector genérico). El
 * contexto (audios y notas) explica esos pagos o, si no hay comprobantes, es la captura misma.
 */
class BundleInterpreter(
    private val texts: DraftInterpreterPort,
) : BundleInterpreterPort {
    override suspend fun interpret(
        texts: List<BundleText>,
        wallets: WalletDirectory,
    ): List<BundlePayment> {
        val receipts = texts.filter { it.role == EvidenceRole.RECEIPT }.flatMap { read(it, wallets, keyed = true) }
        val context = texts.filter { it.role == EvidenceRole.CONTEXT }.flatMap { read(it, wallets, keyed = false) }
        if (receipts.isEmpty()) return context
        if (context.isEmpty()) return receipts
        // Un solo pago: lo dicho lo explica (la nota dicha gana; los hechos del comprobante, también).
        if (receipts.size == 1) {
            val payment = receipts.single()
            val merged = context.fold(payment.proposal) { acc, said -> ProposalMerge.combine(acc, said.proposal) ?: acc }
            return listOf(payment.copy(proposal = merged, evidenceIds = payment.evidenceIds + context.flatMap { it.evidenceIds }))
        }
        return receipts + context
    }

    private suspend fun read(
        text: BundleText,
        wallets: WalletDirectory,
        keyed: Boolean,
    ): List<BundlePayment> {
        val proposals =
            runCatching { texts.interpret(Evidence(text.source, text.text), wallets) }
                .getOrDefault(emptyList())
                .filterNot { it is MovementProposal.Unknown }
        return proposals.mapIndexed { index, proposal ->
            val suffix = if (index == 0) "" else "#$index"
            val key = if (keyed) text.evidenceId + suffix else "context:${text.evidenceId}$suffix"
            BundlePayment(key, proposal, setOf(text.evidenceId))
        }
    }
}
