package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.CaptureStats
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.DiscardDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetCaptureStatsUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Escenario de Feature: la semana de Carlos (Bolivia) en el contador de capturas listas sin revisar. */
class CarlosCaptureStatsFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = DraftContext(InMemoryDraftRepository(), wallets, InMemoryWalletAliasRepository(), ids, clock, device)
    private val capture =
        CaptureDraftUseCase(
            ctx,
            TextDraftInterpreter(FakeInstalledPacks(listOf(LanguagePacks.SPANISH), ReceiptTemplates.all), FixedRegionalSettings()),
        )
    private val stats = GetCaptureStatsUseCase(ctx)

    private fun bolivia(
        day: Int,
        hour: Int,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L) * 1_000L

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun carlos() {
        clock.nowEpochMs = bolivia(1, 7)
        val create = CreateWalletUseCase(ledger)
        assertIs<WalletOperationResult.WalletCreated>(create("carlos", "Billetera", Currency.BOB, WalletType.CASH, bs("100")))
        val bnb =
            assertIs<WalletOperationResult.WalletCreated>(
                create("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, bs("500")),
            ).wallet.id
        SetWalletAliasesUseCase(ctx)("carlos", bnb, setOf("bnb"))
    }

    private suspend fun say(
        phrase: String,
        at: Long,
    ): Draft {
        clock.nowEpochMs = at
        return capture("carlos", Evidence(EvidenceSource.VOICE, phrase), at).single()
    }

    @Test
    fun `dos de tres capturas de la semana quedaron listas sin revisar`() =
        runTest {
            carlos()
            say("pagué 25 de almuerzo bnb", bolivia(14, 12))
            say("pagué 10 de taxi bnb", bolivia(15, 8))
            say("page 3 bs para trasmporte", bolivia(16, 9))

            assertEquals(CaptureStats(total = 3, readyAlone = 2), stats("carlos", sinceEpochMs = bolivia(10, 0)))
        }

    @Test
    fun `lo de hace diez dias y lo descartado no cuentan`() =
        runTest {
            carlos()
            say("pagué 25 de almuerzo bnb", bolivia(6, 12))
            val mistake = say("pagué 10 de error bnb", bolivia(15, 8))
            DiscardDraftUseCase(ctx)(mistake.id)
            say("page 3 bs para trasmporte", bolivia(16, 9))

            assertEquals(CaptureStats(total = 1, readyAlone = 0), stats("carlos", sinceEpochMs = bolivia(10, 0)))
        }
}
