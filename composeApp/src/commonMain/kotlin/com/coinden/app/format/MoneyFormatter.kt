package com.coinden.app.format

import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.interpret.UserLocale

/**
 * Formatea un [Money] según el locale: separador decimal del país y el opuesto para los miles.
 * Parte de [Money.toNumericString] y solo agrega separadores: nunca reescribe ni redondea dígitos.
 */
object MoneyFormatter {
    fun format(
        money: Money,
        locale: UserLocale,
    ): String = "${formatAmount(money, locale)} ${money.currency.symbol ?: money.currency.code}"

    fun formatAmount(
        money: Money,
        locale: UserLocale,
    ): String {
        val decimalSeparator = locale.decimalSeparator
        val groupSeparator = if (decimalSeparator == ',') '.' else ','
        val canonical = money.toNumericString()
        val sign = if (canonical.startsWith("-")) "-" else ""
        val unsigned = canonical.removePrefix("-")
        val integer = unsigned.substringBefore('.')
        val fraction = unsigned.substringAfter('.', "")
        val grouped =
            integer
                .reversed()
                .chunked(3)
                .joinToString(groupSeparator.toString())
                .reversed()
        return if (fraction.isEmpty()) "$sign$grouped" else "$sign$grouped$decimalSeparator$fraction"
    }
}
