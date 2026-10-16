package com.coinden.app.perception

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Un paso del lector falso: el texto leído (null es "sin texto") o una falla. */
private class Step(
    val text: String?,
    val error: Exception? = null,
)

private fun ok(text: String?) = Step(text)

private fun falla(message: String) = Step(null, IllegalStateException(message))

/** Lector que devuelve, en orden, los pasos que la prueba indique. */
private class ScriptedReader(
    vararg steps: Step,
) : ImageTextReader {
    override val engine = Engines.TESSERACT
    private val queue = ArrayDeque(steps.toList())
    val reads = mutableListOf<Int>()

    override suspend fun read(image: ByteArray): String? {
        reads += image.first().toInt()
        val step = queue.removeFirst()
        step.error?.let { throw it }
        return step.text
    }
}

/** La cola de Carlos: fotos y audios que se procesan solos, de a uno, sin perder nada. */
class QueueRunnerTest {
    private fun receipt(amount: String) =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Beneficiario
        TIENDA EJEMPLO
        Importe
        Bs$amount
        """.trimIndent()

    private class Setup(
        val container: AppContainer,
        val runner: QueueRunner,
        val log: InMemoryFailureLog,
        var released: Int = 0,
    )

    private suspend fun carlos(
        reader: ImageTextReader,
        transcribers: List<AudioTranscriber> = emptyList(),
        audioSeconds: Int = 10,
    ): Setup {
        val container = testContainer()
        seedCarlos(container)
        val log = InMemoryFailureLog()
        lateinit var setup: Setup
        val readers =
            PerceptionReaders(
                freeImageReader = reader,
                googleImageReader = { error("sin consentimiento") },
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
                        ) = Pcm(ShortArray(audioSeconds * 16_000), 16_000)
                    },
                transcribers = { transcribers },
                release = { setup.released++ },
            )
        setup = Setup(container, QueueRunner(PerceptionService(container, readers), readers, log), log)
        return setup
    }

    /** Guarda una foto (su primer byte la identifica en el lector) y la deja Capturada. */
    private suspend fun AppContainer.photo(id: Int): Draft {
        val ref = attachments.save(byteArrayOf(id.toByte()), "image/jpeg")
        return captureDraft(ownerId, Evidence(EvidenceSource.PHOTO, attachmentRef = ref)).single()
    }

    private suspend fun AppContainer.audio(): Draft {
        val ref = attachments.save(byteArrayOf(9), "audio/ogg")
        return captureDraft(ownerId, Evidence(EvidenceSource.VOICE, attachmentRef = ref)).single()
    }

    private suspend fun AppContainer.isCaptured(draft: Draft) = drafts.load(draft.id) is Draft.Captured

    @Test
    fun `procesa en orden de captura y de a uno`() =
        runTest {
            val reader =
                ScriptedReader(ok(receipt("10.00")), ok(receipt("20.00")), ok(receipt("30.00")))
            val s = carlos(reader)
            listOf(1, 2, 3).forEach { s.container.photo(it) }

            val result = s.runner.drain()

            assertEquals(listOf(1, 2, 3), reader.reads)
            assertEquals(3, result.processed)
            assertEquals(false, result.retryLater)
        }

    @Test
    fun `una cola cortada retoma solo lo que falta y no repite lo leido`() =
        runTest {
            val reader = ScriptedReader(ok(receipt("10.00")), ok(receipt("20.00")))
            val s = carlos(reader)
            val first = s.container.photo(1)
            s.container.photo(2)
            // La primera ya se leyó antes del corte (por ejemplo, en otro trabajo).
            PerceptionService(s.container, s.runner.readers).perceive(first.id)

            s.runner.drain()

            assertEquals(listOf(1, 2), reader.reads)
        }

    @Test
    fun `una falla pasajera se reintenta y la segunda vez funciona`() =
        runTest {
            val reader = ScriptedReader(falla("sin memoria"), ok(receipt("25.00")))
            val s = carlos(reader)
            val photo = s.container.photo(1)

            val first = s.runner.drain()
            assertTrue(first.retryLater)
            assertTrue(s.container.isCaptured(photo))

            val second = s.runner.drain()

            assertEquals(false, second.retryLater)
            assertIs<Draft.Incomplete>(s.container.drafts.load(photo.id))
        }

    @Test
    fun `tres fallas dejan el borrador capturado y la cola sigue con los demas`() =
        runTest {
            val broken = falla("archivo dañado")
            val reader = ScriptedReader(broken, ok(receipt("10.00")), broken, broken)
            val s = carlos(reader)
            val damaged = s.container.photo(1)
            val good = s.container.photo(2)

            repeat(3) { s.runner.drain() }
            val last = s.runner.drain()

            assertTrue(s.container.isCaptured(damaged))
            assertIs<Draft.Incomplete>(s.container.drafts.load(good.id))
            assertEquals(false, last.retryLater)
            assertEquals(listOf(1, 2, 1, 1), reader.reads)
        }

    @Test
    fun `una foto sin texto o un audio largo no se reintentan`() =
        runTest {
            val reader = ScriptedReader(ok(null))
            val s = carlos(reader, transcribers = listOf(FakeWhisper()), audioSeconds = 12 * 60)
            val blank = s.container.photo(1)
            val long = s.container.audio()

            s.runner.drain()
            val again = s.runner.drain()

            assertEquals(0, again.processed)
            assertTrue(s.container.isCaptured(blank))
            assertTrue(s.container.isCaptured(long))
            assertEquals(listOf(1), reader.reads)
        }

    @Test
    fun `un audio sin modelo espera sin contar como falla y se procesa al haber modelo`() =
        runTest {
            val transcribers = mutableListOf<AudioTranscriber>()
            val container = testContainer()
            seedCarlos(container)
            val readers =
                PerceptionReaders(
                    freeImageReader = ScriptedReader(),
                    googleImageReader = { error("sin consentimiento") },
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
            val runner = QueueRunner(PerceptionService(container, readers), readers, InMemoryFailureLog())
            val audio = container.audio()

            val waiting = runner.drain()
            assertEquals(false, waiting.retryLater)
            assertTrue(container.isCaptured(audio))

            transcribers += FakeWhisper()
            runner.drain()

            assertEquals("taxi", (container.drafts.load(audio.id) as Draft.Ready).movement.note)
        }

    @Test
    fun `al vaciarse la cola libera los modelos`() =
        runTest {
            val s = carlos(ScriptedReader(ok(receipt("10.00"))))
            s.container.photo(1)

            s.runner.drain()

            assertEquals(1, s.released)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `avisa cada borrador procesado para que la pantalla abierta se actualice`() =
        runTest {
            val s = carlos(ScriptedReader(ok(receipt("10.00")), ok(receipt("20.00"))))
            val a = s.container.photo(1)
            val b = s.container.photo(2)
            val seen = mutableListOf<String>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { s.runner.processed.toList(seen) }

            s.runner.drain()
            job.cancel()

            assertEquals(listOf(a.id, b.id), seen)
        }
}

private class FakeWhisper : AudioTranscriber {
    override val engine = Engines.WHISPER

    override suspend fun transcribe(
        pcm: Pcm,
        languageCode: String,
    ) = "gasté 40 en taxi"
}
