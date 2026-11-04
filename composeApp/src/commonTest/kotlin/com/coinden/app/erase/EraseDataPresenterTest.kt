package com.coinden.app.erase

import com.coinden.app.lock.AuthResult
import com.coinden.app.lock.FakeDeviceAuth
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El teléfono simulado: lo de la persona (base, llave, fotos, preferencias). Borra en el orden del
 * diseño y puede cortarse una vez después de la base.
 */
class FakeDataEraser(
    var failAfterDatabase: Boolean = false,
) : DataEraser {
    val personal = mutableSetOf("base", "llave", "fotos", "audios", "cola", "preferencias")
    var restarts = 0

    override suspend fun eraseAll() {
        personal -= setOf("base", "llave")
        if (failAfterDatabase) {
            failAfterDatabase = false
            error("se cortó a mitad")
        }
        personal.clear()
    }

    override fun restart() {
        restarts++
    }
}

class EraseDataPresenterTest {
    private fun presenter(
        eraser: FakeDataEraser = FakeDataEraser(),
        auth: FakeDeviceAuth = FakeDeviceAuth(),
        lockEnabled: Boolean = false,
    ) = EraseDataPresenter(eraser, auth) { lockEnabled }

    @Test
    fun `Carlos abre la confirmacion y cancela sin borrar nada`() =
        runTest {
            val eraser = FakeDataEraser()
            val p = presenter(eraser)
            assertEquals(EraseStep.Idle, p.state.value)
            p.open()
            assertEquals(EraseStep.Confirming, p.state.value)
            assertEquals(6, eraser.personal.size)
            p.cancel()
            assertEquals(EraseStep.Idle, p.state.value)
            assertEquals(6, eraser.personal.size)
            assertEquals(0, eraser.restarts)
        }

    @Test
    fun `Carlos con el bloqueo activo se autentica y sus datos se borran`() =
        runTest {
            val eraser = FakeDataEraser()
            val auth = FakeDeviceAuth()
            val p = presenter(eraser, auth, lockEnabled = true)
            p.open()
            p.confirm()
            assertEquals(1, auth.asked)
            assertEquals(EraseStep.Done, p.state.value)
            assertTrue(eraser.personal.isEmpty())
            assertEquals(1, eraser.restarts)
        }

    @Test
    fun `Carlos cancela la autenticacion y no se borra nada`() =
        runTest {
            val eraser = FakeDataEraser()
            val p = presenter(eraser, FakeDeviceAuth(result = AuthResult.Cancelled), lockEnabled = true)
            p.open()
            p.confirm()
            assertEquals(EraseStep.Confirming, p.state.value)
            assertEquals(6, eraser.personal.size)
            assertEquals(0, eraser.restarts)
        }

    @Test
    fun `una autenticacion fallida tampoco borra`() =
        runTest {
            val eraser = FakeDataEraser()
            val p = presenter(eraser, FakeDeviceAuth(result = AuthResult.Failed), lockEnabled = true)
            p.open()
            p.confirm()
            assertEquals(6, eraser.personal.size)
        }

    @Test
    fun `John sin bloqueo borra sin autenticacion adicional`() =
        runTest {
            val eraser = FakeDataEraser()
            val auth = FakeDeviceAuth()
            val p = presenter(eraser, auth, lockEnabled = false)
            p.open()
            p.confirm()
            assertEquals(0, auth.asked)
            assertEquals(EraseStep.Done, p.state.value)
            assertTrue(eraser.personal.isEmpty())
        }

    @Test
    fun `confirmar sin abrir la confirmacion no borra`() =
        runTest {
            val eraser = FakeDataEraser()
            presenter(eraser).confirm()
            assertEquals(6, eraser.personal.size)
        }

    @Test
    fun `el borrado de Carlos se corta despues de la base y reintentar elimina lo que faltaba`() =
        runTest {
            val eraser = FakeDataEraser(failAfterDatabase = true)
            val p = presenter(eraser)
            p.open()
            p.confirm()
            assertEquals(EraseStep.Failed, p.state.value)
            assertFalse("base" in eraser.personal)
            assertTrue("fotos" in eraser.personal)
            assertEquals(0, eraser.restarts)
            // Cancelar ya no vuelve a una app con datos parciales: solo queda reintentar.
            p.cancel()
            assertEquals(EraseStep.Failed, p.state.value)
            p.retry()
            assertEquals(EraseStep.Done, p.state.value)
            assertTrue(eraser.personal.isEmpty())
            assertEquals(1, eraser.restarts)
        }
}
