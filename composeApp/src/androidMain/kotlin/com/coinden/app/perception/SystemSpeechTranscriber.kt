package com.coinden.app.perception

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.coroutines.resume

/**
 * Respaldo sin descarga: el reconocimiento de voz local del sistema leyendo el audio desde un
 * flujo (Android 13+). Solo si el teléfono lo tiene en modo local; nunca el de la nube.
 */
class SystemSpeechTranscriber(
    context: Context,
) : AudioTranscriber {
    override val engine = Engines.SYSTEM_SPEECH

    private val appContext = context.applicationContext

    override suspend fun transcribe(
        pcm: Pcm,
        languageCode: String,
    ): String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) recognize(pcm, languageCode) else null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun recognize(
        pcm: Pcm,
        languageCode: String,
    ): String? =
        withContext(Dispatchers.Main) {
            val (read, write) = ParcelFileDescriptor.createPipe()
            val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            suspendCancellableCoroutine { cont ->
                fun finish(text: String?) {
                    recognizer.destroy()
                    runCatching { read.close() }
                    if (cont.isActive) cont.resume(text)
                }
                recognizer.setRecognitionListener(
                    object : RecognitionListener {
                        override fun onResults(results: Bundle?) =
                            finish(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())

                        override fun onError(error: Int) = finish(null)

                        override fun onReadyForSpeech(params: Bundle?) = Unit

                        override fun onBeginningOfSpeech() = Unit

                        override fun onRmsChanged(rmsdB: Float) = Unit

                        override fun onBufferReceived(buffer: ByteArray?) = Unit

                        override fun onEndOfSpeech() = Unit

                        override fun onPartialResults(partialResults: Bundle?) = Unit

                        override fun onEvent(
                            eventType: Int,
                            params: Bundle?,
                        ) = Unit
                    },
                )
                recognizer.startListening(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, read)
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, pcm.sampleRate),
                )
                // La sesión termina cuando se cierra el flujo: se escribe todo el audio y se cierra.
                thread(name = "coinden-system-speech") {
                    ParcelFileDescriptor.AutoCloseOutputStream(write).use { out ->
                        val bytes = ByteBuffer.allocate(pcm.samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                        pcm.samples.forEach { bytes.putShort(it) }
                        out.write(bytes.array())
                    }
                }
                cont.invokeOnCancellation { recognizer.destroy() }
            }
        }

    companion object {
        /** Si el teléfono puede transcribir audios guardados sin la nube. */
        fun isAvailable(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }
}
