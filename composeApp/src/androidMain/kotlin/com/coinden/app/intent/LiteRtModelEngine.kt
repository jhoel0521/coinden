package com.coinden.app.intent

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * El modelo de lenguaje corriendo en el teléfono con LiteRT-LM.
 *
 * El motor aplica el **JSON Schema durante la decodificación**, así que la respuesta no puede
 * salirse del formato acordado. Eso es lo que hace cumplible el principio 4 del Manifiesto sin
 * confiar en que el modelo obedezca una instrucción.
 *
 * Nada sale del teléfono: el modelo es un archivo local y la inferencia es local.
 *
 * **El *speculative decoding* queda apagado a propósito.** Con el formato forzado y la
 * especulación juntos hay un fallo conocido en Android donde casi ninguna respuesta sirve; con la
 * especulación apagada, la gran mayoría devuelve JSON válido. Se prefiere correcto a rápido.
 */
class LiteRtModelEngine(
    private val modelFile: File,
) : ModelEngine {
    private var engine: Engine? = null

    /** Carga el modelo. Devuelve false si no se pudo: el teléfono sigue con el respaldo. */
    suspend fun load(): Boolean =
        withContext(Dispatchers.IO) {
            if (engine != null) return@withContext true
            if (!modelFile.isFile) return@withContext false
            runCatching {
                Engine(EngineConfig(modelPath = modelFile.absolutePath)).also {
                    it.initialize()
                    engine = it
                }
            }.isSuccess
        }

    override suspend fun generate(
        systemPrompt: String,
        input: String,
        grammar: String,
        temperature: Float,
    ): String? =
        withContext(Dispatchers.IO) {
            if (engine == null && !load()) return@withContext null
            val motor = engine ?: return@withContext null
            runCatching {
                val conversation =
                    motor.createConversation(
                        ConversationConfig(
                            enableResponseFormat = true,
                            enableSpeculativeDecoding = false,
                            // Temperatura 0 y topK 1: la misma captura da siempre la misma propuesta.
                            samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = temperature.toDouble()),
                        ),
                    )
                // El prompt de sistema va al principio del mensaje: construir `Contents` no es
                // público en esta versión, y para el modelo el resultado es el mismo.
                val respuesta = conversation.sendMessage("$systemPrompt\n\n$input", responseFormat = ResponseFormat.json(grammar))
                conversation.close()
                respuesta.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
            }.getOrNull()
        }

    /** Libera el modelo: ocupa memoria y no hace falta entre capturas. */
    fun release() {
        runCatching { engine?.close() }
        engine = null
    }
}
