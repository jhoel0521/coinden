package com.coinden.app

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppContainerTest {
    @Test
    fun `al iniciar existen la billetera de efectivo por defecto y la de banco con el alias bnb`() =
        runTest {
            val container = testContainer()
            DemoSeed.seed(container)

            val directory = container.drafts.directory(container.ownerId)
            val cash = directory.wallets.single { it.type == WalletType.CASH }
            val bank = directory.wallets.single { it.type == WalletType.BANK }

            assertEquals(Currency.BOB, cash.currency)
            assertEquals(Currency.BOB, bank.currency)
            assertEquals(cash.id, directory.defaultWalletId)
            assertEquals(bank, directory.byHint("bnb"))
        }

    @Test
    fun `al reabrir con datos guardados no se vuelven a sembrar las billeteras`() =
        runTest {
            val storage = AppStorage.inMemory()
            val firstStart = testContainer(storage)
            DemoSeed.seed(firstStart)

            // Mismo almacenamiento y mismo dueño: como reabrir la app sobre la base guardada
            val secondStart = testContainer(storage, ownerId = firstStart.ownerId)
            DemoSeed.seed(secondStart)

            val directory = secondStart.drafts.directory(secondStart.ownerId)
            assertEquals(2, directory.wallets.size)
            assertEquals(mapOf("bnb" to directory.wallets.single { it.type == WalletType.BANK }.id), directory.aliases)
        }
}
