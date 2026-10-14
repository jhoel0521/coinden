package com.coinden.core.data.local

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.port.output.CurrencyCatalogPort
import java.util.Locale

/**
 * Catálogo mundial de monedas con los datos de la plataforma (Android y JVM): ISO 4217 y la
 * moneda de cada país, sin tablas propias. Las monedas sin decimales definidos (metales, fondos)
 * se omiten.
 */
class PlatformCurrencyCatalog : CurrencyCatalogPort {
    private val currencies: Map<String, Currency> by lazy {
        java.util.Currency
            .getAvailableCurrencies()
            .filter { it.defaultFractionDigits in 0..8 }
            .map { Currency(code = it.currencyCode, decimals = it.defaultFractionDigits) }
            .sortedBy { it.code }
            .associateBy { it.code }
    }

    override fun all(): List<Currency> = currencies.values.toList()

    override fun find(code: String): Currency? = currencies[code.uppercase()]

    override fun forRegion(countryCode: String): Currency? =
        runCatching { java.util.Currency.getInstance(Locale.Builder().setRegion(countryCode.uppercase()).build()) }
            .getOrNull()
            ?.let { currencies[it.currencyCode] }
}
