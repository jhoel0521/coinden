package com.coinden.app.format

import com.coinden.app.testLocale
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatterTest {
    private val bolivia = testLocale("es", "BO")
    private val argentina = testLocale("es", "AR")
    private val usa = testLocale("en", "US")

    @Test
    fun `coma decimal y punto de miles en un locale con coma`() {
        val money = Money.parseCanonical("1234.50", Currency.BOB)

        assertEquals("1.234,50", MoneyFormatter.formatAmount(money, argentina))
        assertEquals("1.234,50 BOB", MoneyFormatter.format(money, argentina))
    }

    @Test
    fun `punto decimal y coma de miles en un locale con punto`() {
        val money = Money.parseCanonical("1234567.05", Currency.USD)

        assertEquals("1,234,567.05", MoneyFormatter.formatAmount(money, usa))
        assertEquals("1,234,567.05", MoneyFormatter.formatAmount(money, bolivia))
    }

    @Test
    fun `los digitos no cambian y se respeta el signo`() {
        val money = Money.parseCanonical("-987.60", Currency.BOB)

        assertEquals("-987,60", MoneyFormatter.formatAmount(money, argentina))
        assertEquals("3.00", MoneyFormatter.formatAmount(Money.parseCanonical("3", Currency.BOB), bolivia))
    }

    @Test
    fun `moneda sin decimales no muestra separador decimal`() {
        val money = Money.parseCanonical("1500000", Currency.CLP)

        assertEquals("1.500.000", MoneyFormatter.formatAmount(money, argentina))
        assertEquals("1,500,000 CLP", MoneyFormatter.format(money, usa))
    }

    @Test
    fun `moneda con 8 decimales conserva todos los digitos`() {
        val money = Money.parseCanonical("1234.00000001", Currency.BTC)

        assertEquals("1,234.00000001", MoneyFormatter.formatAmount(money, usa))
        assertEquals("1.234,00000001 BTC", MoneyFormatter.format(money, argentina))
    }

    @Test
    fun `moneda personalizada usa el simbolo que escribio el usuario`() {
        val vale = Currency(code = "VALE", symbol = "vl", decimals = 0, isCustom = true)

        assertEquals("25 vl", MoneyFormatter.format(Money.parseCanonical("25", vale), usa))
    }
}
