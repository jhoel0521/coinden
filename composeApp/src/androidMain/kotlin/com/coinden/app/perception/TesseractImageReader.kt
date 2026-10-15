package com.coinden.app.perception

import android.content.Context
import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lector libre de imágenes (Tesseract, Apache 2.0) que corre en el teléfono y no envía nada.
 * Los datos de idioma (`tessdata_fast` 4.1.0 de eng, spa y por) vienen dentro de la app y se
 * copian una vez al almacenamiento privado. Lee con el idioma de la app más inglés.
 */
class TesseractImageReader(
    context: Context,
    private val languageCode: () -> String,
) : ImageTextReader {
    override val engine = Engines.TESSERACT

    private val appContext = context.applicationContext
    private val dataDir = File(appContext.filesDir, "tesseract")
    private val mutex = Mutex()

    override suspend fun read(image: ByteArray): String? =
        mutex.withLock {
            withContext(Dispatchers.Default) {
                val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size) ?: return@withContext null
                installData()
                val api = TessBaseAPI()
                try {
                    if (!api.init(dataDir.absolutePath, languages())) return@withContext null
                    api.setImage(bitmap)
                    api.utF8Text?.trim()?.takeIf { it.isNotEmpty() }
                } finally {
                    api.recycle()
                    bitmap.recycle()
                }
            }
        }

    /** "spa+eng": el idioma de la app primero; inglés siempre (es la base de los paquetes). */
    private fun languages(): String {
        val app = TESSERACT_CODES[languageCode()]
        return listOfNotNull(app, "eng").distinct().joinToString("+")
    }

    private fun installData() {
        val tessdata = File(dataDir, "tessdata").apply { mkdirs() }
        for (code in TESSERACT_CODES.values) {
            val target = File(tessdata, "$code.traineddata")
            if (target.exists()) continue
            val temp = File(tessdata, "$code.traineddata.tmp")
            appContext.assets.open("tessdata/$code.traineddata").use { input -> temp.outputStream().use { input.copyTo(it) } }
            temp.renameTo(target)
        }
    }

    private companion object {
        /** Idioma de la app (ISO 639-1) -> datos de Tesseract (ISO 639-2). */
        val TESSERACT_CODES = mapOf("es" to "spa", "en" to "eng", "pt" to "por")
    }
}
