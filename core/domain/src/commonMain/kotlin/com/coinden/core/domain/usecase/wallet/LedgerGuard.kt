package com.coinden.core.domain.usecase.wallet

import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.LedgerProjection
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.port.output.WalletRepositoryPort

/**
 * Control estricto de saldo cronológico.
 *
 * Un evento candidato se rechaza en una billetera que no admite negativos cuando su efecto
 * es negativo y deja algún punto negativo desde el momento en que actúa en adelante.
 * Un efecto positivo nunca se rechaza, así un hueco previo (por ejemplo, de una fusión)
 * no bloquea el ajuste que lo corrige.
 */
internal class LedgerGuard(
    private val repository: WalletRepositoryPort,
) {
    suspend fun check(
        wallets: List<Wallet>,
        candidate: LedgerEvent,
    ): WalletError.InsufficientFunds? = check(wallets, candidate, pending = emptyList())

    /**
     * Valida varios candidatos que se escribirán juntos (una anulación y su reemplazo): cada uno
     * se evalúa como si los anteriores ya estuvieran en el libro.
     */
    suspend fun checkAll(
        wallets: List<Wallet>,
        candidates: List<LedgerEvent>,
    ): WalletError.InsufficientFunds? {
        candidates.forEachIndexed { i, candidate ->
            check(wallets, candidate, pending = candidates.take(i))?.let { return it }
        }
        return null
    }

    private suspend fun check(
        wallets: List<Wallet>,
        candidate: LedgerEvent,
        pending: List<LedgerEvent>,
    ): WalletError.InsufficientFunds? {
        for (wallet in wallets) {
            if (wallet.allowsNegative) continue
            val existing = repository.eventsForWallet(wallet.id) + pending.filter { wallet.id in it.affectedWalletIds }
            val effect = effectOf(candidate, wallet, existing) ?: continue
            if (!effect.delta.isNegative) continue

            val withCandidate = LedgerProjection(wallet, existing + candidate)
            val fromEffect =
                withCandidate.runningBalances.filter {
                    LedgerProjection.CHRONOLOGICAL.compare(it.first, effect.anchor) >= 0
                }
            val negatives = fromEffect.filter { it.second.isNegative }
            if (negatives.isNotEmpty()) {
                return WalletError.InsufficientFunds(
                    walletId = wallet.id,
                    shortfall = -negatives.minOf { it.second },
                    firstNegativeAtEpochMs = negatives.first().first.occurredAtEpochMs,
                )
            }
        }
        return null
    }

    private class Effect(
        val delta: Money,
        val anchor: LedgerEvent,
    )

    /** Efecto del candidato en la billetera y el evento desde cuyo momento actúa. */
    private fun effectOf(
        candidate: LedgerEvent,
        wallet: Wallet,
        existing: List<LedgerEvent>,
    ): Effect? {
        if (candidate is LedgerEvent.Annulment) {
            val target = existing.firstOrNull { it.id == candidate.targetId } ?: return null
            val targetDelta = target.deltaFor(wallet.id) ?: return null
            return Effect(-targetDelta, target)
        }
        val delta = candidate.deltaFor(wallet.id) ?: return null
        return Effect(delta, candidate)
    }
}
