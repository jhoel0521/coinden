package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.interpret.AmountReader
import com.coinden.core.domain.model.interpret.MoneyScanner
import com.coinden.core.domain.model.interpret.ReadAmount
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.support.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AmountReaderTest {
    private val bo = testLocale("es", "BO")
    private val ar = testLocale("es", "AR")
    private val us = testLocale("en", "US")
    private val br = testLocale("pt", "BR")

    @Test
    fun `decimales con coma o punto`() {
        assertEquals(ReadAmount("45.50", ambiguous = false), AmountReader.read("45,50", bo))
        assertEquals(ReadAmount("45.50", ambiguous = false), AmountReader.read("45.50", bo))
        assertEquals(ReadAmount("3", ambiguous = false), AmountReader.read("3", bo))
        assertEquals(ReadAmount("0.001", ambiguous = false), AmountReader.read("0.001", bo))
        assertEquals(ReadAmount("11.88", ambiguous = false), AmountReader.read("11.88", bo))
    }

    @Test
    fun `miles y decimales segun el locale`() {
        assertEquals(ReadAmount("12500.00", ambiguous = false), AmountReader.read("12.500,00", ar))
        assertEquals(ReadAmount("12500", ambiguous = false), AmountReader.read("12.500", ar))
        assertEquals(ReadAmount("1234.56", ambiguous = false), AmountReader.read("1,234.56", us))
        assertEquals(ReadAmount("1234567", ambiguous = false), AmountReader.read("1.234.567", br))
    }

    @Test
    fun `separador de grupo igual al decimal del locale es ambiguo`() {
        assertEquals(ReadAmount("1234", ambiguous = true), AmountReader.read("1.234", bo))
        assertEquals(ReadAmount("1234", ambiguous = true), AmountReader.read("1,234", br))
    }

    @Test
    fun `los digitos nunca cambian`() {
        listOf("118.71" to bo, "45,90" to br, "8.000,00" to ar, "25.00" to us).forEach { (raw, locale) ->
            val read = AmountReader.read(raw, locale)!!
            assertEquals(raw.filter { it.isDigit() }.trimStart('0'), read.canonical.filter { it.isDigit() }.trimStart('0'))
        }
    }

    @Test
    fun `texto que no es monto`() {
        assertNull(AmountReader.read("abc", bo))
        assertNull(AmountReader.read("", bo))
    }

    @Test
    fun `monedas por simbolo, codigo o palabra`() {
        fun first(
            line: String,
            locale: UserLocale,
        ) = MoneyScanner.scan(line, locale, mapOf("dolares" to "USD", "reais" to "BRL", "pesos" to null)).first()

        assertEquals(Currency.BOB, first("Bs 36", bo).currency)
        assertEquals("25.00", first("Bs25.00", bo).amount.canonical)
        val debit = first("-Bs118.71", bo)
        assertEquals("118.71", debit.amount.canonical)
        assertTrue(debit.negative)
        assertEquals(Currency.of("BRL"), first("Valor R$ 45,90", br).currency)
        assertEquals(Currency.USD, first("12 usd", bo).currency)
        assertEquals(Currency.USD, first("50 dolares", bo).currency)
        assertEquals(Currency.of("BRL"), first("15 reais", br).currency)
        assertEquals(Currency.BOB, first("3 bs", bo).currency)
        assertNull(first("micro 3", bo).currency)
    }

    @Test
    fun `el signo pesos se resuelve con el pais y queda inferido`() {
        val ars = MoneyScanner.scan("Monto $ 12.500,00", ar, emptyMap()).single()
        assertEquals(Currency.ARS, ars.currency)
        assertEquals("12500.00", ars.amount.canonical)
        assertTrue(ars.currencyInferred)

        val usd = MoneyScanner.scan("Amount \$1,234.56", us, emptyMap()).single()
        assertEquals(Currency.USD, usd.currency)
        assertTrue(usd.currencyInferred)

        val pesos = MoneyScanner.scan("pague 500 pesos", ar, mapOf("pesos" to null)).single()
        assertEquals(Currency.ARS, pesos.currency)
        assertTrue(pesos.currencyInferred)
    }
}
