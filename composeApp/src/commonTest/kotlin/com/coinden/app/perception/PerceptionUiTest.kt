package com.coinden.app.perception

import com.coinden.app.AppContainer
import com.coinden.app.capture.CapturePresenter
import com.coinden.app.capture.ContextPagePresenter
import com.coinden.app.capture.DictationOutcome
import com.coinden.app.capture.DictationPort
import com.coinden.app.capture.DraftStatusView
import com.coinden.app.capture.MovementKindView
import com.coinden.app.capture.SharedContent
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private object Silent : DictationPort {
    override suspend fun listen(languageCode: String) = DictationOutcome.NothingHeard

    override fun cancel() = Unit
}

private class TextReader(
    private val text: String?,
) : ImageTextReader {
    override val engine = Engines.TESSERACT

    override suspend fun read(image: ByteArray) = text
}

/** Modelos de voz falsos: guarda lo "descargado" en memoria. */
private class FakeModels(
    private val profile: DeviceProfile,
    private val corrupt: Boolean = false,
) : VoiceModelManager {
    val installed = mutableListOf<VoiceModel>()

    override fun deviceProfile() = profile

    override fun installed(): List<VoiceModel> = installed.toList()

    override suspend fun download(
        model: VoiceModel,
        onProgress: (Long, Long) -> Unit,
    ): Boolean {
        onProgress(model.sizeBytes, model.sizeBytes)
        if (corrupt) return false
        installed += model
        return true
    }

    override fun delete(model: VoiceModel) {
        installed -= model
    }
}

/** Carlos ve en pantalla la lectura de sus fotos, sus audios y la elección de modelos. */
class PerceptionUiTest {
    private val receipt =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Beneficiario
        POLLOS DEL VALLE
        Importe
        Bs25.00
        Nro. de transacción: 400000001
        """.trimIndent()

    private fun readers(
        imageText: String? = receipt,
        transcribers: List<AudioTranscriber> = emptyList(),
    ) = PerceptionReaders(
        freeImageReader = TextReader(imageText),
        googleImageReader = { TextReader(null) },
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
        transcribers = { transcribers },
    )

    private suspend fun carlos(readers: PerceptionReaders = readers()): Pair<AppContainer, CapturePresenter> {
        val container = testContainer()
        seedCarlos(container)
        val service = PerceptionService(container, readers)
        val queue = InlineProcessingQueue(QueueRunner(service, readers, InMemoryFailureLog()))
        return container to CapturePresenter(container, Silent, queue, service)
    }

    @Test
    fun `la foto se guarda primero y luego la tarjeta muestra el gasto leido`() =
        runTest {
            val (container, presenter) = carlos()

            presenter.photo(byteArrayOf(1, 2, 3), "image/jpeg")
            // Antes del resultado pasa por la página de contexto; Listo la cierra.
            val bundleId = assertNotNull(presenter.state.value.contextBundle)
            ContextPagePresenter(container, bundleId).done()
            presenter.contextClosed(bundleId)

            val card = assertNotNull(presenter.state.value.result).cards.single()
            assertEquals(MovementKindView.EXPENSE, card.kind)
            assertEquals(Money.parseCanonical("25", Currency.BOB), card.amount)
            assertEquals("400000001", card.reference)
            assertEquals(receipt, card.perceivedText)
            assertTrue(
                presenter.state.value.reading
                    .isEmpty(),
            )
        }

    @Test
    fun `una nota de voz sin modelo queda capturada y ofrece descargarlo`() =
        runTest {
            val (_, presenter) = carlos()

            presenter.shared(SharedContent.File(byteArrayOf(1), "audio/ogg"))

            val result = assertNotNull(presenter.state.value.result)
            assertEquals(DraftStatusView.CAPTURED, result.cards.single().status)
            assertEquals(setOf(result.cards.single().draftId), result.voiceModelNeeded)
        }

    @Test
    fun `quien nunca eligio no tiene eleccion, y el lector arranca apagado`() =
        runTest {
            val privacy = PrivacyPresenter(testContainer())
            privacy.load()

            assertFalse(privacy.state.value.chosen, "todavia no eligio: hay que pedirle que elija")
            assertFalse(privacy.state.value.googleReader, "y mientras tanto el lector esta apagado")
        }

    @Test
    fun `elegir el lector de Google lo guarda junto con la eleccion hecha`() =
        runTest {
            val container = testContainer()
            val privacy = PrivacyPresenter(container)
            privacy.load()

            privacy.chooseGoogleReader(true)

            assertTrue(container.storage.preferences.googleReaderEnabled())
            assertTrue(container.storage.preferences.privacyNoticeSeen(), "elegir es haber visto el aviso")
            assertTrue(privacy.state.value.chosen)
        }

    @Test
    fun `elegir seguir sin el lector tambien es una eleccion, no un olvido`() =
        runTest {
            val container = testContainer()
            val privacy = PrivacyPresenter(container)
            privacy.load()

            privacy.chooseGoogleReader(false)

            assertFalse(container.storage.preferences.googleReaderEnabled())
            assertTrue(container.storage.preferences.privacyNoticeSeen(), "decir que no queda registrado igual")
            assertTrue(privacy.state.value.chosen)
        }

    @Test
    fun `quien ya eligio ve su eleccion y puede cambiarla desde Perfil`() =
        runTest {
            val container = testContainer()
            PrivacyPresenter(container).apply { load() }.chooseGoogleReader(true)

            val devuelta = PrivacyPresenter(container).apply { load() }
            assertTrue(devuelta.state.value.chosen, "ya eligio: no se le vuelve a pedir")
            assertTrue(devuelta.state.value.googleReader)

            devuelta.chooseGoogleReader(false)
            assertFalse(container.storage.preferences.googleReaderEnabled())
        }

    @Test
    fun `los modelos muestran el recomendado para el telefono y se descargan con un toque`() =
        runTest {
            val gb = 1_000_000_000L
            val models = FakeModels(DeviceProfile(totalMemoryBytes = 6 * gb, is64Bit = true, freeStorageBytes = 20 * gb))
            val presenter = VoiceModelsPresenter(models)
            presenter.load()

            assertEquals(VoiceRecommendation.Download(VoiceModels.SMALL), presenter.state.value.recommendation)
            assertEquals(
                VoiceModels.all,
                presenter.state.value.models
                    .map { it.model },
            )

            presenter.download(VoiceModels.SMALL)

            val small =
                presenter.state.value.models
                    .single { it.model == VoiceModels.SMALL }
            assertTrue(small.installed)
            presenter.delete(VoiceModels.SMALL)
            assertFalse(
                presenter.state.value.models
                    .single { it.model == VoiceModels.SMALL }
                    .installed,
            )
        }

    @Test
    fun `una descarga alterada avisa que fallo`() =
        runTest {
            val gb = 1_000_000_000L
            val presenter = VoiceModelsPresenter(FakeModels(DeviceProfile(6 * gb, true, 20 * gb), corrupt = true))
            presenter.load()

            presenter.download(VoiceModels.BASE)

            assertEquals(VoiceModels.BASE, presenter.state.value.failed)
            assertFalse(
                presenter.state.value.models
                    .single { it.model == VoiceModels.BASE }
                    .installed,
            )
        }
}
