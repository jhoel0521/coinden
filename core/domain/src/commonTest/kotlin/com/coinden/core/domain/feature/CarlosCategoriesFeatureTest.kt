package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.usecase.ConfigureLocalCurrencyUseCase
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.category.SpendingByCategoryUseCase
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Escenario de Feature: el mes de Carlos responde "¿en qué se me fue el dinero?".
 *
 * El intérprete real lee sus comprobantes (anonimizados) y frases; el paquete "es" sugiere
 * categorías desde el primer día; Carlos completa lo que nadie sabía ("POLLOS DEL VALLE",
 * "trasmporte", un Yape a una persona) y la app lo aprende. El reporte de octubre deja fuera el
 * retiro del cajero, que no es un gasto.
 */
class CarlosCategoriesFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val packs = FakeInstalledPacks(listOf(TestPacks.spanish), TestPacks.templates)
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val categories = CategorizationContext(InMemoryCategorizationRepository(), wallets, packs, ids, clock, device)
    private val ctx = DraftContext(InMemoryDraftRepository(), wallets, InMemoryWalletAliasRepository(), ids, clock, device, categories)
    private val capture = CaptureDraftUseCase(ctx, TextDraftInterpreter(packs, FixedRegionalSettings()))
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val inbox = GetDraftInboxUseCase(ctx)
    private val profiles = InMemoryCurrencyProfileRepository()
    private val report = SpendingByCategoryUseCase(categories, profiles)

    private fun bolivia(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L + minute * 60L) * 1_000L

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private fun base(category: BaseCategory) = CategoryRef.Base(category)

    @Test
    fun `un mes categorizado que aprende de las correcciones`() =
        runTest {
            clock.nowEpochMs = bolivia(1, 8)
            ConfigureLocalCurrencyUseCase(profiles)(
                "carlos",
                Currency.BOB,
                ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20")),
            )
            val create = CreateWalletUseCase(ledger)

            suspend fun wallet(
                name: String,
                opening: String,
                type: WalletType,
            ) = assertIs<WalletOperationResult.WalletCreated>(create("carlos", name, Currency.BOB, type, bs(opening))).wallet.id
            wallet("Billetera", "100", WalletType.CASH)
            val yape = wallet("Yape", "100", WalletType.DIGITAL)
            val be = wallet("BE Bs", "1000", WalletType.BANK)
            SetWalletAliasesUseCase(ctx)("carlos", yape, setOf("yape"))
            SetWalletAliasesUseCase(ctx)("carlos", be, setOf(AnonymizedReceipts.CARLOS_BE_ACCOUNT))

            val captures =
                listOf(
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.YAPE_PAYMENT),
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_QR_CHICKEN),
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_ECOMMERCE_DEBIT),
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_QR_PHARMACY),
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_CASH_WITHDRAWAL),
                    Evidence(EvidenceSource.VOICE, "page 3 bs para trasmporte"),
                    Evidence(EvidenceSource.TEXT, "gaste 2 para un semillita"),
                    Evidence(EvidenceSource.TEXT, "almuerzo 45"),
                )
            val drafts =
                captures.mapIndexed { i, evidence ->
                    clock.nowEpochMs = bolivia(11, 21, i)
                    capture("carlos", evidence).single().id
                }
            val (yapeDraft, chicken, streaming, pharmacy, withdrawal) = drafts
            val (transport, seed, lunch) = drafts.drop(5)

            // En la noche: lo que el paquete ya sabe, y lo que nadie sabe todavía
            val suggestions = inbox("carlos").suggestions
            assertEquals(base(BaseCategory.SUBSCRIPTIONS), suggestions[streaming]?.category)
            assertEquals(base(BaseCategory.HEALTH), suggestions[pharmacy]?.category)
            assertEquals(base(BaseCategory.FOOD), suggestions[seed]?.category)
            assertEquals(base(BaseCategory.FOOD), suggestions[lunch]?.category)
            assertEquals(Confidence.MEDIUM, suggestions[lunch]?.confidence)
            listOf(yapeDraft, chicken, transport, withdrawal).forEach { assertNull(suggestions[it], "sin sugerencia: $it") }

            // Confirma aceptando sugerencias y completando lo desconocido
            clock.nowEpochMs = bolivia(11, 22)
            val chosen =
                mapOf(
                    yapeDraft to base(BaseCategory.PEOPLE),
                    chicken to base(BaseCategory.FOOD),
                    transport to base(BaseCategory.TRANSPORT),
                ) + listOf(streaming, pharmacy, seed, lunch).associateWith { suggestions.getValue(it).category }
            inbox("carlos").pending.forEach { draft ->
                assertIs<DraftResult.Ok>(confirm(draft.id, chosen[draft.id]), "confirmar ${draft.id}")
            }

            // La próxima vez, lo aprendido se sugiere solo y con confianza ALTA
            clock.nowEpochMs = bolivia(12, 13)
            val again = capture("carlos", Evidence(EvidenceSource.VOICE, "page 3 bs para trasmporte")).single().id
            val chickenAgain = capture("carlos", Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_QR_CHICKEN)).single().id
            val learned = inbox("carlos").suggestions
            assertEquals(base(BaseCategory.TRANSPORT), learned[again]?.category)
            assertEquals(Confidence.HIGH, learned[again]?.confidence)
            assertEquals(base(BaseCategory.FOOD), learned[chickenAgain]?.category)
            assertEquals(Confidence.HIGH, learned[chickenAgain]?.confidence)

            // ¿En qué se me fue el dinero en octubre?
            val october = report("carlos", bolivia(1, 0), bolivia(1, 0) + 31L * 86_400_000L)
            assertEquals(
                listOf(
                    base(BaseCategory.SUBSCRIPTIONS) to bs("118.71"),
                    // comida: 25 + 2 + 45
                    base(BaseCategory.FOOD) to bs("72.00"),
                    base(BaseCategory.PEOPLE) to bs("36.00"),
                    base(BaseCategory.HEALTH) to bs("31.70"),
                    base(BaseCategory.TRANSPORT) to bs("3.00"),
                ),
                october.rows.map { it.category to it.consolidated },
            )
            assertEquals(bs("261.41"), october.total) // el retiro del cajero no es gasto
        }
}
