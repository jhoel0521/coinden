package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.NonMovementIntent
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.ReviseDraftUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Escenario de Feature: un día de gastos hormiga de Carlos (Bolivia).
 *
 * Durante el día captura en segundos (texto, foto y voz) sin pensar en saldos. En la noche abre
 * la bandeja, ve el aviso de faltante, descubre que olvidó el cajero, completa la foto del ticket,
 * convierte el "pago recurrente" en un gasto real y confirma todo bajo las reglas estrictas del libro.
 */
class CarlosDraftDayFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val drafts = InMemoryDraftRepository()
    private val aliases = InMemoryWalletAliasRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock(at(7))
    private val device = FixedDevice("celular-carlos")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = DraftContext(drafts, wallets, aliases, ids, clock, device)

    private lateinit var billetera: Wallet
    private lateinit var bnb: Wallet
    private lateinit var colchon: Wallet
    private lateinit var meru: Wallet

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private fun usd(canonical: String) = Money.parseCanonical(canonical, Currency.USD)

    private fun script(): Map<String, List<MovementProposal>> =
        mapOf(
            "almuerzo 45 efectivo" to
                listOf(MovementProposal.Expense(ProposedAmount("45"), WalletRef.ByHint("efectivo"), note = "almuerzo")),
            "micro 3 efectivo" to
                listOf(MovementProposal.Expense(ProposedAmount("3"), WalletRef.ByHint("efectivo"), note = "micro")),
            "foto-ticket" to listOf(MovementProposal.Unknown("HIPERMAXI")),
            "créame un pago recurrente de netflix de 12 dólares" to
                listOf(MovementProposal.NotAMovement(NonMovementIntent.RECURRING_PAYMENT, "netflix 12 usd")),
            "vendí 50$ a 11.88" to
                listOf(
                    MovementProposal.Transfer(
                        out = ProposedAmount("50", Currency.USD),
                        from = WalletRef.ByHint("colchon"),
                        into = ProposedAmount("594"),
                        to = WalletRef.ByHint("efectivo"),
                    ),
                ),
            "saqué 200 del cajero a las 11" to
                listOf(
                    MovementProposal.Transfer(
                        out = ProposedAmount("200"),
                        from = WalletRef.ByHint("qr"),
                        to = WalletRef.ByHint("efectivo"),
                        occurredAtEpochMs = at(11),
                    ),
                ),
        )

    private val capture = CaptureDraftUseCase(ctx, ScriptedInterpreter(script()))
    private val revise = ReviseDraftUseCase(ctx)
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val inbox = GetDraftInboxUseCase(ctx)
    private val status = GetWalletStatusUseCase(wallets)

    private suspend fun captureAt(
        time: Long,
        evidence: Evidence,
    ): Draft {
        clock.nowEpochMs = time
        return capture("carlos", evidence, time).single()
    }

    private fun confirmed(result: DraftResult) = assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(result).draft)

    @Test
    fun `un dia de gastos hormiga capturados rapido y confirmados en la noche`() =
        runTest {
            // Preparación: sus bolsillos y cómo los nombra al hablar
            val create = CreateWalletUseCase(ledger)

            suspend fun wallet(
                name: String,
                opening: Money,
                type: WalletType,
            ) = assertIs<WalletOperationResult.WalletCreated>(
                create("carlos", name, opening.currency, type, opening, occurredAtEpochMs = at(7)),
            ).wallet
            billetera = wallet("Billetera", bs("10"), WalletType.CASH)
            bnb = wallet("BNB Bs", bs("2000"), WalletType.BANK)
            colchon = wallet("Dólares colchón", usd("500"), WalletType.CASH)
            meru = wallet("Meru", usd("100"), WalletType.DIGITAL)
            val setAliases = SetWalletAliasesUseCase(ctx)
            setAliases("carlos", billetera.id, setOf("efectivo", "billetera"))
            setAliases("carlos", bnb.id, setOf("qr"))
            setAliases("carlos", colchon.id, setOf("colchon"))
            setAliases("carlos", meru.id, setOf("meru"))

            // Durante el día: capturas de 2 segundos
            val almuerzo = captureAt(at(12, 58), Evidence(EvidenceSource.TEXT, "almuerzo 45 efectivo"))
            val micro = captureAt(at(13, 40), Evidence(EvidenceSource.TEXT, "micro 3 efectivo"))
            val ticket = captureAt(at(14, 10), Evidence(EvidenceSource.PHOTO, attachmentRef = "foto-ticket"))

            // 15:00: la bandeja ya avisa del faltante, sin bloquear nada
            val midday = inbox("carlos").projections.single { it.walletId == billetera.id }
            assertEquals(bs("-38.00"), midday.projected)
            assertTrue(midday.hasShortfall)

            val recurrente = captureAt(at(17), Evidence(EvidenceSource.VOICE, "créame un pago recurrente de netflix de 12 dólares"))
            val cambio = captureAt(at(17, 10), Evidence(EvidenceSource.TEXT, "vendí 50$ a 11.88"))

            assertIs<Draft.Ready>(almuerzo)
            assertIs<Draft.Ready>(micro)
            assertIs<Draft.Incomplete>(ticket)
            assertIs<Draft.Unsupported>(recurrente)
            assertIs<Draft.Ready>(cambio)

            // 21:00: la revisión de la noche
            clock.nowEpochMs = at(21)
            assertEquals(5, inbox("carlos").pending.size)

            // El almuerzo no pasa: a las 12:58 solo tenía 10 Bs en efectivo
            val rejection = assertIs<DraftResult.Rejected>(confirm(almuerzo.id))
            assertEquals(DraftError.Wallet(WalletError.InsufficientFunds(billetera.id, bs("35.00"), at(12, 58))), rejection.error)
            assertIs<Draft.Ready>(ctx.load(almuerzo.id))

            // ...hasta que recuerda el cajero de las 11:00
            clock.nowEpochMs = at(21, 1)
            val cajero = capture("carlos", Evidence(EvidenceSource.TEXT, "saqué 200 del cajero a las 11")).single()
            confirmed(confirm(cajero.id))
            confirmed(confirm(almuerzo.id))
            confirmed(confirm(micro.id))
            val venta = confirmed(confirm(cambio.id))
            assertEquals("draft:${cambio.id}", venta.ledgerEventId)

            // Completa la foto del ticket y convierte el "recurrente" en el gasto que sí pagó
            clock.nowEpochMs = at(21, 5)
            assertIs<DraftResult.Ok>(
                revise(ticket.id, MovementProposal.Expense(ProposedAmount("18.50"), WalletRef.ByHint("efectivo"), note = "HIPERMAXI")),
            )
            assertIs<DraftResult.Ok>(
                revise(
                    recurrente.id,
                    MovementProposal.Expense(ProposedAmount("12", Currency.USD), WalletRef.ByHint("meru"), note = "netflix"),
                ),
            )
            confirmed(confirm(ticket.id))
            confirmed(confirm(recurrente.id))

            // Resultado: bandeja vacía y saldos exactos
            assertTrue(inbox("carlos").pending.isEmpty())
            // 10 + 200 - 45 - 3 + 594 - 18.50 = 737.50
            assertEquals(bs("737.50"), status(billetera.id)!!.balance)
            assertEquals(bs("1800.00"), status(bnb.id)!!.balance)
            assertEquals(usd("450.00"), status(colchon.id)!!.balance)
            assertEquals(usd("88.00"), status(meru.id)!!.balance)

            // El libro solo contiene lo confirmado: 4 saldos iniciales + 6 movimientos
            val ledgerEvents = listOf(billetera, bnb, colchon, meru).flatMap { wallets.eventsForWallet(it.id) }.distinctBy { it.id }
            assertEquals(10, ledgerEvents.size)
            assertEquals(6, ledgerEvents.count { it.id.startsWith("draft:") })
        }
}
