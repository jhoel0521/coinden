package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.usecase.bundle.AttachEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.CloseBundleUseCase
import com.coinden.core.domain.usecase.bundle.ExcludeEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.PerceiveBundleEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.StartBundleUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El visor de evidencias de un paquete, en la revisión de la noche: qué mandó Carlos, en qué
 * orden y qué se leyó de cada cosa. Lo que quitó sigue ahí, marcado.
 */
class BundleEvidenceViewerTest {
    private fun receipt(text: String) = Evidence(EvidenceSource.TEXT, text)

    private fun audio(ref: String) = Evidence(EvidenceSource.VOICE, attachmentRef = ref, fingerprint = "huella-$ref")

    private fun photo(ref: String) = Evidence(EvidenceSource.PHOTO, attachmentRef = ref, fingerprint = "huella-$ref")

    private suspend fun carlos(): Triple<AppContainer, String, BundleEvidenceViewer> {
        val c = testContainer()
        seedCarlos(c)
        val id = StartBundleUseCase(c.bundles)(c.ownerId, listOf(receipt("Importe Bs25.00")))
        return Triple(c, id, BundleEvidenceViewer(c, id))
    }

    @Test
    fun `muestra cada evidencia en el orden en que llego`() =
        runTest {
            val (c, id, viewer) = carlos()
            AttachEvidenceUseCase(c.bundles)(id, audio("voz"))
            AttachEvidenceUseCase(c.bundles)(id, photo("foto"))
            viewer.load()

            assertEquals(
                listOf(EvidenceSource.TEXT, EvidenceSource.VOICE, EvidenceSource.PHOTO),
                viewer.state.value.map { it.source },
            )
        }

    @Test
    fun `cada evidencia muestra lo que se leyo de ella`() =
        runTest {
            val (c, id, viewer) = carlos()
            AttachEvidenceUseCase(c.bundles)(id, audio("voz"))
            CloseBundleUseCase(c.bundles)(id)
            val audioId =
                c.bundles
                    .load(id)!!
                    .active
                    .first { it.needsReading }
                    .id
            PerceiveBundleEvidenceUseCase(c.bundles)(id, audioId, "esto es de mi comida", "voz")
            viewer.load()

            val leido = viewer.state.value.first { it.evidenceId == audioId }
            assertEquals("esto es de mi comida", leido.text)
            assertFalse(leido.pendingRead)
        }

    @Test
    fun `un audio que todavia no se leyo lo dice`() =
        runTest {
            val (c, id, viewer) = carlos()
            AttachEvidenceUseCase(c.bundles)(id, audio("voz"))
            viewer.load()

            val sinLeer = viewer.state.value.first { it.source == EvidenceSource.VOICE }
            assertTrue(sinLeer.pendingRead, "todavía no se transcribió")
            assertEquals(null, sinLeer.text)
        }

    @Test
    fun `lo quitado se sigue viendo, marcado`() =
        runTest {
            val (c, id, viewer) = carlos()
            AttachEvidenceUseCase(c.bundles)(id, photo("de-mas"))
            val sobrante =
                c.bundles
                    .load(id)!!
                    .evidences
                    .last()
                    .id
            ExcludeEvidenceUseCase(c.bundles)(id, sobrante)
            viewer.load()

            assertEquals(2, viewer.state.value.size, "no se borra: se marca")
            assertTrue(
                viewer.state.value
                    .first { it.evidenceId == sobrante }
                    .excluded,
            )
        }

    @Test
    fun `quitar desde el visor saca la evidencia de la interpretacion`() =
        runTest {
            val (c, id, viewer) = carlos()
            AttachEvidenceUseCase(c.bundles)(id, photo("de-mas"))
            viewer.load()
            val sobrante =
                viewer.state.value
                    .last()
                    .evidenceId

            viewer.remove(sobrante)

            assertTrue(
                viewer.state.value
                    .first { it.evidenceId == sobrante }
                    .excluded,
            )
            assertEquals(1, viewer.state.value.count { !it.excluded })
        }

    @Test
    fun `separar manda la evidencia a un paquete propio`() =
        runTest {
            val (c, id, viewer) = carlos()
            AttachEvidenceUseCase(c.bundles)(id, receipt("Importe Bs60.00"))
            CloseBundleUseCase(c.bundles)(id)
            viewer.load()
            val otro =
                viewer.state.value
                    .last()
                    .evidenceId

            val nuevoPaquete = viewer.split(otro)

            assertTrue(nuevoPaquete != null && nuevoPaquete != id, "era otro pago: va a su propio paquete")
            assertEquals(1, viewer.state.value.count { !it.excluded }, "sale de este paquete")
        }

    @Test
    fun `un paquete que no existe no rompe el visor`() =
        runTest {
            val c = testContainer()
            seedCarlos(c)
            val viewer = BundleEvidenceViewer(c, "no-existe")

            viewer.load()

            assertEquals(emptyList(), viewer.state.value)
        }
}
