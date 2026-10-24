package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.perception.ProcessingQueue
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.usecase.bundle.CloseBundleUseCase
import com.coinden.core.domain.usecase.bundle.StartBundleUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * "Agregar contexto" y "Agregar al último": sumarle una explicación a algo ya capturado.
 *
 * En la noche Carlos ve un comprobante sin glosa, toca Agregar contexto y graba "esto es de mi
 * comida". Se reabre su paquete y, al salir, el borrador se vuelve a interpretar.
 */
class ReopenContextTest {
    private suspend fun carlos(): Pair<AppContainer, ReopenContext> {
        val c = testContainer()
        seedCarlos(c)
        return c to ReopenContext(c)
    }

    private object SinCola : ProcessingQueue {
        override suspend fun kick() = Unit

        override val processed = MutableSharedFlow<String>()
    }

    @Test
    fun `agregar contexto reabre el paquete del borrador`() =
        runTest {
            val (c, reopen) = carlos()
            val bundleId = StartBundleUseCase(c.bundles)(c.ownerId, listOf(Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb")))
            CloseBundleUseCase(c.bundles)(bundleId)
            val draftId =
                c.bundles
                    .draftsOf(c.bundles.load(bundleId)!!)
                    .first()
                    .header.id

            val reabierto = reopen.forDraft(draftId)

            assertEquals(bundleId, reabierto)
            assertEquals(true, c.bundles.load(bundleId)?.open, "queda abierto para sumarle el audio")
        }

    @Test
    fun `un borrador de la 0 1 0 se convierte en paquete al reabrirlo`() =
        runTest {
            val (c, reopen) = carlos()
            // Capturado por el camino viejo: no tiene paquete propio.
            val draft = QuickCapture(c, SinCola).text("almuerzo 45 bnb", EvidenceSource.TEXT).single()

            val bundleId = assertNotNull(reopen.forDraft(draft.header.id), "tiene que poder sumarle contexto igual")

            val bundle = assertNotNull(c.bundles.load(bundleId))
            assertEquals(true, bundle.open)
            assertEquals(1, bundle.evidences.size, "se materializa con su evidencia, sin tocar el borrador")
        }

    @Test
    fun `un borrador que ya no existe no reabre nada`() =
        runTest {
            val (_, reopen) = carlos()

            assertNull(reopen.forDraft("no-existe"))
        }

    @Test
    fun `agregar al ultimo ofrece el borrador pendiente mas reciente`() =
        runTest {
            val (c, reopen) = carlos()
            QuickCapture(c, SinCola).text("almuerzo 45 bnb", EvidenceSource.TEXT)
            QuickCapture(c, SinCola).text("micro 3", EvidenceSource.TEXT)

            val ultimo = assertNotNull(reopen.lastPending())

            assertEquals("micro 3", ultimo.note)
        }

    @Test
    fun `sin borradores pendientes no se ofrece agregar al ultimo`() =
        runTest {
            val (_, reopen) = carlos()

            assertNull(reopen.lastPending(), "la hoja de Capturar no muestra la opción")
        }
}
