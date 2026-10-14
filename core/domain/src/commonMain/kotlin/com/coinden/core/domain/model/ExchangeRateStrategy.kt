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
    val lastKnownRate: ExchangeRate,
    val lastSuccessfulFetchEpochMs: Long,
    val status: SelectorStatus = SelectorStatus.ACTIVE,
) {
    init {
        require(sourceUrl.isNotBlank()) { "La URL fuente no puede estar vacía" }
        require(selectorExpression.isNotBlank()) { "La expresión del selector no puede estar vacía" }
    }

    fun markNeedsReconfirmation(): SavedRateSelector = copy(status = SelectorStatus.NEEDS_RECONFIRMATION)

    fun updateRate(
        newRate: ExchangeRate,
        fetchEpochMs: Long,
    ): SavedRateSelector =
        copy(
            lastKnownRate = newRate,
            lastSuccessfulFetchEpochMs = fetchEpochMs,
            status = SelectorStatus.ACTIVE,
        )
}

/**
 * Estrategia polimórfica para determinar el tipo de cambio de la moneda local.
 *
 * Toda tasa se expresa como [ExchangeRate] desde USD hacia la moneda local
 * ("1 USD = 11.88 BOB"); un usuario cuya moneda local es USD usa la identidad.
 */
sealed interface ExchangeRateStrategy {
    /** Tasa fija invariable (ej. 1 USD = 1.0 USD o paridad fija). */
    data class Fixed(
        val rate: ExchangeRate,
    ) : ExchangeRateStrategy

    /** Tasa introducida manualmente por el usuario día a día. */
    data class ManualDaily(
        val rate: ExchangeRate,
        val updatedAtEpochMs: Long,
    ) : ExchangeRateStrategy

    /** Tasa extraída automáticamente desde una fuente verificada de la lista blanca. */
    data class Verified(
        val sourceId: String,
        val currentRate: ExchangeRate,
    ) : ExchangeRateStrategy {
        init {
            require(sourceId.isNotBlank()) { "El sourceId no puede estar vacío" }
        }
    }

    /** Tasa automatizada con selector web personalizado y ciclo de vida de confirmación. */
    data class CustomSelector(
        val selector: SavedRateSelector,
    ) : ExchangeRateStrategy {
        val currentRate: ExchangeRate get() = selector.lastKnownRate
        val isDegraded: Boolean get() = selector.status == SelectorStatus.NEEDS_RECONFIRMATION
    }
}

/** La tasa que aplica hoy cada estrategia. */
val ExchangeRateStrategy.currentRate: ExchangeRate
    get() =
        when (this) {
            is ExchangeRateStrategy.Fixed -> rate
            is ExchangeRateStrategy.ManualDaily -> rate
            is ExchangeRateStrategy.Verified -> currentRate
            is ExchangeRateStrategy.CustomSelector -> currentRate
        }
