package com.coinden.core.domain.model

/**
 * Configuración de preferencias de divisa del perfil del usuario.
 */
data class UserCurrencyProfile(
    val userId: String,
    val anchorCurrency: Currency = AnchorCurrencies.USD,
    val localCurrency: Currency = AnchorCurrencies.USD,
    val strategy: ExchangeRateStrategy = ExchangeRateStrategy.Fixed(1.0),
)
