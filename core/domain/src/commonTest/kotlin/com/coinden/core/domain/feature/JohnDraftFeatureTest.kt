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
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: John (USA) dicta un pago en BTC y lo confirma en dos dispositivos.
 *
 * - El gasto por voz de 0.001 BTC se registra en satoshis exactos.
 * - El celular y la PC tienen el mismo borrador (sincronizado) y ambos lo confirman antes de
 *   volver a sincronizar: el libro termina con un único movimiento.
 */
class JohnDraftFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("john")
    private val clock = FixedClock(at(9))
    private val aliases = InMemoryWalletAliasRepository()

    private val phoneDrafts = InMemoryDraftRepository()
    private val phone = DraftContext(phoneDrafts, wallets, aliases, ids, clock, FixedDevice("celular-john"))
    private val phoneLedger = LedgerContext(wallets, ids, clock, FixedDevice("celular-john"))

    private val pcDrafts = InMemoryDraftRepository()
    private val pc = DraftContext(pcDrafts, wallets, aliases, ids, clock, FixedDevice("pc-john"))
    private val pcLedger = LedgerContext(wallets, ids, clock, FixedDevice("pc-john"))

    private fun sats(units: Long) = Money.ofMinorUnits(units, Currency.BTC)

    @Test
    fun `pago en BTC por voz confirmado una sola vez aunque se confirme en dos dispositivos`() =
        runTest {
            val ledgerWallet =
                assertIs<WalletOperationResult.WalletCreated>(
                    CreateWalletUseCase(
                        phoneLedger,
                    )("john", "Ledger", Currency.BTC, WalletType.CRYPTO, sats(5_000_000), occurredAtEpochMs = at(8)),
                ).wallet
            SetWalletAliasesUseCase(phone)("john", ledgerWallet.id, setOf("ledger"))

            val voice = "paid 0.001 bitcoin from ledger"
            val interpreter =
                ScriptedInterpreter(
                    mapOf(voice to listOf(MovementProposal.Expense(ProposedAmount("0.001", Currency.BTC), WalletRef.ByHint("ledger")))),
                )
            val draft = CaptureDraftUseCase(phone, interpreter)("john", Evidence(EvidenceSource.VOICE, voice), at(9)).single()
            assertIs<Draft.Ready>(draft)

            // La PC recibe el borrador por sincronización (antes de que nadie lo confirme)
            phoneDrafts.allEvents.forEach { pcDrafts.appendEvent(it) }

            // Ambos confirman sin haber vuelto a sincronizar; el libro es compartido tras la fusión
            clock.nowEpochMs = at(20)
            assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(ConfirmDraftUseCase(phone, phoneLedger)(draft.id)).draft)
            clock.nowEpochMs = at(21)
            assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(ConfirmDraftUseCase(pc, pcLedger)(draft.id)).draft)

            val expenses = wallets.eventsForWallet(ledgerWallet.id).filterIsInstance<LedgerEvent.Expense>()
            assertEquals(1, expenses.size)
            assertEquals("draft:${draft.id}", expenses.single().id)
            assertEquals(sats(100_000), expenses.single().amount)
            assertEquals(sats(4_900_000), GetWalletStatusUseCase(wallets)(ledgerWallet.id)!!.balance)
        }
}
