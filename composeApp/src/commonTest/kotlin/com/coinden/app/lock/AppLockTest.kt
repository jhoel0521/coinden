package com.coinden.app.lock

import com.coinden.app.AppStorage
import com.coinden.core.domain.port.output.ClockPort
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Reloj que solo avanza cuando la prueba lo pide. */
private class ManualClock(
    var now: Long = 1_760_000_000_000,
) : ClockPort {
    override fun nowEpochMs(): Long = now
}

/** Autenticación del teléfono simulada: disponibilidad y resultado fijos, y cuántas veces se pidió. */
class FakeDeviceAuth(
    var available: AuthAvailability = AuthAvailability.Available,
    var result: AuthResult = AuthResult.Success,
) : DeviceAuth {
    var asked = 0
    var settingsOpened = 0

    override fun availability(): AuthAvailability = available

    override fun openScreenLockSettings() {
        settingsOpened++
    }

    override suspend fun authenticate(): AuthResult {
        asked++
        return result
    }
}

class AppLockTest {
    @Test
    fun `desactivado nunca bloquea`() {
        var state = AppLock.start(enabled = false)
        assertFalse(state.locked)
        state = AppLock.onBackground(state, now = 0)
        state = AppLock.onForeground(state, now = 10 * 60_000)
        assertFalse(state.locked)
    }

    @Test
    fun `activo arranca bloqueado`() {
        assertTrue(AppLock.start(enabled = true).locked)
    }

    @Test
    fun `Carlos vuelve en 30 segundos y sigue desbloqueado, en 2 minutos se bloquea`() {
        val open = AppLock.unlock(AppLock.start(enabled = true))
        val quick = AppLock.onForeground(AppLock.onBackground(open, now = 0), now = 30_000)
        assertFalse(quick.locked)
        val late = AppLock.onForeground(AppLock.onBackground(open, now = 0), now = 2 * 60_000)
        assertTrue(late.locked)
    }

    @Test
    fun `el limite es exactamente un minuto`() {
        val open = AppLock.unlock(AppLock.start(enabled = true))
        assertFalse(AppLock.onForeground(AppLock.onBackground(open, now = 0), now = 59_999).locked)
        assertTrue(AppLock.onForeground(AppLock.onBackground(open, now = 0), now = 60_000).locked)
    }

    @Test
    fun `saldos y ajustes piden desbloqueo, capturar no`() {
        val protected =
            listOf(
                LockedDestination.HOME,
                LockedDestination.REVIEW,
                LockedDestination.MOVEMENTS,
                LockedDestination.PROFILE,
                LockedDestination.SETTINGS,
                LockedDestination.CONFIRM,
                LockedDestination.EVIDENCE,
            )
        protected.forEach { assertTrue(AppLock.requiresUnlock(it), "$it") }
        assertFalse(AppLock.requiresUnlock(LockedDestination.CAPTURE))
        assertFalse(AppLock.requiresUnlock(LockedDestination.FORM_NEW))
    }

    private fun controller(
        auth: FakeDeviceAuth = FakeDeviceAuth(),
        storage: AppStorage = AppStorage.inMemory(),
        clock: ManualClock = ManualClock(),
    ) = AppLockController(storage.preferences, clock, auth)

    @Test
    fun `activar con la huella guarda la eleccion y queda desbloqueada`() =
        runTest {
            val storage = AppStorage.inMemory()
            val lock = controller(storage = storage)
            lock.load()
            assertTrue(lock.setEnabled(true))
            assertTrue(storage.preferences.appLockEnabled())
            assertTrue(lock.state.value.enabled)
            assertFalse(lock.state.value.locked)
        }

    @Test
    fun `si Carlos cancela la autenticacion el bloqueo sigue apagado`() =
        runTest {
            val storage = AppStorage.inMemory()
            val lock = controller(FakeDeviceAuth(result = AuthResult.Cancelled), storage)
            lock.load()
            assertFalse(lock.setEnabled(true))
            assertFalse(storage.preferences.appLockEnabled())
            listOf(AuthResult.Failed).forEach {
                val failing = controller(FakeDeviceAuth(result = it), storage)
                assertFalse(failing.setEnabled(true))
            }
            assertFalse(storage.preferences.appLockEnabled())
        }

    @Test
    fun `sin bloqueo de pantalla en el telefono no se puede activar`() =
        runTest {
            val auth = FakeDeviceAuth(available = AuthAvailability.NoScreenLock)
            val lock = controller(auth)
            lock.load()
            assertFalse(lock.setEnabled(true))
            assertEquals(0, auth.asked)
        }

    @Test
    fun `al reabrir con el bloqueo activo la app arranca bloqueada`() =
        runTest {
            val storage = AppStorage.inMemory()
            storage.preferences.setAppLockEnabled(true)
            val lock = controller(storage = storage)
            lock.load()
            assertTrue(lock.state.value.locked)
        }

    @Test
    fun `confirmar bloqueada desbloquea solo con la autenticacion`() =
        runTest {
            val storage = AppStorage.inMemory()
            storage.preferences.setAppLockEnabled(true)
            val auth = FakeDeviceAuth(result = AuthResult.Cancelled)
            val lock = controller(auth, storage)
            lock.load()
            assertFalse(lock.ensureUnlocked())
            assertTrue(lock.state.value.locked)
            auth.result = AuthResult.Success
            assertTrue(lock.ensureUnlocked())
            assertFalse(lock.state.value.locked)
        }

    @Test
    fun `desbloqueada no vuelve a pedir la huella para confirmar`() =
        runTest {
            val auth = FakeDeviceAuth()
            val lock = controller(auth)
            lock.load()
            assertTrue(lock.ensureUnlocked())
            assertEquals(0, auth.asked)
        }

    @Test
    fun `el controlador vuelve a bloquear tras un minuto en segundo plano`() =
        runTest {
            val storage = AppStorage.inMemory()
            storage.preferences.setAppLockEnabled(true)
            val clock = ManualClock()
            val lock = controller(storage = storage, clock = clock)
            lock.load()
            lock.ensureUnlocked()
            lock.onBackground()
            clock.now += 2 * 60_000
            lock.onForeground()
            assertTrue(lock.state.value.locked)
        }

    @Test
    fun `si el telefono pierde su bloqueo de pantalla CoinDen se desbloquea y lo apaga`() =
        runTest {
            val storage = AppStorage.inMemory()
            storage.preferences.setAppLockEnabled(true)
            val lock = controller(FakeDeviceAuth(available = AuthAvailability.NoScreenLock), storage)
            lock.load()
            assertFalse(lock.state.value.locked)
            assertTrue(lock.state.value.lostScreenLock)
            assertFalse(storage.preferences.appLockEnabled())
        }

    @Test
    fun `Carlos configura el bloqueo de pantalla y al volver ya puede activar el de CoinDen`() =
        runTest {
            val auth = FakeDeviceAuth(available = AuthAvailability.NoScreenLock)
            val lock = controller(auth)
            lock.load()
            assertEquals(AuthAvailability.NoScreenLock, lock.availability.value)
            lock.openScreenLockSettings()
            assertEquals(1, auth.settingsOpened)
            // Configura un PIN en el teléfono y vuelve a CoinDen.
            auth.available = AuthAvailability.Available
            lock.onForeground()
            assertEquals(AuthAvailability.Available, lock.availability.value)
            assertTrue(lock.setEnabled(true))
        }
}
