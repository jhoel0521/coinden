package com.coinden.core.domain.port.output

/**
 * SPI: preferencias de la app en el dispositivo. Solo códigos, sin textos: el idioma se guarda
 * como su código ISO 639-1 ("es", "en", "pt").
 */
interface PreferencesRepositoryPort {
    /** Idioma elegido por la persona; null si todavía no eligió. */
    suspend fun languageCode(): String?

    suspend fun setLanguageCode(code: String)
}
