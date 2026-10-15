package com.coinden.app.capture

/** Lo que dejó un dictado. */
sealed interface DictationOutcome {
    /** El texto final reconocido. */
    data class Heard(
        val text: String,
    ) : DictationOutcome

    /** Silencio, cancelado o no se entendió: no se crea borrador. */
    data object NothingHeard : DictationOutcome

    /** El teléfono no tiene reconocimiento de voz local: nunca se usa uno en la nube. */
    data object Unavailable : DictationOutcome
}

/** Reconocimiento de voz del propio teléfono. El audio no se guarda ni sale del dispositivo. */
interface DictationPort {
    /** Escucha hasta detectar silencio, en [languageCode] ("es", "en", "pt"). */
    suspend fun listen(languageCode: String): DictationOutcome

    /** Corta la escucha en curso; [listen] devuelve [DictationOutcome.NothingHeard]. */
    fun cancel()
}
