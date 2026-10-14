package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.FixedClock
import com.coinden.core.domain.feature.FixedDevice
import com.coinden.core.domain.feature.InMemoryDraftRepository
import com.coinden.core.domain.feature.InMemoryWalletAliasRepository
import com.coinden.core.domain.feature.InMemoryWalletRepository
import com.coinden.core.domain.feature.ScriptedInterpreter
import com.coinden.core.domain.feature.SequentialIdGenerator
import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.NonMovementIntent
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DiscardDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.ReviseDraftUseCase
import com.coinden.core.domain.usecase.draft.SetDefaultWalletUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DraftUseCasesTest {
    private val wallets = InMemoryWalletRepository()
    private val drafts = InMemoryDraftRepository()
    private val aliases = InMemoryWalletAliasRepository()
    private val ids = SequentialIdGenerator()
    private val clock = FixedClock(at(12, 58))
    private val device = FixedDevice("celular")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = DraftContext(drafts, wallets, aliases, ids, clock, device)

    private val text = mutableMapOf<String, List<MovementProposal>>()
    private val interpreter = ScriptedInterpreter(text, failingKeys = setOf("audio-roto"))

    private val createWallet = CreateWalletUseCase(ledger)
    private val setAliases = SetWalletAliasesUseCase(ctx)
    private val capture = CaptureDraftUseCase(ctx, interpreter)
    private val revise = ReviseDraftUseCase(ctx)
    private val discard = DiscardDraftUseCase(ctx)
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val inbox = GetDraftInboxUseCase(ctx)
    private val status = GetWalletStatusUseCase(wallets)

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun wallet(
        name: String,
        opening: Money,
        type: WalletType = WalletType.CASH,
    ): Wallet =
        assertIs<WalletOperationResult.WalletCreated>(
            createWallet("carlos", name, opening.currency, type, opening, occurredAtEpochMs = at(7)),
        ).wallet

    private fun textEvidence(value: String) = Evidence(EvidenceSource.TEXT, value)

    private fun expense(
        amount: String,
        wallet: WalletRef,
        note: String? = null,
    ) = MovementProposal.Expense(ProposedAmount(amount), wallet, note = note)

    private fun ok(result: DraftResult) = assertIs<DraftResult.Ok>(result).draft

    private fun rejected(result: DraftResult) = assertIs<DraftResult.Rejected>(result).error

    // --- 3.1 repositorio append-only ---

    @Test
    fun `el historial de borradores es append-only`() =
        runTest {
            val event = DraftEvent.Captured("e-1", "d-1", "carlos", "celular", 0, 0, textEvidence("x"))
            drafts.appendEvent(event)
            assertFailsWith<IllegalStateException> { drafts.appendEvent(event.copy(evidence = textEvidence("y"))) }
            assertEquals(listOf<DraftEvent>(event), drafts.allEvents)
        }

    // --- 4.1 alias ---

    @Test
    fun `alias normalizados y sin duplicar entre billeteras`() =
        runTest {
            val bnb = wallet("BNB Bs", bs("100"), WalletType.BANK)
            val billetera = wallet("Billetera", bs("10"))
            assertNull(setAliases("carlos", bnb.id, setOf(" BNB ", "banco")))
            assertEquals(mapOf("bnb" to bnb.id, "banco" to bnb.id), aliases.aliasesFor("carlos"))
            assertEquals(DraftError.AliasAlreadyUsed("bnb", bnb.id), setAliases("carlos", billetera.id, setOf("Bnb")))
            assertEquals(DraftError.NotFound, setAliases("otro", bnb.id, setOf("x")))
        }

    // --- 4.2 captura ---

    @Test
    fun `captura entendida queda Lista con su evidencia`() =
        runTest {
            val bnb = wallet("BNB Bs", bs("100"), WalletType.BANK)
            setAliases("carlos", bnb.id, setOf("bnb"))
            text["almuerzo 45 bnb"] = listOf(expense("45", WalletRef.ByHint("bnb"), "almuerzo"))

            val draft = assertIs<Draft.Ready>(capture("carlos", textEvidence("almuerzo 45 bnb")).single())
            assertEquals(at(12, 58), draft.header.capturedAtEpochMs)
            assertEquals("almuerzo 45 bnb", draft.header.evidence.text)
            assertEquals(setOf("bnb"), interpreter.lastDirectory!!.aliases.keys)
        }

    @Test
    fun `foto que nadie entiende e interprete que falla quedan Capturados`() =
        runTest {
            val photo = capture("carlos", Evidence(EvidenceSource.PHOTO, attachmentRef = "foto-borrosa")).single()
            assertIs<Draft.Captured>(photo)
            assertEquals("foto-borrosa", photo.header.evidence.attachmentRef)

            val broken = capture("carlos", Evidence(EvidenceSource.VOICE, text = "audio-roto")).single()
            assertIs<Draft.Captured>(broken)
            assertEquals(2, inbox("carlos").pending.size)
        }

    @Test
    fun `dos movimientos en una frase crean dos borradores con la misma evidencia`() =
        runTest {
            val billetera = wallet("Billetera", bs("100"))
            text["almuerzo 45 y micro 3"] =
                listOf(expense("45", WalletRef.ById(billetera.id)), expense("3", WalletRef.ById(billetera.id)))

            val created = capture("carlos", textEvidence("almuerzo 45 y micro 3"))
            assertEquals(2, created.size)
            assertTrue(created.all { it is Draft.Ready })
            assertEquals(1, created.map { it.header.evidence }.distinct().size)
            assertEquals(2, created.map { it.id }.distinct().size)
        }

    // --- 4.3 revisar y descartar ---

    @Test
    fun `completar un incompleto y convertir un no soportado`() =
        runTest {
            val billetera = wallet("Billetera", bs("100"))
            text["HIPERMAXI"] = listOf(MovementProposal.Unknown("HIPERMAXI"))
            text["recurrente netflix"] = listOf(MovementProposal.NotAMovement(NonMovementIntent.RECURRING_PAYMENT, "netflix"))

            val photo = assertIs<Draft.Incomplete>(capture("carlos", textEvidence("HIPERMAXI")).single())
            assertEquals(setOf(DraftField.KIND), photo.missing)
            val recurring = assertIs<Draft.Unsupported>(capture("carlos", textEvidence("recurrente netflix")).single())

            clock.nowEpochMs = at(21) // la revisión de la noche
            assertIs<Draft.Ready>(ok(revise(photo.id, expense("18.50", WalletRef.ById(billetera.id), "HIPERMAXI"))))

            assertIs<Draft.Ready>(ok(revise(recurring.id, expense("12", WalletRef.ById(billetera.id)))))
        }

    @Test
    fun `descartar y estados terminales`() =
        runTest {
            val draft = capture("carlos", textEvidence("error")).single()
            val discarded = assertIs<Draft.Discarded>(ok(discard(draft.id)))
            assertEquals("error", discarded.header.evidence.text)
            assertEquals(DraftError.TerminalState, rejected(discard(draft.id)))
            assertEquals(DraftError.TerminalState, rejected(revise(draft.id, MovementProposal.Unknown())))
            assertEquals(DraftError.NotFound, rejected(discard("no-existe")))
        }

    // --- 4.5 confirmar ---

    @Test
    fun `confirmar el almuerzo usa la hora de captura y el id derivado`() =
        runTest {
            val bnb = wallet("BNB Bs", bs("100"), WalletType.BANK)
            text["almuerzo"] = listOf(expense("45", WalletRef.ById(bnb.id)))
            val draft = capture("carlos", textEvidence("almuerzo")).single()

            clock.nowEpochMs = at(21)
            val confirmed = assertIs<Draft.Confirmed>(ok(confirm(draft.id)))
            assertEquals("draft:${draft.id}", confirmed.ledgerEventId)

            val event = assertIs<LedgerEvent.Expense>(wallets.findEvent(confirmed.ledgerEventId))
            assertEquals(at(12, 58), event.occurredAtEpochMs)
            assertEquals(at(21), event.recordedAtEpochMs)
            assertEquals(bs("55.00"), status(bnb.id)!!.balance)
            assertEquals(DraftError.TerminalState, rejected(confirm(draft.id)))
        }

    @Test
    fun `rechazo por saldo deja el borrador Listo con el motivo`() =
        runTest {
            val billetera = wallet("Billetera", bs("10"))
            text["almuerzo"] = listOf(expense("45", WalletRef.ById(billetera.id)))
            val draft = capture("carlos", textEvidence("almuerzo")).single()

            val error = assertIs<DraftError.Wallet>(rejected(confirm(draft.id)))
            assertEquals(WalletError.InsufficientFunds(billetera.id, bs("35.00"), at(12, 58)), error.error)
            assertIs<Draft.Ready>(ctx.load(draft.id))
            assertEquals(bs("10.00"), status(billetera.id)!!.balance)
        }

    @Test
    fun `confirmar un incompleto indica los faltantes`() =
        runTest {
            text["algo"] = listOf(MovementProposal.Expense(ProposedAmount("5")))
            val draft = capture("carlos", textEvidence("algo")).single()
            assertEquals(DraftError.NotReady(setOf(DraftField.WALLET), emptySet()), rejected(confirm(draft.id)))
        }

    @Test
    fun `ajuste a 140 desde 147 y nada que ajustar`() =
        runTest {
            val billetera = wallet("Billetera", bs("147"))
            text["tengo 140"] = listOf(MovementProposal.AdjustTo(ProposedAmount("140"), WalletRef.ById(billetera.id)))

            val draft = capture("carlos", textEvidence("tengo 140")).single()
            val confirmed = assertIs<Draft.Confirmed>(ok(confirm(draft.id)))
            assertEquals(bs("-7.00"), assertIs<LedgerEvent.Adjustment>(wallets.findEvent(confirmed.ledgerEventId)).amount)
            assertEquals(bs("140.00"), status(billetera.id)!!.balance)

            val again = capture("carlos", textEvidence("tengo 140")).single()
            assertEquals(DraftError.NothingToAdjust, rejected(confirm(again.id)))
            assertIs<Draft.Ready>(ctx.load(again.id))
        }

    @Test
    fun `cambio de moneda confirmado`() =
        runTest {
            val colchon = wallet("Dólares colchón", Money.parseCanonical("500", Currency.USD))
            val billetera = wallet("Billetera", bs("10"))
            text["vendi 50"] =
                listOf(
                    MovementProposal.Transfer(
                        ProposedAmount("50", Currency.USD),
                        WalletRef.ById(colchon.id),
                        ProposedAmount("594"),
                        WalletRef.ById(billetera.id),
                    ),
                )
            val draft = capture("carlos", textEvidence("vendi 50")).single()
            assertIs<Draft.Confirmed>(ok(confirm(draft.id)))
            assertEquals(Money.parseCanonical("450", Currency.USD), status(colchon.id)!!.balance)
            assertEquals(bs("604.00"), status(billetera.id)!!.balance)
        }

    @Test
    fun `confirmacion idempotente cuando el libro ya tiene el evento derivado`() =
        runTest {
            val bnb = wallet("BNB Bs", bs("100"), WalletType.BANK)
            text["almuerzo"] = listOf(expense("45", WalletRef.ById(bnb.id)))
            val draft = capture("carlos", textEvidence("almuerzo")).single()

            // Otro dispositivo ya confirmó este borrador y su movimiento llegó al libro por sincronización.
            wallets.appendEvent(LedgerEvent.Expense("draft:${draft.id}", "pc", at(12, 58), at(21), bnb.id, bs("45")))

            assertIs<Draft.Confirmed>(ok(confirm(draft.id)))
            assertEquals(2, wallets.eventsForWallet(bnb.id).size) // saldo inicial + un solo gasto
            assertEquals(bs("55.00"), status(bnb.id)!!.balance)
        }

    // --- 4.6 bandeja ---

    @Test
    fun `bandeja con pendientes y aviso de faltante proyectado`() =
        runTest {
            val billetera = wallet("Billetera", bs("10"))
            val visa = wallet("Visa", bs("0"), WalletType.CREDIT)
            text["micro"] = listOf(expense("3", WalletRef.ById(billetera.id)))
            text["almuerzo"] = listOf(expense("45", WalletRef.ById(billetera.id)))
            text["cena"] = listOf(expense("80", WalletRef.ById(visa.id)))
            text["regalo"] = listOf(expense("1", WalletRef.ById(billetera.id)))

            capture("carlos", textEvidence("micro"))
            capture("carlos", textEvidence("almuerzo"))
            capture("carlos", textEvidence("cena"))
            val gift = capture("carlos", textEvidence("regalo")).single()
            discard(gift.id)

            val result = inbox("carlos")
            assertEquals(3, result.pending.size)
            val cash = result.projections.single { it.walletId == billetera.id }
            assertEquals(bs("10.00"), cash.current)
            assertEquals(bs("-38.00"), cash.projected)
            assertTrue(cash.hasShortfall)
            val credit = result.projections.single { it.walletId == visa.id }
            assertEquals(bs("-80.00"), credit.projected)
            assertTrue(!credit.hasShortfall)
        }

    @Test
    fun `la bandeja muestra primero lo dudoso`() =
        runTest {
            val billetera = wallet("Billetera", bs("100"))
            text["comprobante"] =
                listOf(
                    MovementProposal.Expense(
                        ProposedAmount("25", Currency.BOB),
                        WalletRef.ById(billetera.id),
                        origins = mapOf(DraftField.AMOUNT to FieldOrigin.EVIDENCE, DraftField.WALLET to FieldOrigin.EVIDENCE),
                    ),
                )
            text["semillita"] =
                listOf(
                    MovementProposal.Expense(
                        ProposedAmount("2"),
                        WalletRef.ById(billetera.id),
                        note = "un semillita",
                        origins = mapOf(DraftField.WALLET to FieldOrigin.SUGGESTED),
                    ),
                )
            text["algo"] =
                listOf(
                    MovementProposal.Expense(
                        ProposedAmount("5"),
                        WalletRef.ById(billetera.id),
                        origins = mapOf(DraftField.KIND to FieldOrigin.INFERRED),
                    ),
                )

            clock.nowEpochMs = at(10)
            val receipt = capture("carlos", textEvidence("comprobante"), at(10)).single()
            clock.nowEpochMs = at(12)
            val medium = capture("carlos", textEvidence("algo"), at(12)).single()
            clock.nowEpochMs = at(15)
            val low = capture("carlos", textEvidence("semillita"), at(15)).single()

            assertEquals(listOf(low.id, medium.id, receipt.id), inbox("carlos").pending.map { it.id })
            assertEquals(listOf(Confidence.LOW, Confidence.MEDIUM, Confidence.HIGH), inbox("carlos").pending.map { it.confidence })
        }

    @Test
    fun `billetera por defecto - unica de efectivo, eleccion explicita y sin defecto`() =
        runTest {
            val billetera = wallet("Billetera", bs("10"))
            val meru = wallet("Meru", Money.parseCanonical("100", Currency.USD), WalletType.DIGITAL)
            assertEquals(billetera.id, ctx.directory("carlos").defaultWalletId)

            val setDefault = SetDefaultWalletUseCase(ctx)
            assertNull(setDefault("carlos", meru.id))
            assertEquals(meru.id, ctx.directory("carlos").defaultWalletId)
            assertEquals(DraftError.NotFound, setDefault("otro", meru.id))

            assertNull(setDefault("carlos", null))
            wallet("Monedero", bs("5"))
            assertNull(ctx.directory("carlos").defaultWalletId)
        }
}
