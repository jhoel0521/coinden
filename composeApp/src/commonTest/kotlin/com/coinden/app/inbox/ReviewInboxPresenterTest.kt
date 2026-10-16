package com.coinden.app.inbox

import com.coinden.app.AppContainer
import com.coinden.app.capture.ConfirmProblem
import com.coinden.app.capture.FormMode
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La noche de Carlos en la bandeja: lo seguro en "Listos", lo dudoso en "Revisar". */
class ReviewInboxPresenterTest {
    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun carlos(): Pair<AppContainer, ReviewInboxPresenter> {
        val container = testContainer()
        seedCarlos(container)
        return container to ReviewInboxPresenter(container)
    }

    private suspend fun AppContainer.say(phrase: String): Draft = captureDraft(ownerId, Evidence(EvidenceSource.VOICE, phrase)).single()

    private suspend fun AppContainer.walletId(name: String) =
        drafts
            .directory(ownerId)
            .wallets
            .single { it.name == name }
            .id

    private suspend fun AppContainer.balanceOf(name: String): Money {
        val id = walletId(name)
        return walletBalances(ownerId).single { it.walletId == id }.balance
    }

    @Test
    fun `lo seguro va en listos y lo dudoso en revisar`() =
        runTest {
            val (container, inbox) = carlos()
            val lunch = container.say("pagué 25 de almuerzo bnb")
            val transport = container.say("page 3 bs para trasmporte")

            inbox.load()

            assertEquals(
                listOf(lunch.id),
                inbox.state.value.ready
                    .map { it.card.draftId },
            )
            assertEquals(
                listOf(transport.id),
                inbox.state.value.review
                    .map { it.card.draftId },
            )
            assertEquals(
                FormMode.Review(transport.id),
                inbox.formFor(
                    inbox.state.value.review
                        .single(),
                ),
            )
        }

    @Test
    fun `el saldo proyectado avisa el faltante`() =
        runTest {
            val (container, inbox) = carlos()
            container.say("pagué 145 de pan billetera")

            inbox.load()

            val cash =
                inbox.state.value.balances
                    .single { it.walletName == "Billetera" }
            assertEquals(bs("-45"), cash.projected)
            assertTrue(cash.shortfall)
        }

    @Test
    fun `sin pendientes la bandeja esta vacia`() =
        runTest {
            val (_, inbox) = carlos()

            inbox.load()

            assertTrue(inbox.state.value.isEmpty)
        }

    @Test
    fun `confirmar los tres listos tras ver el resumen`() =
        runTest {
            val (container, inbox) = carlos()
            container.say("pagué 25 de almuerzo bnb")
            container.say("pagué 10 de taxi bnb")
            container.say("pagué 12 de pan bnb")
            inbox.load()

            inbox.askConfirmReady()
            assertEquals(BatchSummary(3, listOf("BNB Bs" to bs("-47"))), inbox.state.value.summary)
            inbox.confirmReady()

            assertTrue(inbox.state.value.isEmpty)
            assertNull(inbox.state.value.summary)
            assertEquals(bs("453"), container.balanceOf("BNB Bs"))
        }

    @Test
    fun `cancelar el resumen no confirma nada`() =
        runTest {
            val (container, inbox) = carlos()
            container.say("pagué 25 de almuerzo bnb")
            inbox.load()

            inbox.askConfirmReady()
            inbox.cancelSummary()

            assertNull(inbox.state.value.summary)
            assertEquals(1, inbox.state.value.ready.size)
            assertEquals(bs("500"), container.balanceOf("BNB Bs"))
        }

    @Test
    fun `el listo que no alcanza queda con su faltante`() =
        runTest {
            val (container, inbox) = carlos()
            container.say("pagué 25 de almuerzo bnb")
            val bread = container.say("pagué 145 de pan billetera")
            inbox.load()

            inbox.askConfirmReady()
            inbox.confirmReady()

            assertEquals(listOf(bread.id), (inbox.state.value.ready + inbox.state.value.review).map { it.card.draftId })
            assertEquals(ConfirmProblem.InsufficientFunds(bs("45")), inbox.state.value.problems[bread.id])
        }

    @Test
    fun `una transferencia se confirma desde su tarjeta sin abrir el formulario`() =
        runTest {
            val (container, inbox) = carlos()
            val captured = container.captureManual(container.ownerId, Evidence(EvidenceSource.MANUAL)).single()
            val transfer =
                MovementProposal.Transfer(
                    ProposedAmount("50", Currency.BOB),
                    WalletRef.ById(container.walletId("BNB Bs")),
                    to = WalletRef.ById(container.walletId("Billetera")),
                )
            container.reviseDraft(captured.id, transfer)
            inbox.load()

            val item = (inbox.state.value.ready + inbox.state.value.review).single()
            assertFalse(item.opensForm)
            assertNull(inbox.formFor(item))
            inbox.confirm(item.card.draftId)

            assertEquals(bs("150"), container.balanceOf("Billetera"))
            assertTrue(inbox.state.value.isEmpty)
        }

    @Test
    fun `son el mismo con un confirmado abre el formulario de correccion`() =
        runTest {
            val (container, inbox) = carlos()
            val taxi = container.say("pagué 40 de taxi bnb")
            assertIs<DraftResult.Ok>(container.confirmDraft(taxi.id))
            val receipt =
                container
                    .captureDraft(
                        container.ownerId,
                        Evidence(
                            EvidenceSource.TEXT,
                            "Banco ECONOMICO\nPago QR otros bancos\nImporte\nBs40.00\nNro. de transacción: 400000777",
                        ),
                    ).single()
            inbox.load()
            val question =
                assertNotNull(
                    inbox.state.value.review
                        .single()
                        .card.question,
                )
            assertEquals(taxi.id, question.otherDraftId)

            val form = inbox.resolve(receipt.id, taxi.id, same = true)

            assertEquals(FormMode.Correct(taxi.id, receipt.id), form)
            val item =
                inbox.state.value.review
                    .single()
            assertEquals(taxi.id, item.correctionTarget)
            assertEquals(FormMode.Correct(taxi.id, receipt.id), inbox.formFor(item))
        }
}
