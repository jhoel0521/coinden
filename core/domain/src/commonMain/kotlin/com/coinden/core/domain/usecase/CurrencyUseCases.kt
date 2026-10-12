package com.coinden.core.domain.usecase

import com.coinden.core.domain.extractor.HtmlRegexExtractor
import com.coinden.core.domain.extractor.JsonDotNotationExtractor
import com.coinden.core.domain.model.AnchorCurrencies
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.ResponseFormat
import com.coinden.core.domain.model.SavedRateSelector
import com.coinden.core.domain.model.SelectorStatus
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort
import com.coinden.core.domain.port.output.RemoteWebFetcherPort

/**
 * Caso de Uso: Configurar o cambiar la moneda ancla de reserva (USD por defecto, o EUR, CNY, BTC).
 */
class ConfigureAnchorCurrencyUseCase(
    private val repository: CurrencyProfileRepositoryPort,
) {
    suspend operator fun invoke(
        userId: String,
        anchorCurrency: Currency,
    ): UserCurrencyProfile {
        require(AnchorCurrencies.isAnchor(anchorCurrency)) {
            "La moneda ${anchorCurrency.code} no pertenece a las monedas ancla universales (USD, EUR, CNY, BTC)"
        }

        val existing = repository.getProfile(userId) ?: UserCurrencyProfile(userId = userId)
        val updated = existing.copy(anchorCurrency = anchorCurrency)
        repository.saveProfile(updated)
        return updated
    }
}

/**
 * Caso de Uso: Configurar la moneda local y su estrategia de tipo de cambio.
 */
class ConfigureLocalCurrencyUseCase(
    private val repository: CurrencyProfileRepositoryPort,
) {
    suspend operator fun invoke(
        userId: String,
        localCurrency: Currency,
        strategy: ExchangeRateStrategy,
    ): UserCurrencyProfile {
        val existing = repository.getProfile(userId) ?: UserCurrencyProfile(userId = userId)
        val updated =
            existing.copy(
                localCurrency = localCurrency,
                strategy = strategy,
            )
        repository.saveProfile(updated)
        return updated
    }
}

/**
 * Resultado de la prueba de extracción y confirmación del selector web.
 */
sealed interface SelectorConfirmationResult {
    data class Success(val extractedRate: Double, val selector: SavedRateSelector) : SelectorConfirmationResult

    data class Failure(val reason: SelectorFailure) : SelectorConfirmationResult
}

/**
 * Motivo tipado de fallo al confirmar un selector web; la presentación lo traduce.
 */
enum class SelectorFailure {
    /** La URL no respondió o devolvió un error de red/HTTP. */
    FETCH_FAILED,

    /** La página respondió, pero el selector no extrajo una cotización mayor a cero. */
    NO_VALID_RATE,
}

/**
 * Caso de Uso: Probar y confirmar un selector personalizado sobre una web externa.
 *
 * Flujo:
 * 1. Hace fetch al URL remoto usando [RemoteWebFetcherPort].
 * 2. Aplica el extractor según el formato ([JsonDotNotationExtractor] o [HtmlRegexExtractor]).
 * 3. Si extrae una tasa válida, guarda el selector en estado ACTIVE y asocia la estrategia al perfil.
 */
class ConfirmWebSelectorUseCase(
    private val repository: CurrencyProfileRepositoryPort,
    private val webFetcher: RemoteWebFetcherPort,
    private val jsonExtractor: JsonDotNotationExtractor = JsonDotNotationExtractor(),
    private val htmlExtractor: HtmlRegexExtractor = HtmlRegexExtractor(),
) {
    suspend operator fun invoke(
        userId: String,
        targetCurrency: Currency,
        sourceUrl: String,
        format: ResponseFormat,
        selectorExpression: String,
        currentEpochMs: Long = 0L,
    ): SelectorConfirmationResult {
        val payload =
            try {
                webFetcher.fetchUrl(sourceUrl)
            } catch (e: Exception) {
                return SelectorConfirmationResult.Failure(SelectorFailure.FETCH_FAILED)
            }

        val extractedRate =
            when (format) {
                ResponseFormat.JSON -> jsonExtractor.extract(payload, selectorExpression)
                ResponseFormat.HTML -> htmlExtractor.extract(payload, selectorExpression)
            }

        if (extractedRate == null || extractedRate <= 0.0) {
            return SelectorConfirmationResult.Failure(SelectorFailure.NO_VALID_RATE)
        }

        val savedSelector =
            SavedRateSelector(
                id = "custom-${targetCurrency.code.lowercase()}",
                targetCurrency = targetCurrency,
                sourceUrl = sourceUrl,
                format = format,
                selectorExpression = selectorExpression,
                lastKnownRate = extractedRate,
                lastSuccessfulFetchEpochMs = currentEpochMs,
                status = SelectorStatus.ACTIVE,
            )

        val existing = repository.getProfile(userId) ?: UserCurrencyProfile(userId = userId)
        val updated =
            existing.copy(
                localCurrency = targetCurrency,
                strategy = ExchangeRateStrategy.CustomSelector(savedSelector),
            )
        repository.saveProfile(updated)

        return SelectorConfirmationResult.Success(extractedRate, savedSelector)
    }
}

/**
 * Caso de Uso: Actualizar manualmente la tasa de cambio diaria de la moneda local.
 */
class UpdateManualExchangeRateUseCase(
    private val repository: CurrencyProfileRepositoryPort,
) {
    suspend operator fun invoke(
        userId: String,
        newRate: Double,
        currentEpochMs: Long = 0L,
    ): UserCurrencyProfile {
        require(newRate > 0.0) { "La tasa debe ser mayor a cero" }

        val existing =
            repository.getProfile(userId)
                ?: throw IllegalStateException("Perfil de usuario no encontrado")

        val updatedStrategy =
            ExchangeRateStrategy.ManualDaily(
                rate = newRate,
                updatedAtEpochMs = currentEpochMs,
            )

        val updated = existing.copy(strategy = updatedStrategy)
        repository.saveProfile(updated)
        return updated
    }
}
