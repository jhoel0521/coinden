package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.perception.AudioDecoder
import com.coinden.app.perception.Engines
import com.coinden.app.perception.ImageTextReader
import com.coinden.app.perception.InMemoryFailureLog
import com.coinden.app.perception.InlineProcessingQueue
import com.coinden.app.perception.Pcm
import com.coinden.app.perception.PdfReading
import com.coinden.app.perception.PdfTextReader
import com.coinden.app.perception.PerceptionReaders
import com.coinden.app.perception.PerceptionService
import com.coinden.app.perception.QueueRunner
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class Heard(
    private val text: String,
) : DictationPort {
    override suspend fun listen(languageCode: String) = DictationOutcome.Heard(text)

    override fun cancel() = Unit
}

private class Receipt(
    private val text: String?,
) : ImageTextReader {
    override val engine = Engines.TESSERACT

    override suspend fun read(image: ByteArray) = text
}

/**
 * Carlos captura dentro de la app y, antes de ver el resultado, pasa por la página de contexto:
 * lo capturado es un paquete abierto al que puede sumarle una nota, un audio o una foto. Al
 * cerrarla, ve la tarjeta con lo que se entendió de todo junto.
 */
class CaptureContextFlowTest {
    private val receipt =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Importe
        Bs25.00
        Nro. de transacción: 400000001
        """.trimIndent()

    private suspend fun carlos(
        dictation: DictationPort = Heard("almuerzo 45 bnb"),
        imageText: String? = receipt,
    ): Triple<AppContainer, CapturePresenter, InlineProcessingQueue> {
        val container = testContainer()
        seedCarlos(container)
        val readers =
            PerceptionReaders(
                freeImageReader = Receipt(imageText),
                googleImageReader = { Receipt(null) },
                pdfReader =
                    object : PdfTextReader {
                        override suspend fun read(
                            pdf: ByteArray,
                            maxPages: Int,
                        ) = PdfReading.Unreadable
                    },
                audioDecoder =
                    object : AudioDecoder {
                        override suspend fun decode(
                            audio: ByteArray,
                            mimeType: String,
                        ) = Pcm(ShortArray(16_000), 16_000)
                    },
                transcribers = { emptyList() },
            )
        val service = PerceptionService(container, readers)
        val queue = InlineProcessingQueue(QueueRunner(service, readers, InMemoryFailureLog()))
        return Triple(container, CapturePresenter(container, dictation, queue, service), queue)
    }

    @Test
    fun `dictar abre la pagina de contexto con un paquete abierto y todavia sin resultado`() =
        runTest {
            val (container, presenter) = carlos()

            presenter.dictate()

            val bundleId = assertNotNull(presenter.state.value.contextBundle)
            assertTrue(assertNotNull(container.bundles.load(bundleId)).open)
            assertNull(presenter.state.value.result, "el resultado se ve al cerrar la página")
        }

    @Test
    fun `al cerrar la pagina se ve la tarjeta de lo dictado`() =
        runTest {
            val (container, presenter) = carlos()
            presenter.dictate()
            val bundleId = assertNotNull(presenter.state.value.contextBundle)
            presenter.contextOpened()

            ContextPagePresenter(container, bundleId).done()
            presenter.contextClosed(bundleId)

            val card = assertNotNull(presenter.state.value.result).cards.single()
            assertEquals(Money.parseCanonical("45", Currency.BOB), card.amount)
            assertNull(presenter.state.value.contextBundle)
        }

    @Test
    fun `la nota sumada en la pagina explica la foto del comprobante`() =
        runTest {
            val (container, presenter) = carlos()
            presenter.photo(byteArrayOf(1, 2, 3), "image/jpeg")
            val bundleId = assertNotNull(presenter.state.value.contextBundle)
            presenter.contextOpened()

            val page = ContextPagePresenter(container, bundleId)
            page.addNote("utiles del colegio")
            page.done()
            presenter.contextClosed(bundleId)

            val card = assertNotNull(presenter.state.value.result).cards.single()
            assertEquals(Money.parseCanonical("25", Currency.BOB), card.amount)
            assertEquals("400000001", card.reference)
            assertEquals("utiles del colegio", card.note)
        }

    @Test
    fun `salir sin tocar Listo tambien cierra el paquete y muestra el resultado`() =
        runTest {
            val (container, presenter) = carlos()
            presenter.dictate()
            val bundleId = assertNotNull(presenter.state.value.contextBundle)
            presenter.contextOpened()

            ContextPagePresenter(container, bundleId).onLeave()
            presenter.contextClosed(bundleId)

            assertTrue(assertNotNull(container.bundles.load(bundleId)).open.not())
            assertEquals(1, assertNotNull(presenter.state.value.result).cards.size)
        }

    @Test
    fun `una foto que no se pudo leer no se pierde al cerrar la pagina`() =
        runTest {
            val (container, presenter) = carlos(imageText = null)
            presenter.photo(byteArrayOf(1, 2, 3), "image/jpeg")
            val bundleId = assertNotNull(presenter.state.value.contextBundle)
            presenter.contextOpened()

            ContextPagePresenter(container, bundleId).done()
            presenter.contextClosed(bundleId)

            val result = presenter.state.value.result
            val notice = presenter.state.value.notice
            assertTrue(
                result?.cards?.isNotEmpty() == true || notice == CaptureNotice.SAVED_FOR_REVIEW,
                "o se ve su tarjeta, o se avisa que quedó para revisar",
            )
        }
}
