package com.coinden.app

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppContainerTest {
    @Test
    fun `al iniciar existen la billetera de efectivo por defecto y la de banco con el alias qr`() =
        runTest {
            val container = testContainer()
            DemoSeed.seed(container)

            val directory = container.drafts.directory(container.ownerId)
            val cash = directory.wallets.single { it.type == WalletType.CASH }
            val bank = directory.wallets.single { it.type == WalletType.BANK }

            assertEquals(Currency.BOB, cash.currency)
            assertEquals(Currency.BOB, bank.currency)
            assertEquals(cash.id, directory.defaultWalletId)
            assertEquals(bank, directory.byHint("qr"))
        }
}
