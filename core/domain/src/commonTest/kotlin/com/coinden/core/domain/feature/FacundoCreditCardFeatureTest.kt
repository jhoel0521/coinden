package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.MovementKind
import com.coinden.core.domain.usecase.wallet.RegisterMovementUseCase
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * Escenario de Feature: Facundo (Argentina) y su tarjeta de crédito.
 *
 * - Visa Galicia (CREDITO, ARS) admite saldo negativo: la deuda es el saldo.
 * - Paga la tarjeta desde Banco Galicia con una transferencia.
 * - El banco no admite negativos: un pago mayor a su saldo se rechaza.
 */
class FacundoCreditCardFeatureTest {
    private val repo = InMemoryWalletRepository()
    private val ctx = LedgerContext(repo, SequentialIdGenerator("facu"), FixedClock(at(22)), FixedDevice("celular-facu"))
    private val create = CreateWalletUseCase(ctx)
    private val register = RegisterMovementUseCase(ctx)
    private val transfer = RegisterTransferUseCase(ctx)
    private val status = GetWalletStatusUseCase(repo)

    private fun ars(cents: Long) = Money.ofMinorUnits(cents, Currency.ARS)

    private suspend fun wallet(
        name: String,
        opening: Money,
        type: WalletType,
    ): Wallet =
        assertIs<WalletOperationResult.WalletCreated>(
            create("facundo", name, opening.currency, type, opening, occurredAtEpochMs = at(8)),
        ).wallet

    @Test
    fun `Facundo usa la tarjeta, queda en negativo y la paga desde el banco`() =
        runTest {
            val visa = wallet("Visa Galicia", ars(0), WalletType.CREDIT)
            val galicia = wallet("Banco Galicia", ars(85_000_000), WalletType.BANK)

            assertIs<WalletOperationResult.EventRecorded>(register(visa.id, MovementKind.EXPENSE, ars(2_500_000), at(13)))
            val deuda = status(visa.id)!!
            assertEquals(ars(-2_500_000), deuda.balance)
            assertFalse(deuda.isUnbalanced)

            assertIs<WalletOperationResult.EventRecorded>(transfer(galicia.id, visa.id, ars(2_500_000), ars(2_500_000), at(18)))
            assertEquals(ars(0), status(visa.id)!!.balance)
            assertEquals(ars(82_500_000), status(galicia.id)!!.balance)

            val sobrepago = transfer(galicia.id, visa.id, ars(90_000_000), ars(90_000_000), at(19))
            assertEquals(
                WalletError.InsufficientFunds(galicia.id, ars(7_500_000), at(19)),
                assertIs<WalletOperationResult.Rejected>(sobrepago).error,
            )
        }
}
