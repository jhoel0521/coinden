package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.ConfigureLocalCurrencyUseCase
import com.coinden.core.domain.usecase.GetConsolidatedPortfolioUseCase
import com.coinden.core.domain.usecase.wallet.AnnulMovementUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetActiveWalletBalancesUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.MovementKind
import com.coinden.core.domain.usecase.wallet.RegisterMovementUseCase
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: Carlos (Bolivia) y sus bolsillos reales.
 *
 * Flujo de un día:
 * - Billetera (efectivo) con 10 Bs, BNB Bs con 20,000 Bs y Dólares colchón con 500 USD.
 * - Intenta registrar el almuerzo de 45 Bs: rechazado, faltan 35 Bs (olvidó el cajero).
 * - Registra el retiro de 200 Bs del cajero de las 11:00 y el almuerzo pasa.
 * - Vende 50 USD al paralelo por 594 Bs (tasa derivada 11.88).
 * - Registra el micro como 30 Bs por error, lo anula y lo registra bien (3 Bs).
 * - Sus saldos activos alimentan la vista consolidada en BOB con su tasa fija 11.20.
 */
class CarlosWalletsFeatureTest {
    private val repo = InMemoryWalletRepository()
    private val clock = FixedClock(at(21))
    private val ctx = LedgerContext(repo, SequentialIdGenerator("carlos"), clock, FixedDevice("celular-carlos"))
    private val create = CreateWalletUseCase(ctx)
    private val register = RegisterMovementUseCase(ctx)
    private val transfer = RegisterTransferUseCase(ctx)
    private val annul = AnnulMovementUseCase(ctx)
    private val status = GetWalletStatusUseCase(repo)
    private val balances = GetActiveWalletBalancesUseCase(repo)

    private val profiles = InMemoryCurrencyProfileRepository()
    private val configureLocal = ConfigureLocalCurrencyUseCase(profiles)
    private val portfolio = GetConsolidatedPortfolioUseCase(profiles)

    private fun bs(cents: Long) = Money.ofMinorUnits(cents, Currency.BOB)

    private fun usd(cents: Long) = Money.ofMinorUnits(cents, Currency.USD)

    private suspend fun wallet(
        name: String,
        opening: Money,
        type: WalletType,
    ): Wallet =
        assertIs<WalletOperationResult.WalletCreated>(
            create("carlos", name, opening.currency, type, opening, occurredAtEpochMs = at(7)),
        ).wallet

    @Test
    fun `un dia de Carlos con control estricto, cambio al paralelo y patrimonio consolidado`() =
        runTest {
            val billetera = wallet("Billetera", bs(1_000), WalletType.CASH)
            val bnb = wallet("BNB Bs", bs(2_000_000), WalletType.BANK)
            val colchon = wallet("Dólares colchón", usd(50_000), WalletType.CASH)

            // Almuerzo sin haber registrado el cajero: rechazado
            val rejected = register(billetera.id, MovementKind.EXPENSE, bs(4_500), at(13))
            assertEquals(
                WalletError.InsufficientFunds(billetera.id, bs(3_500), at(13)),
                assertIs<WalletOperationResult.Rejected>(rejected).error,
            )

            // Recuerda el cajero de las 11:00 y el almuerzo pasa
            assertIs<WalletOperationResult.EventRecorded>(transfer(bnb.id, billetera.id, bs(20_000), bs(20_000), at(11)))
            assertIs<WalletOperationResult.EventRecorded>(register(billetera.id, MovementKind.EXPENSE, bs(4_500), at(13)))
            assertEquals(bs(16_500), status(billetera.id)!!.balance)

            // Vende 50 USD al paralelo
            val venta = transfer(colchon.id, billetera.id, usd(5_000), bs(59_400), at(15))
            val ventaEvent = assertIs<LedgerEvent.Transfer>(assertIs<WalletOperationResult.EventRecorded>(venta).event)
            assertEquals(ExchangeRate.of(usd(5_000), bs(59_400)), ventaEvent.derivedRate())
            assertEquals(bs(1_188), ventaEvent.derivedRate().quoteFor(usd(100)))

            // Corrige el micro mal registrado
            val micro = assertIs<WalletOperationResult.EventRecorded>(register(billetera.id, MovementKind.EXPENSE, bs(3_000), at(17)))
            assertIs<WalletOperationResult.EventRecorded>(annul(micro.event.id))
            assertIs<WalletOperationResult.EventRecorded>(register(billetera.id, MovementKind.EXPENSE, bs(300), at(17)))

            assertEquals(bs(75_600), status(billetera.id)!!.balance)
            assertEquals(bs(1_980_000), status(bnb.id)!!.balance)
            assertEquals(usd(45_000), status(colchon.id)!!.balance)

            // Patrimonio consolidado desde las billeteras, con su tasa fija de 11.20 Bs por USD
            configureLocal("carlos", Currency.BOB, ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20")))
            val holdings = balances("carlos").map { it.balance }
            val consolidated = portfolio("carlos", holdings)

            assertEquals(Currency.BOB, consolidated.targetCurrency)
            assertEquals(3, consolidated.holdings.size)
            // 756.00 + 19,800.00 + 450 USD x 11.20 (5,040.00) = 25,596.00 Bs
            assertEquals(bs(2_559_600), consolidated.totalUnifiedAmount)
        }
}
