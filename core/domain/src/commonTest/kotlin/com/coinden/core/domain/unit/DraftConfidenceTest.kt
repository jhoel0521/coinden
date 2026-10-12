package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftEvaluator
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.DraftHeader
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.NonMovementIntent
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.draft.markedCorrected
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals

class DraftConfidenceTest {
    private val billetera = Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
    private val directory = WalletDirectory(listOf(billetera))
    private val header = DraftHeader("d-1", "carlos", Evidence(EvidenceSource.TEXT, "page 3 bs para trasmporte"), at(13))

    private fun confidenceOf(proposal: MovementProposal) = DraftEvaluator.evaluate(header, proposal, directory).confidence

    private fun expense(origins: Map<DraftField, FieldOrigin>) =
        MovementProposal.Expense(ProposedAmount("3", Currency.BOB), WalletRef.ById("billetera"), note = "trasmporte", origins = origins)

    @Test
    fun `sin origenes indicados todo cuenta como dicho y la confianza es ALTA`() {
        assertEquals(Confidence.HIGH, confidenceOf(expense(emptyMap())))
    }

    @Test
    fun `evidencia y dicho dan ALTA`() {
        val origins = mapOf(DraftField.AMOUNT to FieldOrigin.EVIDENCE, DraftField.WALLET to FieldOrigin.STATED)
        assertEquals(Confidence.HIGH, confidenceOf(expense(origins)))
    }

    @Test
    fun `un campo inferido da MEDIA`() {
        assertEquals(Confidence.MEDIUM, confidenceOf(expense(mapOf(DraftField.KIND to FieldOrigin.INFERRED))))
    }

    @Test
    fun `una billetera sugerida da BAJA aunque el resto sea evidencia`() {
        val origins =
            mapOf(
                DraftField.AMOUNT to FieldOrigin.STATED,
                DraftField.KIND to FieldOrigin.INFERRED,
                DraftField.WALLET to FieldOrigin.SUGGESTED,
            )
        assertEquals(Confidence.LOW, confidenceOf(expense(origins)))
    }

    @Test
    fun `solo cuentan los campos presentes`() {
        // La billetera sugerida falta: el borrador queda Incompleto y su confianza sale del resto
        val proposal =
            MovementProposal.Expense(ProposedAmount("3"), wallet = null, origins = mapOf(DraftField.WALLET to FieldOrigin.SUGGESTED))
        val draft = DraftEvaluator.evaluate(header, proposal, directory)
        assertEquals(Confidence.HIGH, draft.confidence)
    }

    @Test
    fun `la revision marca todo como corregido y sube a ALTA`() {
        val low = expense(mapOf(DraftField.WALLET to FieldOrigin.SUGGESTED, DraftField.KIND to FieldOrigin.INFERRED))
        val corrected = low.markedCorrected()
        assertEquals(Confidence.HIGH, confidenceOf(corrected))
        assertEquals(setOf(FieldOrigin.CORRECTED), corrected.origins.values.toSet())
        assertEquals(setOf(DraftField.KIND, DraftField.AMOUNT, DraftField.CURRENCY, DraftField.WALLET), corrected.origins.keys)
    }

    @Test
    fun `capturado, desconocido y no soportado son BAJA`() {
        assertEquals(Confidence.LOW, Draft.Captured(header).confidence)
        assertEquals(Confidence.LOW, confidenceOf(MovementProposal.Unknown("algo")))
        assertEquals(Confidence.LOW, confidenceOf(MovementProposal.NotAMovement(NonMovementIntent.QUERY)))
    }
}
