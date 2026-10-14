package com.coinden.app.currency

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.port.output.CurrencyCatalogPort

/** Nombre de una moneda en un idioma, desde los datos de la plataforma. null si no lo conoce. */
fun interface CurrencyNames {
    fun name(
        code: String,
        language: String,
    ): String?
}

/**
 * Monedas sugeridas en el paso de monedas: el dólar, el euro y las de los países de la misma región
 * que el teléfono. Las regiones son datos de la app; las monedas salen del catálogo.
 */
object FrequentCurrencies {
    private val REGIONS: List<Set<String>> =
        listOf(
            // Sudamérica
            setOf("AR", "BO", "BR", "CL", "CO", "EC", "PE", "PY", "UY", "VE"),
            // Norte y Centroamérica, Caribe
            setOf("US", "CA", "MX", "GT", "HN", "SV", "NI", "CR", "PA", "DO", "CU", "PR"),
            // Europa
            setOf("ES", "PT", "FR", "DE", "IT", "NL", "BE", "CH", "GB", "IE", "AT", "PL", "SE", "NO", "DK", "CZ", "HU", "RO"),
            // Asia oriental y sudeste asiático
            setOf("CN", "JP", "KR", "TW", "HK", "SG", "TH", "VN", "PH", "MY", "ID", "IN"),
        )

    fun forRegion(
        country: String,
        local: Currency,
        catalog: CurrencyCatalogPort,
    ): List<Currency> {
        val neighbours = REGIONS.firstOrNull { country.uppercase() in it }.orEmpty().sorted()
        return (listOf("USD", "EUR").mapNotNull(catalog::find) + neighbours.mapNotNull(catalog::forRegion))
            .distinctBy { it.code }
            .filterNot { it.isSameAs(local) }
    }

    /** Busca por código o por nombre en el idioma de la app, sin distinguir tildes ni mayúsculas. */
    fun search(
        query: String,
        language: String,
        catalog: CurrencyCatalogPort,
        names: CurrencyNames,
    ): List<Currency> {
        val q = fold(query.trim())
        if (q.isEmpty()) return emptyList()
        return catalog.all().filter { it.code.lowercase().startsWith(q) || fold(names.name(it.code, language).orEmpty()).contains(q) }
    }

    private fun fold(text: String): String =
        text
            .lowercase()
            .map { c -> ACCENTS[c] ?: c }
            .joinToString("")

    private val ACCENTS =
        mapOf(
            'á' to 'a',
            'é' to 'e',
            'í' to 'i',
            'ó' to 'o',
            'ú' to 'u',
            'ü' to 'u',
            'ñ' to 'n',
            'ã' to 'a',
            'õ' to 'o',
            'ç' to 'c',
            'â' to 'a',
            'ê' to 'e',
            'ô' to 'o',
        )
}
