package com.coinden.app.perception

/** Códigos de los lectores, guardados en el historial del borrador. Sin textos. */
object Engines {
    const val TESSERACT = "tesseract"
    const val MLKIT = "mlkit"
    const val PDF_TEXT = "pdf-text"
    const val WHISPER = "whisper"
    const val SYSTEM_SPEECH = "system-speech"
}

/** Lee el texto de una imagen (JPEG o PNG) en el teléfono. null si no encuentra texto. */
interface ImageTextReader {
    val engine: String

    suspend fun read(image: ByteArray): String?
}

/** Lo que trae un PDF: su texto o, si es escaneado, sus páginas como imágenes. */
sealed interface PdfReading {
    class Text(
        val text: String,
    ) : PdfReading

    class Pages(
        val images: List<ByteArray>,
    ) : PdfReading

    data object Unreadable : PdfReading
}

interface PdfTextReader {
    /** [maxPages]: cuántas páginas dibujar si el PDF no trae texto. */
    suspend fun read(
        pdf: ByteArray,
        maxPages: Int,
    ): PdfReading
}

/** Audio decodificado: PCM de 16 bits, mono, a [sampleRate] Hz. */
class Pcm(
    val samples: ShortArray,
    val sampleRate: Int,
) {
    val seconds: Long get() = samples.size.toLong() / sampleRate
}

/** Decodifica un audio guardado (opus, m4a, mp3...) a PCM de 16 kHz. null si no puede. */
interface AudioDecoder {
    suspend fun decode(
        audio: ByteArray,
        mimeType: String,
    ): Pcm?
}

/** Transcribe en el teléfono. Nunca usa la nube. */
interface AudioTranscriber {
    val engine: String

    suspend fun transcribe(
        pcm: Pcm,
        languageCode: String,
    ): String?
}

/**
 * Los lectores que da la plataforma. El de Google se crea solo si la persona lo aceptó; los
 * transcriptores vienen en orden de preferencia y solo los disponibles (modelo descargado o
 * reconocimiento local del sistema).
 */
class PerceptionReaders(
    val freeImageReader: ImageTextReader,
    val googleImageReader: () -> ImageTextReader,
    val pdfReader: PdfTextReader,
    val audioDecoder: AudioDecoder,
    val transcribers: suspend () -> List<AudioTranscriber>,
    /** Descarga de la memoria los modelos cargados (al vaciarse la cola o tras un rato sin uso). */
    val release: suspend () -> Unit = {},
)
