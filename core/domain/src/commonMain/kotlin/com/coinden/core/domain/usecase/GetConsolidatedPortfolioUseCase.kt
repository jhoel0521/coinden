package com.coinden.core.domain.usecase

import com.coinden.core.domain.model.AnchorCurrencies
import com.coinden.core.domain.model.ConsolidatedPortfolio
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.CurrencyConverter
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.ProviderStamp
import com.coinden.core.domain.model.RateSourceKind
import com.coinden.core.domain.model.SelectorStatus
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort

/**
 * Caso de Uso: Obtener la proyección de cartera consolidada (Vista Unificada y Vista Desglosada).
 */
class GetConsolidatedPortfolioUseCase(
    private val repository: CurrencyProfileRepositoryPort,
    private val cryptoToUsdRates: Map<String, Double> = mapOf("BTC" to 62200.0),
) {
    suspend operator fun invoke(
        userId: String,
        holdings: List<Money>,
        viewInCurrency: Currency? = null,
    ): ConsolidatedPortfolio {
        val profile = repository.getProfile(userId)
        val localCurrency = profile?.localCurrency ?: AnchorCurrencies.USD
        val target = viewInCurrency ?: localCurrency

        var rateToUsd = 1.0
        var kind = RateSourceKind.BASE
        var reference: String? = null
        var isDegraded = false
        var asOfEpochMs = 0L

        if (profile != null) {
            when (val strategy = profile.strategy) {
                is ExchangeRateStrategy.Fixed -> {
                    rateToUsd = strategy.rate
                    kind = RateSourceKind.FIXED
                }
                is ExchangeRateStrategy.ManualDaily -> {
                    rateToUsd = strategy.rate
                    kind = RateSourceKind.MANUAL
                    asOfEpochMs = strategy.updatedAtEpochMs
                }
                is ExchangeRateStrategy.Verified -> {
                    rateToUsd = strategy.currentRate
                    kind = RateSourceKind.VERIFIED
                    reference = strategy.sourceId
                }
                is ExchangeRateStrategy.CustomSelector -> {
                    rateToUsd = strategy.selector.lastKnownRate
                    kind = RateSourceKind.WEB_SELECTOR
                    reference = strategy.selector.sourceUrl
                    isDegraded = strategy.selector.status == SelectorStatus.NEEDS_RECONFIRMATION
                    asOfEpochMs = strategy.selector.lastSuccessfulFetchEpochMs
                }
            }
        }

        val usdToLocalRates =
            mapOf(
                localCurrency.code to rateToUsd,
                AnchorCurrencies.USD.code to 1.0,
            )

        val converter =
            CurrencyConverter(
                usdToLocalRates = usdToLocalRates,
                cryptoToUsdRates = cryptoToUsdRates,
            )

        val stamp =
            ProviderStamp(
                kind = kind,
                reference = reference,
                exchangeRateToUsd = rateToUsd,
                asOfEpochMs = asOfEpochMs,
                isDegradedFallback = isDegraded,
            )

        return converter.consolidate(
            holdings = holdings,
            targetCurrency = target,
            providerStamp = stamp,
        )
    }
}
