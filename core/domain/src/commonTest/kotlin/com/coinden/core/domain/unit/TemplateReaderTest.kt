package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.AnonymizedReceipts
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.draft.confidence
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.InterpretContext
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.TemplateReader
import com.coinden.core.domain.model.interpret.merged
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.support.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TemplateReaderTest {
    private val account = AnonymizedReceipts.CARLOS_BE_ACCOUNT
    private val directory =
        WalletDirectory(
            wallets =
                listOf(
                    Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH),
                    Wallet("yape", "carlos", "Yape", Currency.BOB, WalletType.DIGITAL),
                    Wallet("be", "carlos", "BE Bs", Currency.BOB, WalletType.BANK),
                ),
            aliases = mapOf("yape" to "yape", account to "be"),
            defaultWalletId = "billetera",
        )
    private val ctx =
        InterpretContext(listOf(TestPacks.english, TestPacks.spanish).merged(), testLocale("es", "BO"), -240, directory)

    /** Instante UTC de una hora local de Bolivia (UTC-4) de octubre de 2026. */
    private fun boliviaTime(
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L + minute * 60L + second) * 1_000L

    private fun read(
        template: ReceiptTemplate,
        text: String,
    ): MovementProposal {
        assertTrue(TemplateReader.matches(template, text), "la plantilla ${template.id} deberia coincidir")
        return TemplateReader.read(template, text, ctx).single()
    }

    private val evidence =
        mapOf(
            DraftField.KIND to FieldOrigin.EVIDENCE,
            DraftField.AMOUNT to FieldOrigin.EVIDENCE,
            DraftField.CURRENCY to FieldOrigin.EVIDENCE,
            DraftField.WALLET to FieldOrigin.EVIDENCE,
            DraftField.OCCURRED_AT to FieldOrigin.EVIDENCE,
        )

    @Test
    fun `Yape - gasto desde la billetera del emisor con el destinatario como nota`() {
        val expected =
            MovementProposal.Expense(
                ProposedAmount("36", Currency.BOB),
                WalletRef.ByHint("yape"),
                boliviaTime(3, 12, 54),
                "Mariela Quispe Rojas",
                evidence,
                reference = "900000001",
                // Yape mueve plata de una cuenta, no es efectivo: lo dice la plantilla.
                paymentMethod = PaymentMethod.TRANSFER,
            )
        assertEquals(expected, read(TestPacks.template("bo-yape-payment"), AnonymizedReceipts.YAPE_PAYMENT))
    }

    @Test
    fun `Banco Economico - pago QR con la cuenta de origen resuelta por alias`() {
        val expected =
            MovementProposal.Expense(
                ProposedAmount("25.00", Currency.BOB),
                WalletRef.ByHint(account),
                boliviaTime(10, 14, 22, 50),
                "POLLOS DEL VALLE",
                evidence,
                reference = "400000001",
                // Un pago QR es una transferencia: lo prueba el comprobante.
                paymentMethod = PaymentMethod.TRANSFER,
            )
        val proposal = read(TestPacks.template("bo-be-qr-payment"), AnonymizedReceipts.BE_QR_CHICKEN)
        assertEquals(expected, proposal)
        assertEquals(com.coinden.core.domain.model.draft.Confidence.HIGH, proposal.confidence)

        val pharmacy = read(TestPacks.template("bo-be-qr-payment"), AnonymizedReceipts.BE_QR_PHARMACY) as MovementProposal.Expense
        assertEquals(ProposedAmount("31.70", Currency.BOB), pharmacy.amount)
        assertEquals("FARMACIA EJEMPLO S.A.", pharmacy.note)
        assertEquals(boliviaTime(11, 10, 4, 28), pharmacy.occurredAtEpochMs)
    }

    @Test
    fun `Banco Economico - debito por compra con signo negativo en el texto`() {
        val proposal =
            assertIs<MovementProposal.Expense>(read(TestPacks.template("bo-be-ecommerce-debit"), AnonymizedReceipts.BE_ECOMMERCE_DEBIT))
        assertEquals(ProposedAmount("118.71", Currency.BOB), proposal.amount)
        assertEquals(WalletRef.ByHint(account), proposal.wallet)
        assertEquals("StreamingPremium650-2530000US", proposal.note)
        assertEquals(boliviaTime(9, 18, 49, 22), proposal.occurredAtEpochMs)
    }

    @Test
    fun `Banco Economico - retiro de efectivo hacia la billetera por defecto`() {
        val transfer =
            assertIs<MovementProposal.Transfer>(read(TestPacks.template("bo-be-cash-withdrawal"), AnonymizedReceipts.BE_CASH_WITHDRAWAL))
        assertEquals(ProposedAmount("10.00", Currency.BOB), transfer.out)
        assertEquals(WalletRef.ByHint(account), transfer.from)
        assertEquals(WalletRef.ById("billetera"), transfer.to)
        assertEquals(FieldOrigin.SUGGESTED, transfer.origins[DraftField.TO_WALLET])
        assertEquals(boliviaTime(11, 19, 3, 38), transfer.occurredAtEpochMs)
    }

    @Test
    fun `cuenta de origen sin alias deja la billetera faltante`() {
        val noAlias = ctx.copy(directory = directory.copy(aliases = mapOf("yape" to "yape")))
        val proposal = TemplateReader.read(TestPacks.template("bo-be-qr-payment"), AnonymizedReceipts.BE_QR_CHICKEN, noAlias).single()
        assertEquals(null, (proposal as MovementProposal.Expense).wallet)
    }

    @Test
    fun `una plantilla no coincide con comprobantes de otro formato`() {
        assertFalse(TemplateReader.matches(TestPacks.template("bo-yape-payment"), AnonymizedReceipts.BE_QR_CHICKEN))
        assertFalse(TemplateReader.matches(TestPacks.template("bo-be-qr-payment"), AnonymizedReceipts.BE_CASH_WITHDRAWAL))
    }
}
