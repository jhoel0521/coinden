package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.CaptureCoach
import com.coinden.core.domain.model.draft.CaptureTip
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftEvaluator
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.DraftHeader
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CaptureCoachTest {
    private val cash = Wallet("w-cash", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
    private val bnb = Wallet("w-bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val directory =
        WalletDirectory(listOf(cash, bnb), aliases = mapOf("bnb" to bnb.id, "banco nacional" to bnb.id), defaultWalletId = cash.id)
    private val header = DraftHeader("d1", "carlos", Evidence(EvidenceSource.VOICE, "gasté 10 en power"), 1_000L)

    private fun draft(proposal: MovementProposal) = DraftEvaluator.evaluate(header, proposal, directory)

    private val power =
        MovementProposal.Expense(
            ProposedAmount("10", Currency.BOB),
            WalletRef.ById(cash.id),
            note = "power",
            origins =
                mapOf(
                    DraftField.KIND to FieldOrigin.STATED,
                    DraftField.AMOUNT to FieldOrigin.STATED,
                    DraftField.WALLET to FieldOrigin.SUGGESTED,
                ),
        )

    @Test
    fun `una billetera sugerida pide decir la billetera`() {
        assertEquals(setOf(CaptureTip.SAY_WALLET), CaptureCoach.tipsFor(draft(power)))
    }

    @Test
    fun `un comprobante en foto o PDF no da consejo aunque la billetera quede sugerida`() {
        val photo = DraftHeader("d2", "carlos", Evidence(EvidenceSource.PHOTO, "Nota: StreamingPremium", attachmentRef = "a1"), 1_000L)
        assertEquals(emptySet(), CaptureCoach.tipsFor(DraftEvaluator.evaluate(photo, power, directory)))
    }

    @Test
    fun `un texto de comprobante de varias lineas no da consejo`() {
        val text = listOf("Banco ECONOMICO", "Pago QR otros bancos", "Importe", "Bs10.00").joinToString("\n")
        val receipt = DraftHeader("d3", "carlos", Evidence(EvidenceSource.TEXT, text), 1_000L)
        assertEquals(emptySet(), CaptureCoach.tipsFor(DraftEvaluator.evaluate(receipt, power, directory)))
    }

    @Test
    fun `sin monto pide decir el monto`() {
        val lunch = MovementProposal.Expense(wallet = WalletRef.ById(bnb.id), note = "almuerzo")
        assertEquals(setOf(CaptureTip.SAY_AMOUNT), CaptureCoach.tipsFor(draft(lunch)))
    }

    @Test
    fun `sin billetera ni monto pide los dos`() {
        assertEquals(
            setOf(CaptureTip.SAY_WALLET, CaptureTip.SAY_AMOUNT),
            CaptureCoach.tipsFor(draft(MovementProposal.Expense(note = "algo"))),
        )
    }

    @Test
    fun `completo, confirmado o sin propuesta no pide nada`() {
        val said = power.copy(origins = power.origins + (DraftField.WALLET to FieldOrigin.STATED))
        assertEquals(emptySet(), CaptureCoach.tipsFor(draft(said)))
        val confirmed =
            Draft.Confirmed(
                header,
                ReadyMovement.Expense(Money.parseCanonical("10", Currency.BOB), cash.id, 1_000L, "power"),
                "draft:d1",
            )
        assertEquals(emptySet(), CaptureCoach.tipsFor(confirmed))
        val photo = header.copy(evidence = Evidence(EvidenceSource.PHOTO, attachmentRef = "foto-1"))
        assertEquals(emptySet(), CaptureCoach.tipsFor(Draft.Captured(photo)))
    }

    @Test
    fun `una frase que no se entendio pide decir el monto`() {
        assertEquals(
            setOf(CaptureTip.SAY_AMOUNT),
            CaptureCoach.tipsFor(Draft.Captured(header.copy(evidence = Evidence(EvidenceSource.VOICE, "almuerzo bnb")))),
        )
    }

    @Test
    fun `la palabra de la billetera es su alias mas corto o una palabra de su nombre`() {
        val hints = mapOf("billetera" to cash.id, "bnb" to bnb.id)
        assertEquals("bnb", CaptureCoach.walletWord(bnb.id, directory, hints))
        assertEquals("billetera", CaptureCoach.walletWord(cash.id, directory, hints))
        assertNull(CaptureCoach.walletWord(cash.id, directory, emptyMap()))
    }

    @Test
    fun `el ejemplo usa la billetera sugerida y si no hay la por defecto`() {
        assertEquals(cash.id, CaptureCoach.exampleWalletId(draft(power), directory))
        assertEquals(cash.id, CaptureCoach.exampleWalletId(draft(MovementProposal.Expense(note = "algo")), directory))
    }
}
