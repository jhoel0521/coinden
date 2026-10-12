package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.DraftIssue
import com.coinden.core.domain.model.draft.DraftProjection
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DraftProjectionTest {
    private val billetera = Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
    private val directory = WalletDirectory(listOf(billetera))
    private val evidence = Evidence(EvidenceSource.TEXT, "almuerzo 45")

    private fun captured(time: Long = at(12, 58)) = DraftEvent.Captured("e-1", "d-1", "carlos", "celular", time, time, evidence)

    private fun expense(amount: String) = MovementProposal.Expense(ProposedAmount(amount), WalletRef.ById("billetera"))

    private fun interpreted(
        id: String,
        proposal: MovementProposal,
        time: Long,
        device: String = "celular",
        recorded: Long = time,
    ) = DraftEvent.Interpreted(id, "d-1", "carlos", device, time, recorded, proposal)

    private fun revised(
        id: String,
        proposal: MovementProposal,
        time: Long,
        device: String = "pc",
    ) = DraftEvent.Revised(id, "d-1", "carlos", device, time, time, proposal)

    private fun project(
        vararg events: DraftEvent,
        dir: WalletDirectory = directory,
    ) = DraftProjection.project(events.toList(), dir)

    @Test
    fun `sin evento de captura no hay borrador`() {
        assertNull(project(interpreted("e-2", expense("45"), at(13))))
    }

    @Test
    fun `solo capturado`() {
        val draft = assertIs<Draft.Captured>(project(captured()))
        assertEquals(at(12, 58), draft.header.capturedAtEpochMs)
        assertEquals(evidence, draft.header.evidence)
    }

    @Test
    fun `interpretado queda Listo o Incompleto`() {
        assertIs<Draft.Ready>(project(captured(), interpreted("e-2", expense("45"), at(12, 58))))
        assertIs<Draft.Incomplete>(project(captured(), interpreted("e-2", MovementProposal.Expense(), at(12, 58))))
    }

    @Test
    fun `la revision reemplaza la propuesta entera`() {
        val draft = project(captured(), interpreted("e-2", expense("54"), at(12, 58)), revised("e-3", expense("45"), at(21)))
        val ready = assertIs<Draft.Ready>(draft)
        assertEquals(Money.parseCanonical("45", Currency.BOB), (ready.movement as ReadyMovement.Expense).amount)
    }

    @Test
    fun `revisiones de dos dispositivos se aplican en orden cronologico sin importar como llegan`() {
        val fromPhone = revised("e-3", expense("40"), at(20), device = "celular")
        val fromPc = revised("e-4", expense("45"), at(21), device = "pc")
        val a = project(fromPc, captured(), fromPhone, interpreted("e-2", expense("54"), at(12, 58)))
        val b = project(captured(), interpreted("e-2", expense("54"), at(12, 58)), fromPhone, fromPc)
        assertEquals(a, b)
        assertEquals(Money.parseCanonical("45", Currency.BOB), ((a as Draft.Ready).movement as ReadyMovement.Expense).amount)
    }

    @Test
    fun `confirmado es terminal e ignora eventos posteriores`() {
        val movement = ReadyMovement.Expense(Money.parseCanonical("45", Currency.BOB), "billetera", at(12, 58), null)
        val confirmed = DraftEvent.Confirmed("e-3", "d-1", "carlos", "pc", at(21), at(21), "draft:d-1", movement)
        val laterRevision = revised("e-4", expense("99"), at(22), device = "celular")
        val laterDiscard = DraftEvent.Discarded("e-5", "d-1", "carlos", "celular", at(23), at(23))

        val draft =
            assertIs<Draft.Confirmed>(
                project(captured(), interpreted("e-2", expense("45"), at(12, 58)), confirmed, laterRevision, laterDiscard),
            )
        assertEquals("draft:d-1", draft.ledgerEventId)
        assertEquals(movement, draft.movement)
    }

    @Test
    fun `descartado es terminal`() {
        val discarded = DraftEvent.Discarded("e-3", "d-1", "carlos", "pc", at(21), at(21))
        assertIs<Draft.Discarded>(project(captured(), discarded, revised("e-4", expense("45"), at(22))))
    }

    @Test
    fun `archivar la billetera recalcula el estado a Incompleto`() {
        val events = arrayOf(captured(), interpreted("e-2", expense("45"), at(12, 58)))
        assertIs<Draft.Ready>(project(*events))
        val archived = WalletDirectory(listOf(billetera.archived()))
        val draft = assertIs<Draft.Incomplete>(project(*events, dir = archived))
        assertEquals(setOf(DraftIssue.WALLET_ARCHIVED), draft.issues)
    }
}
