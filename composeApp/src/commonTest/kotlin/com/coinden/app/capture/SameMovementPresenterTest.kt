package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.SameMovementReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Dictado falso que devuelve, en orden, lo que la prueba diga que se escuchó. */
private class ScriptedDictation(
    vararg heard: String,
) : DictationPort {
    private val queue = ArrayDeque(heard.toList())

    override suspend fun listen(languageCode: String): DictationOutcome = DictationOutcome.Heard(queue.removeFirst())

    override fun cancel() = Unit
}

/** Carlos dicta el power, luego comparte su comprobante, y responde si son el mismo gasto. */
class SameMovementPresenterTest {
    // Pago QR con la estructura del Banco Económico, datos inventados y sin fecha (se toma la de captura)
    private val powerReceipt =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Beneficiario
        TIENDA EJEMPLO
        Importe
        Bs10.00
        Nro. de transacción: 400000123
        """.trimIndent()

    private suspend fun carlos(vararg heard: String): Pair<AppContainer, CapturePresenter> {
        val container = testContainer()
        seedCarlos(container)
        return container to CapturePresenter(container, ScriptedDictation(*heard))
    }

    private fun CapturePresenter.card() = assertNotNull(state.value.result).cards.single()

    private suspend fun AppContainer.pendingCount() = draftInbox(ownerId).pending.size

    @Test
    fun `compartir el comprobante despues de dictar el power pregunta si es el mismo gasto`() =
        runTest {
            val (_, presenter) = carlos("gasté 10 en power")
            presenter.dictate()
            val audioId = presenter.card().draftId

            presenter.shared(SharedContent.Text(powerReceipt))

            val card = presenter.card()
            assertEquals("400000123", card.reference)
            val question = assertNotNull(card.question)
            assertEquals(SameMovementReason.SAME_DAY_AMOUNT, question.reason)
            assertEquals(audioId, question.otherDraftId)
            assertEquals("power", question.otherNote)
            assertEquals(Money.parseCanonical("10", Currency.BOB), question.otherAmount)
            assertEquals("Billetera", question.otherWalletName)
        }

    @Test
    fun `son el mismo deja un pendiente menos y muestra el borrador unido`() =
        runTest {
            val (container, presenter) = carlos("gasté 10 en power")
            presenter.dictate()
            val audioId = presenter.card().draftId
            presenter.shared(SharedContent.Text(powerReceipt))
            assertEquals(2, container.pendingCount())

            presenter.resolve(presenter.card().draftId, same = true)

            assertEquals(1, container.pendingCount())
            val kept = presenter.card()
            assertEquals(audioId, kept.draftId)
            assertEquals("400000123", kept.reference)
            assertEquals(1, kept.extraEvidence)
            assertNull(kept.question)
        }

    @Test
    fun `son distintos quita la pregunta y quedan los dos`() =
        runTest {
            val (container, presenter) = carlos("gasté 10 bs por qr", "gasté 10 bs por qr")
            presenter.dictate()
            presenter.dictate()
            assertNotNull(presenter.card().question)

            presenter.resolve(presenter.card().draftId, same = false)

            assertNull(presenter.card().question)
            assertEquals(2, container.pendingCount())
        }

    @Test
    fun `la misma foto guardada dos veces se reconoce por su huella`() =
        runTest {
            val (_, presenter) = carlos()
            val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 7, 7, 7)

            presenter.photo(jpeg, "image/jpeg")
            presenter.photo(jpeg.copyOf(), "image/jpeg")

            assertEquals(SameMovementReason.SAME_FILE, presenter.card().question?.reason)
        }
}
