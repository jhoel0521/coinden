package com.coinden.app.i18n

import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.port.output.InstalledPacksPort

/** Idiomas de la interfaz. Agregar uno es agregar sus recursos y su código aquí. */
object AppLanguage {
    val SUPPORTED = listOf("es", "en", "pt")

    /** El idioma del teléfono si la app lo tiene; si no, inglés. */
    fun proposedFor(deviceLanguage: String): String = deviceLanguage.lowercase().takeIf { it in SUPPORTED } ?: "en"
}

/**
 * Paquetes de idioma del intérprete según el idioma elegido: el inglés (base, siempre activo en el
 * intérprete) más el del idioma de la app. Lee el idioma en cada captura, así un cambio aplica al instante.
 */
class LanguageAwarePacks(
    private val currentLanguage: () -> String,
    private val templates: List<ReceiptTemplate> = ReceiptTemplates.all,
) : InstalledPacksPort {
    override fun languagePacks(): List<LanguagePack> =
        when (currentLanguage()) {
            "es" -> listOf(LanguagePacks.SPANISH)
            "pt" -> listOf(LanguagePacks.PORTUGUESE)
            else -> emptyList()
        }

    override fun receiptTemplates(): List<ReceiptTemplate> = templates
}
