package com.coinden.core.domain.model

/**
 * Tasa de cambio exacta: "X de [from] equivale a Y de [to]".
 *
 * Internamente es una fracción reducida: cuántas unidades mínimas de [to] vale una unidad
 * mínima de [from] (`numerator / denominator`). Nunca usa coma flotante:
 * - `money * rate` convierte con un único redondeo half-even (ver [Money.times]).
 * - `rate1 * rate2` compone tasas sin redondear (BTC->USD por USD->BOB = BTC->BOB).
 * - [inverse] da la tasa en sentido contrario.
 */
class ExchangeRate private constructor(
    val from: Currency,
    val to: Currency,
    internal val numerator: Long,
    internal val denominator: Long,
) {
    fun inverse(): ExchangeRate = create(to, from, denominator, numerator)

    /** Compone esta tasa ([from] -> [to]) con [next] ([to] -> otra), sin redondeo. */
    operator fun times(next: ExchangeRate): ExchangeRate {
        require(to.isSameAs(next.from) && to.decimals == next.from.decimals) {
            "No se puede componer ${from.code}->${to.code} con ${next.from.code}->${next.to.code}"
        }
        val g1 = ExactMath.gcd(numerator, next.denominator)
        val g2 = ExactMath.gcd(next.numerator, denominator)
        return create(
            from,
            next.to,
            ExactMath.multiplyExact(numerator / g1, next.numerator / g2),
            ExactMath.multiplyExact(denominator / g2, next.denominator / g1),
        )
    }

    /** Cuánto de [to] equivale a [base]; con base = 1 unidad da la cotización "1 USD = 11.88 BOB". */
    fun quoteFor(base: Money): Money = base * this

    val isIdentity: Boolean get() = from == to

    override fun equals(other: Any?): Boolean =
        other is ExchangeRate &&
            other.from == from &&
            other.to == to &&
            other.numerator == numerator &&
            other.denominator == denominator

    override fun hashCode(): Int = ((from.hashCode() * 31 + to.hashCode()) * 31 + numerator.hashCode()) * 31 + denominator.hashCode()

    /** Forma canónica: "1 USD = 11.88 BOB" (cotización de una unidad mayor, redondeada solo para mostrar). */
    override fun toString(): String = "1 ${from.code} = ${quoteFor(Money.parseCanonical("1", from))}"

    companion object {
        /** "[base] equivale a [quote]", por ejemplo 50.00 USD = 594.00 BOB. */
        fun of(
            base: Money,
            quote: Money,
        ): ExchangeRate {
            require(!base.currency.isSameAs(quote.currency)) { "Una tasa necesita dos monedas distintas" }
            require(base.isPositive && quote.isPositive) { "Los montos de una tasa deben ser positivos" }
            return create(base.currency, quote.currency, quote.minorUnits, base.minorUnits)
        }

        /** Tasa 1:1 de una moneda consigo misma (por ejemplo, un usuario cuya moneda local es USD). */
        fun identity(currency: Currency): ExchangeRate = ExchangeRate(currency, currency, 1L, 1L)

        /**
         * Lee "[perUnit] de [to] por una unidad de [from]" desde un decimal canónico sin límite
         * de decimales ("11.88", "1420.50", "0.923456"), sin pasar por coma flotante.
         */
        fun parse(
            from: Currency,
            to: Currency,
            perUnit: String,
        ): ExchangeRate {
            require(Regex("""^\d+(\.\d+)?$""").matches(perUnit)) { "Tasa no canónica: '$perUnit'" }
            require(!from.isSameAs(to)) { "Una tasa necesita dos monedas distintas" }
            val fraction = perUnit.substringAfter(".", "")
            val digits = (perUnit.substringBefore(".") + fraction).trimStart('0').ifEmpty { "0" }.toLong()
            require(digits > 0L) { "La tasa debe ser positiva: '$perUnit'" }
            val numerator = ExactMath.multiplyExact(digits, Money.scaleFactor(to.decimals))
            val denominator = ExactMath.multiplyExact(pow10(fraction.length), Money.scaleFactor(from.decimals))
            return create(from, to, numerator, denominator)
        }

        private fun pow10(exponent: Int): Long {
            var result = 1L
            repeat(exponent) { result = ExactMath.multiplyExact(result, 10L) }
            return result
        }

        private fun create(
            from: Currency,
            to: Currency,
            numerator: Long,
            denominator: Long,
        ): ExchangeRate {
            val g = ExactMath.gcd(numerator, denominator)
            return ExchangeRate(from, to, numerator / g, denominator / g)
        }
    }
}
