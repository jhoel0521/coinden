package com.coinden.core.data.local.json

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * DTOs de valores compartidos por todos los formatos guardados. El dominio no lleva anotaciones
 * de serialización: estos tipos son la forma en disco y se convierten con toDto()/toDomain().
 * Dinero y tasas se guardan como texto exacto (forma canónica), nunca como número flotante.
 */

internal val CoinDenJson =
    Json {
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

@Serializable
internal data class CurrencyDto(
    val code: String,
    val decimals: Int,
    val symbol: String? = null,
    val name: String? = null,
    val custom: Boolean = false,
)

/** Monto exacto: [value] es `Money.toNumericString()` ("45.00", "-3.25", "0.00000001"). */
@Serializable
internal data class MoneyDto(
    val value: String,
    val currency: CurrencyDto,
)

/** Tasa exacta: [fraction] es `ExchangeRate.toCanonical()` ("112/11"). */
@Serializable
internal data class RateDto(
    val from: CurrencyDto,
    val to: CurrencyDto,
    val fraction: String,
)

internal fun Currency.toDto() = CurrencyDto(code, decimals, symbol, name, isCustom)

internal fun CurrencyDto.toDomain() = Currency(code = code, symbol = symbol, decimals = decimals, name = name, isCustom = custom)

internal fun Money.toDto() = MoneyDto(toNumericString(), currency.toDto())

internal fun MoneyDto.toDomain() = Money.parseCanonical(value, currency.toDomain())

internal fun ExchangeRate.toDto() = RateDto(from.toDto(), to.toDto(), toCanonical())

internal fun RateDto.toDomain() = ExchangeRate.parseCanonical(fraction, from.toDomain(), to.toDomain())
