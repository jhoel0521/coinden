package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.support.testLocale
import com.coinden.core.domain.usecase.ConfigureLocalCurrencyUseCase
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.category.SpendingByCategoryUseCase
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: las mismas reglas en inglés (John, EEUU) y portugués (Ana, Brasil).
 * Cada uno dicta sus gastos, acepta las categorías sugeridas por su paquete y ve su mes.
 */
class WorldCategoriesFeatureTest {
    private inner class Person(
        val ownerId: String,
        locale: UserLocale,
        installed: List<LanguagePack>,
    ) {
        val wallets = InMemoryWalletRepository()
        private val ids = SequentialIdGenerator(ownerId)
        val clock = FixedClock(1_790_812_800_000L) // 1 de octubre de 2026
        private val device = FixedDevice("phone-$ownerId")
        private val packs = FakeInstalledPacks(installed)
        val ledger = LedgerContext(wallets, ids, clock, device)
        val categories = CategorizationContext(InMemoryCategorizationRepository(), wallets, packs, ids, clock, device)
        val ctx = DraftContext(InMemoryDraftRepository(), wallets, InMemoryWalletAliasRepository(), ids, clock, device, categories)
        val profiles = InMemoryCurrencyProfileRepository()
        private val capture = CaptureDraftUseCase(ctx, TextDraftInterpreter(packs, FixedRegionalSettings(locale, -240)))

        suspend fun cash(opening: Money) {
            assertIs<WalletOperationResult.WalletCreated>(
                CreateWalletUseCase(
                    ledger,
                )(ownerId, "Cash", opening.currency, WalletType.CASH, opening, occurredAtEpochMs = 1_790_000_000_000L),
            )
        }

        /** Dicta las frases, acepta las categorías sugeridas y confirma todo. */
        suspend fun dictateAndConfirm(vararg phrases: String): Map<String, CategoryRef?> {
            val drafts =
                phrases.map { phrase ->
                    clock.nowEpochMs += 60_000L
                    phrase to capture(ownerId, Evidence(EvidenceSource.VOICE, phrase)).single().id
                }
            val suggestions = GetDraftInboxUseCase(ctx)(ownerId).suggestions
            drafts.forEach { (_, id) -> assertIs<DraftResult.Ok>(ConfirmDraftUseCase(ctx, ledger)(id, suggestions[id]?.category)) }
            return drafts.associate { (phrase, id) -> phrase to suggestions[id]?.category }
        }

        suspend fun october() =
            SpendingByCategoryUseCase(
                categories,
                profiles,
            )(ownerId, 1_790_812_800_000L, 1_790_812_800_000L + 31L * 86_400_000L)
    }

    private val food = CategoryRef.Base(BaseCategory.FOOD)
    private val transport = CategoryRef.Base(BaseCategory.TRANSPORT)

    @Test
    fun `John en ingles`() =
        runTest {
            val john = Person("john", testLocale("en", "US"), emptyList())
            john.cash(Money.parseCanonical("200", Currency.USD))

            val suggested = john.dictateAndConfirm("lunch 12 usd", "uber 8 usd")
            assertEquals(mapOf("lunch 12 usd" to food, "uber 8 usd" to transport), suggested)

            val month = john.october()
            assertEquals(Currency.USD, month.currency)
            assertEquals(
                listOf(food to Money.parseCanonical("12.00", Currency.USD), transport to Money.parseCanonical("8.00", Currency.USD)),
                month.rows.map { it.category to it.consolidated },
            )
            assertEquals(Money.parseCanonical("20.00", Currency.USD), month.total)
        }

    @Test
    fun `Ana en portugues`() =
        runTest {
            val ana = Person("ana", testLocale("pt", "BR"), listOf(TestPacks.portuguese))
            val brl = Currency.of("BRL")
            ConfigureLocalCurrencyUseCase(
                ana.profiles,
            )("ana", brl, ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, brl, "5.00")))
            ana.cash(Money.parseCanonical("300", brl))

            val suggested = ana.dictateAndConfirm("paguei 15 reais de almoço", "uber 20 reais")
            assertEquals(mapOf("paguei 15 reais de almoço" to food, "uber 20 reais" to transport), suggested)

            val month = ana.october()
            assertEquals(brl, month.currency)
            assertEquals(
                listOf(transport to Money.parseCanonical("20.00", brl), food to Money.parseCanonical("15.00", brl)),
                month.rows.map { it.category to it.consolidated },
            )
            assertEquals(Money.parseCanonical("35.00", brl), month.total)
        }
}
