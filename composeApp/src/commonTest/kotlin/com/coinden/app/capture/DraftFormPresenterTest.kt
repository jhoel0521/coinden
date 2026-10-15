package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.wallet.PaymentMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Carlos (Bolivia) abre Escribir: el formulario completo, con o sin frase. */
class DraftFormPresenterTest {
    private suspend fun carlos(): Pair<AppContainer, DraftFormPresenter> {
        val container = testContainer()
        seedCarlos(container)
        return container to DraftFormPresenter(container).also { it.load() }
    }

    private fun DraftFormPresenter.walletNamed(name: String) = state.value.wallets.single { it.name == name }

    private suspend fun AppContainer.onlyPending(): Draft = draftInbox(ownerId).pending.single()

    @Test
    fun `la frase almuerzo 45 bnb precarga gasto, 45, BNB Bs y comida`() =
        runTest {
            val (_, form) = carlos()

            form.onPhraseChange("almuerzo 45 bnb")
            form.fillFromPhrase()

            val state = form.state.value
            assertEquals(FormKind.EXPENSE, state.kind)
            assertEquals("45", state.amount)
            assertEquals(Currency.BOB, state.currency)
            assertEquals(form.walletNamed("BNB Bs").id, state.walletId)
            assertEquals(CategoryRef.Base(BaseCategory.FOOD), state.category)
            assertEquals("almuerzo", state.note)
        }

    @Test
    fun `lo precargado se puede cambiar antes de guardar`() =
        runTest {
            val (container, form) = carlos()
            form.onPhraseChange("almuerzo 45 bnb")
            form.fillFromPhrase()

            form.onAmountChange("50")
            form.onWalletChange(form.walletNamed("Billetera").id)
            form.save()

            val draft = assertIs<Draft.Ready>(container.onlyPending())
            val movement = assertIs<ReadyMovement.Expense>(draft.movement)
            assertEquals(Money.parseCanonical("50", Currency.BOB), movement.amount)
            assertEquals(form.walletNamed("Billetera").id, movement.walletId)
            assertEquals("almuerzo 45 bnb", draft.header.evidence.text)
            assertEquals(EvidenceSource.MANUAL, draft.header.evidence.source)
        }

    @Test
    fun `el formulario completo a mano queda Listo con confianza alta y la categoria elegida`() =
        runTest {
            val (container, form) = carlos()
            val yesterday = 1_759_900_000_000

            form.onKindChange(FormKind.EXPENSE)
            form.onAmountChange("120")
            form.onCurrencyChange(Currency.BOB)
            form.onWalletChange(form.walletNamed("BNB Bs").id)
            form.onMethodChange(PaymentMethod.CARD)
            form.onCategoryChange(CategoryRef.Base(BaseCategory.GROCERIES))
            form.onDateChange(yesterday)
            val result = form.save()

            val draft = assertIs<Draft.Ready>(container.onlyPending())
            assertEquals(Confidence.HIGH, draft.confidence)
            val movement = assertIs<ReadyMovement.Expense>(draft.movement)
            assertEquals(Money.parseCanonical("120", Currency.BOB), movement.amount)
            assertEquals(form.walletNamed("BNB Bs").id, movement.walletId)
            assertEquals(yesterday, movement.occurredAtEpochMs)
            assertEquals(PaymentMethod.CARD, (draft.proposal as MovementProposal.Expense).paymentMethod)
            assertNull(draft.header.evidence.text)
            val card = result.cards.single()
            assertEquals(DraftStatusView.READY, card.status)
            assertEquals(CategoryRef.Base(BaseCategory.GROCERIES), card.category?.ref)
        }

    @Test
    fun `sin monto se guarda como Incompleto sin perder lo escrito`() =
        runTest {
            val (container, form) = carlos()

            form.onWalletChange(form.walletNamed("BNB Bs").id)
            form.onNoteChange("farmacia")
            form.save()

            val draft = assertIs<Draft.Incomplete>(container.onlyPending())
            val proposal = assertIs<MovementProposal.Expense>(draft.proposal)
            assertNull(proposal.amount)
            assertEquals("farmacia", proposal.note)
        }

    @Test
    fun `un monto con coma decimal se guarda exacto`() =
        runTest {
            val (container, form) = carlos()

            form.onAmountChange("12,50")
            form.onWalletChange(form.walletNamed("Billetera").id)
            form.save()

            val movement = assertIs<ReadyMovement.Expense>(assertIs<Draft.Ready>(container.onlyPending()).movement)
            assertEquals(Money.parseCanonical("12.50", Currency.BOB), movement.amount)
        }

    @Test
    fun `la referencia escrita a mano se guarda en el borrador`() =
        runTest {
            val (container, form) = carlos()

            form.onAmountChange("120")
            form.onWalletChange(form.walletNamed("BNB Bs").id)
            form.onReferenceChange(" 77001234 ")
            form.save()

            val draft = assertIs<Draft.Ready>(container.onlyPending())
            assertEquals("77001234", (draft.movement as ReadyMovement.Expense).reference)
        }

    @Test
    fun `pegar un comprobante en la frase llena la referencia`() =
        runTest {
            val (_, form) = carlos()
            // Estructura de un pago QR del Banco Económico con datos inventados
            form.onPhraseChange(
                """
                Banco ECONOMICO
                Pago QR otros bancos
                Beneficiario
                POLLOS DEL VALLE
                Importe
                Bs25.00
                Nro. de transacción: 400000001
                """.trimIndent(),
            )
            form.fillFromPhrase()

            assertEquals("400000001", form.state.value.reference)
            assertEquals("25.00", form.state.value.amount)
        }
}
