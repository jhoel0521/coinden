package com.coinden.core.domain.model

/**
 * Configuración de preferencias de divisa del perfil del usuario.
 */
data class UserCurrencyProfile(
    val userId: String,
    val anchorCurrency: Currency = AnchorCurrencies.USD,
    val localCurrency: Currency = AnchorCurrencies.USD,
    val strategy: ExchangeRateStrategy = ExchangeRateStrategy.Fixed(ExchangeRate.identity(AnchorCurrencies.USD)),
    /** Otras monedas que usa además de la local (del catálogo o propias). */
    val currencies: List<Currency> = emptyList(),
    /**
     * Tasa de referencia de cada moneda hacia la local (código -> estrategia). Solo sirve para
     * mostrar totales: una operación entre monedas guarda siempre su propia tasa real.
     */
    val referenceRates: Map<String, ExchangeRateStrategy> = emptyMap(),
) {
    /**
     * Estrategia de referencia de [currency] hacia la moneda local. Un perfil anterior guardaba una
     * sola tasa (normalmente USD -> local): se lee como la referencia de su moneda de origen.
     */
    fun referenceRateFor(currency: Currency): ExchangeRateStrategy? {
        if (currency.isSameAs(localCurrency)) return null
        referenceRates[currency.code]?.let { return it }
        return strategy.takeIf { legacy ->
            val rate = legacy.currentRate
            !rate.isIdentity && rate.from.isSameAs(currency) && rate.to.isSameAs(localCurrency)
        }
    }

    /** Todas las tasas de referencia vigentes, de cada moneda hacia la local. */
    fun referenceExchangeRates(): List<ExchangeRate> =
        (currencies + anchorCurrency + strategy.currentRate.from)
            .distinctBy { it.code }
            .mapNotNull { referenceRateFor(it)?.currentRate }
}
