package com.coinden.app.intent

import com.coinden.core.domain.port.output.LocalModelPort

/**
 * El motor que corre el modelo en el teléfono. Lo implementa `llama.cpp` por NDK, igual que
 * whisper.cpp para la voz.
 *
 * [grammar] no es una sugerencia: el motor la aplica durante la decodificación, enmascarando los
 * tokens que la violarían, así que la respuesta no puede salirse del formato.
 */
interface ModelEngine {
    suspend fun generate(
        systemPrompt: String,
        input: String,
        grammar: String,
        temperature: Float,
    ): String?
}

/**
 * Adaptador del modelo de lenguaje local.
 *
 * Fija lo que el dominio no debe conocer: el prompt de sistema, la temperatura 0 y la aplicación
 * de la gramática. Mientras no haya un motor instalado —porque el teléfono no puede, porque la
 * persona no descargó el modelo o porque todavía no está integrado— devuelve `null`, y eso no es
 * un error: es el caso normal de la mitad de los teléfonos, y la app sigue con el respaldo.
 */
class AndroidLocalModel(
    private val engine: ModelEngine? = null,
) : LocalModelPort {
    override suspend fun complete(
        input: String,
        grammar: String,
    ): String? {
        val motor = engine ?: return null
        // Temperatura 0: la misma captura tiene que dar siempre la misma propuesta (principio 4).
        return runCatching { motor.generate(SYSTEM_PROMPT, input, grammar, temperature = 0f) }.getOrNull()
    }

    companion object {
        /**
         * El prompt es fijo y no se arma con datos de la persona: lo único que varía es la entrada.
         *
         * Está en inglés a propósito, no por descuido: los modelos pequeños (0,5B a 1B) siguen
         * instrucciones bastante mejor en inglés, aunque el texto que interpretan esté en español.
         * No es texto para nadie, es un parámetro del motor; lo que la persona lee sale de los
         * recursos de la app.
         */
        const val SYSTEM_PROMPT =
            "You extract payments from a capture bundle. " +
                "Each line is one piece of evidence: [RECEIPT id] is a payment receipt, [CONTEXT id] is what the person said or wrote. " +
                "A receipt states facts. Context explains them: what it was for, who was paid. " +
                "Rules you must follow: " +
                "never invent an amount that is not in the evidence; " +
                "never change an amount, currency, time or reference that a receipt states; " +
                "only use a wallet or category from the list the grammar allows, or null; " +
                "one payment per real payment, not one per line. " +
                "Answer only with the required JSON. No explanation."
    }
}
