package com.coinden.app.intent

import com.coinden.core.domain.port.output.LocalModelPort
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Un motor que se carga cuando hace falta y se libera cuando ya no. */
interface LoadableEngine : ModelEngine {
    /** Carga el modelo. Devuelve false si no se pudo: el teléfono sigue con el respaldo. */
    suspend fun load(): Boolean

    fun release()
}

/**
 * El modelo de lenguaje que hay en el teléfono **ahora mismo**.
 *
 * Mira el archivo en cada captura, no solo al abrir la app: un modelo recién descargado se usa en
 * seguida, y uno borrado libera su memoria y deja la interpretación en manos del respaldo. Si el
 * archivo no carga (dañado, sin memoria), se sigue con el respaldo y se avisa una sola vez por
 * archivo ([consumeLoadFailure]); no se reintenta hasta que el archivo cambie.
 *
 * Nunca lanza: sin modelo es el caso normal, no un error.
 */
class InstalledLanguageModel(
    private val file: File,
    private val newEngine: (File) -> LoadableEngine,
) : LocalModelPort {
    private val lock = Mutex()
    private var engine: LoadableEngine? = null
    private var failedStamp: String? = null
    private val failureUnseen = AtomicBoolean(false)

    private fun stamp() = "${file.length()}:${file.lastModified()}"

    override suspend fun complete(
        input: String,
        grammar: String,
    ): String? {
        val motor = ready() ?: return null
        return runCatching { motor.generate(AndroidLocalModel.SYSTEM_PROMPT, input, grammar, temperature = 0f) }.getOrNull()
    }

    private suspend fun ready(): LoadableEngine? =
        lock.withLock {
            if (!file.isFile) {
                engine?.release()
                engine = null
                failedStamp = null
                return@withLock null
            }
            val actual = stamp()
            if (failedStamp == actual) return@withLock null
            engine?.let { return@withLock it }
            val nuevo = newEngine(file)
            if (runCatching { nuevo.load() }.getOrDefault(false)) {
                engine = nuevo
                nuevo
            } else {
                failedStamp = actual
                failureUnseen.set(true)
                null
            }
        }

    /** true la primera vez que se pregunta después de un fallo de carga; después, false. */
    fun consumeLoadFailure(): Boolean = failureUnseen.getAndSet(false)

    /** Libera la memoria del motor, por ejemplo al borrar el modelo desde la pantalla. */
    suspend fun release() =
        lock.withLock {
            engine?.release()
            engine = null
        }
}
