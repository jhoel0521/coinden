package com.coinden.core.domain.support

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.port.output.CurrencyCatalogPort

/**
 * Catálogo de monedas de prueba: un recorte de ISO 4217 con los decimales oficiales. En la app
 * los datos salen de la plataforma; aquí son datos fijos para que las pruebas sean deterministas.
 */
object FakeCurrencyCatalog : CurrencyCatalogPort {
    private val byCode =
        listOf(
            Currency.USD,
            Currency.EUR,
            Currency.BOB,
            Currency.ARS,
            Currency.BRL,
            Currency.CLP,
            Currency.PEN,
            Currency.CNY,
            Currency("MXN", decimals = 2),
            Currency("DOP", decimals = 2),
            Currency("PYG", decimals = 0),
            Currency("UYU", decimals = 2),
            Currency("GBP", decimals = 2),
            Currency("BHD", decimals = 3),
            Currency("JPY", decimals = 0),
        ).associateBy { it.code }

    private val byRegion =
        mapOf(
            "US" to "USD",
            "BO" to "BOB",
            "AR" to "ARS",
            "BR" to "BRL",
            "CL" to "CLP",
            "MX" to "MXN",
            "PE" to "PEN",
            "DO" to "DOP",
            "PY" to "PYG",
            "UY" to "UYU",
            "ES" to "EUR",
            "FR" to "EUR",
            "DE" to "EUR",
            "PT" to "EUR",
            "GB" to "GBP",
            "CN" to "CNY",
            "BH" to "BHD",
            "JP" to "JPY",
        )

    override fun all(): List<Currency> = byCode.values.toList()

    override fun find(code: String): Currency? = byCode[code.uppercase()]

    override fun forRegion(countryCode: String): Currency? = byRegion[countryCode.uppercase()]?.let(byCode::get)
}

/** Países que escriben con coma decimal (dato de prueba; en la app lo da la plataforma). */
private val COMMA_DECIMAL = setOf("AR", "BR", "CL", "CO", "UY", "PY", "VE", "ES", "PT", "DE", "FR", "IT", "DO")

/** Locale de una persona de prueba, como lo armaría el adaptador regional de la plataforma. */
fun testLocale(
    language: String,
    country: String,
): UserLocale =
    UserLocale(
        language = language,
        country = country,
        currency = FakeCurrencyCatalog.forRegion(country) ?: Currency.USD,
        decimalSeparator = if (country.uppercase() in COMMA_DECIMAL) ',' else '.',
    )
