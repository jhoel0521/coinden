package com.coinden.app.capture

import com.coinden.app.perception.ProcessingQueue
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.usecase.bundle.CloseBundleUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Compartir varias cosas a la vez: dos fotos elegidas juntas en WhatsApp son **un pago con dos
 * comprobantes**, no dos capturas sueltas.
 */
class SharedBundleTest {
    private fun foto(nombre: String) = SharedContent.File(nombre.encodeToByteArray(), "image/jpeg")

    /** La cola no corre en estas pruebas: acá solo importa qué queda guardado. */
    private object SinCola : ProcessingQueue {
        override suspend fun kick() = Unit

        override val processed = MutableSharedFlow<String>()
    }

    private suspend fun carlos(): Pair<com.coinden.app.AppContainer, QuickCapture> {
        val c = testContainer()
        seedCarlos(c)
        return c to QuickCapture(c, SinCola)
    }

    @Test
    fun `dos fotos compartidas juntas entran al mismo paquete`() =
        runTest {
            val (c, capture) = carlos()

            val bundleId = assertNotNull(capture.sharedBundle(listOf(foto("qr-1"), foto("qr-2"))))

            val bundle = assertNotNull(c.bundles.load(bundleId))
            assertEquals(2, bundle.evidences.size)
            assertTrue(bundle.open, "queda abierto para que la página de contexto le sume algo")
        }

    @Test
    fun `cerrar en el acto se comporta como compartir y volver a la otra app`() =
        runTest {
            val (c, capture) = carlos()

            val bundleId = assertNotNull(capture.sharedBundle(listOf(foto("qr")), closeNow = true))

            assertEquals(false, c.bundles.load(bundleId)?.open)
        }

    @Test
    fun `un texto compartido tambien es un paquete`() =
        runTest {
            val (c, capture) = carlos()

            val bundleId = assertNotNull(capture.sharedBundle(listOf(SharedContent.Text("Importe Bs25.00"))))

            val evidencia = assertNotNull(c.bundles.load(bundleId)).evidences.single()
            assertEquals(EvidenceSource.TEXT, evidencia.evidence.source)
            assertEquals("Importe Bs25.00", evidencia.evidence.text)
        }

    @Test
    fun `cada archivo se guarda cifrado y con su huella`() =
        runTest {
            val (c, capture) = carlos()

            val bundleId = assertNotNull(capture.sharedBundle(listOf(foto("qr"))))

            val evidencia = assertNotNull(c.bundles.load(bundleId)).evidences.single().evidence
            assertNotNull(evidencia.attachmentRef, "el archivo se guarda cifrado, no en el evento")
            assertNotNull(evidencia.fingerprint, "sin huella no se podría evitar repetirlo")
        }

    @Test
    fun `lo capturado en la app tambien es un paquete, y queda abierto`() =
        runTest {
            val (c, capture) = carlos()

            val bundleId = capture.capturedBundle(Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb"))

            val bundle = assertNotNull(c.bundles.load(bundleId))
            assertTrue(bundle.open, "la página de contexto puede sumarle algo antes de cerrarlo")
            assertEquals(1, bundle.evidences.size)
        }

    @Test
    fun `al cerrar lo capturado por texto se interpreta en el acto`() =
        runTest {
            val (c, capture) = carlos()
            val bundleId = capture.capturedBundle(Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb"))

            CloseBundleUseCase(c.bundles)(bundleId)

            // Un texto no tiene nada que leer: no pasa por la cola, capturar sigue siendo inmediato.
            assertEquals(1, c.bundles.draftsOf(assertNotNull(c.bundles.load(bundleId))).size)
        }

    @Test
    fun `compartir nada no crea un paquete vacio`() =
        runTest {
            val (_, capture) = carlos()

            assertNull(capture.sharedBundle(emptyList()))
            assertNull(capture.sharedBundle(listOf(SharedContent.Text("   "))))
        }
}
