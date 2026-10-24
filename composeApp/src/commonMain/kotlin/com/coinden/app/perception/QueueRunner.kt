package com.coinden.app.perception

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registro de intentos de la cola en este dispositivo. No es un dato del dominio: otro
 * dispositivo puede intentar leer el mismo borrador.
 */
interface FailureLog {
    /** Suma un intento fallido y devuelve cuántos lleva. */
    suspend fun fail(draftId: String): Int

    /** El borrador ya no se procesa en la cola (agotó sus intentos, no tiene texto o es muy largo). */
    suspend fun settle(draftId: String)

    suspend fun isSettled(draftId: String): Boolean

    /** Se leyó bien o dejó de estar pendiente: se olvidan sus intentos. */
    suspend fun clear(draftId: String)
}

class InMemoryFailureLog : FailureLog {
    private val attempts = mutableMapOf<String, Int>()
    private val settled = mutableSetOf<String>()

    override suspend fun fail(draftId: String): Int = ((attempts[draftId] ?: 0) + 1).also { attempts[draftId] = it }

    override suspend fun settle(draftId: String) {
        settled += draftId
    }

    override suspend fun isSettled(draftId: String) = draftId in settled

    override suspend fun clear(draftId: String) {
        attempts -= draftId
        settled -= draftId
    }
}

/** Lo que hizo una pasada de la cola. */
data class DrainResult(
    val processed: Int,
    val failed: Int,
    /** Quedó algo que falló y conviene reintentar más tarde. */
    val retryLater: Boolean,
)

/**
 * Cola de lo pendiente: procesa de a uno, en orden de captura, los borradores Capturados con
 * archivo que todavía no se leyeron. Lo pendiente sale de los datos, así una pasada cortada se
 * retoma sin repetir. Al terminar libera los modelos. No usa Compose ni Android.
 */
class QueueRunner(
    private val service: PerceptionService,
    val readers: PerceptionReaders,
    private val failures: FailureLog,
    private val maxAttempts: Int = 3,
) {
    private val mutex = Mutex()
    private val processedFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    /** Cada borrador procesado, para que la pantalla abierta lo actualice. */
    val processed: SharedFlow<String> = processedFlow.asSharedFlow()

    suspend fun drain(): DrainResult =
        mutex.withLock {
            var processed = 0
            var failed = 0
            var retryLater = false
            try {
                for (draftId in service.pendingDraftIds()) {
                    if (failures.isSettled(draftId)) continue
                    val outcome =
                        try {
                            service.perceive(draftId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            failed++
                            if (failures.fail(draftId) >= maxAttempts) failures.settle(draftId) else retryLater = true
                            continue
                        }
                    when (outcome) {
                        is PerceptionOutcome.Read -> failures.clear(draftId)
                        // Sin texto o demasiado largo: reintentarlo daría lo mismo.
                        PerceptionOutcome.NothingRead, PerceptionOutcome.TooLong -> failures.settle(draftId)
                        // Espera un modelo de voz: no es una falla; se procesa al descargarlo.
                        PerceptionOutcome.NeedsVoiceModel, PerceptionOutcome.Skipped -> continue
                    }
                    processed++
                    processedFlow.emit(draftId)
                }
                // Los paquetes cerrados se leen de a una evidencia; el dominio interpreta solo
                // cuando ya no queda nada por leer. Los borradores sueltos de la 0.1.0 siguen por
                // el camino de arriba, intactos.
                for (bundleId in service.pendingBundleIds()) {
                    if (failures.isSettled(bundleId)) continue
                    val outcome =
                        try {
                            service.perceiveBundle(bundleId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            failed++
                            if (failures.fail(bundleId) >= maxAttempts) failures.settle(bundleId) else retryLater = true
                            continue
                        }
                    when (outcome) {
                        is PerceptionOutcome.Read -> failures.clear(bundleId)
                        PerceptionOutcome.NothingRead, PerceptionOutcome.TooLong -> failures.clear(bundleId)
                        PerceptionOutcome.NeedsVoiceModel, PerceptionOutcome.Skipped -> continue
                    }
                    processed++
                    processedFlow.emit(bundleId)
                    // Queda alguna evidencia más de este paquete: se retoma en la próxima vuelta.
                    retryLater = retryLater || service.pendingBundleIds().contains(bundleId)
                }
            } finally {
                readers.release()
            }
            DrainResult(processed, failed, retryLater)
        }
}

/**
 * La cola vista desde la app: [kick] avisa que hay algo nuevo y vuelve enseguida; en Android la
 * trabaja WorkManager aunque se cierre la app. [processed] avisa cada borrador procesado.
 */
interface ProcessingQueue {
    suspend fun kick()

    val processed: SharedFlow<String>
}

/** Cola que procesa en el momento, en la misma corrutina (pruebas y plataformas sin segundo plano). */
class InlineProcessingQueue(
    private val runner: QueueRunner,
) : ProcessingQueue {
    var kicks = 0
        private set

    override suspend fun kick() {
        kicks++
        runner.drain()
    }

    override val processed: SharedFlow<String> get() = runner.processed
}
