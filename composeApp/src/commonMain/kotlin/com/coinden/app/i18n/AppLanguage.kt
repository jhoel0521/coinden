package com.coinden.app.i18n

import com.coinden.core.data.packs.BundledPacks
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.port.output.InstalledPacksPort

/** Idiomas de la interfaz. Agregar uno es agregar sus recursos y su código aquí. */
object AppLanguage {
    val SUPPORTED = listOf("es", "en", "pt")

    /** El idioma del teléfono si la app lo tiene; si no, inglés. */
    fun proposedFor(deviceLanguage: String): String = deviceLanguage.lowercase().takeIf { it in SUPPORTED } ?: "en"
}

/**
 * Paquetes de idioma del intérprete según el idioma elegido. Los archivos que viajan con la app
 * traen el inglés, que siempre está; el paquete del idioma de la app va al final para ganar al
 * unirlos. Lee el idioma en cada captura, así un cambio aplica al instante.
 *
 * [downloaded] son los archivos que la persona descargó; mientras no haya descargas, va vacío.
 */
class LanguageAwarePacks(
    private val currentLanguage: () -> String,
    private val downloaded: () -> List<String> = { emptyList() },
) : InstalledPacksPort {
    override fun languagePacks(): List<LanguagePack> =
        BundledPacks.languagePacks(downloaded = downloaded(), appLanguage = currentLanguage())

    override fun receiptTemplates(): List<ReceiptTemplate> = BundledPacks.receiptTemplates(downloaded = downloaded())
}
