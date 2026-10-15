package com.coinden.app.perception

import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder

/**
 * Decodifica un audio guardado (la nota de voz Opus de WhatsApp, m4a, mp3...) con los códecs del
 * teléfono, lo mezcla a mono y lo remuestrea a 16 kHz para la transcripción.
 */
class AndroidAudioDecoder(
    /** Corta la decodificación pasado este largo (el servicio no transcribe audios más largos). */
    private val maxSeconds: Long = PerceptionService.MAX_AUDIO_SECONDS + 1,
) : AudioDecoder {
    override suspend fun decode(
        audio: ByteArray,
        mimeType: String,
    ): Pcm? =
        withContext(Dispatchers.Default) {
            runCatching { decodeAll(audio) }.getOrNull()
        }

    private fun decodeAll(audio: ByteArray): Pcm? {
        val extractor = MediaExtractor()
        extractor.setDataSource(BytesSource(audio))
        val track =
            (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") ==
                    true
            }
        if (track == null) {
            extractor.release()
            return null
        }
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val pcm = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val out = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        sampleRate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    out >= 0 -> {
                        val buffer = codec.getOutputBuffer(out)!!
                        val bytes = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.get(bytes)
                        pcm.write(bytes)
                        codec.releaseOutputBuffer(out, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        // Un audio más largo que el máximo no se transcribe: no hace falta decodificarlo entero.
                        if (pcm.size() > maxSeconds * sampleRate * channels * 2) outputDone = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }
        return Pcm(resample(toMono(pcm.toByteArray(), channels), sampleRate, TARGET_RATE), TARGET_RATE)
    }

    private fun toMono(
        bytes: ByteArray,
        channels: Int,
    ): ShortArray {
        val shorts =
            java.nio.ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
        val frames = shorts.remaining() / channels
        return ShortArray(frames) { frame ->
            var sum = 0
            for (c in 0 until channels) sum += shorts.get(frame * channels + c)
            (sum / channels).toShort()
        }
    }

    /** Remuestreo lineal: suficiente para voz. */
    private fun resample(
        samples: ShortArray,
        from: Int,
        to: Int,
    ): ShortArray {
        if (from == to || samples.isEmpty()) return samples
        val length = (samples.size.toLong() * to / from).toInt()
        return ShortArray(length) { i ->
            val position = i.toDouble() * from / to
            val left = position.toInt().coerceAtMost(samples.size - 1)
            val right = (left + 1).coerceAtMost(samples.size - 1)
            val fraction = position - left
            (samples[left] + (samples[right] - samples[left]) * fraction).toInt().toShort()
        }
    }

    /** El audio ya está en memoria (descifrado): se lee sin escribirlo a disco. */
    private class BytesSource(
        private val bytes: ByteArray,
    ) : MediaDataSource() {
        override fun readAt(
            position: Long,
            buffer: ByteArray,
            offset: Int,
            size: Int,
        ): Int {
            if (position >= bytes.size) return -1
            val count = minOf(size.toLong(), bytes.size - position).toInt()
            System.arraycopy(bytes, position.toInt(), buffer, offset, count)
            return count
        }

        override fun getSize(): Long = bytes.size.toLong()

        override fun close() = Unit
    }

    private companion object {
        const val TARGET_RATE = 16_000
        const val TIMEOUT_US = 10_000L
    }
}
