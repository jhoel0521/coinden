package com.coinden.app.perception

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Lector falso: devuelve el texto que la prueba diga y cuenta cuántas veces lo llamaron. */
private class FakeImageReader(
    override val engine: String,
    private val text: String?,
) : ImageTextReader {
    var calls = 0

    override suspend fun read(image: ByteArray): String? {
        calls++
        return text
    }
}

private class FakePdfReader(
    private val reading: PdfReading,
) : PdfTextReader {
    override suspend fun read(
        pdf: ByteArray,
        maxPages: Int,
    ) = reading
}

private class FakeDecoder(
    private val seconds: Int,
) : AudioDecoder {
    override suspend fun decode(
        audio: ByteArray,
        mimeType: String,
    ) = Pcm(ShortArray(seconds * 16_000), 16_000)
}

private class FakeTranscriber(
    private val text: String,
) : AudioTranscriber {
    override val engine = Engines.WHISPER
    var language: String? = null

    override suspend fun transcribe(
        pcm: Pcm,
        languageCode: String,
    ): String {
        language = languageCode
        return text
    }
}

/** Carlos (Bolivia): fotos, PDF y audios que ya están guardados se leen en el teléfono. */
class PerceptionServiceTest {
    // Lecturas inventadas de un mismo pago QR: una completa (plantilla y referencia) y otra parcial.
    private val fullReading =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Beneficiario
        POLLOS DEL VALLE
        Importe
        Bs25.00
        Nro. de transacción: 400000001
        """.trimIndent()
    private val partialReading =
        """
        Pago realizado
        Importe Bs 25.00
        Beneficiario POLLOS DEL VALLE
        """.trimIndent()

    private class Setup(
        val container: AppContainer,
        val free: FakeImageReader,
        val google: FakeImageReader,
        var googleCreated: Int = 0,
    )

    private suspend fun carlos(
        free: String? = fullReading,
        google: String? = null,
        pdf: PdfReading = PdfReading.Unreadable,
        audioSeconds: Int = 10,
        transcribers: List<AudioTranscriber> = emptyList(),
    ): Pair<Setup, PerceptionService> {
        val container = testContainer()
        seedCarlos(container)
        val setup = Setup(container, FakeImageReader(Engines.TESSERACT, free), FakeImageReader(Engines.MLKIT, google))
        val readers =
            PerceptionReaders(
                freeImageReader = setup.free,
                googleImageReader = {
                    setup.googleCreated++
                    setup.google
                },
                pdfReader = FakePdfReader(pdf),
                audioDecoder = FakeDecoder(audioSeconds),
                transcribers = { transcribers },
            )
        return setup to PerceptionService(container, readers)
    }

    private suspend fun AppContainer.captured(
        source: EvidenceSource,
        mime: String,
    ): Draft {
        val ref = attachments.save(byteArrayOf(1, 2, 3), mime)
        return captureDraft(ownerId, Evidence(source, attachmentRef = ref)).single()
    }

    private fun Draft.expense() =
        assertIs<MovementProposal.Expense>((this as? Draft.Incomplete)?.proposal ?: (this as Draft.Ready).proposal)

    @Test
    fun `una foto leida propone el gasto del comprobante`() =
        runTest {
            val (setup, service) = carlos()
            val draft = setup.container.captured(EvidenceSource.PHOTO, "image/jpeg")

            val outcome = assertIs<PerceptionOutcome.Read>(service.perceive(draft.id))

            val read = outcome.drafts.single()
            assertEquals(ProposedAmount("25.00", Currency.BOB), read.expense().amount)
            assertEquals("400000001", read.expense().reference)
        }

    @Test
    fun `gana la lectura con plantilla aunque sea la del lector de Google`() =
        runTest {
            val (setup, service) = carlos(free = partialReading, google = fullReading)
            setup.container.storage.preferences
                .setGoogleReaderEnabled(true)
            val draft = setup.container.captured(EvidenceSource.PHOTO, "image/jpeg")

            val outcome = assertIs<PerceptionOutcome.Read>(service.perceive(draft.id))

            assertEquals(
                "400000001",
                outcome.drafts
                    .single()
                    .expense()
                    .reference,
            )
            assertEquals(
                fullReading,
                outcome.drafts
                    .single()
                    .header.perceivedText,
            )
        }

    @Test
    fun `si empatan gana el lector libre`() =
        runTest {
            val (setup, service) = carlos(free = partialReading, google = partialReading.replace("POLLOS", "POYOS"))
            setup.container.storage.preferences
                .setGoogleReaderEnabled(true)
            val draft = setup.container.captured(EvidenceSource.PHOTO, "image/jpeg")

            val outcome = assertIs<PerceptionOutcome.Read>(service.perceive(draft.id))

            assertEquals(
                partialReading,
                outcome.drafts
                    .single()
                    .header.perceivedText,
            )
        }

    @Test
    fun `sin consentimiento el lector de Google nunca se crea`() =
        runTest {
            val (setup, service) = carlos(free = partialReading, google = fullReading)
            val draft = setup.container.captured(EvidenceSource.PHOTO, "image/jpeg")

            service.perceive(draft.id)

            assertEquals(0, setup.googleCreated)
            assertEquals(0, setup.google.calls)
        }

    @Test
    fun `un PDF con texto se lee sin pasar por el lector de imagenes`() =
        runTest {
            val (setup, service) = carlos(pdf = PdfReading.Text(fullReading))
            val draft = setup.container.captured(EvidenceSource.PHOTO, "application/pdf")

            val outcome = assertIs<PerceptionOutcome.Read>(service.perceive(draft.id))

            assertEquals(
                "400000001",
                outcome.drafts
                    .single()
                    .expense()
                    .reference,
            )
            assertEquals(0, setup.free.calls)
        }

    @Test
    fun `un PDF escaneado pasa sus paginas al lector de imagenes`() =
        runTest {
            val (setup, service) = carlos(pdf = PdfReading.Pages(listOf(byteArrayOf(9))))
            val draft = setup.container.captured(EvidenceSource.PHOTO, "application/pdf")

            val outcome = assertIs<PerceptionOutcome.Read>(service.perceive(draft.id))

            assertEquals(1, setup.free.calls)
            assertEquals(
                "400000001",
                outcome.drafts
                    .single()
                    .expense()
                    .reference,
            )
        }

    @Test
    fun `un audio se transcribe en el idioma de la app`() =
        runTest {
            val transcriber = FakeTranscriber("gasté 40 en taxi")
            val (setup, service) = carlos(transcribers = listOf(transcriber))
            val draft = setup.container.captured(EvidenceSource.VOICE, "audio/ogg")

            val outcome = assertIs<PerceptionOutcome.Read>(service.perceive(draft.id))

            assertEquals("es", transcriber.language)
            assertEquals(
                "taxi",
                outcome.drafts
                    .single()
                    .expense()
                    .note,
            )
        }

    @Test
    fun `un audio de mas de 5 minutos no se transcribe`() =
        runTest {
            val (setup, service) = carlos(audioSeconds = 12 * 60, transcribers = listOf(FakeTranscriber("gasté 40 en taxi")))
            val draft = setup.container.captured(EvidenceSource.VOICE, "audio/ogg")

            assertEquals(PerceptionOutcome.TooLong, service.perceive(draft.id))
            assertIs<Draft.Captured>(setup.container.drafts.load(draft.id))
        }

    @Test
    fun `sin modelo ni reconocimiento local el audio queda capturado y se ofrece la descarga`() =
        runTest {
            val (setup, service) = carlos(transcribers = emptyList())
            val draft = setup.container.captured(EvidenceSource.VOICE, "audio/ogg")

            assertEquals(PerceptionOutcome.NeedsVoiceModel, service.perceive(draft.id))
            assertIs<Draft.Captured>(setup.container.drafts.load(draft.id))
        }

    @Test
    fun `una foto sin texto queda capturada`() =
        runTest {
            val (setup, service) = carlos(free = null)
            val draft = setup.container.captured(EvidenceSource.PHOTO, "image/jpeg")

            assertEquals(PerceptionOutcome.NothingRead, service.perceive(draft.id))
            assertIs<Draft.Captured>(setup.container.drafts.load(draft.id))
        }

    @Test
    fun `leer capturas guardadas recorre los capturados con archivo todavia sin leer`() =
        runTest {
            val (setup, service) = carlos()
            val first = setup.container.captured(EvidenceSource.PHOTO, "image/jpeg")
            val second = setup.container.captured(EvidenceSource.PHOTO, "image/png")
            setup.container.captureDraft(setup.container.ownerId, Evidence(EvidenceSource.TEXT, "hola"))

            assertEquals(setOf(first.id, second.id), service.pendingDraftIds().toSet())
            service.perceiveAll()

            assertTrue(service.pendingDraftIds().isEmpty())
            assertEquals(2, setup.free.calls)
        }
}
