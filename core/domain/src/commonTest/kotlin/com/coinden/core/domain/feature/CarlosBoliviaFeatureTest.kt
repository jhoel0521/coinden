package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.RateSourceKind
import com.coinden.core.domain.model.ResponseFormat
import com.coinden.core.domain.model.SelectorStatus
import com.coinden.core.domain.usecase.ConfirmWebSelectorUseCase
import com.coinden.core.domain.usecase.GetConsolidatedPortfolioUseCase
import com.coinden.core.domain.usecase.SelectorConfirmationResult
import com.coinden.core.domain.usecase.SelectorFailure
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Escenario de Feature 2: Carlos (Bolivia)
 *
 * Flujo:
 * - Ingresa URL de cotización (dolarbluebolivia.click) y selector visual.
 * - Confirma con éxito y el sistema persiste la regla en estado ACTIVE con tasa 11.88 BOB.
 * - Consulta cartera de 0.15 BTC + 100 USD -> 112,028.40 BOB.
 * - Simulación de rediseño de sitio web:
 *   La web remota cambia de estructura HTML. El sistema no crashea, mantiene el fallback
 *   con lastKnownRate (11.88 BOB), marca el estado NEEDS_RECONFIRMATION y alerta al usuario.
 */
class CarlosBoliviaFeatureTest {
    private val repository = InMemoryCurrencyProfileRepository()
    private val webFetcher = FakeWebFetcher()
    private val confirmSelectorUseCase = ConfirmWebSelectorUseCase(repository, webFetcher)
    private val getPortfolioUseCase =
        GetConsolidatedPortfolioUseCase(
            repository = repository,
            cryptoToUsdRates = mapOf("BTC" to 62200.0),
        )

    private val targetCurrency = Currency.of("BOB", 2)
    private val targetUrl = "https://www.dolarbluebolivia.click/"
    private val selector = """id="usdRate">[^<]*?(\d+(?:\.\d+)?)"""

    private val originalWebHtml =
        """
        <html><body>
        <div id="usdRate">Bs 11.88</div>
        </body></html>
        """.trimIndent()

    private val redesignedWebHtml =
        """
        <html><body>
        <div class="nueva-estructura-2026">Bs 12.50</div>
        </body></html>
        """.trimIndent()

    @Test
    fun `carlos confirma selector web sobre dolarbluebolivia y activa fallback seguro ante redisenio`() =
        runTest {
            val userId = "carlos-bolivia-002"

            // 1. Simular respuesta web disponible
            webFetcher.setResponse(targetUrl, originalWebHtml)

            // 2. Carlos prueba y confirma el selector
            val confirmation =
                confirmSelectorUseCase(
                    userId = userId,
                    targetCurrency = targetCurrency,
                    sourceUrl = targetUrl,
                    format = ResponseFormat.HTML,
                    selectorExpression = selector,
                    currentEpochMs = 1700000000000L,
                )

            assertIs<SelectorConfirmationResult.Success>(confirmation)
            assertEquals(11.88, confirmation.extractedRate)
            assertEquals(SelectorStatus.ACTIVE, confirmation.selector.status)

            // 3. Consulta de cartera inicial (0.15 BTC y 100 USD)
            val holdings =
                listOf(
                    Money.fromDecimal(0.15, Currency.BTC),
                    Money.fromDecimal(100.0, Currency.USD),
                )

            val portfolioDay1 = getPortfolioUseCase(userId, holdings)

            // Total: (0.15 * 62,200 * 11.88) + (100 * 11.88) = 110,840.40 + 1,188.00 = 112,028.40 BOB
            assertEquals("112028.40 BOB", portfolioDay1.totalUnifiedAmount.toString())
            assertEquals(false, portfolioDay1.primaryProviderStamp?.isDegradedFallback)
            assertEquals(RateSourceKind.WEB_SELECTOR, portfolioDay1.primaryProviderStamp?.kind)
            assertEquals(targetUrl, portfolioDay1.primaryProviderStamp?.reference)

            // 4. Al día siguiente: la web externa cambia su diseño
            webFetcher.setResponse(targetUrl, redesignedWebHtml)

            // El selector falla al consultar la nueva web
            val profile = repository.getProfile(userId)!!
            val currentStrategy = profile.strategy as ExchangeRateStrategy.CustomSelector
            val degradedSelector = currentStrategy.selector.markNeedsReconfirmation()

            // Actualizamos el perfil con el estado degradado pero conservando lastKnownRate
            repository.saveProfile(profile.copy(strategy = ExchangeRateStrategy.CustomSelector(degradedSelector)))

            // 5. Carlos abre la app: su cartera no crashea ni queda en blanco
            val portfolioDay2 = getPortfolioUseCase(userId, holdings)

            assertEquals("112028.40 BOB", portfolioDay2.totalUnifiedAmount.toString())
            // El sello de procedencia alerta al usuario que está en modo degraded fallback
            assertTrue(portfolioDay2.primaryProviderStamp?.isDegradedFallback == true)
            assertEquals(11.88, portfolioDay2.primaryProviderStamp?.exchangeRateToUsd)
            assertEquals(RateSourceKind.WEB_SELECTOR, portfolioDay2.primaryProviderStamp?.kind)
            assertEquals(targetUrl, portfolioDay2.primaryProviderStamp?.reference)
        }

    @Test
    fun `fallos del selector se informan tipados y no guardan el perfil`() =
        runTest {
            val userId = "carlos-bolivia-fallos"

            // La URL no responde
            val unreachable =
                confirmSelectorUseCase(userId, targetCurrency, targetUrl, ResponseFormat.HTML, selector)
            assertEquals(SelectorConfirmationResult.Failure(SelectorFailure.FETCH_FAILED), unreachable)

            // La página responde, pero ya no tiene una cotización válida
            webFetcher.setResponse(targetUrl, redesignedWebHtml)
            val noRate =
                confirmSelectorUseCase(userId, targetCurrency, targetUrl, ResponseFormat.HTML, selector)
            assertEquals(SelectorConfirmationResult.Failure(SelectorFailure.NO_VALID_RATE), noRate)

            assertNull(repository.getProfile(userId))
        }
}
