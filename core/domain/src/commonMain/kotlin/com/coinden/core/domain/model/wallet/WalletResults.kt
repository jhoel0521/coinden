package com.coinden.core.domain.model.wallet

import com.coinden.core.domain.model.Money

/**
 * Motivos de rechazo de una operación sobre billeteras.
 *
 * No contienen textos: la capa de presentación los traduce según el idioma del usuario.
 */
sealed interface WalletError {
    data object InvalidName : WalletError

    data object NotFound : WalletError

    data object WalletArchived : WalletError

    data object CurrencyMismatch : WalletError

    data object InvalidAmount : WalletError

    data object SameWallet : WalletError

    data object InconsistentTransferAmounts : WalletError

    data object AlreadyAnnulled : WalletError

    data object CannotAnnulAnnulment : WalletError

    /**
     * El movimiento dejaría [walletId] con saldo negativo desde [firstNegativeAtEpochMs];
     * [shortfall] es el faltante máximo (positivo) en la moneda de la billetera.
     */
    data class InsufficientFunds(
        val walletId: String,
        val shortfall: Money,
        val firstNegativeAtEpochMs: Long,
    ) : WalletError
}

/**
 * Resultado de una operación sobre billeteras o su libro.
 */
sealed interface WalletOperationResult {
    data class WalletCreated(
        val wallet: Wallet,
        val openingBalance: LedgerEvent.OpeningBalance,
    ) : WalletOperationResult

    data class WalletUpdated(
        val wallet: Wallet,
    ) : WalletOperationResult

    data class EventRecorded(
        val event: LedgerEvent,
    ) : WalletOperationResult

    data class Rejected(
        val error: WalletError,
    ) : WalletOperationResult
}
