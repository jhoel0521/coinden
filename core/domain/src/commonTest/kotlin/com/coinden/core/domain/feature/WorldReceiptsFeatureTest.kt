package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.testLocale
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.ReviseDraftUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Escenario de Feature: el mismo intérprete en tres idiomas y tres países, con bancos que nunca vio.
 *
 * - Facundo (es-AR): comprobante desconocido en pesos con "$" y fecha ambigua.
 * - John (en-US): comprobante en inglés sin cuenta y la frase "I pay 3 dollars for transportation".
 * - Ana (pt-BR): comprobante Pix y "paguei 15 reais de almoço" con el paquete "pt", y el mismo
 *   Pix sin el paquete, leído solo por su símbolo.
 */
class WorldReceiptsFeatureTest {
    /** Un usuario con su propio dispositivo, locale y paquetes instalados. */
    private inner class Person(
        val ownerId: String,
        locale: UserLocale,
        offsetMinutes: Int,
        installed: List<LanguagePack>,
    ) {
        val wallets = InMemoryWalletRepository()
        val aliases = InMemoryWalletAliasRepository()
        private val ids = SequentialIdGenerator(ownerId)
        private val clock = FixedClock(1_790_812_800_000L) // octubre de 2026
        private val device = FixedDevice("phone-$ownerId")
        val ledger = LedgerContext(wallets, ids, clock, device)
        val ctx = DraftContext(InMemoryDraftRepository(), wallets, aliases, ids, clock, device)
        val packs = FakeInstalledPacks(installed)
        val capture = CaptureDraftUseCase(ctx, TextDraftInterpreter(packs, FixedRegionalSettings(locale, offsetMinutes)))
        val confirm = ConfirmDraftUseCase(ctx, ledger)
        val revise = ReviseDraftUseCase(ctx)
        val status = GetWalletStatusUseCase(wallets)

        suspend fun wallet(
            name: String,
            opening: Money,
            type: WalletType,
        ): Wallet =
            assertIs<WalletOperationResult.WalletCreated>(
                CreateWalletUseCase(ledger)(ownerId, name, opening.currency, type, opening, occurredAtEpochMs = 1_790_000_000_000L),
            ).wallet

        suspend fun captureOne(evidence: Evidence): Draft = capture(ownerId, evidence).single()
    }

    @Test
    fun `Facundo en Argentina con un banco desconocido`() =
        runTest {
            val facundo = Person("facundo", testLocale("es", "AR"), -180, listOf(LanguagePacks.SPANISH))
            val galicia = facundo.wallet("Banco Galicia", Money.parseCanonical("50000", Currency.ARS), WalletType.BANK)
            facundo.wallet("Efectivo", Money.parseCanonical("1000", Currency.ARS), WalletType.CASH)
            SetWalletAliasesUseCase(facundo.ctx)("facundo", galicia.id, setOf("4444555566"))

            val draft = assertIs<Draft.Ready>(facundo.captureOne(Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.AR_UNKNOWN_TRANSFER)))
            assertEquals(Confidence.MEDIUM, draft.confidence) // "$" y la fecha 11/10 se resolvieron por el locale
            assertIs<DraftResult.Ok>(facundo.confirm(draft.id))
            assertEquals(Money.parseCanonical("42000.00", Currency.ARS), facundo.status(galicia.id)!!.balance)
        }

    @Test
    fun `John en EEUU con un comprobante en ingles y una frase dictada`() =
        runTest {
            val john = Person("john", testLocale("en", "US"), -240, emptyList())
            val chase = john.wallet("Chase", Money.parseCanonical("500", Currency.USD), WalletType.BANK)
            val cash = john.wallet("Cash", Money.parseCanonical("40", Currency.USD), WalletType.CASH)

            // El comprobante no dice la cuenta: queda Incompleto y John elige Chase en la noche
            val receipt = assertIs<Draft.Incomplete>(john.captureOne(Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.US_PAYMENT)))
            assertEquals(setOf(DraftField.WALLET), receipt.missing)
            val proposal = receipt.proposal as MovementProposal.Expense
            assertEquals("Corner Coffee Shop", proposal.note)
            assertIs<DraftResult.Ok>(john.revise(receipt.id, proposal.copy(wallet = WalletRef.ById(chase.id))))
            assertIs<DraftResult.Ok>(john.confirm(receipt.id))

            val phrase = assertIs<Draft.Ready>(john.captureOne(Evidence(EvidenceSource.VOICE, "I pay 3 dollars for transportation")))
            assertEquals(Confidence.LOW, phrase.confidence) // nadie dijo de qué billetera salió
            assertIs<DraftResult.Ok>(john.confirm(phrase.id))

            assertEquals(Money.parseCanonical("475.00", Currency.USD), john.status(chase.id)!!.balance)
            assertEquals(Money.parseCanonical("37.00", Currency.USD), john.status(cash.id)!!.balance)
        }

    @Test
    fun `Ana en Brasil con Pix y una frase en portugues`() =
        runTest {
            val ana = Person("ana", testLocale("pt", "BR"), -180, listOf(LanguagePacks.PORTUGUESE))
            val nubank = ana.wallet("Nubank", Money.parseCanonical("300", Currency.of("BRL")), WalletType.BANK)
            val carteira = ana.wallet("Carteira", Money.parseCanonical("50", Currency.of("BRL")), WalletType.CASH)

            val pix = assertIs<Draft.Incomplete>(ana.captureOne(Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BR_PIX)))
            val pixProposal = pix.proposal as MovementProposal.Expense
            assertEquals(ProposedAmount("45.90", Currency.of("BRL")), pixProposal.amount)
            assertEquals("Padaria Pão Quente", pixProposal.note)
            assertIs<DraftResult.Ok>(ana.revise(pix.id, pixProposal.copy(wallet = WalletRef.ById(nubank.id))))
            assertIs<DraftResult.Ok>(ana.confirm(pix.id))

            val lunch = assertIs<Draft.Ready>(ana.captureOne(Evidence(EvidenceSource.TEXT, "paguei 15 reais de almoço")))
            assertEquals("almoço", ((lunch.proposal) as MovementProposal.Expense).note)
            assertIs<DraftResult.Ok>(ana.confirm(lunch.id))

            assertEquals(Money.parseCanonical("254.10", Currency.of("BRL")), ana.status(nubank.id)!!.balance)
            assertEquals(Money.parseCanonical("35.00", Currency.of("BRL")), ana.status(carteira.id)!!.balance)

            // El mismo Pix sin el paquete "pt": solo el símbolo R$ se reconoce, con menos respaldo
            ana.packs.packs = emptyList()
            val withoutPack = assertIs<Draft.Incomplete>(ana.captureOne(Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BR_PIX)))
            val bare = withoutPack.proposal as MovementProposal.Expense
            assertEquals(ProposedAmount("45.90", Currency.of("BRL")), bare.amount)
            assertNull(bare.note)
            assertEquals(Confidence.MEDIUM, withoutPack.confidence)
        }
}
