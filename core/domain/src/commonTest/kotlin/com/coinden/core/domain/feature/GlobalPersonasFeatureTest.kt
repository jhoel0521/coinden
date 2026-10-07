package com.coinden.core.domain.feature

import com.coinden.core.domain.model.AnchorCurrencies
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.usecase.ConfigureAnchorCurrencyUseCase
import com.coinden.core.domain.usecase.ConfigureLocalCurrencyUseCase
import com.coinden.core.domain.usecase.GetConsolidatedPortfolioUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Escenarios de Feature 4 y 5: Sophie (Francia / EUR) y Wei (China / CNY).
 *
 * Valida la internacionalización limpia y el intercambio de anclas universales.
 */
class GlobalPersonasFeatureTest {
    private val repository = InMemoryCurrencyProfileRepository()
    private val configureAnchorUseCase = ConfigureAnchorCurrencyUseCase(repository)
    private val configureLocalUseCase = ConfigureLocalCurrencyUseCase(repository)
    private val getPortfolioUseCase =
        GetConsolidatedPortfolioUseCase(
            repository = repository,
            cryptoToUsdRates = mapOf("BTC" to 62200.0),
        )

    @Test
    fun `sophie en francia unifica su cartera en EUR con tasa oficial del BCE`() =
        runTest {
            val userId = "sophie-france-004"

            // 1. Configurar ancla EUR y moneda local EUR con tasa 0.92
            configureAnchorUseCase(userId, AnchorCurrencies.EUR)
            configureLocalUseCase(
                userId = userId,
                localCurrency = Currency.EUR,
                strategy = ExchangeRateStrategy.Fixed(0.92),
            )

            // 2. Cartera con 1,000 USD
            val holdings =
                listOf(
                    Money.fromDecimal(1000.0, Currency.USD),
                )

            // 3. Consolidación
            val portfolio = getPortfolioUseCase(userId, holdings)

            // 1,000 USD * 0.92 = 920.00 EUR
            assertEquals(Currency.EUR, portfolio.targetCurrency)
            assertEquals("920.00 EUR", portfolio.totalUnifiedAmount.toFormattedString())
        }

    @Test
    fun `wei en china unifica cartera multiactivo en Yuanes CNY usando formula transitiva`() =
        runTest {
            val userId = "wei-china-005"

            // 1. Configurar CNY con tasa 7.15 USD/CNY
            val cny = Currency.CNY
            configureAnchorUseCase(userId, AnchorCurrencies.CNY)
            configureLocalUseCase(
                userId = userId,
                localCurrency = cny,
                strategy = ExchangeRateStrategy.Fixed(7.15),
            )

            // 2. Cartera: 2,000 USD y 0.1 BTC ($6,220 USD)
            val holdings =
                listOf(
                    Money.fromDecimal(2000.0, Currency.USD),
                    Money.fromDecimal(0.1, Currency.BTC),
                )

            // 3. Consolidación
            val portfolio = getPortfolioUseCase(userId, holdings)

            // Total USD = 2,000 + 6,220 = 8,220 USD
            // 8,220 * 7.15 = 58,773.00 CNY
            assertEquals("58773.00 CNY", portfolio.totalUnifiedAmount.toFormattedString())
            assertEquals(5877300L, portfolio.totalUnifiedAmount.amountInMinorUnits)
            assertEquals(2, portfolio.holdings.size)
        }
}
