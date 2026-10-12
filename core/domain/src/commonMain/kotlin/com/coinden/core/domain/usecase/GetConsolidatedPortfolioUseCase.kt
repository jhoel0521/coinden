package com.coinden.core.domain.usecase

import com.coinden.core.domain.model.ConsolidatedPortfolio
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.ProfileRates
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort

/**
 * Caso de Uso: Obtener la proyección de cartera consolidada (Vista Unificada y Vista Desglosada).
 */
class GetConsolidatedPortfolioUseCase(
    private val repository: CurrencyProfileRepositoryPort,
    private val cryptoRates: List<ExchangeRate> = ProfileRates.DEFAULT_CRYPTO_RATES,
) {
    suspend operator fun invoke(
        userId: String,
        holdings: List<Money>,
        viewInCurrency: Currency? = null,
    ): ConsolidatedPortfolio {
        val rates = ProfileRates.of(repository.getProfile(userId), cryptoRates)
        return rates.converter.consolidate(
            holdings = holdings,
            targetCurrency = viewInCurrency ?: rates.localCurrency,
            providerStamp = rates.stamp,
        )
    }
}
