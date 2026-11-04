package com.coinden.app.erase

import com.coinden.app.lock.AuthResult
import com.coinden.app.lock.DeviceAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Puerto hacia el borrado total de este dispositivo. Solo borra aquí: no envía ninguna orden ni
 * dato a otros dispositivos ni a ningún servidor.
 */
interface DataEraser {
    /**
     * Borra la base, su llave, los adjuntos, la cola y los restos temporales de la persona; los
     * modelos y paquetes descargados (datos públicos) se quedan. Es idempotente: reintentar
     * completa lo que faltó. Lanza una excepción si no termina.
     */
    suspend fun eraseAll()

    /** Arranca la app como recién instalada (en Android reinicia el proceso). */
    fun restart()
}

/** Pasos del borrado. Desde [Failed] solo se reintenta: la app no vuelve a mostrar datos a medias. */
enum class EraseStep { Idle, Confirming, Authenticating, Erasing, Failed, Done }

/** "Borrar mis datos": confirmar, autenticar si hay bloqueo, borrar y reiniciar. Sin Compose. */
class EraseDataPresenter(
    private val eraser: DataEraser,
    private val auth: DeviceAuth,
    private val lockEnabled: () -> Boolean,
) {
    private val mutableState = MutableStateFlow(EraseStep.Idle)
    val state: StateFlow<EraseStep> = mutableState.asStateFlow()

    fun open() {
        if (state.value == EraseStep.Idle) mutableState.value = EraseStep.Confirming
    }

    fun cancel() {
        if (state.value == EraseStep.Confirming) mutableState.value = EraseStep.Idle
    }

    /** "Borrar todo". Con el bloqueo activo, una autenticación fallida o cancelada no borra nada. */
    suspend fun confirm() {
        if (state.value != EraseStep.Confirming) return
        if (lockEnabled()) {
            mutableState.value = EraseStep.Authenticating
            if (auth.authenticate() != AuthResult.Success) {
                mutableState.value = EraseStep.Confirming
                return
            }
        }
        erase()
    }

    suspend fun retry() {
        if (state.value == EraseStep.Failed) erase()
    }

    private suspend fun erase() {
        mutableState.value = EraseStep.Erasing
        try {
            eraser.eraseAll()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = EraseStep.Failed
            return
        }
        mutableState.value = EraseStep.Done
        eraser.restart()
    }
}
