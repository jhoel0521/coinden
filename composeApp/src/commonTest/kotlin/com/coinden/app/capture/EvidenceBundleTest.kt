package com.coinden.app.capture

import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.usecase.bundle.CloseBundleUseCase
import com.coinden.core.domain.usecase.bundle.StartBundleUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Carlos abre el comprobante de un borrador desde Revisar: si vino en un paquete, ve todo lo que
 * mandó junto, y desde ahí puede quitar lo que no es de ese pago o separarlo.
 */
class EvidenceBundleTest {
    @Test
    fun `el visor de un borrador en paquete muestra todo lo que se mando junto`() =
        runTest {
            val c = testContainer()
            seedCarlos(c)
            val bundleId =
                StartBundleUseCase(c.bundles)(
                    c.ownerId,
                    listOf(Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb"), Evidence(EvidenceSource.MANUAL, "con Ana")),
                )
            val draft = CloseBundleUseCase(c.bundles)(bundleId).first()

            val presenter = EvidencePresenter(c, draft.id).also { it.load() }

            assertEquals(2, presenter.state.value.items.size)
            val bundle = assertNotNull(presenter.bundle)
            bundle.load()
            val nota = bundle.state.value.last()
            bundle.remove(nota.evidenceId)
            assertTrue(
                bundle.state.value
                    .last()
                    .excluded,
                "quitada, pero sigue a la vista",
            )
        }

    @Test
    fun `un borrador sin paquete se ve como siempre`() =
        runTest {
            val c = testContainer()
            seedCarlos(c)
            val draft = c.captureDraft(c.ownerId, Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb")).single()

            val presenter = EvidencePresenter(c, draft.id).also { it.load() }

            assertEquals(1, presenter.state.value.items.size)
            assertNull(presenter.bundle)
        }
}
