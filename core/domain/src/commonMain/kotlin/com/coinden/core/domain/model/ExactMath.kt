package com.coinden.core.domain.model

import kotlin.math.abs

/**
 * Aritmética entera verificada para dinero y tasas.
 *
 * Un desbordamiento lanza [ArithmeticException] en lugar de devolver un número equivocado.
 * Solo la usan [Money] y [ExchangeRate]: si algún día la representación pasa a enteros de
 * precisión arbitraria, el cambio queda contenido aquí.
 */
internal object ExactMath {
    fun addExact(
        a: Long,
        b: Long,
    ): Long {
        val result = a + b
        if (((a xor result) and (b xor result)) < 0) throw ArithmeticException("Desbordamiento al sumar $a + $b")
        return result
    }

    fun negateExact(a: Long): Long {
        if (a == Long.MIN_VALUE) throw ArithmeticException("Desbordamiento al negar $a")
        return -a
    }

    fun multiplyExact(
        a: Long,
        b: Long,
    ): Long {
        if (a == 0L || b == 0L) return 0L
        val result = a * b
        if ((a == -1L && b == Long.MIN_VALUE) || (b == -1L && a == Long.MIN_VALUE) || result / b != a) {
            throw ArithmeticException("Desbordamiento al multiplicar $a * $b")
        }
        return result
    }

    fun gcd(
        a: Long,
        b: Long,
    ): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val t = x % y
            x = y
            y = t
        }
        return x
    }

    /** División entera con redondeo half-even (empates al par). */
    fun divideHalfEven(
        numerator: Long,
        denominator: Long,
    ): Long {
        if (denominator == 0L) throw ArithmeticException("División por cero")
        val quotient = numerator / denominator
        val remainder = abs(numerator % denominator)
        if (remainder == 0L) return quotient
        val absDenominator = abs(denominator)
        val step = if ((numerator < 0) xor (denominator < 0)) -1L else 1L
        val distanceToNext = absDenominator - remainder
        return when {
            remainder > distanceToNext -> quotient + step
            remainder == distanceToNext && quotient % 2 != 0L -> quotient + step
            else -> quotient
        }
    }
}
