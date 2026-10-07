package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun `suma y resta montos de la misma divisa con precision exacta en enteros`() {
        val m1 = Money.fromFormatted("35.50", Currency.BOB)
        val m2 = Money.fromFormatted("14.50", Currency.BOB)

        val sum = m1 + m2
        assertEquals(5000L, sum.amountInMinorUnits)
        assertEquals("50.00", sum.toNumericString())
        assertEquals("Bs 50.00", sum.formatted())

        val diff = sum - m2
        assertEquals(m1, diff)
    }

    @Test
    fun `prohibe operaciones aritmeticas entre divisas distintas`() {
        val bob = Money.fromFormatted("35.50", Currency.BOB)
        val usd = Money.fromFormatted("10.00", Currency.USD)

        assertFailsWith<IllegalArgumentException> {
            bob + usd
        }
        assertFailsWith<IllegalArgumentException> {
            bob - usd
        }
    }

    @Test
    fun `maneja correctamente monedas sin centavos como pesos chilenos o tokens enteros`() {
        val vale = Currency.custom(code = "VALE", symbol = "V", decimals = 0, name = "Token")
        val t1 = Money.fromFormatted("50", vale)
        val t2 = Money.fromFormatted("25", vale)

        val total = t1 + t2
        assertEquals(75L, total.amountInMinorUnits)
        assertEquals("75", total.toNumericString())
        assertEquals("V 75", total.formatted())
    }

    @Test
    fun `soporta Bitcoin con precision de 8 decimales en Satoshis sin coma flotante`() {
        // 50,000 Satoshis = 0.00050000 BTC
        val sats = Money(amountInMinorUnits = 50_000L, currency = Currency.BTC)
        assertEquals("0.00050000", sats.toNumericString())
        assertEquals("₿ 0.00050000", sats.formatted())

        // 1 BTC entero = 100,000,000 Satoshis
        val oneBtc = Money.fromMajorUnits(majorUnits = 1L, currency = Currency.BTC)
        assertEquals(100_000_000L, oneBtc.amountInMinorUnits)

        val remaining = oneBtc - sats
        assertEquals(99_950_000L, remaining.amountInMinorUnits)
        assertEquals("0.99950000", remaining.toNumericString())
    }

    @Test
    fun `multiplica por factores enteros e invierte el signo correctamente`() {
        val base = Money.fromFormatted("12.50", Currency.USD)
        val tripled = base * 3

        assertEquals(3750L, tripled.amountInMinorUnits)
        assertEquals("$ 37.50", tripled.formatted())

        val negative = -base
        assertEquals(-1250L, negative.amountInMinorUnits)
        assertTrue(negative.isNegative)
        assertEquals("-$ 12.50", negative.formatted())
    }

    @Test
    fun `compara montos de la misma divisa`() {
        val smaller = Money.fromFormatted("10.00", Currency.USD)
        val larger = Money.fromFormatted("20.00", Currency.USD)

        assertTrue(smaller < larger)
        assertTrue(larger > smaller)
    }

    @Test
    fun `admite tanto punto como coma en el parseo`() {
        val withDot = Money.fromFormatted("35.50", Currency.BOB)
        val withComma = Money.fromFormatted("35,50", Currency.BOB)

        assertEquals(withDot, withComma)
    }
}
