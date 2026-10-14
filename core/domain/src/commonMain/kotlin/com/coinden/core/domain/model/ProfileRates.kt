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

            val localCurrency = profile?.localCurrency ?: AnchorCurrencies.USD
            return ProfileRates(
                localCurrency = localCurrency,
                localRate = localRate,
                stamp = ProviderStamp(kind, reference, localRate, asOfEpochMs, isDegraded),
                converter = converterFor(profile, localCurrency, cryptoRates),
            )
        }

        /**
         * Conversor con la moneda local como puente: cada moneda con su tasa de referencia hacia la
         * local, y las cripto (cotizadas en USD) solo si hay tasa del dólar. Sin perfil, el puente es USD.
         */
        private fun converterFor(
            profile: UserCurrencyProfile?,
            localCurrency: Currency,
            cryptoRates: List<ExchangeRate>,
        ): CurrencyConverter {
            if (profile == null || localCurrency.isSameAs(Currency.USD)) {
                return CurrencyConverter(cryptoRates + (profile?.referenceExchangeRates().orEmpty()), pivot = Currency.USD)
            }
            val references = profile.referenceExchangeRates()
            val usdToLocal = references.firstOrNull { it.from.isSameAs(Currency.USD) }
            val crypto =
                if (usdToLocal ==
                    null
                ) {
                    emptyList()
                } else {
                    cryptoRates.filter { it.to.isSameAs(Currency.USD) }.map { it * usdToLocal }
                }
            return CurrencyConverter(references + crypto, pivot = localCurrency)
        }
    }
}
