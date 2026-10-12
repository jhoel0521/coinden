package com.coinden.core.domain.model

/**
 * Tasas de un perfil de moneda listas para usar: moneda local, tasa desde USD, sello del proveedor
 * y un conversor con esa tasa más los precios cripto. El patrimonio consolidado y el reporte de
 * gastos las obtienen del mismo lugar, así nunca usan tasas distintas.
 */
data class ProfileRates(
    val localCurrency: Currency,
    val localRate: ExchangeRate,
    val stamp: ProviderStamp,
    val converter: CurrencyConverter,
) {
    companion object {
        val DEFAULT_CRYPTO_RATES: List<ExchangeRate> = listOf(ExchangeRate.parse(Currency.BTC, Currency.USD, "62200"))

        fun of(
            profile: UserCurrencyProfile?,
            cryptoRates: List<ExchangeRate> = DEFAULT_CRYPTO_RATES,
        ): ProfileRates {
            var localRate = ExchangeRate.identity(AnchorCurrencies.USD)
            var kind = RateSourceKind.BASE
            var reference: String? = null
            var isDegraded = false
            var asOfEpochMs = 0L

            when (val strategy = profile?.strategy) {
                null -> Unit
                is ExchangeRateStrategy.Fixed -> {
                    localRate = strategy.rate
                    kind = RateSourceKind.FIXED
                }
                is ExchangeRateStrategy.ManualDaily -> {
                    localRate = strategy.rate
                    kind = RateSourceKind.MANUAL
                    asOfEpochMs = strategy.updatedAtEpochMs
                }
                is ExchangeRateStrategy.Verified -> {
                    localRate = strategy.currentRate
                    kind = RateSourceKind.VERIFIED
                    reference = strategy.sourceId
                }
                is ExchangeRateStrategy.CustomSelector -> {
                    localRate = strategy.selector.lastKnownRate
                    kind = RateSourceKind.WEB_SELECTOR
                    reference = strategy.selector.sourceUrl
                    isDegraded = strategy.selector.status == SelectorStatus.NEEDS_RECONFIRMATION
                    asOfEpochMs = strategy.selector.lastSuccessfulFetchEpochMs
                }
            }

            return ProfileRates(
                localCurrency = profile?.localCurrency ?: AnchorCurrencies.USD,
                localRate = localRate,
                stamp = ProviderStamp(kind, reference, localRate, asOfEpochMs, isDegraded),
                converter = CurrencyConverter(cryptoRates + localRate),
            )
        }
    }
}
