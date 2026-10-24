package com.coinden.app.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Lee lo que otra app compartió con CoinDen (`ACTION_SEND` o `ACTION_SEND_MULTIPLE`): un texto, o
 * una o varias imágenes, PDF o
 * audio. El archivo se copia entero: el permiso sobre el `Uri` de la otra app vence después.
 */
object SharedIntents {
    /** Tope de lo que se copia (una nota de voz de WhatsApp pesa pocos KB por minuto). */
    private const val MAX_BYTES = 25L * 1024 * 1024

    fun read(
        context: Context,
        intent: Intent?,
    ): SharedContent? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val type = intent.type.orEmpty()
        val stream = intent.streamUri()
        if (stream == null) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
            return text?.takeIf { it.isNotEmpty() }?.let { SharedContent.Text(it) }
        }
        val mime = context.contentResolver.getType(stream) ?: type
        if (!accepted(mime)) return null
        val bytes =
            runCatching {
                context.contentResolver.openInputStream(stream)?.use { input ->
                    val data = input.readNBytesCompat(MAX_BYTES + 1)
                    data.takeIf { it.size <= MAX_BYTES }
                }
            }.getOrNull() ?: return null
        return SharedContent.File(bytes, mime)
    }

    /**
     * Todo lo que la otra app compartió: uno solo (`ACTION_SEND`) o varios a la vez
     * (`ACTION_SEND_MULTIPLE`), que es lo que pasa al elegir dos fotos en WhatsApp.
     *
     * Lo que no se puede leer se saltea en lugar de perder el resto: si de dos fotos una falla,
     * la otra igual entra al paquete.
     */
    fun readAll(
        context: Context,
        intent: Intent?,
    ): List<SharedContent> =
        when (intent?.action) {
            Intent.ACTION_SEND -> listOfNotNull(read(context, intent))
            Intent.ACTION_SEND_MULTIPLE -> intent.streamUris().mapNotNull { readFile(context, it, intent.type.orEmpty()) }
            else -> emptyList()
        }

    /** Un archivo compartido, ya copiado: el permiso sobre el Uri de la otra app vence después. */
    private fun readFile(
        context: Context,
        stream: Uri,
        fallbackMime: String,
    ): SharedContent.File? {
        val mime = context.contentResolver.getType(stream) ?: fallbackMime
        if (!accepted(mime)) return null
        val bytes =
            runCatching {
                context.contentResolver.openInputStream(stream)?.use { input ->
                    input.readNBytesCompat(MAX_BYTES + 1).takeIf { it.size <= MAX_BYTES }
                }
            }.getOrNull() ?: return null
        return SharedContent.File(bytes, mime)
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamUris(): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            (getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)).orEmpty()
        }

    private fun accepted(mime: String) = mime.startsWith("image/") || mime == "application/pdf" || mime.startsWith("audio/")

    @Suppress("DEPRECATION")
    private fun Intent.streamUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }

    private fun java.io.InputStream.readNBytesCompat(limit: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (total < limit) {
            val n = read(buffer, 0, minOf(buffer.size.toLong(), limit - total).toInt())
            if (n < 0) break
            out.write(buffer, 0, n)
            total += n
        }
        return out.toByteArray()
    }
}
