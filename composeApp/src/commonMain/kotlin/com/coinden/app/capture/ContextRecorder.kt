package com.coinden.app.capture

import com.coinden.core.domain.model.draft.Evidence

/** Cómo terminó una grabación. */
sealed interface RecordingOutcome {
    /** El audio quedó guardado y cifrado, listo para sumarse al paquete. */
    data class Recorded(
        val evidence: Evidence,
    ) : RecordingOutcome

    /** La persona no dio permiso de micrófono. La página sigue con las demás acciones. */
    data object NoPermission : RecordingOutcome

    /** La persona canceló, o no se grabó nada. */
    data object Nothing : RecordingOutcome

    /** El teléfono no pudo grabar. No es culpa de la persona y no rompe la captura. */
    data object Failed : RecordingOutcome
}

/**
 * Graba el audio que explica un pago, dentro del teléfono.
 *
 * Nunca lanza: quedarse sin micrófono o sin permiso es un caso normal, no un error, y la página
 * de contexto tiene que seguir funcionando con escribir nota y sumar foto.
 */
interface ContextRecorder {
    suspend fun record(): RecordingOutcome

    /** Corta la grabación en curso, por ejemplo si la persona sale de la página. */
    fun cancel()
}
