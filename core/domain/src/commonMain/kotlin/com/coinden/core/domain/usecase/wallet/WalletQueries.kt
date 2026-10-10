package com.coinden.core.domain.usecase.wallet

import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerProjection
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.port.output.WalletRepositoryPort

/**
 * Saldo de una billetera en su propia moneda.
 */
data class WalletBalance(
    val walletId: String,
    val balance: Money,
)

/**
 * Estado de una billetera: saldo derivado, ciclo de vida y cuadre.
 *
 * [isUnbalanced] (DESCUADRADA) indica un negativo prohibido en algún punto del libro;
 * [shortfall] es el faltante máximo y [firstNegativeAtEpochMs] dónde empieza.
 */
data class WalletState(
    val wallet: Wallet,
    val balance: Money,
    val isUnbalanced: Boolean,
    val shortfall: Money,
    val firstNegativeAtEpochMs: Long?,
)

/**
 * Caso de Uso: consultar saldo, estado y cuadre de una billetera.
 */
class GetWalletStatusUseCase(
    private val repository: WalletRepositoryPort,
) {
    suspend operator fun invoke(walletId: String): WalletState? {
        val wallet = repository.getWallet(walletId) ?: return null
        val projection = LedgerProjection(wallet, repository.eventsForWallet(walletId))
        return WalletState(
            wallet = wallet,
            balance = projection.balance,
            isUnbalanced = projection.isUnbalanced,
            shortfall = if (projection.isUnbalanced) projection.shortfall else Money.zero(wallet.currency),
            firstNegativeAtEpochMs = projection.firstNegativeAtEpochMs.takeIf { projection.isUnbalanced },
        )
    }
}

/**
 * Caso de Uso: saldos de las billeteras activas, fuente de la vista consolidada.
 */
class GetActiveWalletBalancesUseCase(
    private val repository: WalletRepositoryPort,
) {
    suspend operator fun invoke(ownerId: String): List<WalletBalance> =
        repository
            .listWallets(ownerId)
            .filter { it.isActive }
            .map { WalletBalance(it.id, LedgerProjection(it, repository.eventsForWallet(it.id)).balance) }
}
