package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: John (USA) mueve satoshis exactos.
 *
 * - Ledger con 0.05 BTC, Hot wallet vacía y Chase con 4,500 USD.
 * - Mueve 100,000 satoshis exactos del Ledger a la Hot wallet.
 * - Vende esos 0.001 BTC por 65.00 USD a Chase: tasa derivada 65,000.00 USD por BTC.
 */
class JohnSatoshiWalletFeatureTest {
    private val repo = InMemoryWalletRepository()
    private val ctx = LedgerContext(repo, SequentialIdGenerator("john"), FixedClock(at(20)), FixedDevice("pc-john"))
    private val create = CreateWalletUseCase(ctx)
    private val transfer = RegisterTransferUseCase(ctx)
    private val status = GetWalletStatusUseCase(repo)

    private fun sats(units: Long) = Money(units, Currency.BTC)

    private fun usd(cents: Long) = Money(cents, Currency.USD)

    private suspend fun wallet(
        name: String,
        opening: Money,
        type: WalletType,
    ): Wallet =
        assertIs<WalletOperationResult.WalletCreated>(
            create("john", name, opening.currency, type, opening, occurredAtEpochMs = at(8)),
        ).wallet

    @Test
    fun `John mueve y vende satoshis sin perder precision`() =
        runTest {
            val ledger = wallet("Ledger", sats(5_000_000), WalletType.CRYPTO)
            val hot = wallet("Hot wallet", sats(0), WalletType.CRYPTO)
            val chase = wallet("Chase", usd(450_000), WalletType.BANK)

            assertIs<WalletOperationResult.EventRecorded>(transfer(ledger.id, hot.id, sats(100_000), sats(100_000), at(10)))
            assertEquals(sats(4_900_000), status(ledger.id)!!.balance)
            assertEquals("0.04900000", status(ledger.id)!!.balance.toNumericString())
            assertEquals(sats(100_000), status(hot.id)!!.balance)

            val venta = transfer(hot.id, chase.id, sats(100_000), usd(6_500), at(11))
            val event = assertIs<LedgerEvent.Transfer>(assertIs<WalletOperationResult.EventRecorded>(venta).event)
            assertEquals(usd(6_500_000), event.derivedRate())

            assertEquals(sats(0), status(hot.id)!!.balance)
            assertEquals(usd(456_500), status(chase.id)!!.balance)
        }
}
