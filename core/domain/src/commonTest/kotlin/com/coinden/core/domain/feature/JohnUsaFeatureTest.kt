package com.coinden.core.domain.feature

import com.coinden.core.domain.model.AnchorCurrencies
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.RateSourceKind
import com.coinden.core.domain.usecase.ConfigureAnchorCurrencyUseCase
import com.coinden.core.domain.usecase.ConfigureLocalCurrencyUseCase
import com.coinden.core.domain.usecase.GetConsolidatedPortfolioUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Escenario de Feature 1: John (Estados Unidos)
 *
 * Flujo:
 * - Onboarding express en USD (sin configuración cambiaria requerida).
 * - Cartera: 0.15 BTC (precio spot $62,200 USD).
 * - Resultado:
 *   - Vista Unificada: $9,330.00 USD.
 *   - Vista Desglosada: 1 posición (0.15 BTC -> $9,330.00 USD).
 *   - Sello de proveedor: Base USD.
 */
class JohnUsaFeatureTest {
    private val repository = InMemoryCurrencyProfileRepository()
    private val configureAnchorUseCase = ConfigureAnchorCurrencyUseCase(repository)
    private val configureLocalUseCase = ConfigureLocalCurrencyUseCase(repository)
    private val getPortfolioUseCase =
        GetConsolidatedPortfolioUseCase(
            repository = repository,
            cryptoToUsdRates = mapOf("BTC" to 62200.0),
        )

    @Test
    fun `john completa onboarding express en USD y visualiza cartera unificada de 9330 USD`() =
        runTest {
            val userId = "john-usa-001"

            // 1. Configuración de perfil
            configureAnchorUseCase(userId, AnchorCurrencies.USD)
            configureLocalUseCase(userId, Currency.USD, ExchangeRateStrategy.Fixed(1.0))

            // 2. Posición de cartera (0.15 BTC)
            val holdings =
                listOf(
                    Money.fromDecimal(0.15, Currency.BTC),
                )

            // 3. Consulta de cartera consolidada
            val portfolio = getPortfolioUseCase(userId, holdings)

            // 4. Verificación de Vista Unificada
            assertEquals(Currency.USD, portfolio.targetCurrency)
            assertEquals("9330.00 USD", portfolio.totalUnifiedAmount.toString())
            assertEquals(933000L, portfolio.totalUnifiedAmount.amountInMinorUnits)

            // 5. Verificación de Vista Desglosada
            assertEquals(1, portfolio.holdings.size)
            val btcItem = portfolio.holdings[0]
            assertEquals("0.15000000 BTC", btcItem.originalAmount.toString())
            assertEquals("9330.00 USD", btcItem.convertedAmount.toString())

            // 6. Verificación de Sello de Proveedor
            val stamp = portfolio.primaryProviderStamp
            assertNotNull(stamp)
            assertFalse(stamp.isDegradedFallback)
            assertEquals(1.0, stamp.exchangeRateToUsd)
            assertEquals(RateSourceKind.FIXED, stamp.kind)
            assertNull(stamp.reference)
        }
}
