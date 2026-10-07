package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.VerifiedSourceDirectory
import com.coinden.core.domain.usecase.ConfigureLocalCurrencyUseCase
import com.coinden.core.domain.usecase.GetConsolidatedPortfolioUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Escenario de Feature 3: Facundo (Argentina)
 *
 * Flujo:
 * - Selección de un solo toque desde la Lista Blanca comunitaria (DolarApi Dólar Blue).
 * - Cartera: 500 USD en efectivo y 0.05 BTC ($3,110 USD a precio spot $62,200).
 * - Tasa aplicada: 1,420.00 ARS/USD.
 * - Resultado:
 *   Total USD: $3,610.00 USD -> 5,126,200.00 ARS.
 */
class FacundoArgentinaFeatureTest {
    private val repository = InMemoryCurrencyProfileRepository()
    private val configureLocalUseCase = ConfigureLocalCurrencyUseCase(repository)
    private val getPortfolioUseCase =
        GetConsolidatedPortfolioUseCase(
            repository = repository,
            cryptoToUsdRates = mapOf("BTC" to 62200.0),
        )

    @Test
    fun `facundo selecciona dolar blue desde lista blanca y consolida cartera exacta en ARS`() =
        runTest {
            val userId = "facundo-argentina-003"

            // 1. Facundo elige la fuente preconfigurada de Argentina
            val verifiedSource = VerifiedSourceDirectory.ARGENTINA_DOLAR_BLUE
            assertEquals("ar-dolar-blue", verifiedSource.id)

            // 2. Configuración en un solo paso
            val currentBlueRate = 1420.0
            configureLocalUseCase(
                userId = userId,
                localCurrency = verifiedSource.targetCurrency,
                strategy =
                    ExchangeRateStrategy.Verified(
                        sourceId = verifiedSource.id,
                        currentRate = currentBlueRate,
                    ),
            )

            // 3. Cartera de Facundo: 500 USD + 0.05 BTC
            val holdings =
                listOf(
                    Money.fromDecimal(500.0, Currency.USD),
                    Money.fromDecimal(0.05, Currency.BTC),
                )

            // 4. Consolidación
            val portfolio = getPortfolioUseCase(userId, holdings)

            // Cálculo esperado:
            // 500 USD * 1420 = 710,000.00 ARS
            // 0.05 BTC = 3,110 USD * 1420 = 4,416,200.00 ARS
            // Total = 5,126,200.00 ARS
            assertEquals("5126200.00 ARS", portfolio.totalUnifiedAmount.toFormattedString())
            assertEquals(512620000L, portfolio.totalUnifiedAmount.amountInMinorUnits)

            // Verificación de sello de auditoría
            val stamp = portfolio.primaryProviderStamp
            assertEquals("Fuente Verificada: ar-dolar-blue", stamp?.sourceName)
            assertEquals(1420.0, stamp?.exchangeRateToUsd)
            assertFalse(stamp?.isDegradedFallback ?: true)
        }
}
