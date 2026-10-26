package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.port.output.BundlePayment
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.port.output.IntentInterpreterPort
import com.coinden.core.domain.port.output.LocalModelPort

/**
 * Le pide al modelo local que entienda el paquete, y devuelve propuestas del dominio.
 *
 * Arma tres cosas y confía en ninguna: la entrada (los textos con su papel), la gramática que
 * cierra las respuestas posibles, y la lectura de lo que vuelva. Si el modelo no está o contesta
 * cualquier cosa, devuelve `null` y manda el respaldo determinista.
 *
 * El prompt de sistema y la temperatura 0 los fija el adaptador del motor, no el dominio: acá no
 * hay textos para nadie, solo datos.
 */
class ModelIntentInterpreter(
    private val model: LocalModelPort,
) : IntentInterpreterPort {
    override suspend fun interpret(
        texts: List<BundleText>,
        wallets: WalletDirectory,
        categories: Set<String>,
    ): List<BundlePayment>? {
        val utiles = texts.filter { it.text.isNotBlank() }
        // Sin nada que interpretar no se despierta al modelo: cargarlo cuesta memoria y batería.
        if (utiles.isEmpty()) return null

        val respuesta = model.complete(input = inputOf(utiles), grammar = IntentGrammar.of(wallets, categories)) ?: return null
        return IntentResponseReader.read(respuesta, wallets, categories)
    }

    /**
     * El paquete como entrada: cada evidencia con su id y su papel, en el orden en que llegó.
     * El papel es lo que le permite al modelo distinguir un hecho (el comprobante) de una
     * explicación (lo que la persona dijo).
     */
    private fun inputOf(texts: List<BundleText>): String =
        texts.joinToString("\n") { "[${it.role.name} ${it.evidenceId}] ${it.text.trim()}" }
}
