package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmReadyDraftsUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: la noche de Carlos (Bolivia). La bandeja separa lo que ya está seguro
 * ("Listos") de lo que hay que mirar ("Revisar"), y los listos se confirman de una vez.
 */
class CarlosReviewInboxFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val packs = FakeInstalledPacks(listOf(LanguagePacks.SPANISH), ReceiptTemplates.all)
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val categories = CategorizationContext(InMemoryCategorizationRepository(), wallets, packs, ids, clock, device)
    private val ctx = DraftContext(InMemoryDraftRepository(), wallets, InMemoryWalletAliasRepository(), ids, clock, device, categories)
    private val capture = CaptureDraftUseCase(ctx, TextDraftInterpreter(packs, FixedRegionalSettings()))
    private val inbox = GetDraftInboxUseCase(ctx)
    private val confirmReady = ConfirmReadyDraftsUseCase(ctx, ledger)

    private fun bolivia(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L + minute * 60L) * 1_000L

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    /** "Billetera" (efectivo, 10 Bs, alias "efectivo") y "BNB Bs" (500 Bs, alias "bnb"). */
    private suspend fun carlos() {
        clock.nowEpochMs = bolivia(1, 7)
        val create = CreateWalletUseCase(ledger)
        val cash =
            assertIs<WalletOperationResult.WalletCreated>(
                create("carlos", "Billetera", Currency.BOB, WalletType.CASH, bs("10")),
            ).wallet.id
        val bnb =
            assertIs<WalletOperationResult.WalletCreated>(
                create("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, bs("500")),
            ).wallet.id
        SetWalletAliasesUseCase(ctx)("carlos", cash, setOf("efectivo"))
        SetWalletAliasesUseCase(ctx)("carlos", bnb, setOf("bnb"))
    }

    private suspend fun say(
        phrase: String,
        at: Long,
    ): Draft {
        clock.nowEpochMs = at
        return capture("carlos", Evidence(EvidenceSource.VOICE, phrase), at).single()
    }

    private suspend fun batch() = inbox("carlos", utcOffsetMinutes = -240).batchReady

    @Test
    fun `lo seguro va en listos y lo dudoso o con pregunta va en revisar`() =
        runTest {
            carlos()
            val lunch = say("pagué 25 de almuerzo bnb", bolivia(16, 12))
            val transport = say("page 3 bs para trasmporte", bolivia(16, 13))
            val taxi = say("pagué 12 de taxi bnb", bolivia(16, 18))
            val again = say("pagué 12 de taxi bnb", bolivia(16, 19))

            assertIs<Draft.Ready>(lunch)
            assertEquals(listOf(lunch.id, taxi.id), batch())
            // El segundo taxi pregunta si es el mismo que el primero; el transporte tiene la billetera sugerida
            val pending = inbox("carlos", utcOffsetMinutes = -240).pending.map { it.id }
            assertEquals(setOf(lunch.id, transport.id, taxi.id, again.id), pending.toSet())
        }

    @Test
    fun `sin verbo, con monto y billetera dicha, el gasto va en listos como enseña la app`() =
        runTest {
            carlos()
            val lunch = say("almuerzo 25 bnb", bolivia(16, 12))
            val taxi = say("taxi 10 bnb", bolivia(16, 18))

            assertIs<Draft.Ready>(lunch)
            assertIs<Draft.Ready>(taxi)
            assertEquals(listOf(lunch.id, taxi.id), batch())
        }

    @Test
    fun `confirmar los listos registra cada uno con su categoria sugerida y deja el rechazado con su motivo`() =
        runTest {
            carlos()
            val lunch = say("pagué 25 de almuerzo bnb", bolivia(16, 12))
            val taxi = say("pagué 10 de taxi bnb", bolivia(16, 18))
            val bread = say("pagué 12 de pan efectivo", bolivia(16, 19))
            assertEquals(listOf(lunch.id, taxi.id, bread.id), batch())

            val results = confirmReady(batch())

            assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(results.getValue(lunch.id)).draft)
            assertIs<DraftResult.Ok>(results.getValue(taxi.id))
            val rejected = assertIs<DraftError.Wallet>(assertIs<DraftResult.Rejected>(results.getValue(bread.id)).error).error
            assertEquals(bs("2"), assertIs<WalletError.InsufficientFunds>(rejected).shortfall)
            assertEquals(listOf(bread.id), inbox("carlos").pending.map { it.id })
            val confirmedLunch = assertIs<Draft.Confirmed>(ctx.load(lunch.id))
            assertEquals(CategoryRef.Base(BaseCategory.FOOD), categories.current(confirmedLunch.ledgerEventId)?.category)
        }
}
