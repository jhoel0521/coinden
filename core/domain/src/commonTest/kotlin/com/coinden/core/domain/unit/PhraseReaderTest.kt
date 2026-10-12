package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.InterpretContext
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.PhraseReader
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.interpret.merged
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals

class PhraseReaderTest {
    private val billetera = Wallet("billetera", "u", "Billetera", Currency.BOB, WalletType.CASH)
    private val bnb = Wallet("bnb", "u", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val colchon = Wallet("colchon", "u", "Dólares colchón", Currency.USD, WalletType.CASH)
    private val directory = WalletDirectory(listOf(billetera, bnb, colchon), mapOf("qr" to "bnb"), defaultWalletId = "billetera")

    private val en = listOf(LanguagePacks.ENGLISH).merged()
    private val es = listOf(LanguagePacks.ENGLISH, LanguagePacks.SPANISH).merged()
    private val pt = listOf(LanguagePacks.ENGLISH, LanguagePacks.PORTUGUESE).merged()

    private fun read(
        text: String,
        pack: LanguagePack = es,
        locale: UserLocale = UserLocale("es", "BO"),
    ) = PhraseReader.read(text, InterpretContext(pack, locale, -240, directory))

    private val suggestedCash = WalletRef.ById("billetera")

    // --- Español (frases reales de Carlos) ---

    @Test
    fun `page 3 bs para trasmporte`() {
        val expected =
            MovementProposal.Expense(
                ProposedAmount("3", Currency.BOB),
                suggestedCash,
                note = "trasmporte",
                origins =
                    mapOf(
                        // "page" ~ "pague"
                        DraftField.KIND to FieldOrigin.INFERRED,
                        DraftField.AMOUNT to FieldOrigin.STATED,
                        DraftField.CURRENCY to FieldOrigin.STATED,
                        DraftField.WALLET to FieldOrigin.SUGGESTED,
                    ),
            )
        assertEquals(listOf<MovementProposal>(expected), read("page 3 bs para trasmporte"))
    }

    @Test
    fun `gaste 2 para un semillita`() {
        val expected =
            MovementProposal.Expense(
                ProposedAmount("2"),
                suggestedCash,
                note = "un semillita",
                origins =
                    mapOf(
                        DraftField.KIND to FieldOrigin.STATED,
                        DraftField.AMOUNT to FieldOrigin.STATED,
                        DraftField.WALLET to FieldOrigin.SUGGESTED,
                    ),
            )
        assertEquals(listOf<MovementProposal>(expected), read("gaste 2 para un semillita"))
    }

    @Test
    fun `almuerzo 45 qr usa el alias y la nota son las palabras sueltas`() {
        val expected =
            MovementProposal.Expense(
                ProposedAmount("45"),
                WalletRef.ByHint("qr"),
                note = "almuerzo",
                origins =
                    mapOf(
                        DraftField.KIND to FieldOrigin.INFERRED,
                        DraftField.AMOUNT to FieldOrigin.STATED,
                        DraftField.WALLET to FieldOrigin.STATED,
                    ),
            )
        assertEquals(listOf<MovementProposal>(expected), read("almuerzo 45 qr"))
    }

    @Test
    fun `coma decimal escrita por el usuario`() {
        val proposal = read("almuerzo 45,50").single() as MovementProposal.Expense
        assertEquals(ProposedAmount("45.50"), proposal.amount)
    }

    @Test
    fun `dos movimientos en una frase`() {
        val proposals = read("almuerzo 45 y micro 3").map { it as MovementProposal.Expense }
        assertEquals(listOf("45", "3"), proposals.map { it.amount!!.value })
        assertEquals(listOf("almuerzo", "micro"), proposals.map { it.note })
    }

    @Test
    fun `cambio de moneda dictado`() {
        val transfer = read("vendi 50 dolares a 11.88").single() as MovementProposal.Transfer
        assertEquals(ProposedAmount("50", Currency.USD), transfer.out)
        assertEquals(ProposedAmount("594.00", Currency.BOB), transfer.into)
        assertEquals(suggestedCash, transfer.to)
        assertEquals(null, transfer.from)
        assertEquals(FieldOrigin.STATED, transfer.origins[DraftField.KIND])
        assertEquals(FieldOrigin.INFERRED, transfer.origins[DraftField.TO_AMOUNT])
        assertEquals(FieldOrigin.SUGGESTED, transfer.origins[DraftField.TO_WALLET])
    }

    @Test
    fun `frase en espanol sin el paquete es`() {
        val proposal = read("gaste 2 para un semillita", pack = en).single() as MovementProposal.Expense
        assertEquals(ProposedAmount("2"), proposal.amount)
        assertEquals(FieldOrigin.INFERRED, proposal.origins[DraftField.KIND])
    }

    // --- Inglés (paquete base) ---

    @Test
    fun `I pay 3 dollars for transportation`() {
        val expected =
            MovementProposal.Expense(
                ProposedAmount("3", Currency.USD),
                suggestedCash,
                note = "transportation",
                origins =
                    mapOf(
                        DraftField.KIND to FieldOrigin.STATED,
                        DraftField.AMOUNT to FieldOrigin.STATED,
                        DraftField.CURRENCY to FieldOrigin.STATED,
                        DraftField.WALLET to FieldOrigin.SUGGESTED,
                    ),
            )
        assertEquals(
            listOf<MovementProposal>(expected),
            read("I pay 3 dollars for transportation", pack = en, locale = UserLocale("en", "US")),
        )
    }

    @Test
    fun `paid 12 usd for lunch`() {
        val proposal = read("paid 12 usd for lunch", pack = en, locale = UserLocale("en", "US")).single() as MovementProposal.Expense
        assertEquals(ProposedAmount("12", Currency.USD), proposal.amount)
        assertEquals("lunch", proposal.note)
    }

    @Test
    fun `two expenses joined with and`() {
        val proposals = read("coffee 4 usd and taxi 15 usd", pack = en, locale = UserLocale("en", "US"))
        assertEquals(listOf("coffee", "taxi"), proposals.map { (it as MovementProposal.Expense).note })
    }

    // --- Portugués ---

    @Test
    fun `paguei 15 reais de almoco`() {
        val proposal = read("paguei 15 reais de almoço", pack = pt, locale = UserLocale("pt", "BR")).single() as MovementProposal.Expense
        assertEquals(ProposedAmount("15", Currency.of("BRL")), proposal.amount)
        assertEquals("almoço", proposal.note)
        assertEquals(FieldOrigin.STATED, proposal.origins[DraftField.KIND])
    }

    @Test
    fun `recebi 200 reais e gastei 30 no mercado`() {
        val proposals = read("recebi 200 reais e gastei 30 reais com mercado", pack = pt, locale = UserLocale("pt", "BR"))
        assertEquals(2, proposals.size)
        assertEquals(ProposedAmount("200", Currency.of("BRL")), (proposals[0] as MovementProposal.Income).amount)
        assertEquals("mercado", (proposals[1] as MovementProposal.Expense).note)
    }

    // --- Sin número no hay propuesta ---

    @Test
    fun `texto sin monto no propone nada`() {
        assertEquals(emptyList(), read("hola que tal"))
    }
}
