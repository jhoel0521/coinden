package com.coinden.core.data.local

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ReopenAndUnknownEventsTest {
    private val file = File.createTempFile("coinden-", ".db").also { it.delete() }

    @AfterTest
    fun cleanUp() {
        file.delete()
    }

    private fun bs(v: String) = Money.parseCanonical(v, Currency.BOB)

    private val t = 1_760_000_000_000L

    @Test
    fun `los datos, el orden y la identidad sobreviven al cerrar y reabrir la base`() =
        runTest {
            val firstDriver = JvmDatabase.driver(file.path)
            val first = JvmDatabase.open(firstDriver)
            val identity = LocalIdentity(first) { UUID.randomUUID().toString() }
            val device = identity.deviceId()
            val owner = identity.ownerId()
            assertNotEquals(device, owner)

            val wallets = LocalWalletRepository(first, Dispatchers.Unconfined)
            val billetera = Wallet("w1", owner, "Billetera", Currency.BOB, WalletType.CASH)
            wallets.saveWallet(billetera)
            val events =
                listOf(
                    LedgerEvent.OpeningBalance("e1", device, t, t, "w1", bs("20")),
                    LedgerEvent.Expense("e2", device, t + 1, t + 1, "w1", bs("3")),
                    LedgerEvent.Expense("e3", device, t + 2, t + 2, "w1", bs("45")),
                )
            events.forEach { wallets.appendEvent(it) }
            firstDriver.close()

            val again = JvmDatabase.open(JvmDatabase.driver(file.path))
            val reopenedIdentity = LocalIdentity(again) { error("no debe generar ids nuevos al reabrir") }
            assertEquals(device, reopenedIdentity.deviceId())
            assertEquals(owner, reopenedIdentity.ownerId())

            val reopened = LocalWalletRepository(again, Dispatchers.Unconfined)
            assertEquals(listOf(billetera), reopened.listWallets(owner))
            assertEquals(events, reopened.eventsForWallet("w1"))
        }

    @Test
    fun `un evento desconocido se conserva intacto y se omite al leer`() =
        runTest {
            val db = JvmDatabase.open(JvmDatabase.driver())
            val wallets = LocalWalletRepository(db, Dispatchers.Unconfined)
            val known = LedgerEvent.Expense("e1", "dev", t, t, "w1", bs("3"))
            wallets.appendEvent(known)

            // Llegado de un dispositivo con una versión más nueva de la app
            val payload = """{"evento":"del futuro","monto":"10.00"}"""
            db.coinDenQueries.insertEvent("e2", "ledger", null, "otro-dev", t + 1, t + 1, "ledger.commitment_paid", 1, payload)
            db.coinDenQueries.insertEventRef("e2", "w1")
            db.coinDenQueries.insertEvent("e3", "ledger", null, "otro-dev", t + 2, t + 2, "ledger.expense", 2, payload)
            db.coinDenQueries.insertEventRef("e3", "w1")

            assertEquals(listOf(known), wallets.eventsForWallet("w1"))
            assertEquals(null, wallets.findEvent("e2"))
            val raw = db.coinDenQueries.eventById("e2", "ledger").executeAsOne()
            assertEquals("ledger.commitment_paid", raw.type)
            assertEquals(payload, raw.payload)
        }
}
