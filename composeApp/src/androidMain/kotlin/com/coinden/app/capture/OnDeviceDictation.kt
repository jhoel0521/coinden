package com.coinden.app.capture

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Dictado con el reconocimiento de voz del propio teléfono (Android 12+). Nunca usa
 * `createSpeechRecognizer`, que puede enviar el audio a la nube: sin modelo local, no hay dictado.
 */
class OnDeviceDictation(
    private val context: Context,
) : DictationPort {
    private var recognizer: SpeechRecognizer? = null
    private var pending: CancellableContinuation<DictationOutcome>? = null

    override suspend fun listen(languageCode: String): DictationOutcome =
        withContext(Dispatchers.Main) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                return@withContext DictationOutcome.Unavailable
            }
            suspendCancellableCoroutine<DictationOutcome> { cont ->
                val r = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                recognizer = r
                pending = cont
                r.setRecognitionListener(listener())
                r.startListening(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag(languageCode))
                        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true),
                )
                cont.invokeOnCancellation { finish(null) }
            }
        }

    override fun cancel() {
        recognizer?.cancel()
        finish(DictationOutcome.NothingHeard)
    }

    /** El idioma de la app con la región del teléfono si coincide ("es" + BO -> "es-BO"). */
    private fun languageTag(languageCode: String): String {
        val device = Locale.getDefault()
        return if (device.language == languageCode && device.country.isNotEmpty()) "$languageCode-${device.country}" else languageCode
    }

    private fun finish(outcome: DictationOutcome?) {
        val cont = pending
        pending = null
        recognizer?.destroy()
        recognizer = null
        if (outcome != null && cont != null && cont.isActive) cont.resume(outcome)
    }

    private fun listener() =
        object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                finish(text?.takeIf { it.isNotBlank() }?.let { DictationOutcome.Heard(it) } ?: DictationOutcome.NothingHeard)
            }

            override fun onError(error: Int) {
                val unavailable =
                    error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                        error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                finish(if (unavailable) DictationOutcome.Unavailable else DictationOutcome.NothingHeard)
            }

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
        }
}
