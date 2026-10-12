package com.coinden.core.domain.model.draft

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.Wallet

/** Datos comunes a todo borrador: quién, qué evidencia y cuándo se capturó. */
data class DraftHeader(
    val id: String,
    val ownerId: String,
    val evidence: Evidence,
    val capturedAtEpochMs: Long,
)

/**
 * Movimiento completo y sin contradicciones, listo para el libro. No tiene nulos: solo se
 * construye desde [DraftEvaluator], así que el compilador garantiza que se puede confirmar.
 */
sealed interface ReadyMovement {
    val occurredAtEpochMs: Long
    val note: String?

    data class Expense(
        val amount: Money,
        val walletId: String,
        override val occurredAtEpochMs: Long,
        override val note: String?,
    ) : ReadyMovement

    data class Income(
        val amount: Money,
        val walletId: String,
        override val occurredAtEpochMs: Long,
        override val note: String?,
    ) : ReadyMovement

    data class Transfer(
        val out: Money,
        val fromWalletId: String,
        val into: Money,
        val toWalletId: String,
        override val occurredAtEpochMs: Long,
        override val note: String?,
    ) : ReadyMovement {
        /** Tasa exacta de la operación (identidad si es la misma moneda). */
        val rate: ExchangeRate
            get() = if (out.currency == into.currency) ExchangeRate.identity(out.currency) else ExchangeRate.of(out, into)
    }

    /** "Mi saldo real es [target]": la diferencia se calcula al confirmar. */
    data class AdjustTo(
        val target: Money,
        val walletId: String,
        override val occurredAtEpochMs: Long,
        override val note: String?,
    ) : ReadyMovement
}

/**
 * Borrador como máquina de estados. Cada estado guarda solo sus datos:
 *
 * ```
 * Captured -> Incomplete <-> Ready -> Confirmed
 *     \            \           \
 *      +------------+-----------+--> Discarded        Unsupported -> Ready | Discarded
 * ```
 * Confirmed y Discarded son terminales.
 */
sealed interface Draft {
    val header: DraftHeader

    val id: String get() = header.id
    val isTerminal: Boolean get() = this is Confirmed || this is Discarded

    /**
     * Confianza del borrador: la de su campo propuesto más débil. Capturado y No soportado son
     * BAJA (nadie propuso un movimiento); Confirmado es ALTA (ya es la verdad del libro).
     */
    val confidence: Confidence
        get() =
            when (this) {
                is Captured, is Unsupported, is Discarded -> Confidence.LOW
                is Incomplete -> proposal.confidence
                is Ready -> proposal.confidence
                is Confirmed -> Confidence.HIGH
            }

    /** Recién capturado: hay evidencia, todavía sin propuesta. */
    data class Captured(
        override val header: DraftHeader,
    ) : Draft

    data class Incomplete(
        override val header: DraftHeader,
        val proposal: MovementProposal,
        val missing: Set<DraftField>,
        val issues: Set<DraftIssue>,
    ) : Draft

    data class Ready(
        override val header: DraftHeader,
        val proposal: MovementProposal,
        val movement: ReadyMovement,
    ) : Draft

    /** La captura no es un movimiento (recurrente, préstamo, meta...): se guarda, no se pierde. */
    data class Unsupported(
        override val header: DraftHeader,
        val intent: NonMovementIntent,
        val note: String?,
    ) : Draft

    data class Confirmed(
        override val header: DraftHeader,
        val movement: ReadyMovement,
        val ledgerEventId: String,
    ) : Draft

    data class Discarded(
        override val header: DraftHeader,
    ) : Draft
}

/**
 * Decide, de forma pura y determinista, en qué estado queda un borrador con una propuesta
 * y el directorio actual de billeteras: Listo, Incompleto o No soportado.
 */
object DraftEvaluator {
    fun evaluate(
        header: DraftHeader,
        proposal: MovementProposal,
        directory: WalletDirectory,
    ): Draft {
        val check = Check()
        val movement =
            when (proposal) {
                is MovementProposal.NotAMovement -> return Draft.Unsupported(header, proposal.intent, proposal.note)
                is MovementProposal.Unknown -> {
                    check.missing += DraftField.KIND
                    null
                }
                is MovementProposal.Expense -> {
                    val wallet = check.wallet(proposal.wallet, directory, DraftField.WALLET)
                    val amount = check.positiveAmount(proposal.amount, wallet, DraftField.AMOUNT)
                    if (wallet != null && amount != null) {
                        ReadyMovement.Expense(amount, wallet.id, proposal.occurredAtEpochMs ?: header.capturedAtEpochMs, proposal.note)
                    } else {
                        null
                    }
                }
                is MovementProposal.Income -> {
                    val wallet = check.wallet(proposal.wallet, directory, DraftField.WALLET)
                    val amount = check.positiveAmount(proposal.amount, wallet, DraftField.AMOUNT)
                    if (wallet != null && amount != null) {
                        ReadyMovement.Income(amount, wallet.id, proposal.occurredAtEpochMs ?: header.capturedAtEpochMs, proposal.note)
                    } else {
                        null
                    }
                }
                is MovementProposal.AdjustTo -> {
                    val wallet = check.wallet(proposal.wallet, directory, DraftField.WALLET)
                    val target = check.amount(proposal.target, wallet, DraftField.AMOUNT)
                    if (wallet != null && target != null) {
                        ReadyMovement.AdjustTo(target, wallet.id, proposal.occurredAtEpochMs ?: header.capturedAtEpochMs, proposal.note)
                    } else {
                        null
                    }
                }
                is MovementProposal.Transfer -> transfer(header, proposal, directory, check)
            }

        return if (movement != null && check.isClean) {
            Draft.Ready(header, proposal, movement)
        } else {
            Draft.Incomplete(header, proposal, check.missing.toSet(), check.issues.toSet())
        }
    }

    private fun transfer(
        header: DraftHeader,
        proposal: MovementProposal.Transfer,
        directory: WalletDirectory,
        check: Check,
    ): ReadyMovement? {
        val from = check.wallet(proposal.from, directory, DraftField.WALLET)
        val to = check.wallet(proposal.to, directory, DraftField.TO_WALLET)
        if (from != null && to != null && from.id == to.id) check.issues += DraftIssue.SAME_WALLET

        val out = check.positiveAmount(proposal.out, from, DraftField.AMOUNT)
        val into =
            when {
                proposal.into != null -> check.positiveAmount(proposal.into, to, DraftField.TO_AMOUNT)
                // Misma moneda sin monto de llegada ("saqué 200 del cajero"): llega lo mismo que sale.
                out != null && to != null && to.accepts(out.currency) -> out
                else -> {
                    check.missing += DraftField.TO_AMOUNT
                    null
                }
            }
        if (out != null && into != null && out.currency == into.currency && out != into) {
            check.issues += DraftIssue.INCONSISTENT_AMOUNTS
        }
        if (from == null || to == null || out == null || into == null) return null
        return ReadyMovement.Transfer(out, from.id, into, to.id, proposal.occurredAtEpochMs ?: header.capturedAtEpochMs, proposal.note)
    }

    /** Acumula faltantes y problemas mientras se resuelven los campos. */
    private class Check {
        val missing = mutableSetOf<DraftField>()
        val issues = mutableSetOf<DraftIssue>()
        val isClean: Boolean get() = missing.isEmpty() && issues.isEmpty()

        fun wallet(
            ref: WalletRef?,
            directory: WalletDirectory,
            field: DraftField,
        ): Wallet? {
            val wallet =
                when (ref) {
                    null -> null
                    is WalletRef.ById -> directory.byId(ref.walletId) ?: null.also { issues += DraftIssue.UNKNOWN_WALLET }
                    is WalletRef.ByHint -> directory.byHint(ref.hint) ?: null.also { issues += DraftIssue.UNKNOWN_WALLET_HINT }
                }
            if (wallet == null) {
                missing += field
                return null
            }
            if (!wallet.isActive) issues += DraftIssue.WALLET_ARCHIVED
            return wallet
        }

        fun positiveAmount(
            proposed: ProposedAmount?,
            wallet: Wallet?,
            field: DraftField,
        ): Money? {
            val money = amount(proposed, wallet, field) ?: return null
            if (!money.isPositive) {
                issues += DraftIssue.INVALID_AMOUNT
                return null
            }
            return money
        }

        fun amount(
            proposed: ProposedAmount?,
            wallet: Wallet?,
            field: DraftField,
        ): Money? {
            if (proposed == null) {
                missing += field
                return null
            }
            // Sin moneda ni billetera todavía no se puede construir; se resolverá al completar la billetera.
            val currency: Currency = proposed.currency ?: wallet?.currency ?: return null
            val money =
                runCatching { Money.parseCanonical(proposed.value, currency) }.getOrNull()
                    ?: return null.also { issues += DraftIssue.INVALID_AMOUNT }
            if (wallet != null && !wallet.accepts(money.currency)) {
                issues += DraftIssue.CURRENCY_MISMATCH
                return null
            }
            return money
        }
    }
}
