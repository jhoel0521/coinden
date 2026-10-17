package com.coinden.app.insights

import com.coinden.app.AppContainer
import com.coinden.app.capture.DraftFormPresenter
import com.coinden.app.capture.FormMode
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.category.ArchiveCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.CategorizationResult
import com.coinden.core.domain.usecase.category.ClassifyExpenseUseCase
import com.coinden.core.domain.usecase.category.CreateCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.SpendingReport
import com.coinden.core.domain.usecase.wallet.ArchiveWalletUseCase
import com.coinden.core.domain.usecase.wallet.MovementKind
import com.coinden.core.domain.usecase.wallet.RegisterMovementUseCase
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SpendingInsightsPresenterTest {
    private fun money(
        value: String,
        currency: Currency = Currency.BOB,
    ) = Money.parseCanonical(value, currency)

    private suspend fun AppContainer.confirm(phrase: String): String {
        val draft = captureDraft(ownerId, Evidence(EvidenceSource.TEXT, phrase)).single()
        assertIs<DraftResult.Ok>(confirmDraft(draft.id, categorization.suggest(ownerId, phrase)?.category))
        return draft.id
    }

    @Test
    fun `Carlos consulta octubre con orden total y sin tocar saldos`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            c.confirm("almuerzo 72 bnb")
            c.confirm("netflix 118.71 bnb")
            c.confirm("farmacia 31.70 bnb")
            c.confirm("taxi 3 bnb")
            c.captureDraft(c.ownerId, Evidence(EvidenceSource.TEXT, "almuerzo 50 bnb"))
            c.confirm("recibi 100 bnb")
            val wallets = c.storage.wallets.listWallets(c.ownerId)
            assertIs<WalletOperationResult.EventRecorded>(
                RegisterTransferUseCase(c.ledger)(
                    wallets.first { it.type == WalletType.BANK }.id,
                    wallets
                        .first {
                            it.type ==
                                WalletType.CASH
                        }.id,
                    money("10"),
                    money("10"),
                ),
            )
            val before = c.walletBalances(c.ownerId)
            val p = SpendingInsightsPresenter(c)
            p.load()
            val s = p.state.value
            assertEquals(SpendingMonth(2025, 10), s.month)
            assertEquals(money("225.41"), s.total)
            assertEquals(
                listOf(BaseCategory.SUBSCRIPTIONS, BaseCategory.FOOD, BaseCategory.HEALTH, BaseCategory.TRANSPORT),
                s.rows.map {
                    (it.total.category as CategoryRef.Base).category
                },
            )
            assertEquals(before, c.walletBalances(c.ownerId))
            assertFalse(s.canNext)
            p.previous()
            assertTrue(p.state.value.empty)
            p.next()
            assertEquals(money("225.41"), p.state.value.total)
            p.next()
            assertEquals(s.month, p.state.value.month)
        }

    @Test
    fun `Carlos corrige el almuerzo y conserva Semillitas archivadas y sin categoria`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val id = c.confirm("almuerzo 35 bnb")
            val p = SpendingInsightsPresenter(c)
            p.load()
            val form = DraftFormPresenter(c, FormMode.Correct(id)).also { it.load() }
            form.onAmountChange("32")
            assertTrue(form.confirm())
            val bank =
                c.storage.wallets
                    .listWallets(c.ownerId)
                    .first { it.type == WalletType.BANK }
            val event =
                assertIs<WalletOperationResult.EventRecorded>(
                    RegisterMovementUseCase(c.ledger)(bank.id, MovementKind.EXPENSE, money("8")),
                ).event
            val custom =
                assertIs<CategorizationResult.Ok<*>>(
                    CreateCustomCategoryUseCase(c.categorization)(c.ownerId, "Semillitas"),
                ).value as com.coinden.core.domain.model.category.CustomCategory
            ClassifyExpenseUseCase(c.categorization)(c.ownerId, event.id, CategoryRef.Custom(custom.id))
            RegisterMovementUseCase(c.ledger)(bank.id, MovementKind.EXPENSE, money("5"))
            ArchiveCustomCategoryUseCase(c.categorization)(c.ownerId, custom.id)
            ArchiveWalletUseCase(c.storage.wallets)(bank.id)
            p.load()
            assertEquals(money("45"), p.state.value.total)
            assertEquals(
                "Semillitas",
                p.state.value.rows
                    .single { it.customName != null }
                    .customName,
            )
            assertEquals(
                money("5"),
                p.state.value.rows
                    .single { it.total.category == null }
                    .total.consolidated,
            )
        }

    private suspend fun AppContainer.spend(
        value: String,
        currency: Currency,
    ) {
        val wallet =
            assertIs<WalletOperationResult.WalletCreated>(
                createWallet(ownerId, currency.code, currency, WalletType.CASH, money("100", currency)),
            ).wallet
        assertIs<WalletOperationResult.EventRecorded>(
            RegisterMovementUseCase(ledger)(wallet.id, MovementKind.EXPENSE, money(value, currency)),
        )
    }

    @Test
    fun `Carlos usa tasas actuales y deja BRL y BTC sin referencia visibles`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }

            suspend fun rate(value: String) {
                c.storage.profiles.saveProfile(
                    UserCurrencyProfile(
                        userId = c.ownerId,
                        localCurrency = Currency.BOB,
                        referenceRates =
                            mapOf(
                                "USD" to ExchangeRateStrategy.ManualDaily(ExchangeRate.parse(Currency.USD, Currency.BOB, value), 1),
                            ),
                    ),
                )
            }
            rate("11.20")
            c.spend("12", Currency.USD)
            val p = SpendingInsightsPresenter(c)
            p.load()
            assertEquals(money("134.40"), p.state.value.total)
            assertEquals(
                listOf(money("12", Currency.USD)),
                p.state.value.rows
                    .single()
                    .total.byCurrency,
            )
            rate("10")
            p.load()
            assertEquals(money("120"), p.state.value.total)
            c.spend("50", Currency.BRL)
            c.spend("0.001", Currency.BTC)
            p.load()
            assertEquals(money("120"), p.state.value.total)
            assertTrue(p.state.value.partial)
            assertTrue(p.state.value.hasConverted)
            assertEquals(
                setOf("USD"),
                p.state.value.rows
                    .single()
                    .convertedCurrencies,
            )
        }

    @Test
    fun `todo sin convertir no es un mes vacio y cero redondeado si esta convertido`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            c.spend("50", Currency.BRL)
            val p = SpendingInsightsPresenter(c)
            p.load()
            assertFalse(p.state.value.empty)
            assertFalse(p.state.value.hasConverted)
            assertTrue(p.state.value.partial)
            c.storage.profiles.saveProfile(
                UserCurrencyProfile(
                    userId = c.ownerId,
                    localCurrency = Currency.BOB,
                    currencies = listOf(Currency.BRL),
                    referenceRates =
                        mapOf(
                            "BRL" to ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.BRL, Currency.BOB, "0.00001")),
                        ),
                ),
            )
            p.load()
            assertTrue(p.state.value.hasConverted)
            assertFalse(p.state.value.partial)
            assertEquals(money("0"), p.state.value.total)
        }

    @Test
    fun `John sin gastos y errores reintentos cancelacion y respuestas fuera de orden`() =
        runTest {
            val c = testContainer(country = "US", language = "en")
            val month = SpendingMonth(2025, 10)
            val empty = SpendingSnapshot(SpendingReport(Currency.USD, emptyList(), money("0", Currency.USD)))
            val delayed = CompletableDeferred<SpendingSnapshot>()
            var fail = true
            var cancel = false
            val p =
                SpendingInsightsPresenter(c, query = { selected, _ ->
                    if (cancel) throw CancellationException()
                    if (fail) error("read failed")
                    if (selected == month.shift(-1)) delayed.await() else empty
                })
            p.load()
            assertEquals(SpendingLoad.ERROR, p.state.value.load)
            assertEquals(month, p.state.value.month)
            fail = false
            p.load()
            assertTrue(p.state.value.empty)
            assertEquals(money("0", Currency.USD), p.state.value.total)
            val first = launch { p.previous() }
            runCurrent()
            assertEquals(SpendingLoad.LOADING, p.state.value.load)
            assertEquals(null, p.state.value.total)
            p.previous()
            delayed.complete(empty)
            first.join()
            assertEquals(month.shift(-2), p.state.value.month)
            assertEquals(SpendingLoad.READY, p.state.value.load)
            cancel = true
            var cancelled = false
            try {
                p.load()
            } catch (_: CancellationException) {
                cancelled = true
            }
            assertTrue(cancelled)
        }

    @Test
    fun `Sophie consulta febrero bisiesto y Carlos los limites exactos de octubre`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val bank =
                c.storage.wallets
                    .listWallets(c.ownerId)
                    .first { it.type == WalletType.BANK }
            val old = SpendingMonth(2024, 1).bounds(0).first
            val wallet =
                assertIs<WalletOperationResult.WalletCreated>(
                    c.createWallet(c.ownerId, "Sophie", Currency.EUR, WalletType.CASH, money("500", Currency.EUR), occurredAtEpochMs = old),
                ).wallet
            val register = RegisterMovementUseCase(c.ledger)
            val february = SpendingMonth(2024, 2).bounds(60)
            assertIs<WalletOperationResult.EventRecorded>(
                register(
                    wallet.id,
                    MovementKind.EXPENSE,
                    money("7", Currency.EUR),
                    february.second - 1,
                ),
            )
            assertIs<WalletOperationResult.EventRecorded>(
                register(wallet.id, MovementKind.EXPENSE, money("9", Currency.EUR), february.second),
            )
            val feb = c.spendingByCategory(c.ownerId, february.first, february.second)
            assertEquals(listOf(money("7", Currency.EUR)), feb.rows.single().byCurrency)
            val october = SpendingMonth(2025, 10).bounds(-240)
            assertIs<WalletOperationResult.EventRecorded>(register(bank.id, MovementKind.EXPENSE, money("2"), october.second - 1))
            assertIs<WalletOperationResult.EventRecorded>(register(bank.id, MovementKind.EXPENSE, money("3"), october.second))
            val p = SpendingInsightsPresenter(c)
            p.load()
            assertEquals(money("2"), p.state.value.total)
        }

    @Test
    fun `una consulta fallida retira los importes previos y el reintento conserva el mes`() =
        runTest {
            val c = testContainer()
            var fail = false
            val p =
                SpendingInsightsPresenter(c, query = { _, _ ->
                    if (fail) error("disk")
                    SpendingSnapshot(SpendingReport(Currency.BOB, emptyList(), money("0")))
                })
            p.load()
            fail = true
            p.previous()
            assertEquals(SpendingLoad.ERROR, p.state.value.load)
            assertEquals(null, p.state.value.total)
            assertTrue(
                p.state.value.rows
                    .isEmpty(),
            )
            val month = p.state.value.month
            fail = false
            p.load()
            assertEquals(month, p.state.value.month)
            assertEquals(SpendingLoad.READY, p.state.value.load)
        }
}
