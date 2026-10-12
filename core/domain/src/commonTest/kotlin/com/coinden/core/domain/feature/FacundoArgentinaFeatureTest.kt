package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.RateSourceKind
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
            cryptoRates = listOf(ExchangeRate.parse(Currency.BTC, Currency.USD, "62200")),
        )

    @Test
    fun `facundo selecciona dolar blue desde lista blanca y consolida cartera exacta en ARS`() =
        runTest {
            val userId = "facundo-argentina-003"

            // 1. Facundo elige la fuente preconfigurada de Argentina
            val verifiedSource = VerifiedSourceDirectory.ARGENTINA_DOLAR_BLUE
            assertEquals("ar-dolar-blue", verifiedSource.id)

            // 2. Configuración en un solo paso
            val currentBlueRate = ExchangeRate.parse(Currency.USD, Currency.ARS, "1420")
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
                    Money.parseCanonical("500.0", Currency.USD),
                    Money.parseCanonical("0.05", Currency.BTC),
                )

            // 4. Consolidación
            val portfolio = getPortfolioUseCase(userId, holdings)

            // Cálculo esperado:
            // 500 USD * 1420 = 710,000.00 ARS
            // 0.05 BTC = 3,110 USD * 1420 = 4,416,200.00 ARS
            // Total = 5,126,200.00 ARS
            assertEquals("5126200.00 ARS", portfolio.totalUnifiedAmount.toString())
            assertEquals(Money.ofMinorUnits(512_620_000L, Currency.ARS), portfolio.totalUnifiedAmount)

            // Verificación de sello de auditoría
            val stamp = portfolio.primaryProviderStamp
            assertEquals(RateSourceKind.VERIFIED, stamp?.kind)
            assertEquals("ar-dolar-blue", stamp?.reference)
            assertEquals(ExchangeRate.parse(Currency.USD, Currency.ARS, "1420"), stamp?.rate)
            assertFalse(stamp?.isDegradedFallback ?: true)
        }
}
