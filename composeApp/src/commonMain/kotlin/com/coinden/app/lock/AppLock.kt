package com.coinden.app.lock

import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.PreferencesRepositoryPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Lugares de la app y si muestran o cambian saldos. */
enum class LockedDestination { HOME, REVIEW, MOVEMENTS, PROFILE, SETTINGS, CONFIRM, EVIDENCE, CAPTURE, FORM_NEW }

/**
 * Estado del bloqueo. [enabled]: la persona lo activó. [unlocked]: se autenticó en esta sesión.
 * [backgroundSinceMs]: desde cuándo la app está en segundo plano. [lostScreenLock]: el teléfono
 * ya no tiene bloqueo de pantalla y CoinDen apagó el suyo para no dejar a nadie fuera.
 */
data class AppLockState(
    val enabled: Boolean = false,
    val unlocked: Boolean = true,
    val backgroundSinceMs: Long? = null,
    val lostScreenLock: Boolean = false,
) {
    val locked: Boolean get() = enabled && !unlocked
}

/** Regla pura del bloqueo: cuándo se bloquea y qué protege. Capturar nunca pide desbloqueo. */
object AppLock {
    /** Tiempo en segundo plano a partir del cual se vuelve a bloquear. */
    const val RELOCK_AFTER_MS = 60_000L

    private val open = setOf(LockedDestination.CAPTURE, LockedDestination.FORM_NEW)

    fun start(enabled: Boolean): AppLockState = AppLockState(enabled = enabled, unlocked = !enabled)

    fun onBackground(
        state: AppLockState,
        now: Long,
    ): AppLockState = if (state.enabled) state.copy(backgroundSinceMs = now) else state

    fun onForeground(
        state: AppLockState,
        now: Long,
    ): AppLockState {
        val since = state.backgroundSinceMs ?: return state
        return state.copy(backgroundSinceMs = null, unlocked = state.unlocked && now - since < RELOCK_AFTER_MS)
    }

    fun unlock(state: AppLockState): AppLockState = state.copy(unlocked = true)

    fun requiresUnlock(destination: LockedDestination): Boolean = destination !in open
}

/**
 * El bloqueo de la app en vivo: lee la elección guardada, pide la autenticación del teléfono y
 * comparte el estado con la barra, las pestañas y la confirmación.
 */
class AppLockController(
    private val preferences: PreferencesRepositoryPort,
    private val clock: ClockPort,
    private val auth: DeviceAuth,
) {
    private val mutableState = MutableStateFlow(AppLockState())
    val state: StateFlow<AppLockState> = mutableState.asStateFlow()

    private var loaded = false

    private val mutableAvailability = MutableStateFlow(AuthAvailability.Unsupported)

    /** Si el teléfono puede autenticar; se vuelve a mirar cada vez que la app vuelve al frente. */
    val availability: StateFlow<AuthAvailability> = mutableAvailability.asStateFlow()

    fun openScreenLockSettings() = auth.openScreenLockSettings()

    /** Carga la elección la primera vez; recrear la pantalla (girarla) no vuelve a bloquear. */
    suspend fun loadOnce() {
        if (loaded) return
        load()
        loaded = true
    }

    suspend fun load() {
        mutableAvailability.value = auth.availability()
        val enabled = preferences.appLockEnabled()
        if (enabled && auth.availability() != AuthAvailability.Available) {
            preferences.setAppLockEnabled(false)
            mutableState.value = AppLockState(lostScreenLock = true)
            return
        }
        mutableState.value = AppLock.start(enabled)
    }

    fun onBackground() = mutableState.update { AppLock.onBackground(it, clock.nowEpochMs()) }

    fun onForeground() {
        // Quien fue a configurar el bloqueo de pantalla vuelve y lo encuentra disponible.
        mutableAvailability.value = auth.availability()
        mutableState.update { AppLock.onForeground(it, clock.nowEpochMs()) }
    }

    /** Desbloqueada sigue sin preguntar; bloqueada pide la autenticación y solo pasa con éxito. */
    suspend fun ensureUnlocked(): Boolean {
        if (!state.value.locked) return true
        if (auth.authenticate() != AuthResult.Success) return false
        mutableState.update(AppLock::unlock)
        return true
    }

    /** Activar o desactivar pide autenticarse una vez; si no se logra, la elección no cambia. */
    suspend fun setEnabled(enabled: Boolean): Boolean {
        if (auth.availability() != AuthAvailability.Available) return false
        if (auth.authenticate() != AuthResult.Success) return false
        preferences.setAppLockEnabled(enabled)
        mutableState.value = AppLockState(enabled = enabled, unlocked = true)
        return true
    }
}
