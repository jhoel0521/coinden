package com.coinden.core.domain.unit

import com.coinden.core.domain.model.ExactMath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExactMathTest {
    @Test
    fun `suma exacta y desbordamiento explicito`() {
        assertEquals(30L, ExactMath.addExact(10L, 20L))
        assertEquals(-5L, ExactMath.addExact(10L, -15L))
        assertFailsWith<ArithmeticException> { ExactMath.addExact(Long.MAX_VALUE, 1L) }
        assertFailsWith<ArithmeticException> { ExactMath.addExact(Long.MIN_VALUE, -1L) }
    }

    @Test
    fun `multiplicacion exacta y desbordamiento explicito`() {
        assertEquals(1_099_999_999_995_000_000L, ExactMath.multiplyExact(15_000_000L, 73_333_333_333L))
        assertEquals(-60L, ExactMath.multiplyExact(-6L, 10L))
        assertEquals(0L, ExactMath.multiplyExact(0L, Long.MAX_VALUE))
        assertFailsWith<ArithmeticException> { ExactMath.multiplyExact(Long.MAX_VALUE, 2L) }
        assertFailsWith<ArithmeticException> { ExactMath.multiplyExact(4_000_000_000L, 4_000_000_000L) }
        assertFailsWith<ArithmeticException> { ExactMath.multiplyExact(Long.MIN_VALUE, -1L) }
    }

    @Test
    fun `maximo comun divisor`() {
        assertEquals(100L, ExactMath.gcd(1_000L, 100L))
        assertEquals(200L, ExactMath.gcd(59_400L, 5_000L))
        assertEquals(7L, ExactMath.gcd(-21L, 14L))
        assertEquals(5L, ExactMath.gcd(0L, 5L))
    }

    @Test
    fun `division half-even`() {
        assertEquals(3L, ExactMath.divideHalfEven(9L, 3L)) // exacta
        assertEquals(1L, ExactMath.divideHalfEven(5L, 4L)) // 1.25 -> 1
        assertEquals(2L, ExactMath.divideHalfEven(7L, 4L)) // 1.75 -> 2
        assertEquals(2L, ExactMath.divideHalfEven(5L, 2L)) // 2.5 -> 2 (par)
        assertEquals(4L, ExactMath.divideHalfEven(7L, 2L)) // 3.5 -> 4 (par)
        assertEquals(-2L, ExactMath.divideHalfEven(-5L, 2L)) // -2.5 -> -2 (par)
        assertEquals(-4L, ExactMath.divideHalfEven(-7L, 2L)) // -3.5 -> -4 (par)
        assertEquals(-2L, ExactMath.divideHalfEven(7L, -4L)) // -1.75 -> -2
        assertFailsWith<ArithmeticException> { ExactMath.divideHalfEven(1L, 0L) }
    }
}
