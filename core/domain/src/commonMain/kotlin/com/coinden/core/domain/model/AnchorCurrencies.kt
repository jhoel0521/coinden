package com.coinden.core.domain.model

/**
 * Catálogo de Monedas Ancla Universales soportadas por CoinDen.
 *
 * Estas monedas actúan como anclas de reserva globales a las que cualquier divisa
 * o criptomoneda puede ser referenciada.
 */
object AnchorCurrencies {
    val USD: Currency = Currency.USD
    val EUR: Currency = Currency.EUR
    val CNY: Currency = Currency.CNY
    val BTC: Currency = Currency.BTC

    val all: List<Currency> = listOf(USD, EUR, CNY, BTC)

    fun isAnchor(currency: Currency): Boolean = all.any { it.code == currency.code }
}

/**
 * Tipo de formato de respuesta esperado por el proveedor externo.
 */
enum class ResponseFormat {
    JSON,
    HTML,
}

/**
 * Definición inmutable de una fuente verificada en la Lista Blanca comunitaria.
 *
 * En lugar de inflar la aplicación con una clase/interfaz específica por cada país o API,
 * las fuentes verificadas son datos puros configurables.
 *
 * [name] es solo el nombre propio del proveedor (no se traduce). Qué cotiza la fuente
 * (blue, paralelo, oficial) se deduce de [id], [countryIso] y [targetCurrency].
 */
data class RateSourceDefinition(
    val id: String,
    val name: String,
    val countryIso: String,
    val targetCurrency: Currency,
    val url: String,
    val responseFormat: ResponseFormat,
    val defaultSelector: String,
)

/**
 * Directorio de Lista Blanca Comunitaria (Verified Sources).
 *
 * Fuentes auditadas y preconfiguradas para onboarding instantáneo de un solo toque:
 * - Argentina (ARS Dólar Blue vía DolarApi)
 * - Bolivia (BOB Dólar Blue vía DolarBlueBolivia)
 * - Europa (EUR oficial vía Frankfurter / BCE)
 * - Crypto (BTC / USDT vía CoinGecko)
 */
object VerifiedSourceDirectory {
    val ARGENTINA_DOLAR_BLUE =
        RateSourceDefinition(
            id = "ar-dolar-blue",
            name = "DolarApi",
            countryIso = "AR",
            targetCurrency = Currency.of("ARS", decimals = 2),
            url = "https://dolarapi.com/v1/dolares/blue",
            responseFormat = ResponseFormat.JSON,
            defaultSelector = "venta",
        )

    val BOLIVIA_DOLAR_BLUE =
        RateSourceDefinition(
            id = "bo-dolar-blue",
            name = "DolarBlueBolivia",
            countryIso = "BO",
            targetCurrency = Currency.of("BOB", decimals = 2),
            url = "https://www.dolarbluebolivia.click/",
            responseFormat = ResponseFormat.HTML,
            defaultSelector = """id="usdRate">[^<]*?(\d+(?:\.\d+)?)""",
        )

    val FRANKFURTER_EUR =
        RateSourceDefinition(
            id = "eu-frankfurter-eur",
            name = "Frankfurter",
            countryIso = "EU",
            targetCurrency = Currency.EUR,
            url = "https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR",
            responseFormat = ResponseFormat.JSON,
            defaultSelector = "rates.EUR",
        )

    val COINGECKO_BTC_USD =
        RateSourceDefinition(
            id = "crypto-coingecko-btc",
            name = "CoinGecko",
            countryIso = "GLOBAL",
            targetCurrency = Currency.BTC,
            url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd",
            responseFormat = ResponseFormat.JSON,
            defaultSelector = "bitcoin.usd",
        )

    val all: List<RateSourceDefinition> =
        listOf(
            ARGENTINA_DOLAR_BLUE,
            BOLIVIA_DOLAR_BLUE,
            FRANKFURTER_EUR,
            COINGECKO_BTC_USD,
        )

    fun findById(id: String): RateSourceDefinition? = all.firstOrNull { it.id.equals(id, ignoreCase = true) }

    fun findByCountry(countryIso: String): List<RateSourceDefinition> = all.filter { it.countryIso.equals(countryIso, ignoreCase = true) }

    fun findByCurrency(currencyCode: String): List<RateSourceDefinition> =
        all.filter { it.targetCurrency.code.equals(currencyCode, ignoreCase = true) }
}
