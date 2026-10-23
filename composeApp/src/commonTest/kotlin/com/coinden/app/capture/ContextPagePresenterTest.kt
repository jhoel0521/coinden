package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.usecase.bundle.StartBundleUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La página de contexto rápido: lo que Carlos ve después de compartir.
 *
 * No muestra montos ni saldos, así que no pide desbloquear aunque la app esté bloqueada. Todo lo
 * que se agrega entra al mismo paquete, y salir de la página lo cierra.
 */
class ContextPagePresenterTest {
    private val comprobante = Evidence(EvidenceSource.PHOTO, null, "att-qr", "huella-qr")

    private suspend fun carlos(): Triple<AppContainer, String, ContextPagePresenter> {
        val c = testContainer()
        seedCarlos(c)
        val bundleId = StartBundleUseCase(c.bundles)(c.ownerId, listOf(comprobante))
        val p = ContextPagePresenter(c, bundleId).also { it.load() }
        return Triple(c, bundleId, p)
    }

    @Test
    fun `muestra lo que se compartio`() =
        runTest {
            val (_, _, p) = carlos()

            assertEquals(1, p.state.value.items.size)
            assertFalse(p.state.value.closed)
        }

    @Test
    fun `agrega audio, nota y foto en loop, en el orden en que se agregaron`() =
        runTest {
            val (_, _, p) = carlos()

            p.addAudio(Evidence(EvidenceSource.VOICE, null, "att-audio-1", "h1"))
            p.addAudio(Evidence(EvidenceSource.VOICE, null, "att-audio-2", "h2"))
            p.addNote("esto es de mi comida")

            assertEquals(4, p.state.value.items.size, "el comprobante, los dos audios y la nota")
            assertEquals(
                "esto es de mi comida",
                p.state.value.items
                    .last()
                    .text,
            )
        }

    @Test
    fun `quitar deja la evidencia a la vista, marcada, y fuera de la interpretacion`() =
        runTest {
            val (_, _, p) = carlos()
            p.addPhoto(Evidence(EvidenceSource.PHOTO, null, "att-de-mas", "h-de-mas"))
            val sobrante =
                p.state.value.items
                    .last()

            p.remove(sobrante.evidenceId)

            val item =
                p.state.value.items
                    .first { it.evidenceId == sobrante.evidenceId }
            assertTrue(item.excluded, "se sigue viendo, pero marcada como quitada")
            assertEquals(
                1,
                p.state.value.items
                    .count { !it.excluded },
            )
        }

    @Test
    fun `el mismo archivo compartido dos veces no entra dos veces`() =
        runTest {
            val (_, _, p) = carlos()

            val agregado = p.addPhoto(comprobante)

            assertFalse(agregado, "misma huella: ya está en el paquete")
            assertEquals(1, p.state.value.items.size)
        }

    @Test
    fun `Listo cierra el paquete`() =
        runTest {
            val (c, bundleId, p) = carlos()

            p.done()

            assertTrue(p.state.value.closed)
            assertEquals(false, c.bundles.load(bundleId)?.open)
        }

    @Test
    fun `salir de la pagina cierra el paquete igual que Listo`() =
        runTest {
            val (c, bundleId, p) = carlos()

            p.onLeave()

            assertEquals(false, c.bundles.load(bundleId)?.open, "salir por cualquier motivo cierra")
        }

    @Test
    fun `cerrar dos veces no rompe nada`() =
        runTest {
            val (c, bundleId, p) = carlos()

            p.done()
            p.onLeave()

            assertEquals(false, c.bundles.load(bundleId)?.open)
        }

    @Test
    fun `sin permiso de microfono la pagina sigue con las demas acciones`() =
        runTest {
            val c = testContainer()
            seedCarlos(c)
            val bundleId = StartBundleUseCase(c.bundles)(c.ownerId, listOf(comprobante))
            val p = ContextPagePresenter(c, bundleId, canRecord = { false }).also { it.load() }

            assertFalse(p.state.value.canRecord)
            assertTrue(p.addNote("una nota"), "escribir nota sigue disponible")
            assertEquals(2, p.state.value.items.size)
        }

    @Test
    fun `la pagina no muestra montos ni saldos`() =
        runTest {
            val (_, _, p) = carlos()
            p.addNote("gaste 25 bs en comida")

            // Lo que se muestra de cada evidencia es su texto y su tipo, nunca un monto interpretado.
            val campos =
                p.state.value.items
                    .map { it.text }

            assertTrue(campos.none { it != null && it.contains("Bs ") }, "la página no interpreta ni muestra montos")
            assertFalse(p.state.value.showsAmounts, "con la app bloqueada no habría nada que ocultar")
        }

    @Test
    fun `una nota en blanco no se agrega`() =
        runTest {
            val (_, _, p) = carlos()

            assertFalse(p.addNote("   "))
            assertEquals(1, p.state.value.items.size)
        }
}
