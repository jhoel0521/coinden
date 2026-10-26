package com.coinden.app.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Resultado de bajar un archivo. */
sealed interface DownloadResult {
    data class Ready(
        val file: File,
    ) : DownloadResult

    /** La huella no coincide: el archivo se borró y no se usa. */
    data object Corrupted : DownloadResult

    data object Failed : DownloadResult
}

/**
 * Baja un archivo y lo deja en su sitio **solo si su huella coincide** con la esperada.
 *
 * Lo usan los modelos de voz y los paquetes de idioma: el trato es el mismo para todo lo que se
 * descarga, así que la comprobación vive en un solo lugar. Se baja a un `.part` y recién al
 * verificar se renombra, para que nunca quede a medias un archivo que parezca bueno.
 *
 * La petición no lleva nada de la persona: es un GET a una dirección pública.
 */
object VerifiedDownload {
    suspend fun to(
        target: File,
        url: String,
        sha256: String,
        expectedSize: Long = 0,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): DownloadResult =
        withContext(Dispatchers.IO) {
            target.parentFile?.mkdirs()
            val temp = File(target.parentFile, "${target.name}.part")
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                val digest = MessageDigest.getInstance("SHA-256")
                connection.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var total = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            total += n
                            onProgress(total, expectedSize)
                        }
                    }
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                if (!hash.equals(sha256, ignoreCase = true)) {
                    temp.delete()
                    DownloadResult.Corrupted
                } else {
                    target.delete()
                    if (temp.renameTo(target)) DownloadResult.Ready(target) else DownloadResult.Failed
                }
            }.getOrElse {
                temp.delete()
                DownloadResult.Failed
            }
        }
}
