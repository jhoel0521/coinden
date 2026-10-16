package com.coinden.app.perception

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.coinden.app.AppContainer
import com.coinden.app.CoinDenApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Lectura en segundo plano de toda la aplicación (una por proceso): los lectores, la cola y los
 * modelos de voz. La comparten la pantalla y el trabajador de WorkManager; al vaciarse la cola se
 * liberan los modelos cargados.
 */
class AppPerception(
    context: Context,
    container: AppContainer,
) {
    private val appContext = context.applicationContext
    val modelStore = VoiceModelStore(appContext)

    // Modelos cargados: se crean al primer uso y se liberan con release().
    private val loaded = Mutex()
    private var mlKit: MlKitImageReader? = null
    private val whispers = mutableMapOf<String, WhisperTranscriber>()

    val readers =
        PerceptionReaders(
            freeImageReader = TesseractImageReader(appContext) { container.languageCode },
            googleImageReader = { synchronized(this) { mlKit ?: MlKitImageReader().also { mlKit = it } } },
            pdfReader = AndroidPdfReader(appContext),
            audioDecoder = AndroidAudioDecoder(),
            transcribers = {
                // El modelo descargado más preciso y, de respaldo, el reconocimiento local del sistema.
                val whisper =
                    modelStore.installed().firstOrNull()?.let { model ->
                        loaded.withLock { whispers.getOrPut(model.id) { WhisperTranscriber(modelStore.fileOf(model)) } }
                    }
                listOfNotNull(whisper, SystemSpeechTranscriber(appContext).takeIf { SystemSpeechTranscriber.isAvailable(appContext) })
            },
            release = {
                loaded.withLock {
                    whispers.values.forEach { it.close() }
                    whispers.clear()
                }
                synchronized(this) {
                    mlKit?.close()
                    mlKit = null
                }
            },
        )

    val service = PerceptionService(container, readers)
    val runner = QueueRunner(service, readers, FileFailureLog(File(appContext.filesDir, "queue-failures.tsv")))
    val queue: ProcessingQueue = WorkManagerQueue(appContext, runner)

    fun forgetVoiceModel(model: VoiceModel) {
        whispers.remove(model.id)
        modelStore.delete(model)
    }
}

/** La cola en WorkManager: sigue aunque se cierre la app y se retoma después de un reinicio. */
private class WorkManagerQueue(
    private val context: Context,
    runner: QueueRunner,
) : ProcessingQueue {
    override val processed: SharedFlow<String> = runner.processed

    override suspend fun kick() {
        val request =
            OneTimeWorkRequestBuilder<PerceptionWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        // Una captura nueva mientras la cola corre encadena otra pasada; nunca dos a la vez.
        WorkManager.getInstance(context).enqueueUniqueWork(QUEUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val QUEUE_NAME = "coinden-queue"
    }
}

/** Trabaja una pasada de la cola. Si algo falló y conviene reintentar, WorkManager lo repite más tarde. */
class PerceptionWorker
    @JvmOverloads
    constructor(
        context: Context,
        params: WorkerParameters,
        private val runner: QueueRunner? = null,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val queue = runner ?: (applicationContext as CoinDenApplication).perception.runner
            val result = queue.drain()
            return if (result.retryLater) Result.retry() else Result.success()
        }
    }

/**
 * Intentos por borrador en un archivo privado de este dispositivo ("id\tintentos\tasentado").
 * Es un dato operativo: no viaja con la sincronización.
 */
class FileFailureLog(
    private val file: File,
) : FailureLog {
    private val mutex = Mutex()

    private data class Entry(
        val attempts: Int,
        val settled: Boolean,
    )

    override suspend fun fail(draftId: String): Int =
        update { entries ->
            val attempts = (entries[draftId]?.attempts ?: 0) + 1
            entries[draftId] = Entry(attempts, entries[draftId]?.settled ?: false)
            attempts
        }

    override suspend fun settle(draftId: String) = update { it[draftId] = Entry(it[draftId]?.attempts ?: 0, true) }

    override suspend fun isSettled(draftId: String): Boolean = update { it[draftId]?.settled == true }

    override suspend fun clear(draftId: String) = update { it.remove(draftId) }.let { }

    private suspend fun <T> update(block: (MutableMap<String, Entry>) -> T): T =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val entries = read()
                val before = entries.toMap()
                val result = block(entries)
                if (entries != before) write(entries)
                result
            }
        }

    private fun read(): MutableMap<String, Entry> =
        if (!file.isFile) {
            mutableMapOf()
        } else {
            file
                .readLines()
                .mapNotNull { line ->
                    val parts = line.split('\t')
                    if (parts.size != 3) null else parts[0] to Entry(parts[1].toIntOrNull() ?: 0, parts[2] == "1")
                }.toMap(mutableMapOf())
        }

    private fun write(entries: Map<String, Entry>) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(entries.entries.joinToString("\n") { (id, e) -> "$id\t${e.attempts}\t${if (e.settled) 1 else 0}" })
        temp.renameTo(file)
    }
}
