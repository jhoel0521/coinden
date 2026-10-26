package com.coinden.app.packs

import android.content.Context
import com.coinden.app.download.DownloadResult
import com.coinden.app.download.VerifiedDownload
import com.coinden.core.data.packs.DownloadablePack
import com.coinden.core.data.packs.PackCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Los paquetes de idioma y las plantillas que la persona descargó.
 *
 * Se guardan en el almacenamiento privado de la app y se suman a los que viajan dentro de ella.
 * Cada archivo se verifica por su huella antes de quedar en su sitio, con el mismo mecanismo que
 * los modelos de voz. Nada de esto manda datos de la persona: son GET a direcciones públicas.
 */
class PackStore(
    context: Context,
    private val catalogUrl: String = CATALOG_URL,
) {
    private val dir = File(context.applicationContext.filesDir, "packs").apply { mkdirs() }

    /** El contenido de lo descargado, listo para que el cargador lo sume a lo que viaja con la app. */
    fun downloadedTexts(): List<String> =
        dir
            .listFiles()
            .orEmpty()
            .filter { it.extension == "json" }
            .mapNotNull { runCatching { it.readText() }.getOrNull() }

    fun installedIds(): Set<String> =
        dir
            .listFiles()
            .orEmpty()
            .filter { it.extension == "json" }
            .map { it.nameWithoutExtension }
            .toSet()

    fun delete(id: String): Boolean = File(dir, "$id.json").delete()

    /**
     * Lo que hay para descargar. El catálogo solo dice qué existe y cuánto pesa; lo que se
     * comprueba de verdad es la huella de cada archivo al bajarlo.
     */
    suspend fun catalog(): List<DownloadablePack> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(catalogUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.inputStream.use { PackCatalog.parse(it.readBytes().decodeToString()) }
            }.getOrDefault(emptyList())
        }

    suspend fun download(
        pack: DownloadablePack,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): DownloadResult = VerifiedDownload.to(File(dir, pack.fileName), pack.url, pack.sha256, pack.sizeBytes, onProgress)

    companion object {
        /** Dirección pública fija, publicada junto al sitio del proyecto. */
        const val CATALOG_URL = "https://jhoel0521.github.io/coinden/packs/catalog.json"
    }
}
