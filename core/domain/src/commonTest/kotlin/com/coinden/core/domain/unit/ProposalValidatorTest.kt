package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.ProposalValidator
import com.coinden.core.domain.model.interpret.ValidationIssue
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.BundleText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * El validador: lo único que separa "el modelo propuso algo" de "esto se puede dar por bueno".
 *
 * Es determinista y trata igual a quien proponga, reglas o modelo. Nunca corrige una propuesta:
 * dice qué no cuadra, y con eso el borrador va a revisar en vez de registrarse en silencio.
 */
class ProposalValidatorTest {
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val billetera = Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
    private val directory = WalletDirectory(listOf(bnb, billetera), mapOf("bnb" to "bnb"), defaultWalletId = "billetera")

    private val comprobante =
        BundleText(
            evidenceId = "e1",
            role = EvidenceRole.RECEIPT,
            source = EvidenceSource.PHOTO,
            text = "Pago QR otros bancos Importe Bs25.00 Nro. de transaccion: 400000001",
        )
    private val audio =
        BundleText("e2", EvidenceRole.CONTEXT, EvidenceSource.VOICE, "esto es de mi comida")

    private val textos = listOf(comprobante, audio)

    private fun gasto(
        monto: String,
        moneda: Currency = Currency.BOB,
        wallet: WalletRef? = WalletRef.ById("bnb"),
        nota: String? = null,
        referencia: String? = null,
    ) = MovementProposal.Expense(
        amount = ProposedAmount(monto, moneda),
        wallet = wallet,
        note = nota,
        reference = referencia,
        origins = mapOf(DraftField.AMOUNT to FieldOrigin.EVIDENCE),
    )

    // --- El monto tiene que estar en algún lado ---

    @Test
    fun `una propuesta respaldada por el comprobante pasa`() {
        val problemas = ProposalValidator.validate(gasto("25", nota = "comida"), textos, directory)

        assertEquals(emptyList(), problemas)
    }

    @Test
    fun `un monto que nadie dijo ni trae el comprobante se rechaza`() {
        val problemas = ProposalValidator.validate(gasto("45"), textos, directory)

        val problema = assertIs<ValidationIssue.AmountNotInText>(problemas.single())
        assertEquals("45", problema.amount)
    }

    @Test
    fun `el monto se reconoce aunque el comprobante lo escriba con decimales`() {
        // El comprobante dice "Bs25.00" y el modelo propone "25": es el mismo monto.
        assertEquals(emptyList(), ProposalValidator.validate(gasto("25"), textos, directory))
        assertEquals(emptyList(), ProposalValidator.validate(gasto("25.00"), textos, directory))
    }

    @Test
    fun `un monto dicho en el audio tambien respalda`() {
        val dicho = listOf(BundleText("e3", EvidenceRole.CONTEXT, EvidenceSource.VOICE, "le di 25 en efectivo"))

        assertEquals(emptyList(), ProposalValidator.validate(gasto("25", wallet = null), dicho, directory))
    }

    // --- La billetera y la moneda tienen que existir ---

    @Test
    fun `una billetera que no existe se rechaza`() {
        val problemas = ProposalValidator.validate(gasto("25", wallet = WalletRef.ById("inventada")), textos, directory)

        val problema = assertIs<ValidationIssue.UnknownWallet>(problemas.single())
        assertEquals("inventada", problema.wallet)
    }

    @Test
    fun `un alias que no corresponde a ninguna billetera se rechaza`() {
        val problemas = ProposalValidator.validate(gasto("25", wallet = WalletRef.ByHint("yape")), textos, directory)

        assertIs<ValidationIssue.UnknownWallet>(problemas.single())
    }

    @Test
    fun `un alias que si existe pasa`() {
        assertEquals(emptyList(), ProposalValidator.validate(gasto("25", wallet = WalletRef.ByHint("bnb")), textos, directory))
    }

    @Test
    fun `una categoria que la persona no tiene se rechaza`() {
        val problemas =
            ProposalValidator.validate(
                gasto("25"),
                textos,
                directory,
                categories = setOf("comida", "transporte"),
                proposedCategory = "criptomonedas",
            )

        val problema = assertIs<ValidationIssue.UnknownCategory>(problemas.single())
        assertEquals("criptomonedas", problema.category)
    }

    @Test
    fun `una categoria que si tiene pasa`() {
        val problemas =
            ProposalValidator.validate(gasto("25"), textos, directory, categories = setOf("comida"), proposedCategory = "comida")

        assertEquals(emptyList(), problemas)
    }

    // --- Los hechos del comprobante no se tocan ---

    @Test
    fun `cambiar el monto del comprobante se rechaza`() {
        val delComprobante = gasto("25")

        val problemas = ProposalValidator.validate(gasto("30"), textos, directory, receiptFacts = delComprobante)

        assertTrue(problemas.any { it is ValidationIssue.ChangedEvidenceFact && it.field == DraftField.AMOUNT })
    }

    @Test
    fun `cambiar la moneda del comprobante se rechaza`() {
        val delComprobante = gasto("25", moneda = Currency.BOB)

        val problemas = ProposalValidator.validate(gasto("25", moneda = Currency.USD), textos, directory, receiptFacts = delComprobante)

        val problema = problemas.filterIsInstance<ValidationIssue.ChangedEvidenceFact>().single()
        assertEquals(DraftField.CURRENCY, problema.field)
        assertEquals("BOB", problema.fromEvidence)
        assertEquals("USD", problema.proposed)
    }

    @Test
    fun `cambiar la referencia del comprobante se rechaza`() {
        val delComprobante = gasto("25", referencia = "400000001")

        val problemas =
            ProposalValidator.validate(gasto("25", referencia = "999"), textos, directory, receiptFacts = delComprobante)

        val problema = assertIs<ValidationIssue.ChangedReference>(problemas.single())
        assertEquals("400000001", problema.fromEvidence)
        assertEquals("999", problema.proposed)
    }

    @Test
    fun `completar lo que el comprobante no trae si esta permitido`() {
        // El comprobante no identifica billetera ni nota; el contexto las completa.
        val delComprobante = gasto("25", wallet = null)

        val problemas =
            ProposalValidator.validate(
                gasto("25", wallet = WalletRef.ById("bnb"), nota = "comida"),
                textos,
                directory,
                receiptFacts = delComprobante,
            )

        assertEquals(emptyList(), problemas)
    }

    @Test
    fun `la nota del comprobante si se puede mejorar`() {
        val delComprobante = gasto("25", nota = "POLLOS DEL VALLE")

        val problemas = ProposalValidator.validate(gasto("25", nota = "comida"), textos, directory, receiptFacts = delComprobante)

        assertEquals(emptyList(), problemas, "la nota no es un hecho protegido: lo dicho la explica mejor")
    }

    // --- El validador no distingue quién propuso ---

    @Test
    fun `trata igual una propuesta del modelo y una de las reglas`() {
        val mismaPropuesta = gasto("45")

        val comoDelModelo = ProposalValidator.validate(mismaPropuesta, textos, directory)
        val comoDeLasReglas = ProposalValidator.validate(mismaPropuesta, textos, directory)

        assertEquals(comoDelModelo, comoDeLasReglas)
    }

    @Test
    fun `sin textos no hay nada que respalde un monto`() {
        val problemas = ProposalValidator.validate(gasto("25"), emptyList(), directory)

        assertIs<ValidationIssue.AmountNotInText>(problemas.single())
    }

    @Test
    fun `una propuesta sin monto no se rechaza por el monto`() {
        val sinMonto = MovementProposal.Expense(amount = null, wallet = WalletRef.ById("bnb"))

        assertEquals(emptyList(), ProposalValidator.validate(sinMonto, textos, directory))
    }

    @Test
    fun `informa todos los problemas, no solo el primero`() {
        val problemas = ProposalValidator.validate(gasto("45", wallet = WalletRef.ById("inventada")), textos, directory)

        assertEquals(2, problemas.size)
        assertTrue(problemas.any { it is ValidationIssue.AmountNotInText })
        assertTrue(problemas.any { it is ValidationIssue.UnknownWallet })
    }
}
