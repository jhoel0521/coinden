package com.coinden.app.perception

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * Se ejecuta en el emulador: decodificar audio, descargar y verificar el modelo de voz y
 * transcribir con Whisper una frase sintetizada (si el emulador tiene voz en español).
 */
@RunWith(AndroidJUnit4::class)
class VoicePipelineTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = VoiceModelStore(context)

    /** 2 segundos de un tono en AAC estéreo a 44,1 kHz, como un audio de otra app. */
    private fun aacTone(): ByteArray {
        val rate = 44_100
        val file = File(context.cacheDir, "tono.m4a")
        val format =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, 2).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val pcm = ByteArray(rate * 2 * 2 * 2)
        for (i in 0 until rate * 2) {
            val v = (sin(2 * PI * 440 * i / rate) * 8000).toInt()
            for (c in 0 until 2) {
                pcm[(i * 2 + c) * 2] = (v and 0xFF).toByte()
                pcm[(i * 2 + c) * 2 + 1] = (v shr 8).toByte()
            }
        }
        var offset = 0
        var track = -1
        val info = MediaCodec.BufferInfo()
        var done = false
        while (!done) {
            val inIndex = encoder.dequeueInputBuffer(10_000)
            if (inIndex >= 0) {
                val buffer = encoder.getInputBuffer(inIndex)!!
                val n = minOf(buffer.capacity(), pcm.size - offset)
                if (n <= 0) {
                    encoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    buffer.put(pcm, offset, n)
                    encoder.queueInputBuffer(inIndex, 0, n, offset / 4 * 1_000_000L / rate, 0)
                    offset += n
                }
            }
            val outIndex = encoder.dequeueOutputBuffer(info, 10_000)
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                track = muxer.addTrack(encoder.outputFormat)
                muxer.start()
            } else if (outIndex >= 0) {
                if (info.size > 0 && track >= 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                    muxer.writeSampleData(track, encoder.getOutputBuffer(outIndex)!!, info)
                }
                encoder.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
            }
        }
        encoder.stop()
        encoder.release()
        muxer.stop()
        muxer.release()
        return file.readBytes().also { file.delete() }
    }

    @Test
    fun un_audio_estereo_se_decodifica_a_mono_de_16_khz() =
        runBlocking {
            val pcm = AndroidAudioDecoder().decode(aacTone(), "audio/mp4")!!
            assertEquals(16_000, pcm.sampleRate)
            assertTrue("duró ${pcm.samples.size} muestras", pcm.samples.size in 28_000..36_000)
        }

    @Test
    fun una_descarga_con_otra_huella_se_borra() =
        runBlocking {
            // Un archivo público pequeño cuya huella no es la del modelo.
            val fake = VoiceModels.BASE.copy(fileName = "prueba-huella.bin")
            val result = store.download(fake, url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/README.md")
            assertEquals(DownloadResult.Corrupted, result)
            assertFalse(store.fileOf(fake).exists())
        }

    /** Sintetiza [phrase] en español con la voz del sistema; null si el emulador no la tiene. */
    private fun synthesize(phrase: String): ByteArray? {
        val ready = CountDownLatch(1)
        var status = TextToSpeech.ERROR
        val tts =
            TextToSpeech(context) {
                status = it
                ready.countDown()
            }
        ready.await(10, TimeUnit.SECONDS)
        if (status != TextToSpeech.SUCCESS || tts.setLanguage(Locale("es", "ES")) < TextToSpeech.LANG_AVAILABLE) {
            tts.shutdown()
            return null
        }
        val file = File(context.cacheDir, "frase.wav")
        val done = CountDownLatch(1)
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) = done.countDown()

                @Deprecated("Requerido por la API")
                override fun onError(utteranceId: String?) = done.countDown()
            },
        )
        tts.synthesizeToFile(phrase, null, file, "frase")
        done.await(30, TimeUnit.SECONDS)
        tts.shutdown()
        return file.takeIf { it.length() > 1_000 }?.readBytes()?.also { file.delete() }
    }

    @Test
    fun whisper_transcribe_una_frase_dicha_en_espanol() =
        runBlocking {
            val audio = synthesize("gasté cuarenta bolivianos en taxi")
            assumeTrue("el emulador no tiene voz en español para sintetizar", audio != null)
            val model =
                store.fileOf(VoiceModels.BASE).takeIf { it.isFile }
                    ?: (store.download(VoiceModels.BASE) as DownloadResult.Ready).file
            val pcm = AndroidAudioDecoder().decode(audio!!, "audio/wav")!!
            val text = WhisperTranscriber(model).transcribe(pcm, "es").orEmpty().lowercase()
            assertTrue(text, text.contains("taxi"))
        }
}
