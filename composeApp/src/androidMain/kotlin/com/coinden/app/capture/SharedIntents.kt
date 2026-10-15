package com.coinden.app.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Lee lo que otra app compartió con CoinDen (`ACTION_SEND`): un texto, o una imagen, un PDF o un
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
