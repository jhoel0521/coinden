package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MoneyTest {
    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private fun usd(canonical: String) = Money.parseCanonical(canonical, Currency.USD)

    @Test
    fun `suma y resta montos de la misma divisa con precision exacta`() {
        val sum = bs("35.50") + bs("14.50")
        assertEquals(bs("50.00"), sum)
        assertEquals("50.00 BOB", sum.toString())
        assertEquals(bs("35.50"), sum - bs("14.50"))
    }

    @Test
    fun `0_10 mas 0_20 es exactamente 0_30`() {
        assertEquals(bs("0.30"), bs("0.10") + bs("0.20"))
    }

    @Test
    fun `prohibe operaciones aritmeticas entre divisas distintas`() {
        assertFailsWith<IllegalArgumentException> { bs("35.50") + usd("10.00") }
        assertFailsWith<IllegalArgumentException> { bs("35.50") - usd("10.00") }
        assertFailsWith<IllegalArgumentException> { bs("35.50") < usd("10.00") }
    }

    @Test
    fun `el desbordamiento falla explicitamente`() {
        val max = Money.ofMinorUnits(Long.MAX_VALUE, Currency.USD)
        val min = Money.ofMinorUnits(Long.MIN_VALUE, Currency.USD)
        assertFailsWith<ArithmeticException> { max + usd("0.01") }
        assertFailsWith<ArithmeticException> { min - usd("0.01") }
        assertFailsWith<ArithmeticException> { -min }
    }

    @Test
    fun `igualdad y hashCode por moneda y monto`() {
        assertEquals(bs("10.00"), Money.ofMinorUnits(1_000L, Currency.BOB))
        assertEquals(bs("10.00").hashCode(), Money.ofMinorUnits(1_000L, Currency.BOB).hashCode())
        assertNotEquals(bs("10.00"), usd("10.00"))
        assertEquals(setOf(bs("1.00")), setOf(Money.ofMinorUnits(100L, Currency.BOB)))
    }

    @Test
    fun `monedas sin centavos como tokens enteros`() {
        val vale = Currency.custom(code = "VALE", symbol = "V", decimals = 0, name = "Token")
        val total = Money.parseCanonical("50", vale) + Money.parseCanonical("25", vale)
        assertEquals(Money.ofMinorUnits(75L, vale), total)
        assertEquals("75", total.toNumericString())
        assertEquals("75 VALE", total.toString())
    }

    @Test
    fun `Bitcoin con precision de 8 decimales en satoshis`() {
        val sats = Money.ofMinorUnits(50_000L, Currency.BTC)
        assertEquals("0.00050000", sats.toNumericString())
        assertEquals("0.00050000 BTC", sats.toString())

        val remaining = Money.parseCanonical("1", Currency.BTC) - sats
        assertEquals("0.99950000 BTC", remaining.toString())
    }

    @Test
    fun `signo, negacion y cero`() {
        val negative = -usd("12.50")
        assertTrue(negative.isNegative)
        assertEquals("-12.50 USD", negative.toString())
        assertTrue(Money.zero(Currency.USD).isZero)
        assertTrue(usd("0.01").isPositive)
        assertFalse(usd("0.01").isNegative)
    }

    @Test
    fun `compara montos de la misma divisa`() {
        assertTrue(usd("10.00") < usd("20.00"))
        assertTrue(usd("20.00") > usd("10.00"))
    }

    @Test
    fun `lee la forma canonica con punto decimal y signo opcional`() {
        assertEquals(Money.ofMinorUnits(3550L, Currency.BOB), bs("35.50"))
        assertEquals(Money.ofMinorUnits(3550L, Currency.BOB), bs("35.5"))
        assertEquals(Money.ofMinorUnits(-1250L, Currency.USD), usd("-12.5"))
        assertEquals(Money.ofMinorUnits(1200L, Currency.USD), usd("12"))
        assertEquals(Money.ofMinorUnits(50_000L, Currency.BTC), Money.parseCanonical("0.0005", Currency.BTC))
    }

    @Test
    fun `rechaza formatos regionales y decimales de mas`() {
        listOf("35,50", "1.234,56", "1,234.56", "Bs 35.50", "", " ", "12.", ".5", "--1", "12.505").forEach { raw ->
            assertFailsWith<IllegalArgumentException>("deberia rechazar '$raw'") {
                Money.parseCanonical(raw, Currency.USD)
            }
        }
        assertFailsWith<IllegalArgumentException> { Money.parseCanonical("1.5", Currency.CLP) }
    }

    @Test
    fun `la representacion canonica no tiene simbolo ni separadores de miles`() {
        assertEquals("1188.00 BOB", Money.ofMinorUnits(118_800L, Currency.BOB).toString())
        assertEquals("1234567.89 USD", Money.ofMinorUnits(123_456_789L, Currency.USD).toString())
    }
}
