package com.coinden.core.domain.model

/**
 * Estado del selector web guardado por el usuario.
 */
enum class SelectorStatus {
    /** El selector sigue funcionando y extrayendo datos con éxito en cada ciclo diario. */
    ACTIVE,

    /** El selector falló (la web cambió de diseño o estructura); se requiere que el usuario reconfirme. */
    NEEDS_RECONFIRMATION,
}

/**
 * Entidad que encapsula un selector personalizado confirmado por el usuario para una web específica.
 *
 * Implementa resiliencia y degradación elegante:
 * Si una página web cambia su DOM/HTML, el sistema no crashea ni deja en blanco la cartera:
 * retiene [lastKnownRate], marca el estado como [NEEDS_RECONFIRMATION] y alerta al usuario.
 */
data class SavedRateSelector(
    val id: String,
    val targetCurrency: Currency,
    val sourceUrl: String,
    val format: ResponseFormat,
    val selectorExpression: String,
    val lastKnownRate: Double,
    val lastSuccessfulFetchEpochMs: Long,
    val status: SelectorStatus = SelectorStatus.ACTIVE,
) {
    init {
        require(sourceUrl.isNotBlank()) { "La URL fuente no puede estar vacía" }
        require(selectorExpression.isNotBlank()) { "La expresión del selector no puede estar vacía" }
        require(lastKnownRate > 0.0) { "La tasa conocida debe ser mayor a cero" }
    }

    fun markNeedsReconfirmation(): SavedRateSelector = copy(status = SelectorStatus.NEEDS_RECONFIRMATION)

    fun updateRate(
        newRate: Double,
        fetchEpochMs: Long,
    ): SavedRateSelector {
        require(newRate > 0.0) { "La nueva tasa debe ser mayor a cero" }
        return copy(
            lastKnownRate = newRate,
            lastSuccessfulFetchEpochMs = fetchEpochMs,
            status = SelectorStatus.ACTIVE,
        )
    }
}

/**
 * Estrategia polimórfica para determinar el tipo de cambio de una divisa hacia USD.
 */
sealed interface ExchangeRateStrategy {
    /** Tasa fija invariable (ej. 1 USD = 1.0 USD o paridad fija). */
    data class Fixed(val rate: Double) : ExchangeRateStrategy {
        init {
            require(rate > 0.0) { "La tasa fija debe ser estrictamente positiva" }
        }
    }

    /** Tasa introducida manualmente por el usuario día a día. */
    data class ManualDaily(
        val rate: Double,
        val updatedAtEpochMs: Long,
    ) : ExchangeRateStrategy {
        init {
            require(rate > 0.0) { "La tasa manual debe ser estrictamente positiva" }
        }
    }

    /** Tasa extraída automáticamente desde una fuente verificada de la lista blanca. */
    data class Verified(
        val sourceId: String,
        val currentRate: Double,
    ) : ExchangeRateStrategy {
        init {
            require(sourceId.isNotBlank()) { "El sourceId no puede estar vacío" }
            require(currentRate > 0.0) { "La tasa verificada debe ser estrictamente positiva" }
        }
    }

    /** Tasa automatizada con selector web personalizado y ciclo de vida de confirmación. */
    data class CustomSelector(
        val selector: SavedRateSelector,
    ) : ExchangeRateStrategy {
        val currentRate: Double get() = selector.lastKnownRate
        val isDegraded: Boolean get() = selector.status == SelectorStatus.NEEDS_RECONFIRMATION
    }
}
