package com.coinden.app.perception

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Lector de imágenes de Google (ML Kit, modelo latino dentro de la app). Lee en el teléfono:
 * Google no recibe la imagen ni el texto, solo métricas técnicas de uso. Por eso solo se crea si
 * la persona lo aceptó (ver `PerceptionReaders.googleImageReader`); crear este objeto ya inicia ML Kit.
 */
class MlKitImageReader : ImageTextReader {
    override val engine = Engines.MLKIT

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun read(image: ByteArray): String? {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size) ?: return null
        return suspendCancellableCoroutine { cont ->
            recognizer
                .process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text.trim().takeIf { text -> text.isNotEmpty() }) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    fun close() = recognizer.close()
}
