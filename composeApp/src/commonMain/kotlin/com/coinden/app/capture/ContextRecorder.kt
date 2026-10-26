package com.coinden.app.capture

import com.coinden.core.domain.model.draft.Evidence

/** Cómo salió el intento de empezar a grabar. */
sealed interface RecordingStart {
    /** Está grabando. La persona habla y después toca detener. */
    data object Started : RecordingStart

    /** La persona no dio permiso de micrófono. La página sigue con las demás acciones. */
    data object NoPermission : RecordingStart

    /** El teléfono no pudo grabar. No es culpa de la persona y no rompe la captura. */
    data object Failed : RecordingStart
}

/**
 * Graba el audio que explica un pago, dentro del teléfono.
 *
 * Grabar tiene dos momentos, y el puerto los refleja: [start] abre el micrófono y [stop] cierra y
 * devuelve lo grabado, ya cifrado. Entre uno y otro, la persona habla el tiempo que quiera.
 *
 * Nunca lanza: quedarse sin micrófono o sin permiso es un caso normal, no un error, y la página
 * de contexto tiene que seguir funcionando con escribir nota y sumar foto.
 */
interface ContextRecorder {
    suspend fun start(): RecordingStart

    /** Cierra la grabación y devuelve el audio cifrado, o null si no se grabó nada. */
    suspend fun stop(): Evidence?

    /** Corta y descarta lo grabado, por ejemplo si la persona sale de la página. */
    fun cancel()
}
