package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.CorrectionField
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldChange
import com.coinden.core.domain.model.draft.ReadyMovement
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Carlos revisa en la noche y corrige lo confirmado: siempre en el mismo formulario, y nada cambia sin su toque. */
class DraftFormModesTest {
    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun carlos(): AppContainer = testContainer().also { seedCarlos(it) }

    private suspend fun AppContainer.say(phrase: String): Draft = captureDraft(ownerId, Evidence(EvidenceSource.VOICE, phrase)).single()

    private suspend fun AppContainer.balanceOf(name: String): Money {
        val id =
            drafts
                .directory(ownerId)
                .wallets
                .single { it.name == name }
                .id
        return walletBalances(ownerId).single { it.walletId == id }.balance
    }

    private suspend fun AppContainer.pending() = draftInbox(ownerId).pending.map { it.id }

    private suspend fun AppContainer.form(mode: FormMode) = DraftFormPresenter(this, mode).also { it.load() }

    private suspend fun AppContainer.confirmed(phrase: String): Draft.Confirmed {
        val draft = say(phrase)
        return assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(confirmDraft(draft.id)).draft)
    }

    // --- Revisar un borrador ---

    @Test
    fun `la IA leyo 320, Carlos lo cambia a 32 y confirma`() =
        runTest {
            val container = carlos()
            val draft = container.say("pagué 320 de almuerzo bnb")
            val form = container.form(FormMode.Review(draft.id))
            assertEquals("320", form.state.value.amount)
            assertEquals("almuerzo", form.state.value.note)

            form.onAmountChange("32")
            assertTrue(form.confirm())

            assertEquals(emptyList(), container.pending())
            assertEquals(bs("468"), container.balanceOf("BNB Bs"))
        }

    @Test
    fun `completar una foto sin leer y guardar la deja lista sin tocar el saldo`() =
        runTest {
            val container = carlos()
            val photo = container.captureManual(container.ownerId, Evidence(EvidenceSource.PHOTO, attachmentRef = "foto-1")).single()
            val form = container.form(FormMode.Review(photo.id))
            assertEquals(photo.id, form.state.value.evidenceDraftId)

            form.onAmountChange("18,50")
            form.onWalletChange(
                form.state.value.wallets
                    .single { it.name == "Billetera" }
                    .id,
            )
            assertTrue(form.keep())

            assertIs<Draft.Ready>(container.drafts.load(photo.id))
            assertEquals(listOf(photo.id), container.pending())
            assertEquals(bs("100"), container.balanceOf("Billetera"))
        }

    @Test
    fun `si no alcanza el saldo el formulario sigue abierto con lo que falta`() =
        runTest {
            val container = carlos()
            val draft = container.say("pagué 145 de almuerzo billetera")
            val form = container.form(FormMode.Review(draft.id))

            assertFalse(form.confirm())

            assertEquals(ConfirmProblem.InsufficientFunds(bs("45")), form.state.value.problem)
            assertEquals(listOf(draft.id), container.pending())
        }

    @Test
    fun `descartar desde el formulario`() =
        runTest {
            val container = carlos()
            val draft = container.say("pagué 10 de error bnb")

            assertTrue(container.form(FormMode.Review(draft.id)).discard())

            assertIs<Draft.Discarded>(container.drafts.load(draft.id))
            assertEquals(emptyList(), container.pending())
        }

    // --- Corregir un movimiento confirmado a mano ---

    @Test
    fun `corregir a mano resalta lo que cambia y aplica todo junto`() =
        runTest {
            val container = carlos()
            val lunch = container.confirmed("pagué 35 de almuerzo bnb")
            val form = container.form(FormMode.Correct(lunch.id))
            assertEquals(emptyList(), form.state.value.changes)

            form.onAmountChange("320")
            assertTrue(form.state.value.largeAmount)
            form.onAmountChange("32")
            form.onReferenceChange("400000001")

            assertEquals(
                listOf(FieldChange.Amount(bs("35"), bs("32"), large = false), FieldChange.Reference(null, "400000001")),
                form.state.value.changes,
            )
            assertFalse(form.state.value.largeAmount)
            assertTrue(form.confirm())
            val fixed = assertIs<ReadyMovement.Expense>(assertIs<Draft.Confirmed>(container.drafts.load(lunch.id)).movement)
            assertEquals(bs("32"), fixed.amount)
            assertEquals("400000001", fixed.reference)
            assertEquals(bs("468"), container.balanceOf("BNB Bs"))
        }

    @Test
    fun `salir sin confirmar no cambia nada`() =
        runTest {
            val container = carlos()
            val taxi = container.confirmed("pagué 40 de taxi bnb")
            val form = container.form(FormMode.Correct(taxi.id))

            form.onNoteChange("taxi a la u")
            assertEquals(
                listOf(CorrectionField.NOTE),
                form.state.value.changes
                    .map { it.field },
            )

            assertEquals(taxi.movement, assertIs<Draft.Confirmed>(container.drafts.load(taxi.id)).movement)
        }

    // --- Cambio pendiente de un comprobante ---

    private val receipt =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Beneficiario
        TAXI EJEMPLO
        Importe
        Bs40.00
        Nro. de transacción: 400000777
        """.trimIndent()

    private suspend fun taxiWithReceipt(): Triple<AppContainer, Draft.Confirmed, Draft> {
        val container = carlos()
        val taxi = container.confirmed("pagué 40 de taxi bnb")
        val shared = container.captureDraft(container.ownerId, Evidence(EvidenceSource.TEXT, receipt)).single()
        assertIs<DraftResult.Ok>(container.resolveSameMovement(shared.id, taxi.id, true))
        return Triple(container, taxi, shared)
    }

    @Test
    fun `el cambio pendiente abre el formulario con la referencia resaltada y confirmar la aplica`() =
        runTest {
            val (container, taxi, shared) = taxiWithReceipt()
            val form = container.form(FormMode.Correct(taxi.id, shared.id))

            assertEquals("400000777", form.state.value.reference)
            assertTrue(FieldChange.Reference(null, "400000777") in form.state.value.changes)
            assertEquals(shared.id, form.state.value.evidenceDraftId)
            assertTrue(form.confirm())

            val fixed = assertIs<Draft.Confirmed>(container.drafts.load(taxi.id))
            assertEquals("400000777", (fixed.movement as ReadyMovement.Expense).reference)
            assertIs<Draft.Discarded>(container.drafts.load(shared.id))
            assertEquals(emptyList(), container.pending())
            assertEquals(bs("460"), container.balanceOf("BNB Bs"))
        }

    @Test
    fun `descartar el cambio pendiente deja el taxi como estaba`() =
        runTest {
            val (container, taxi, shared) = taxiWithReceipt()

            assertTrue(container.form(FormMode.Correct(taxi.id, shared.id)).discard())

            val still = assertIs<Draft.Confirmed>(container.drafts.load(taxi.id))
            assertNull((still.movement as ReadyMovement.Expense).reference)
            assertEquals(1, still.header.extraEvidence.size)
            assertEquals(emptyList(), container.pending())
        }

    @Test
    fun `son distintos deja el comprobante como borrador propio`() =
        runTest {
            val (container, taxi, shared) = taxiWithReceipt()

            assertTrue(container.form(FormMode.Correct(taxi.id, shared.id)).distinct())

            assertEquals(listOf(shared.id), container.pending())
            assertNull(
                container.drafts
                    .load(shared.id)!!
                    .header.correctionTarget,
            )
        }
}
