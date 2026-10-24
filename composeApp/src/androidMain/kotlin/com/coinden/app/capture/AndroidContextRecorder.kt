package com.coinden.app.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Graba el audio que explica un pago, con el grabador del sistema (AAC en m4a).
 *
 * El audio se graba a un archivo temporal, se cifra con [attachments] y el temporal se borra en
 * el acto: en el almacenamiento de la app no queda ninguna copia sin cifrar. Nada sale del
 * teléfono.
 *
 * Nunca lanza. Sin permiso, sin micrófono o si el grabador falla, devuelve el resultado que
 * corresponda y la página de contexto sigue con escribir nota y sumar foto.
 */
class AndroidContextRecorder(
    context: Context,
    private val attachments: AttachmentStore,
) : ContextRecorder {
    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var temp: File? = null

    override suspend fun start(): RecordingStart {
        if (!hasPermission()) return RecordingStart.NoPermission
        return withContext(Dispatchers.IO) {
            val archivo = File(appContext.cacheDir, "contexto-${System.nanoTime()}.m4a")
            temp = archivo
            val exito =
                runCatching {
                    nuevoGrabador().apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioSamplingRate(SAMPLE_RATE)
                        setAudioEncodingBitRate(BIT_RATE)
                        setOutputFile(archivo.absolutePath)
                        prepare()
                        start()
                        recorder = this
                    }
                }.isSuccess
            if (exito) {
                RecordingStart.Started
            } else {
                cleanUp()
                RecordingStart.Failed
            }
        }
    }

    override suspend fun stop(): Evidence? =
        withContext(Dispatchers.IO) {
            val grabador = recorder ?: return@withContext null
            val archivo = temp
            runCatching {
                grabador.stop()
            }
            runCatching { grabador.release() }
            recorder = null

            val bytes = archivo?.takeIf { it.isFile && it.length() > 0 }?.readBytes()
            // El temporal se borra siempre, haya salido bien o mal: nunca queda audio sin cifrar.
            archivo?.delete()
            temp = null
            if (bytes == null) return@withContext null

            val ref = attachments.save(bytes, MIME)
            Evidence(EvidenceSource.VOICE, attachmentRef = ref, fingerprint = fingerprintOf(bytes))
        }

    override fun cancel() {
        cleanUp()
    }

    private fun cleanUp() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        temp?.delete()
        temp = null
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun nuevoGrabador(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(appContext) else MediaRecorder()

    /** La misma huella que el resto de las evidencias: un archivo repetido no entra dos veces. */
    private fun fingerprintOf(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MIME = "audio/mp4"
        const val SAMPLE_RATE = 16_000
        const val BIT_RATE = 32_000
    }
}
