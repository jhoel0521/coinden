package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.AnonymizedReceipts
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.InstalledPacksPort
import com.coinden.core.domain.port.output.RegionalSettingsPort
import com.coinden.core.domain.support.testLocale
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** El número de transacción se lee de los comprobantes (con plantilla o sin ella) y nunca de una frase. */
class ReceiptReferenceTest {
    private val directory =
        WalletDirectory(
            wallets =
                listOf(
                    Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH),
                    Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK),
                ),
            defaultWalletId = "billetera",
        )

    private suspend fun reference(
        text: String,
        language: String = "es",
        country: String = "BO",
        source: EvidenceSource = EvidenceSource.TEXT,
    ): String? {
        val packs =
            object : InstalledPacksPort {
                override fun languagePacks(): List<LanguagePack> = listOf(LanguagePacks.SPANISH, LanguagePacks.PORTUGUESE)

                override fun receiptTemplates(): List<ReceiptTemplate> = ReceiptTemplates.all
            }
        val regional =
            object : RegionalSettingsPort {
                override fun settings() = RegionalSettings(testLocale(language, country), utcOffsetMinutes = -240)
            }
        val proposal = TextDraftInterpreter(packs, regional).interpret(Evidence(source, text), directory).single()
        return when (proposal) {
            is MovementProposal.Expense -> proposal.reference
            is MovementProposal.Income -> proposal.reference
            is MovementProposal.Transfer -> proposal.reference
            else -> error("propuesta sin referencia posible: $proposal")
        }
    }

    @Test
    fun `pago QR del Banco Economico con su plantilla`() =
        runTest { assertEquals("400000001", reference(AnonymizedReceipts.BE_QR_CHICKEN)) }

    @Test
    fun `Yape sin dos puntos despues de la etiqueta`() = runTest { assertEquals("900000001", reference(AnonymizedReceipts.YAPE_PAYMENT)) }

    @Test
    fun `debito con espacio antes de los dos puntos`() =
        runTest { assertEquals("1000000001", reference(AnonymizedReceipts.BE_ECOMMERCE_DEBIT)) }

    @Test
    fun `retiro con cedula, numero de pago y codigo de autorizacion toma el numero de transaccion`() =
        runTest { assertEquals("8000001", reference(AnonymizedReceipts.BE_CASH_WITHDRAWAL)) }

    @Test
    fun `banco desconocido de Argentina con Operacion N°`() =
        runTest { assertEquals("123456", reference(AnonymizedReceipts.AR_UNKNOWN_TRANSFER, "es", "AR")) }

    @Test
    fun `Pix de Brasil con ID da transacao`() =
        runTest { assertEquals("E00000000000000000001", reference(AnonymizedReceipts.BR_PIX, "pt", "BR")) }

    @Test
    fun `pago de EEUU con Confirmation`() = runTest { assertEquals("000111222", reference(AnonymizedReceipts.US_PAYMENT, "en", "US")) }

    @Test
    fun `una frase escrita o dictada no trae referencia`() =
        runTest {
            assertNull(reference("almuerzo 45 bnb"))
            assertNull(reference("almuerzo 45 bnb", source = EvidenceSource.VOICE))
        }

    @Test
    fun `una etiqueta sin numero no es referencia`() =
        runTest {
            val text =
                """
                Pago realizado
                Importe Bs 20.00
                Beneficiario TIENDA EJEMPLO
                Referencia: almuerzo de equipo
                """.trimIndent()
            assertNull(reference(text))
        }
}
