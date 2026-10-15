package com.coinden.app.perception

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Funciones nativas de `whisper_jni.c`. */
internal object WhisperNative {
    init {
        System.loadLibrary("coinden_whisper")
    }

    external fun load(modelPath: String): Long

    external fun transcribe(
        handle: Long,
        samples: FloatArray,
        language: String,
        threads: Int,
    ): String?

    external fun free(handle: Long)
}

/**
 * Transcribe en el teléfono con Whisper (whisper.cpp, MIT) y un modelo descargado y verificado.
 * El modelo se carga al primer uso y se libera con [close]. Nunca usa la red.
 */
class WhisperTranscriber(
    private val model: File,
) : AudioTranscriber {
    override val engine = Engines.WHISPER

    private val mutex = Mutex()
    private var handle = 0L

    override suspend fun transcribe(
        pcm: Pcm,
        languageCode: String,
    ): String? =
        mutex.withLock {
            withContext(Dispatchers.Default) {
                if (handle == 0L) handle = WhisperNative.load(model.absolutePath)
                if (handle == 0L) return@withContext null
                val samples = FloatArray(pcm.samples.size) { pcm.samples[it] / 32768f }
                val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
                WhisperNative.transcribe(handle, samples, languageCode, threads)?.trim()?.takeIf { it.isNotEmpty() }
            }
        }

    suspend fun close() =
        mutex.withLock {
            if (handle != 0L) WhisperNative.free(handle)
            handle = 0L
        }
}
