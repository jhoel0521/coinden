package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.ReadyMovement
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/** Cola falsa: solo cuenta los avisos (en Android, WorkManager trabaja después). */
private class CountingQueue : com.coinden.app.perception.ProcessingQueue {
    var kicks = 0

    override suspend fun kick() {
        kicks++
    }

    override val processed = MutableSharedFlow<String>()
}

/** Carlos comparte desde otras apps: se guarda al instante y lo pesado queda para la cola. */
class QuickCaptureTest {
    private suspend fun carlos(): Triple<AppContainer, QuickCapture, CountingQueue> {
        val container = testContainer()
        seedCarlos(container)
        val queue = CountingQueue()
        return Triple(container, QuickCapture(container, queue), queue)
    }

    @Test
    fun `un texto compartido se interpreta al instante sin pasar por la cola`() =
        runTest {
            val (_, quick, queue) = carlos()

            val drafts = quick.shared(SharedContent.Text("almuerzo 45 bnb"))

            val ready = assertIs<Draft.Ready>(drafts.single())
            assertEquals(Money.parseCanonical("45", Currency.BOB), (ready.movement as ReadyMovement.Expense).amount)
            assertEquals(0, queue.kicks)
        }

    @Test
    fun `imagen, PDF y audio se guardan cifrados con su huella y avisan a la cola`() =
        runTest {
            val (container, quick, queue) = carlos()
            val files =
                listOf(
                    "image/jpeg" to EvidenceSource.PHOTO,
                    "application/pdf" to EvidenceSource.PHOTO,
                    "audio/ogg" to EvidenceSource.VOICE,
                )

            for ((mime, source) in files) {
                val bytes = mime.encodeToByteArray()
                val draft = assertIs<Draft.Captured>(quick.shared(SharedContent.File(bytes, mime)).single())

                val evidence = draft.header.evidence
                assertEquals(source, evidence.source, mime)
                assertEquals(sha256Hex(bytes), evidence.fingerprint)
                val saved = assertNotNull(container.attachments.read(assertNotNull(evidence.attachmentRef)))
                assertContentEquals(bytes, saved.bytes)
            }
            assertEquals(3, queue.kicks)
        }

    @Test
    fun `un texto vacio no crea nada`() =
        runTest {
            val (container, quick, _) = carlos()

            assertEquals(emptyList(), quick.shared(SharedContent.Text("   ")))
            assertEquals(0, container.draftInbox(container.ownerId).pending.size)
        }
}
