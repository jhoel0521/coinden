package com.coinden.core.domain.model

import kotlin.math.abs

/**
 * Value Object inmutable que representa un monto monetario seguro.
 *
 * Implementa el patrón "Money Pattern" (Martin Fowler) para escala mundial:
 * - Almacena el valor en unidades mínimas enteras ([amountInMinorUnits] / Long),
 *   eliminando por completo los errores de redondeo de punto flotante (Double/Float).
 * - Respeta dinámicamente la escala de la divisa (0 decimales para CLP/SATS/VALE, 2 para BOB/USD, 8 para BTC).
 * - Prohibe operaciones aritméticas entre distintas monedas sin conversión previa.
 * - Sin idioma: [toString] es la forma canónica ("1188.00 BOB": punto decimal, sin separadores
 *   de miles ni símbolo) para pruebas, registros y serialización. El formato regional es tarea
 *   de la presentación.
 */
data class Money(
    val amountInMinorUnits: Long,
    val currency: Currency = Currency.USD,
) : Comparable<Money> {
    val amountInCents: Long get() = amountInMinorUnits

    val isZero: Boolean get() = amountInMinorUnits == 0L
    val isPositive: Boolean get() = amountInMinorUnits > 0L
    val isNegative: Boolean get() = amountInMinorUnits < 0L

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amountInMinorUnits + other.amountInMinorUnits, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amountInMinorUnits - other.amountInMinorUnits, currency)
    }

    operator fun times(factor: Long): Money = Money(amountInMinorUnits * factor, currency)

    operator fun times(factor: Int): Money = times(factor.toLong())

    operator fun unaryMinus(): Money = Money(-amountInMinorUnits, currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amountInMinorUnits.compareTo(other.amountInMinorUnits)
    }

    /**
     * Devuelve el monto formateado numéricamente con signo (ej. "35.50", "-12.50", "5000", "0.00050000").
     */
    fun toNumericString(): String {
        val decimals = currency.decimals
        if (decimals == 0) {
            return amountInMinorUnits.toString()
        }

        val scale = scaleFactor(decimals)
        val isNeg = amountInMinorUnits < 0
        val absValue = abs(amountInMinorUnits)
        val majorPart = absValue / scale
        val minorPart = absValue % scale
        val minorStr = minorPart.toString().padStart(decimals, '0')
        val sign = if (isNeg) "-" else ""

        return "$sign$majorPart.$minorStr"
    }

    fun toUnsignedNumericString(): String {
        val decimals = currency.decimals
        val absValue = abs(amountInMinorUnits)
        if (decimals == 0) {
            return absValue.toString()
        }

        val scale = scaleFactor(decimals)
        val majorPart = absValue / scale
        val minorPart = absValue % scale
        val minorStr = minorPart.toString().padStart(decimals, '0')

        return "$majorPart.$minorStr"
    }

    fun toDecimalDouble(): Double {
        val scale = scaleFactor(currency.decimals)
        return amountInMinorUnits.toDouble() / scale.toDouble()
    }

    override fun toString(): String {
        return "${toNumericString()} ${currency.code}"
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "No se pueden operar monedas distintas: '${currency.code}' y '${other.currency.code}'"
        }
    }

    companion object {
        fun scaleFactor(decimals: Int): Long {
            var factor = 1L
            repeat(decimals) { factor *= 10L }
            return factor
        }

        fun fromMinorUnits(
            units: Long,
            currency: Currency,
        ): Money = Money(units, currency)

        fun zero(currency: Currency = Currency.USD): Money = Money(0L, currency)

        fun fromMajorUnits(
            majorUnits: Long,
            minorUnits: Long = 0,
            currency: Currency = Currency.USD,
        ): Money {
            val scale = scaleFactor(currency.decimals)
            val sign = if (majorUnits < 0) -1L else 1L
            val totalMinor = (majorUnits * scale) + (sign * abs(minorUnits))
            return Money(totalMinor, currency)
        }

        /**
         * Lee un monto en forma canónica: signo opcional, dígitos y, si la moneda tiene decimales,
         * un punto seguido de 1 a [Currency.decimals] dígitos. Ej.: "35.50", "-12.5", "1200".
         *
         * Rechaza formatos regionales ("35,50", "1.234,56", "1,234.56") y decimales de más:
         * interpretar lo que escribe el usuario corresponde a la presentación o al parser de borradores.
         */
        fun parseCanonical(
            canonical: String,
            currency: Currency,
        ): Money {
            val decimals = currency.decimals
            val pattern = if (decimals == 0) Regex("""^-?\d+$""") else Regex("""^-?\d+(\.\d{1,$decimals})?$""")
            require(pattern.matches(canonical)) { "Monto no canónico para ${currency.code}: '$canonical'" }

            val isNegative = canonical.startsWith("-")
            val unsigned = canonical.removePrefix("-")
            val major = unsigned.substringBefore(".")
            val minor = unsigned.substringAfter(".", "").padEnd(decimals, '0')
            val total = major.toLong() * scaleFactor(decimals) + (if (minor.isEmpty()) 0L else minor.toLong())
            return Money(if (isNegative) -total else total, currency)
        }

        fun fromDecimal(
            amount: Double,
            currency: Currency = Currency.USD,
        ): Money {
            val scale = scaleFactor(currency.decimals)
            val minor = kotlin.math.round(amount * scale).toLong()
            return Money(minor, currency)
        }
    }
}
