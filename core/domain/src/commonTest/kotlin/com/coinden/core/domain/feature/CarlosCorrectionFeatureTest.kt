package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.CorrectionOrigin
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.CorrectMovementUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetActiveWalletBalancesUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: Carlos (Bolivia) corrige gastos ya confirmados. Nada cambia sin que él lo
 * apruebe; corregir el dinero anula el gasto anterior y registra el nuevo, y lo demás no toca el libro.
 */
class CarlosCorrectionFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val drafts = InMemoryDraftRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val packs = FakeInstalledPacks(listOf(TestPacks.spanish), TestPacks.templates)
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val categories = CategorizationContext(InMemoryCategorizationRepository(), wallets, packs, ids, clock, device)
    private val ctx = DraftContext(drafts, wallets, InMemoryWalletAliasRepository(), ids, clock, device, categories)
    private val capture = CaptureDraftUseCase(ctx, TextDraftInterpreter(packs, FixedRegionalSettings()))
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val correct = CorrectMovementUseCase(ctx, ledger)

    private lateinit var cash: String
    private lateinit var bnb: String

    private fun bolivia(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L + minute * 60L) * 1_000L

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private val food = CategoryRef.Base(BaseCategory.FOOD)
    private val leisure = CategoryRef.Base(BaseCategory.LEISURE)

    /** Carlos: "Billetera" en efectivo con 60 Bs y "BNB Bs" con 500 Bs (alias "bnb"). */
    private suspend fun carlos() {
        clock.nowEpochMs = bolivia(1, 7)
        val create = CreateWalletUseCase(ledger)
        cash =
            assertIs<WalletOperationResult.WalletCreated>(create("carlos", "Billetera", Currency.BOB, WalletType.CASH, bs("60"))).wallet.id
        bnb = assertIs<WalletOperationResult.WalletCreated>(create("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, bs("500"))).wallet.id
        SetWalletAliasesUseCase(ctx)("carlos", bnb, setOf("bnb"))
    }

    /** Dicta y confirma un gasto con su categoría. */
    private suspend fun confirmed(
        phrase: String,
        at: Long,
        category: CategoryRef? = null,
    ): Draft.Confirmed {
        clock.nowEpochMs = at
        val draft = capture("carlos", Evidence(EvidenceSource.VOICE, phrase), at).single()
        return assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(confirm(draft.id, category)).draft)
    }

    /** Lo que Carlos deja en el formulario de corrección. */
    private fun form(
        amount: String,
        wallet: String,
        at: Long,
        note: String?,
        reference: String? = null,
    ) = MovementProposal.Expense(ProposedAmount(amount, Currency.BOB), WalletRef.ById(wallet), at, note, reference = reference)

    private suspend fun balanceOf(walletId: String): Money =
        GetActiveWalletBalancesUseCase(wallets)("carlos")
            .single {
                it.walletId ==
                    walletId
            }.balance

    private fun expense(draft: Draft?) = assertIs<ReadyMovement.Expense>(assertIs<Draft.Confirmed>(draft).movement)

    @Test
    fun `el almuerzo era de 32 - se anula el de 35 y se registra el de 32 con su categoria`() =
        runTest {
            carlos()
            val lunch = confirmed("almuerzo 35 bnb", bolivia(12, 12, 40), food)
            clock.nowEpochMs = bolivia(17, 21)

            val result = correct(lunch.id, form("32", bnb, bolivia(12, 12, 40), "almuerzo"), food)

            val fixed = assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(result).draft)
            assertEquals(bs("32"), expense(fixed).amount)
            val book = wallets.eventsForWallet(bnb).drop(1)
            assertEquals(3, book.size)
            assertEquals(bs("35"), assertIs<LedgerEvent.Expense>(book[0]).amount)
            assertEquals(lunch.ledgerEventId, assertIs<LedgerEvent.Annulment>(book[1]).targetId)
            assertEquals(fixed.ledgerEventId, assertIs<LedgerEvent.Expense>(book[2]).id)
            assertEquals(bs("32"), (book[2] as LedgerEvent.Expense).amount)
            assertEquals(food, categories.current(fixed.ledgerEventId)?.category)
            assertEquals("almuerzo", categories.current(fixed.ledgerEventId)?.description)
            assertEquals(bs("468"), balanceOf(bnb))
        }

    @Test
    fun `el taxi era de otra billetera - Billetera recupera los 40 y BNB baja 40`() =
        runTest {
            carlos()
            val taxi = confirmed("pagué 40 de taxi", bolivia(15, 8))
            assertEquals(bs("20"), balanceOf(cash))

            assertIs<DraftResult.Ok>(correct(taxi.id, form("40", bnb, bolivia(15, 8), "taxi"), null))

            assertEquals(bs("60"), balanceOf(cash))
            assertEquals(bs("460"), balanceOf(bnb))
        }

    @Test
    fun `solo la referencia no toca el libro`() =
        runTest {
            carlos()
            val taxi = confirmed("pagué 40 de taxi bnb", bolivia(15, 8))
            val book = wallets.allEvents

            val result = correct(taxi.id, form("40", bnb, bolivia(15, 8), "taxi", reference = "400000777"), null)

            assertEquals("400000777", expense(assertIs<DraftResult.Ok>(result).draft).reference)
            assertEquals(book, wallets.allEvents)
            assertEquals(taxi.ledgerEventId, assertIs<Draft.Confirmed>(ctx.load(taxi.id)).ledgerEventId)
        }

    @Test
    fun `solo la categoria agrega una clasificacion y el libro no cambia`() =
        runTest {
            carlos()
            val power = confirmed("gasté 10 en power bnb", bolivia(14, 10, 40), food)
            val book = wallets.allEvents
            clock.nowEpochMs = bolivia(14, 21)

            assertIs<DraftResult.Ok>(correct(power.id, form("10", bnb, bolivia(14, 10, 40), "power"), leisure))

            assertEquals(book, wallets.allEvents)
            assertEquals(leisure, categories.current(power.ledgerEventId)?.category)
        }

    @Test
    fun `una correccion rechazada por saldo no aplica nada`() =
        runTest {
            carlos()
            val taxi = confirmed("pagué 10 de taxi", bolivia(15, 8))
            val book = wallets.allEvents

            val result = correct(taxi.id, form("90", cash, bolivia(15, 8), "taxi a la u", reference = "77001234"), null)

            val error = assertIs<DraftError.Wallet>(assertIs<DraftResult.Rejected>(result).error).error
            assertEquals(bs("30"), assertIs<WalletError.InsufficientFunds>(error).shortfall)
            assertEquals(book, wallets.allEvents)
            val unchanged = expense(ctx.load(taxi.id))
            assertEquals(bs("10"), unchanged.amount)
            assertEquals("taxi", unchanged.note)
            assertEquals(null, unchanged.reference)
        }

    @Test
    fun `reintentar con el mismo id de correccion no duplica nada`() =
        runTest {
            carlos()
            val lunch = confirmed("almuerzo 35 bnb", bolivia(12, 12, 40), food)
            val fixed = form("32", bnb, bolivia(12, 12, 40), "almuerzo")

            assertIs<DraftResult.Ok>(correct(lunch.id, fixed, food, correctionId = "corr-1"))
            val book = wallets.allEvents
            val history = drafts.allEvents

            assertIs<DraftResult.Ok>(correct(lunch.id, fixed, food, correctionId = "corr-1"))

            assertEquals(book, wallets.allEvents)
            assertEquals(history, drafts.allEvents)
            assertEquals(bs("468"), balanceOf(bnb))
        }

    @Test
    fun `otro dispositivo ve la misma correccion sin registrar nada mas`() =
        runTest {
            carlos()
            val lunch = confirmed("almuerzo 35 bnb", bolivia(12, 12, 40), food)
            assertIs<DraftResult.Ok>(correct(lunch.id, form("32", bnb, bolivia(12, 12, 40), "almuerzo"), food))
            val book = wallets.allEvents

            // La PC recibe el mismo historial: proyecta el gasto de 32 sin tocar el libro
            val pc = DraftContext(drafts, wallets, InMemoryWalletAliasRepository(), ids, clock, FixedDevice("pc-carlos"), categories)

            assertEquals(bs("32"), expense(pc.load(lunch.id)).amount)
            assertEquals(book, wallets.allEvents)
        }

    @Test
    fun `corregir a mano la nota guarda el origen manual y la nota anterior sigue en el historial`() =
        runTest {
            carlos()
            val taxi = confirmed("pagué 40 de taxi bnb", bolivia(15, 8))

            assertIs<DraftResult.Ok>(correct(taxi.id, form("40", bnb, bolivia(15, 8), "taxi a la u"), null))

            val events = drafts.eventsFor(taxi.id)
            val corrected = events.filterIsInstance<DraftEvent.Corrected>().single()
            assertEquals(CorrectionOrigin.MANUAL, corrected.origin)
            assertEquals("taxi a la u", corrected.movement.note)
            assertEquals(
                "taxi",
                events
                    .filterIsInstance<DraftEvent.Confirmed>()
                    .single()
                    .movement.note,
            )
        }

    @Test
    fun `sin cambios no se registra nada`() =
        runTest {
            carlos()
            val taxi = confirmed("pagué 40 de taxi bnb", bolivia(15, 8))
            val history = drafts.allEvents

            assertIs<DraftResult.Ok>(correct(taxi.id, form("40", bnb, bolivia(15, 8), "taxi"), null))

            assertEquals(history, drafts.allEvents)
        }

    @Test
    fun `solo se corrige un movimiento confirmado`() =
        runTest {
            carlos()
            clock.nowEpochMs = bolivia(15, 8)
            val pending = capture("carlos", Evidence(EvidenceSource.VOICE, "pagué 40 de taxi"), bolivia(15, 8)).single()

            val result = correct(pending.id, form("40", bnb, bolivia(15, 8), "taxi"), null)

            assertEquals(DraftError.NotConfirmed, assertIs<DraftResult.Rejected>(result).error)
        }
}
