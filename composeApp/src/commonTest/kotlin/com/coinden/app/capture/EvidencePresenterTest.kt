package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.perception.ProcessingQueue
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Carlos mira la evidencia de un gasto: el dictado y la foto del comprobante que se unió. */
class EvidencePresenterTest {
    private suspend fun carlos(): AppContainer = testContainer().also { seedCarlos(it) }

    @Test
    fun `se elige el visor segun el tipo del archivo`() {
        assertEquals(EvidenceKind.IMAGE, EvidenceKind.of("image/jpeg"))
        assertEquals(EvidenceKind.PDF, EvidenceKind.of("application/pdf"))
        assertEquals(EvidenceKind.AUDIO, EvidenceKind.of("audio/ogg"))
        assertEquals(EvidenceKind.OTHER, EvidenceKind.of("application/zip"))
    }

    @Test
    fun `recorre el texto dictado y la foto unida, descifrada solo en memoria`() =
        runTest {
            val container = carlos()
            val said = container.captureDraft(container.ownerId, Evidence(EvidenceSource.VOICE, "gasté 10 en power bnb")).single()
            val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
            val photo = QuickCapture(container, NoQueue).file(jpeg, "image/jpeg")
            assertIs<DraftResult.Ok>(container.resolveSameMovement(photo.id, said.id, true))
            val viewer = EvidencePresenter(container, said.id)

            viewer.load()
            assertEquals(
                "gasté 10 en power bnb",
                viewer.state.value.current
                    ?.text,
            )
            assertNull(viewer.state.value.attachment)

            viewer.show(1)
            val shown = viewer.state.value.attachment!!
            assertEquals("image/jpeg", shown.mimeType)
            assertContentEquals(jpeg, shown.bytes)

            viewer.close()
            assertNull(viewer.state.value.attachment)
        }

    @Test
    fun `un archivo que no se puede leer se avisa`() =
        runTest {
            val container = carlos()
            val lost = container.captureManual(container.ownerId, Evidence(EvidenceSource.PHOTO, attachmentRef = "no-existe")).single()
            val viewer = EvidencePresenter(container, lost.id)

            viewer.load()

            assertTrue(viewer.state.value.unavailable)
        }

    private object NoQueue : ProcessingQueue {
        override suspend fun kick() = Unit

        override val processed = MutableSharedFlow<String>()
    }
}
