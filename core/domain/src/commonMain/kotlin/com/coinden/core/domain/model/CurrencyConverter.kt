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
 * es null en los demás tipos. [rate] es la tasa de la moneda local desde USD.
 */
data class ProviderStamp(
    val kind: RateSourceKind,
    val reference: String?,
    val rate: ExchangeRate,
    val asOfEpochMs: Long,
    val isDegradedFallback: Boolean = false,
)

/**
 * Desglose individual de una posición/activo de la cartera.
 *
 * [appliedRate] es la tasa compuesta exacta usada para convertir (null si la posición ya
 * estaba en la moneda destino).
 */
data class PortfolioHoldingItem(
    val originalAmount: Money,
    val convertedAmount: Money,
    val appliedRate: ExchangeRate?,
    val providerStamp: ProviderStamp? = null,
)

/**
 * Proyección de cartera consolidada según los requisitos de CoinDen:
 * - Vista Unificada: Un monto único total en la moneda objetivo con sello de procedencia.
 * - Vista Detallada: Lista completa desglosada por cada activo poseído.
 *
 * El total unificado es, por construcción, la suma exacta de los montos convertidos del desglose.
 */
data class ConsolidatedPortfolio(
    val targetCurrency: Currency,
    val totalUnifiedAmount: Money,
    val holdings: List<PortfolioHoldingItem>,
    val primaryProviderStamp: ProviderStamp?,
)

/**
 * Motor de conversión multidivisa por composición exacta de tasas.
 *
 * Recibe tasas que involucran a USD (en cualquier sentido: "1 USD = 11.88 BOB" o
 * "1 BTC = 62,200 USD"). Para convertir A -> B compone (A -> USD) * (USD -> B) sin redondear
 * y aplica `money * tasa` una sola vez: un único redondeo half-even por posición.
 */
class CurrencyConverter(
    rates: Collection<ExchangeRate> = emptyList(),
) {
    private val fromUsd: Map<String, ExchangeRate> =
        rates
            .filterNot { it.isIdentity }
            .map { rate ->
                when {
                    rate.from.isSameAs(PIVOT) -> rate
                    rate.to.isSameAs(PIVOT) -> rate.inverse()
                    else -> throw IllegalArgumentException("La tasa ${rate.from.code}->${rate.to.code} no involucra a USD")
                }
            }.associateBy { it.to.code }

    /** Tasa compuesta exacta de [source] a [target], o null si son la misma moneda. */
    fun rateBetween(
        source: Currency,
        target: Currency,
    ): ExchangeRate? {
        if (source.isSameAs(target)) return null
        val toUsd = if (source.isSameAs(PIVOT)) null else usdRateFor(source).inverse()
        val usdToTarget = if (target.isSameAs(PIVOT)) null else usdRateFor(target)
        return when {
            toUsd == null -> usdToTarget
            usdToTarget == null -> toUsd
            else -> toUsd * usdToTarget
        }
    }

    /** Convierte [source] a [target] con un único redondeo. */
    fun convert(
        source: Money,
        target: Currency,
    ): Money {
        val rate = rateBetween(source.currency, target) ?: return source
        return source * rate
    }

    /**
     * Consolida una lista de activos heterogéneos en una vista unificada y detallada.
     */
    fun consolidate(
        holdings: List<Money>,
        targetCurrency: Currency,
        providerStamp: ProviderStamp? = null,
    ): ConsolidatedPortfolio {
        var total = Money.zero(targetCurrency)
        val items =
            holdings.map { holding ->
                val rate = rateBetween(holding.currency, targetCurrency)
                val converted = if (rate == null) holding else holding * rate
                total += converted
                PortfolioHoldingItem(
                    originalAmount = holding,
                    convertedAmount = converted,
                    appliedRate = rate,
                    providerStamp = providerStamp,
                )
            }

        return ConsolidatedPortfolio(
            targetCurrency = targetCurrency,
            totalUnifiedAmount = total,
            holdings = items,
            primaryProviderStamp = providerStamp,
        )
    }

    private fun usdRateFor(currency: Currency): ExchangeRate =
        fromUsd[currency.code]
            ?: throw IllegalArgumentException("No hay tasa de cambio registrada entre USD y ${currency.code}")

    private companion object {
        val PIVOT = Currency.USD
    }
}
