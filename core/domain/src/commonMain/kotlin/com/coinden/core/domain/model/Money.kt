package com.coinden.core.domain.model

import kotlin.math.abs

/**
 * Value Object inmutable que representa un monto monetario exacto.
 *
 * - Representación encapsulada: el monto vive en unidades mínimas enteras (centavos, satoshis)
 *   y nadie fuera de [Money], [ExchangeRate] y [ExactMath] lo lee. Así la representación
 *   puede cambiar (por ejemplo, a enteros de precisión arbitraria) sin tocar el resto del dominio.
 * - Solo operaciones de dominio: [plus]/[minus] en la misma moneda y [times] por una
 *   [ExchangeRate], la única forma de cruzar monedas: `bs + (usd * usdToBs)`.
 * - Desbordamiento explícito: si un resultado no cabe, lanza [ArithmeticException].
 * - Sin idioma: [toString] es la forma canónica ("1188.00 BOB"), también para la persistencia.
 */
class Money private constructor(
    internal val minorUnits: Long,
    val currency: Currency,
) : Comparable<Money> {
    val isZero: Boolean get() = minorUnits == 0L
    val isPositive: Boolean get() = minorUnits > 0L
    val isNegative: Boolean get() = minorUnits < 0L

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(ExactMath.addExact(minorUnits, other.minorUnits), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(ExactMath.addExact(minorUnits, ExactMath.negateExact(other.minorUnits)), currency)
    }

    operator fun unaryMinus(): Money = Money(ExactMath.negateExact(minorUnits), currency)

    /**
     * Convierte este monto a la moneda destino de [rate]: exacto hasta el final y un único
     * redondeo half-even a la unidad mínima destino.
     */
    operator fun times(rate: ExchangeRate): Money {
        require(currency.isSameAs(rate.from) && currency.decimals == rate.from.decimals) {
            "La tasa ${rate.from.code}->${rate.to.code} no aplica a un monto en ${currency.code}"
        }
        val scaled = ExactMath.multiplyExact(minorUnits, rate.numerator)
        return Money(ExactMath.divideHalfEven(scaled, rate.denominator), rate.to)
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    /** Número canónico con signo: "35.50", "-12.50", "5000", "0.00050000". */
    fun toNumericString(): String {
        val decimals = currency.decimals
        if (decimals == 0) return minorUnits.toString()

        val scale = scaleFactor(decimals)
        val absValue = abs(minorUnits)
        val majorPart = absValue / scale
        val minorStr = (absValue % scale).toString().padStart(decimals, '0')
        val sign = if (minorUnits < 0) "-" else ""
        return "$sign$majorPart.$minorStr"
    }

    override fun toString(): String = "${toNumericString()} ${currency.code}"

    override fun equals(other: Any?): Boolean = other is Money && other.minorUnits == minorUnits && other.currency == currency

    override fun hashCode(): Int = 31 * minorUnits.hashCode() + currency.hashCode()

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "No se pueden operar monedas distintas: '${currency.code}' y '${other.currency.code}'"
        }
    }

    companion object {
        internal fun scaleFactor(decimals: Int): Long {
            var factor = 1L
            repeat(decimals) { factor *= 10L }
            return factor
        }

        /** Crea un monto a partir de unidades mínimas (centavos, satoshis). */
        fun ofMinorUnits(
            units: Long,
            currency: Currency,
        ): Money = Money(units, currency)

        fun zero(currency: Currency): Money = Money(0L, currency)

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
            val major = unsigned.substringBefore(".").toLong()
            val minorText = unsigned.substringAfter(".", "").padEnd(decimals, '0')
            val minor = if (minorText.isEmpty()) 0L else minorText.toLong()
            val total = ExactMath.addExact(ExactMath.multiplyExact(major, scaleFactor(decimals)), minor)
            return Money(if (isNegative) -total else total, currency)
        }
    }
}
