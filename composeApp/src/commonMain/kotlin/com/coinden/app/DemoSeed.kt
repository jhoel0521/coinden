package com.coinden.app

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType

/**
 * Billeteras de ejemplo para la demostración, creadas con los casos de uso del dominio.
 * Desaparece cuando exista la pantalla de billeteras (onboarding-wallets-ui).
 */
object DemoSeed {
    suspend fun seed(container: AppContainer) {
        val owner = container.ownerId
        val cash = create(container, "Billetera", WalletType.CASH, "100")
        val bank = create(container, "BNB Bs", WalletType.BANK, "500")
        container.setDefaultWallet(owner, cash)
        container.setWalletAliases(owner, bank, setOf("qr"))
    }

    private suspend fun create(
        container: AppContainer,
        name: String,
        type: WalletType,
        opening: String,
    ): String {
        val result = container.createWallet(container.ownerId, name, Currency.BOB, type, Money.parseCanonical(opening, Currency.BOB))
        return (result as WalletOperationResult.WalletCreated).wallet.id
    }
}
