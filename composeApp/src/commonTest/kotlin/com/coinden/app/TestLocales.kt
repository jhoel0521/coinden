package com.coinden.app

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.interpret.UserLocale

/** Locales de prueba de las personas, como los armaría el adaptador regional de la plataforma. */
fun testLocale(
    language: String,
    country: String,
): UserLocale =
    when (country) {
        "BO" -> UserLocale(language, country, Currency.BOB, '.')
        "AR" -> UserLocale(language, country, Currency.ARS, ',')
        "BR" -> UserLocale(language, country, Currency.BRL, ',')
        "FR" -> UserLocale(language, country, Currency.EUR, ',')
        else -> UserLocale(language, country, Currency.USD, '.')
    }
