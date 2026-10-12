package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * Escenario de Feature: Facundo (Argentina) usa la tarjeta y cuadra el banco por voz.
 *
 * - Una cena en la tarjeta de crédito deja la Visa en negativo sin aviso de faltante.
 * - "En Galicia tengo 820000" se confirma como un ajuste calculado contra el saldo real.
 */
class FacundoDraftFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val drafts = InMemoryDraftRepository()
    private val aliases = InMemoryWalletAliasRepository()
    private val ids = SequentialIdGenerator("facu")
    private val clock = FixedClock(at(20))
    private val device = FixedDevice("celular-facu")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = DraftContext(drafts, wallets, aliases, ids, clock, device)

    private fun ars(canonical: String) = Money.parseCanonical(canonical, Currency.ARS)

    @Test
    fun `cena en la tarjeta y ajuste del banco`() =
        runTest {
            val create = CreateWalletUseCase(ledger)
            val visa =
                assertIs<WalletOperationResult.WalletCreated>(
                    create("facundo", "Visa Galicia", Currency.ARS, WalletType.CREDIT, ars("0"), occurredAtEpochMs = at(8)),
                ).wallet
            val galicia =
                assertIs<WalletOperationResult.WalletCreated>(
                    create("facundo", "Banco Galicia", Currency.ARS, WalletType.BANK, ars("850000"), occurredAtEpochMs = at(8)),
                ).wallet
            val setAliases = SetWalletAliasesUseCase(ctx)
            setAliases("facundo", visa.id, setOf("visa", "tarjeta"))
            setAliases("facundo", galicia.id, setOf("galicia"))

            val interpreter =
                ScriptedInterpreter(
                    mapOf(
                        "cena 25000 con la tarjeta" to
                            listOf(MovementProposal.Expense(ProposedAmount("25000"), WalletRef.ByHint("tarjeta"), note = "cena")),
                        "en galicia tengo 820000" to
                            listOf(MovementProposal.AdjustTo(ProposedAmount("820000"), WalletRef.ByHint("Galicia"))),
                    ),
                )
            val capture = CaptureDraftUseCase(ctx, interpreter)
            val confirm = ConfirmDraftUseCase(ctx, ledger)

            val cena = capture("facundo", Evidence(EvidenceSource.VOICE, "cena 25000 con la tarjeta")).single()
            val visaProjection = GetDraftInboxUseCase(ctx)("facundo").projections.single { it.walletId == visa.id }
            assertEquals(ars("-25000.00"), visaProjection.projected)
            assertFalse(visaProjection.hasShortfall)
            assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(confirm(cena.id)).draft)

            clock.nowEpochMs = at(21)
            val ajuste = capture("facundo", Evidence(EvidenceSource.VOICE, "en galicia tengo 820000")).single()
            val confirmed = assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(confirm(ajuste.id)).draft)
            assertEquals(ars("-30000.00"), assertIs<LedgerEvent.Adjustment>(wallets.findEvent(confirmed.ledgerEventId)).amount)

            val status = GetWalletStatusUseCase(wallets)
            assertEquals(ars("-25000.00"), status(visa.id)!!.balance)
            assertEquals(ars("820000.00"), status(galicia.id)!!.balance)
        }
}
