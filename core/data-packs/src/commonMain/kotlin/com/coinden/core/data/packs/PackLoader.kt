package com.coinden.core.data.packs

import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate

/** Lo que se pudo cargar, y cuántos archivos hubo que descartar por no entenderse. */
data class LoadedPacks(
    val languagePacks: List<LanguagePack>,
    val receiptTemplates: List<ReceiptTemplate>,
    val descartados: Int,
)

/**
 * Junta los paquetes y plantillas que vienen con la app con los que la persona descargó.
 *
 * Recibe el contenido de los archivos, no sus rutas: leerlos es trabajo de cada plataforma, y así
 * esta lógica se prueba igual en todas. Un archivo que no se entiende se descarta y se cuenta;
 * nunca impide cargar los demás.
 */
object PackLoader {
    fun load(
        bundled: List<String>,
        downloaded: List<String>,
        appLanguage: String,
    ): LoadedPacks {
        val packs = parse(bundled, downloaded, PackParser::languagePack) { it.code }
        val templates = parse(bundled, downloaded, PackParser::receiptTemplate) { it.id }
        val leidos = packs.size + templates.size
        return LoadedPacks(
            languagePacks = packs.sortedBy { it.code == appLanguage },
            receiptTemplates = templates.sortedBy { it.id },
            descartados = bundled.size + downloaded.size - leidos,
        )
    }

    fun languagePacks(
        bundled: List<String>,
        downloaded: List<String>,
        appLanguage: String,
    ): List<LanguagePack> = load(bundled, downloaded, appLanguage).languagePacks

    fun receiptTemplates(
        bundled: List<String>,
        downloaded: List<String>,
    ): List<ReceiptTemplate> = load(bundled, downloaded, appLanguage = "").receiptTemplates

    /**
     * Lee ambos orígenes y, ante la misma clave, se queda con el descargado: una corrección
     * publicada después gana sobre la que viajó con la app.
     */
    private fun <T : Any> parse(
        bundled: List<String>,
        downloaded: List<String>,
        read: (String) -> T?,
        keyOf: (T) -> String,
    ): List<T> {
        val porClave = linkedMapOf<String, T>()
        (bundled + downloaded).forEach { texto -> read(texto)?.let { porClave[keyOf(it)] = it } }
        return porClave.values.toList()
    }
}
