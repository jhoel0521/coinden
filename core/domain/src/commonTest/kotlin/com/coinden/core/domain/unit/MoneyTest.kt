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
        val m1 = Money.parseCanonical("35.50", Currency.BOB)
        val m2 = Money.parseCanonical("14.50", Currency.BOB)

        val sum = m1 + m2
        assertEquals(5000L, sum.amountInMinorUnits)
        assertEquals("50.00", sum.toNumericString())
        assertEquals("50.00 BOB", sum.toString())

        val diff = sum - m2
        assertEquals(m1, diff)
    }

    @Test
    fun `prohibe operaciones aritmeticas entre divisas distintas`() {
        val bob = Money.parseCanonical("35.50", Currency.BOB)
        val usd = Money.parseCanonical("10.00", Currency.USD)

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
        val t1 = Money.parseCanonical("50", vale)
        val t2 = Money.parseCanonical("25", vale)

        val total = t1 + t2
        assertEquals(75L, total.amountInMinorUnits)
        assertEquals("75", total.toNumericString())
        assertEquals("75 VALE", total.toString())
    }

    @Test
    fun `soporta Bitcoin con precision de 8 decimales en Satoshis sin coma flotante`() {
        // 50,000 Satoshis = 0.00050000 BTC
        val sats = Money(amountInMinorUnits = 50_000L, currency = Currency.BTC)
        assertEquals("0.00050000", sats.toNumericString())
        assertEquals("0.00050000 BTC", sats.toString())

        // 1 BTC entero = 100,000,000 Satoshis
        val oneBtc = Money.fromMajorUnits(majorUnits = 1L, currency = Currency.BTC)
        assertEquals(100_000_000L, oneBtc.amountInMinorUnits)

        val remaining = oneBtc - sats
        assertEquals(99_950_000L, remaining.amountInMinorUnits)
        assertEquals("0.99950000", remaining.toNumericString())
    }

    @Test
    fun `multiplica por factores enteros e invierte el signo correctamente`() {
        val base = Money.parseCanonical("12.50", Currency.USD)
        val tripled = base * 3

        assertEquals(3750L, tripled.amountInMinorUnits)
        assertEquals("37.50 USD", tripled.toString())

        val negative = -base
        assertEquals(-1250L, negative.amountInMinorUnits)
        assertTrue(negative.isNegative)
        assertEquals("-12.50 USD", negative.toString())
    }

    @Test
    fun `compara montos de la misma divisa`() {
        val smaller = Money.parseCanonical("10.00", Currency.USD)
        val larger = Money.parseCanonical("20.00", Currency.USD)

        assertTrue(smaller < larger)
        assertTrue(larger > smaller)
    }

    @Test
    fun `lee la forma canonica con punto decimal y signo opcional`() {
        assertEquals(Money(3550L, Currency.BOB), Money.parseCanonical("35.50", Currency.BOB))
        assertEquals(Money(3550L, Currency.BOB), Money.parseCanonical("35.5", Currency.BOB))
        assertEquals(Money(-1250L, Currency.USD), Money.parseCanonical("-12.5", Currency.USD))
        assertEquals(Money(1200L, Currency.USD), Money.parseCanonical("12", Currency.USD))
        assertEquals(Money(50_000L, Currency.BTC), Money.parseCanonical("0.0005", Currency.BTC))
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
        assertEquals("1188.00 BOB", Money(118_800L, Currency.BOB).toString())
        assertEquals("1234567.89 USD", Money(123_456_789L, Currency.USD).toString())
    }
}
