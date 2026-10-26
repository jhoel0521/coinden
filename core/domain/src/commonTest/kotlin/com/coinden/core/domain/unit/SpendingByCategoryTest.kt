package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.FakeInstalledPacks
import com.coinden.core.domain.feature.FixedClock
import com.coinden.core.domain.feature.FixedDevice
import com.coinden.core.domain.feature.InMemoryCategorizationRepository
import com.coinden.core.domain.feature.InMemoryCurrencyProfileRepository
import com.coinden.core.domain.feature.InMemoryWalletRepository
import com.coinden.core.domain.feature.SequentialIdGenerator
import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.usecase.ConfigureLocalCurrencyUseCase
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.category.ClassifyExpenseUseCase
import com.coinden.core.domain.usecase.category.SpendingByCategoryUseCase
import com.coinden.core.domain.usecase.wallet.AnnulMovementUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.MovementKind
import com.coinden.core.domain.usecase.wallet.RegisterMovementUseCase
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SpendingByCategoryTest {
    private val wallets = InMemoryWalletRepository()
    private val categorization = InMemoryCategorizationRepository()
    private val profiles = InMemoryCurrencyProfileRepository()
    private val ids = SequentialIdGenerator()
    private val clock = FixedClock(at(21))
    private val device = FixedDevice("celular")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = CategorizationContext(categorization, wallets, FakeInstalledPacks(listOf(TestPacks.spanish)), ids, clock, device)
    private val register = RegisterMovementUseCase(ledger)
    private val classify = ClassifyExpenseUseCase(ctx)
    private val report = SpendingByCategoryUseCase(ctx, profiles)

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private fun usd(canonical: String) = Money.parseCanonical(canonical, Currency.USD)

    private suspend fun wallet(
        name: String,
        opening: Money,
        type: WalletType,
    ) = assertIs<WalletOperationResult.WalletCreated>(
        CreateWalletUseCase(ledger)("carlos", name, opening.currency, type, opening, occurredAtEpochMs = at(1)),
    ).wallet.id

    private suspend fun spend(
        walletId: String,
        amount: Money,
        hour: Int,
        category: BaseCategory?,
        description: String,
    ): String {
        val id = assertIs<WalletOperationResult.EventRecorded>(register(walletId, MovementKind.EXPENSE, amount, at(hour))).event.id
        classify("carlos", id, category?.let { CategoryRef.Base(it) }, description)
        return id
    }

    @Test
    fun `gasto del periodo por categoria, multimoneda y sin anulados ni transferencias`() =
        runTest {
            ConfigureLocalCurrencyUseCase(
                profiles,
            )("carlos", Currency.BOB, ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20")))
            val cash = wallet("Billetera", bs("500"), WalletType.CASH)
            val bank = wallet("BE Bs", bs("1000"), WalletType.BANK)
            val meru = wallet("Meru", usd("100"), WalletType.DIGITAL)

            spend(bank, bs("45"), 10, BaseCategory.FOOD, "almuerzo")
            spend(bank, bs("25"), 11, BaseCategory.FOOD, "POLLOS DEL VALLE")
            spend(cash, bs("2"), 12, BaseCategory.FOOD, "semillita")
            spend(bank, bs("118.71"), 13, BaseCategory.SUBSCRIPTIONS, "StreamingPremium")
            spend(meru, usd("12"), 14, BaseCategory.SUBSCRIPTIONS, "netflix")
            spend(bank, bs("31.70"), 15, BaseCategory.HEALTH, "FARMACIA EJEMPLO")
            spend(cash, bs("3"), 16, BaseCategory.TRANSPORT, "trasmporte")
            spend(cash, bs("10"), 17, null, "Mariela Quispe Rojas")

            // Anulado: no cuenta
            val wrong = spend(cash, bs("54"), 18, BaseCategory.FOOD, "almuerzo")
            AnnulMovementUseCase(ledger)(wrong)
            // Retiro: no es gasto
            RegisterTransferUseCase(ledger)(bank, cash, bs("200"), bs("200"), at(9))
            // Fuera del periodo
            spend(cash, bs("99"), 23, BaseCategory.FOOD, "cena")

            val result = report("carlos", fromEpochMs = at(0), toEpochMs = at(20))

            assertEquals(Currency.BOB, result.currency)
            val rows = result.rows.associate { it.category to it.consolidated }
            assertEquals(
                listOf(
                    CategoryRef.Base(BaseCategory.SUBSCRIPTIONS),
                    CategoryRef.Base(BaseCategory.FOOD),
                    CategoryRef.Base(BaseCategory.HEALTH),
                    null,
                    CategoryRef.Base(BaseCategory.TRANSPORT),
                ),
                result.rows.map { it.category },
            )
            // 118.71 Bs + 12 USD x 11.20 (134.40) = 253.11
            assertEquals(bs("253.11"), rows[CategoryRef.Base(BaseCategory.SUBSCRIPTIONS)])
            assertEquals(listOf(bs("118.71"), usd("12.00")), result.rows.first().byCurrency)
            assertEquals(bs("72.00"), rows[CategoryRef.Base(BaseCategory.FOOD)])
            assertEquals(bs("10.00"), rows[null])
            assertEquals(bs("369.81"), result.total)
        }

    @Test
    fun `sin gastos el reporte esta vacio`() =
        runTest {
            wallet("Billetera", bs("10"), WalletType.CASH)
            val result = report("carlos", at(0), at(20))
            assertEquals(emptyList(), result.rows)
            assertEquals(Money.zero(Currency.USD), result.total) // sin perfil: USD
        }

    @Test
    fun `el desbordamiento de Carlos no se presenta como si faltara una tasa`() =
        runTest {
            ConfigureLocalCurrencyUseCase(
                profiles,
            )("carlos", Currency.BOB, ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20")))
            val meru = wallet("Meru", usd("90000000000000000"), WalletType.DIGITAL)
            spend(meru, usd("90000000000000000"), 12, BaseCategory.SUBSCRIPTIONS, "suscripcion")
            assertFailsWith<ArithmeticException> { report("carlos", at(0), at(20)) }
        }
}
