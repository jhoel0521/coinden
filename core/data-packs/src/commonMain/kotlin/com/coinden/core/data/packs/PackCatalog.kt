package com.coinden.core.data.packs

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Qué es lo que se descarga: las palabras de un idioma o el formato de un banco. */
enum class PackKind {
    LANGUAGE,
    TEMPLATE,
}

/**
 * Algo que se puede descargar, con lo necesario para decidir y para verificar: el tamaño se le
 * muestra a la persona **antes** de bajar nada, y la huella se comprueba después.
 */
data class DownloadablePack(
    val id: String,
    val kind: PackKind,
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
) {
    /** Con el que se guarda, y el mismo con el que lo encuentra el cargador. */
    val fileName: String get() = "$id.json"
}

/**
 * El catálogo de lo que hay para descargar, publicado junto a los archivos.
 *
 * El catálogo dice qué hay; la huella de cada archivo es lo que se verifica al bajarlo. Una
 * entrada que no se podría verificar —sin huella, con una huella que no lo parece o que no va por
 * HTTPS— se descarta, porque ofrecerla sería ofrecer algo que no se puede comprobar.
 */
object PackCatalog {
    private const val CATALOG_VERSION = 1
    private val json = Json { ignoreUnknownKeys = true }
    private val huella = Regex("^[0-9a-f]{64}$")

    fun parse(text: String): List<DownloadablePack> {
        val file = runCatching { json.decodeFromString<CatalogFile>(text) }.getOrNull() ?: return emptyList()
        if (file.formatVersion == null || file.formatVersion > CATALOG_VERSION) return emptyList()
        return file.entries.mapNotNull { it.toPack() }
    }

    private fun CatalogEntry.toPack(): DownloadablePack? {
        val tipo = PackKind.entries.firstOrNull { it.name == kind } ?: return null
        if (id.isBlank() || sizeBytes <= 0) return null
        if (!url.startsWith("https://")) return null
        if (!huella.matches(sha256.lowercase())) return null
        return DownloadablePack(id, tipo, url, sha256.lowercase(), sizeBytes)
    }
}

@Serializable
private data class CatalogFile(
    val formatVersion: Int? = null,
    val entries: List<CatalogEntry> = emptyList(),
)

@Serializable
private data class CatalogEntry(
    val id: String = "",
    val kind: String = "",
    val url: String = "",
    val sha256: String = "",
    val sizeBytes: Long = 0,
)
