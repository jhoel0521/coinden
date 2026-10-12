package com.coinden.core.domain.model

/**
 * Tipo de fuente de una tasa de cambio. La presentación arma el texto en el idioma del usuario.
 */
enum class RateSourceKind {
    /** Sin perfil configurado: se valora en USD. */
    BASE,
    FIXED,
    MANUAL,
    VERIFIED,
    WEB_SELECTOR,
}

/**
 * Sello de auditoría y procedencia del proveedor de tipo de cambio, como datos estructurados.
 *
 * [reference] es el id de la fuente verificada (VERIFIED) o la URL configurada (WEB_SELECTOR);
 * es null en los demás tipos.
 */
data class ProviderStamp(
    val kind: RateSourceKind,
    val reference: String?,
    val exchangeRateToUsd: Double,
    val asOfEpochMs: Long,
    val isDegradedFallback: Boolean = false,
)

/**
 * Desglose individual de una posición/activo de la cartera.
 */
data class PortfolioHoldingItem(
    val originalAmount: Money,
    val convertedAmount: Money,
    val exchangeRateToUsd: Double,
    val providerStamp: ProviderStamp? = null,
)

/**
 * Proyección de cartera consolidada según los requisitos de CoinDen:
 * - Vista Unificada: Un monto único total en la moneda objetivo con sello de procedencia.
 * - Vista Detallada: Lista completa desglosada por cada activo poseído.
 */
data class ConsolidatedPortfolio(
    val targetCurrency: Currency,
    val totalUnifiedAmount: Money,
    val holdings: List<PortfolioHoldingItem>,
    val primaryProviderStamp: ProviderStamp?,
)

/**
 * Motor de conversión multidivisa transitivo universal.
 *
 * Fórmula Matemática:
 * Todos los activos transitan obligatoriamente a través del pivote USD:
 * 1. (Monto en X) -> USD: Monto * Rate(X -> USD)
 * 2. USD -> (Moneda Local L): Monto en USD * Rate(USD -> L)
 */
class CurrencyConverter(
    private val usdToLocalRates: Map<String, Double> = emptyMap(),
    private val cryptoToUsdRates: Map<String, Double> = emptyMap(),
) {
    /**
     * Convierte un monto [source] a la divisa [target].
     *
     * @param source Dinero original (ej. 0.15 BTC o 100 USD)
     * @param target Divisa destino deseada (ej. BOB, ARS, EUR)
     */
    fun convert(
        source: Money,
        target: Currency,
    ): Money {
        if (source.currency.code == target.code) {
            return source
        }

        // Paso 1: Convertir de origen a USD
        val valueInUsd: Double =
            when {
                source.currency.isSameAs(Currency.USD) -> {
                    source.toDecimalDouble()
                }
                cryptoToUsdRates.containsKey(source.currency.code) -> {
                    val rate = cryptoToUsdRates.getValue(source.currency.code)
                    source.toDecimalDouble() * rate
                }
                usdToLocalRates.containsKey(source.currency.code) -> {
                    // Si la moneda origen es una moneda local (ej. BOB -> USD): dividir por la tasa USD->BOB
                    val rate = usdToLocalRates.getValue(source.currency.code)
                    require(rate > 0.0) { "La tasa de cambio debe ser positiva" }
                    source.toDecimalDouble() / rate
                }
                else -> {
                    throw IllegalArgumentException("No hay tasa de cambio registrada para convertir ${source.currency.code} a USD")
                }
            }

        // Paso 2: Convertir de USD a moneda destino
        val finalAmountDouble: Double =
            when {
                target.isSameAs(Currency.USD) -> {
                    valueInUsd
                }
                usdToLocalRates.containsKey(target.code) -> {
                    val rate = usdToLocalRates.getValue(target.code)
                    valueInUsd * rate
                }
                cryptoToUsdRates.containsKey(target.code) -> {
                    val rate = cryptoToUsdRates.getValue(target.code)
                    require(rate > 0.0) { "La tasa crypto debe ser positiva" }
                    valueInUsd / rate
                }
                else -> {
                    throw IllegalArgumentException("No hay tasa de cambio registrada para convertir USD a ${target.code}")
                }
            }

        return Money.fromDecimal(finalAmountDouble, target)
    }

    /**
     * Consolida una lista de activos heterogéneos en una vista unificada y detallada.
     */
    fun consolidate(
        holdings: List<Money>,
        targetCurrency: Currency,
        providerStamp: ProviderStamp? = null,
    ): ConsolidatedPortfolio {
        var totalUnits: Long = 0L
        val items = mutableListOf<PortfolioHoldingItem>()

        for (holding in holdings) {
            val converted = convert(holding, targetCurrency)
            totalUnits += converted.amountInMinorUnits

            val rateApplied =
                if (holding.currency.isSameAs(targetCurrency)) {
                    1.0
                } else {
                    converted.toDecimalDouble() / holding.toDecimalDouble().coerceAtLeast(0.00000001)
                }

            items.add(
                PortfolioHoldingItem(
                    originalAmount = holding,
                    convertedAmount = converted,
                    exchangeRateToUsd = rateApplied,
                    providerStamp = providerStamp,
                ),
            )
        }

        val total = Money.fromMinorUnits(totalUnits, targetCurrency)

        return ConsolidatedPortfolio(
            targetCurrency = targetCurrency,
            totalUnifiedAmount = total,
            holdings = items,
            primaryProviderStamp = providerStamp,
        )
    }
}
