package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.FixedClock
import com.coinden.core.domain.feature.FixedDevice
import com.coinden.core.domain.feature.InMemoryCurrencyProfileRepository
import com.coinden.core.domain.feature.InMemoryWalletRepository
import com.coinden.core.domain.feature.SequentialIdGenerator
import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.GetConsolidatedPortfolioUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Las tasas de referencia por moneda solo sirven para mostrar un total en la moneda local. */
class ReferenceRatesTest {
    private fun bs(v: String) = Money.parseCanonical(v, Currency.BOB)

    private fun usd(v: String) = Money.parseCanonical(v, Currency.USD)

    private fun brl(v: String) = Money.parseCanonical(v, Currency.BRL)

    private val usdRate = ExchangeRateStrategy.ManualDaily(ExchangeRate.parse(Currency.USD, Currency.BOB, "10.20"), 1_000)
    private val brlRate = ExchangeRateStrategy.ManualDaily(ExchangeRate.parse(Currency.BRL, Currency.BOB, "1.85"), 1_000)

    private suspend fun consolidate(
        profile: UserCurrencyProfile,
        vararg holdings: Money,
    ) = GetConsolidatedPortfolioUseCase(InMemoryCurrencyProfileRepository().also { it.saveProfile(profile) })("carlos", holdings.toList())

    @Test
    fun `cada moneda se suma con su propia tasa hacia la moneda local`() =
        runTest {
            val profile =
                UserCurrencyProfile(
                    userId = "carlos",
                    localCurrency = Currency.BOB,
                    currencies = listOf(Currency.USD, Currency.BRL),
                    referenceRates = mapOf("USD" to usdRate, "BRL" to brlRate),
                )
            val portfolio = consolidate(profile, bs("600"), usd("100"), brl("50"))

            // 600 + 100 x 10.20 + 50 x 1.85 = 600 + 1020.00 + 92.50
            assertEquals(bs("1712.50"), portfolio.totalUnifiedAmount)
            assertEquals(emptyList(), portfolio.unconverted)
        }

    @Test
    fun `reales sin dolares tambien se convierten, sin pasar por el dolar`() =
        runTest {
            val profile =
                UserCurrencyProfile(
                    userId = "carlos",
                    localCurrency = Currency.BOB,
                    currencies = listOf(Currency.BRL),
                    referenceRates =
                        mapOf(
                            "BRL" to brlRate,
                        ),
                )
            assertEquals(bs("692.50"), consolidate(profile, bs("600"), brl("50")).totalUnifiedAmount)
        }

    @Test
    fun `una moneda sin tasa no se suma y se informa aparte`() =
        runTest {
            val profile = UserCurrencyProfile(userId = "carlos", localCurrency = Currency.BOB, currencies = listOf(Currency.BRL))
            val portfolio = consolidate(profile, bs("600"), brl("50"))

            assertEquals(bs("600"), portfolio.totalUnifiedAmount)
            assertEquals(listOf(brl("50")), portfolio.unconverted)
        }

    @Test
    fun `un perfil anterior con una sola tasa se lee como la referencia del dolar`() =
        runTest {
            val legacy = UserCurrencyProfile(userId = "carlos", localCurrency = Currency.BOB, strategy = usdRate)

            assertEquals(usdRate, legacy.referenceRateFor(Currency.USD))
            assertEquals(bs("1620.00"), consolidate(legacy, bs("600"), usd("100")).totalUnifiedAmount)
        }

    @Test
    fun `solo moneda local no necesita ninguna tasa`() =
        runTest {
            val profile = UserCurrencyProfile(userId = "carlos", localCurrency = Currency.BOB)
            assertEquals(bs("600"), consolidate(profile, bs("600")).totalUnifiedAmount)
        }

    @Test
    fun `un cambio real guarda su propia tasa y no toca la de referencia`() =
        runTest {
            val profiles = InMemoryCurrencyProfileRepository()
            val profile = UserCurrencyProfile(userId = "carlos", localCurrency = Currency.BOB, referenceRates = mapOf("USD" to usdRate))
            profiles.saveProfile(profile)
            val wallets = InMemoryWalletRepository()
            val ctx = LedgerContext(wallets, SequentialIdGenerator("carlos"), FixedClock(at(10)), FixedDevice("celular-carlos"))
            val create = CreateWalletUseCase(ctx)
            val colchon =
                (
                    create(
                        "carlos",
                        "Dólares colchón",
                        Currency.USD,
                        WalletType.CASH,
                        usd("100"),
                    ) as WalletOperationResult.WalletCreated
                ).wallet
            val billetera =
                (
                    create(
                        "carlos",
                        "Billetera",
                        Currency.BOB,
                        WalletType.CASH,
                        bs("0"),
                    ) as WalletOperationResult.WalletCreated
                ).wallet

            val result = RegisterTransferUseCase(ctx)(colchon.id, billetera.id, usd("11"), bs("112"))

            val transfer = (result as WalletOperationResult.EventRecorded).event as LedgerEvent.Transfer
            assertEquals("112/11", transfer.derivedRate().toCanonical())
            assertEquals(profile, profiles.getProfile("carlos"))
        }
}
