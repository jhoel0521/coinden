package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.AnonymizedReceipts
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.GenericReceiptReader
import com.coinden.core.domain.model.interpret.InterpretContext
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.interpret.merged
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.support.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenericReceiptReaderTest {
    private val cash = Wallet("efectivo", "u", "Efectivo", Currency.BOB, WalletType.CASH)
    private val directory = WalletDirectory(listOf(cash), defaultWalletId = "efectivo")

    private fun ctx(
        pack: LanguagePack,
        locale: UserLocale,
        offsetMinutes: Int,
    ) = InterpretContext(pack, locale, offsetMinutes, directory)

    private fun utc(
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + hour * 3_600L + minute * 60L) * 1_000L

    @Test
    fun `Argentina - transferencia de un banco nunca visto`() {
        val es = listOf(TestPacks.english, TestPacks.spanish).merged()
        val proposal = GenericReceiptReader.read(AnonymizedReceipts.AR_UNKNOWN_TRANSFER, ctx(es, testLocale("es", "AR"), -180)).single()
        val expense = assertIs<MovementProposal.Expense>(proposal)
        assertEquals(ProposedAmount("8000.00", Currency.ARS), expense.amount)
        assertEquals("Kiosco Don Pepe", expense.note)
        assertEquals(utc(11, 12, 15), expense.occurredAtEpochMs) // 09:15 en UTC-3
        assertEquals(FieldOrigin.EVIDENCE, expense.origins[DraftField.AMOUNT])
        assertEquals(FieldOrigin.INFERRED, expense.origins[DraftField.CURRENCY]) // "$" resuelto por el pais
        assertEquals(FieldOrigin.INFERRED, expense.origins[DraftField.OCCURRED_AT]) // 11/10 ambiguo
        assertNull(expense.wallet) // la cuenta no es alias de ninguna billetera
    }

    @Test
    fun `Brasil - Pix leido con el paquete pt`() {
        val pt = listOf(TestPacks.english, TestPacks.portuguese).merged()
        val expense =
            assertIs<MovementProposal.Expense>(
                GenericReceiptReader.read(AnonymizedReceipts.BR_PIX, ctx(pt, testLocale("pt", "BR"), -180)).single(),
            )
        assertEquals(ProposedAmount("45.90", Currency.of("BRL")), expense.amount)
        assertEquals("Padaria Pão Quente", expense.note)
        assertEquals(utc(10, 17, 22), expense.occurredAtEpochMs)
        assertEquals(FieldOrigin.EVIDENCE, expense.origins[DraftField.AMOUNT])
        assertEquals(FieldOrigin.EVIDENCE, expense.origins[DraftField.CURRENCY])
    }

    @Test
    fun `Brasil - el mismo Pix sin el paquete pt se lee por el simbolo con menos confianza`() {
        val en = listOf(TestPacks.english).merged()
        val expense =
            assertIs<MovementProposal.Expense>(
                GenericReceiptReader.read(AnonymizedReceipts.BR_PIX, ctx(en, testLocale("pt", "BR"), -180)).single(),
            )
        assertEquals(ProposedAmount("45.90", Currency.of("BRL")), expense.amount)
        assertEquals(FieldOrigin.INFERRED, expense.origins[DraftField.AMOUNT])
        assertNull(expense.note) // "Para" no es una etiqueta conocida sin el paquete pt
    }

    @Test
    fun `EEUU - pago en ingles`() {
        val en = listOf(TestPacks.english).merged()
        val expense =
            assertIs<MovementProposal.Expense>(
                GenericReceiptReader.read(AnonymizedReceipts.US_PAYMENT, ctx(en, testLocale("en", "US"), -240)).single(),
            )
        assertEquals(ProposedAmount("25.00", Currency.USD), expense.amount)
        assertEquals("Corner Coffee Shop", expense.note)
        assertEquals(utc(10, 18, 22), expense.occurredAtEpochMs)
    }

    @Test
    fun `retiro generico hacia la billetera por defecto`() {
        val es = listOf(TestPacks.english, TestPacks.spanish).merged()
        val text = "Banco Desconocido\nRetiro de efectivo\nMonto Bs10.00"
        val transfer = assertIs<MovementProposal.Transfer>(GenericReceiptReader.read(text, ctx(es, testLocale("es", "BO"), -240)).single())
        assertEquals(ProposedAmount("10.00", Currency.BOB), transfer.out)
        assertEquals(WalletRef.ById("efectivo"), transfer.to)
        assertEquals(FieldOrigin.SUGGESTED, transfer.origins[DraftField.TO_WALLET])
    }

    @Test
    fun `reconoce un comprobante por sus etiquetas`() {
        val es = listOf(TestPacks.english, TestPacks.spanish).merged()
        assertTrue(GenericReceiptReader.looksLikeReceipt(AnonymizedReceipts.AR_UNKNOWN_TRANSFER, fromPhoto = false, es))
        assertTrue(!GenericReceiptReader.looksLikeReceipt("almuerzo 45 qr", fromPhoto = false, es))
        assertTrue(GenericReceiptReader.looksLikeReceipt("cualquier cosa", fromPhoto = true, es))
    }
}
