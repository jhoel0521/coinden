package com.coinden.app.intent

import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest

/** Si el teléfono tiene el modelo del sistema listo para usar. */
enum class SystemModelStatus { UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE }

/** Lo que la app necesita del modelo del sistema; detrás está ML Kit, aquí se puede falsear. */
interface SystemModelClient {
    suspend fun status(): SystemModelStatus

    suspend fun generate(
        systemPrompt: String,
        input: String,
        temperature: Float,
    ): String?
}

/**
 * El modelo del sistema (Gemini Nano, vía ML Kit GenAI) como motor del intérprete de intenciones.
 *
 * Solo se consulta si el teléfono lo tiene listo **y** la persona aceptó el motor de Google
 * ([enabled] se vuelve a mirar en cada captura). Si no, devuelve `null` y mandan las reglas: es el
 * caso normal de casi todos los teléfonos. Nunca lanza.
 *
 * Esta API no obliga el esquema JSON durante la decodificación, así que se pide en el prompt, se
 * quitan las vallas de código y lo demás lo verifica `IntentResponseReader` y el validador.
 */
class GeminiNanoEngine(
    private val client: SystemModelClient,
    private val enabled: suspend () -> Boolean,
) : ModelEngine {
    override suspend fun generate(
        systemPrompt: String,
        input: String,
        grammar: String,
        temperature: Float,
    ): String? {
        if (!enabled()) return null
        return runCatching {
            if (client.status() != SystemModelStatus.AVAILABLE) return@runCatching null
            // Temperatura 0 siempre: la misma captura tiene que dar la misma propuesta.
            client.generate(systemPrompt, input, temperature = 0f)?.let(::stripFences)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun stripFences(text: String): String =
        text
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
}

/** El cliente real: ML Kit GenAI sobre AICore. No descarga nada propio; el sistema gestiona su modelo. */
class MlKitSystemModelClient : SystemModelClient {
    private val model: GenerativeModel by lazy { Generation.getClient() }

    override suspend fun status(): SystemModelStatus =
        when (model.checkStatus()) {
            // FeatureStatus de ML Kit: 0 no disponible, 1 descargable, 2 descargando, 3 disponible.
            3 -> SystemModelStatus.AVAILABLE
            2 -> SystemModelStatus.DOWNLOADING
            1 -> SystemModelStatus.DOWNLOADABLE
            else -> SystemModelStatus.UNAVAILABLE
        }

    override suspend fun generate(
        systemPrompt: String,
        input: String,
        temperature: Float,
    ): String? {
        val request =
            generateContentRequest(TextPart("$systemPrompt\n\n$input")) {
                this.temperature = temperature
                topK = 1
                candidateCount = 1
            }
        return model
            .generateContent(request)
            .candidates
            .firstOrNull()
            ?.text
    }
}
