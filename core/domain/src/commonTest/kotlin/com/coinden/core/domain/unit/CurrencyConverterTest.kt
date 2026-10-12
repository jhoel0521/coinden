package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.CurrencyConverter
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CurrencyConverterTest {
    private val bob = Currency.of("BOB", 2)
    private val ars = Currency.of("ARS", 2)
    private val eur = Currency.EUR
    private val usd = Currency.USD
    private val btc = Currency.BTC

    private val converter =
        CurrencyConverter(
            listOf(
                ExchangeRate.parse(usd, bob, "11.88"),
                ExchangeRate.parse(usd, ars, "1420"),
                ExchangeRate.parse(usd, eur, "0.92"),
                ExchangeRate.parse(btc, usd, "62200"),
            ),
        )

    @Test
    fun `convierte directamente entre misma divisa sin alterar monto`() {
        val original = Money.parseCanonical("50.0", usd)
        val converted = converter.convert(original, usd)
        assertEquals(original, converted)
    }

    @Test
    fun `convierte USD a moneda local usando la tasa directa`() {
        val hundredUsd = Money.parseCanonical("100.0", usd)
        val inBob = converter.convert(hundredUsd, bob)

        // 100 * 11.88 = 1188.00 BOB
        assertEquals(Money.ofMinorUnits(118_800L, Currency.BOB), inBob)
        assertEquals("1188.00 BOB", inBob.toString())
    }

    @Test
    fun `convierte Bitcoin a USD usando el precio spot`() {
        val btcAmount = Money.parseCanonical("0.15", btc)
        val inUsd = converter.convert(btcAmount, usd)

        // 0.15 * 62,200 = 9330.00 USD
        assertEquals(Money.ofMinorUnits(933_000L, Currency.USD), inUsd)
        assertEquals("9330.00 USD", inUsd.toString())
    }

    @Test
    fun `aplica la formula transitiva universal BTC hacia BOB transitando por USD`() {
        val btcAmount = Money.parseCanonical("0.15", btc)
        val inBob = converter.convert(btcAmount, bob)

        // 0.15 BTC -> 9,330 USD -> (9,330 * 11.88) = 110,840.40 BOB
        assertEquals(Money.ofMinorUnits(11_084_040L, Currency.BOB), inBob)
        assertEquals("110840.40 BOB", inBob.toString())
    }

    @Test
    fun `consolida cartera multiactivo sumando correctamente montos unificados y desglosados`() {
        // 0.15 BTC equivale a 110,840.40 BOB
        // 100 USD equivale a 1,188.00 BOB
        val holdings =
            listOf(
                Money.parseCanonical("0.15", btc),
                Money.parseCanonical("100.0", usd),
            )

        val consolidated = converter.consolidate(holdings, bob)

        // Total: 110,840.40 + 1,188.00 = 112,028.40 BOB
        assertEquals(Money.ofMinorUnits(11_202_840L, Currency.BOB), consolidated.totalUnifiedAmount)
        assertEquals("112028.40 BOB", consolidated.totalUnifiedAmount.toString())
        assertEquals(2, consolidated.holdings.size)
    }

    @Test
    fun `el consolidado es exactamente la suma de su desglose y cada posicion redondea una vez`() {
        val holdings =
            listOf(
                Money.parseCanonical("0.33333333", btc),
                Money.parseCanonical("0.07", usd),
                Money.parseCanonical("19.99", eur),
                Money.parseCanonical("1", ars),
            )

        val consolidated = converter.consolidate(holdings, bob)

        val sumOfItems = consolidated.holdings.map { it.convertedAmount }.reduce { acc, m -> acc + m }
        assertEquals(sumOfItems, consolidated.totalUnifiedAmount)
        consolidated.holdings.forEach { item ->
            assertEquals(item.appliedRate?.let { item.originalAmount * it } ?: item.originalAmount, item.convertedAmount)
        }
        // la tasa aplicada a BTC es la composicion exacta BTC -> USD -> BOB
        assertEquals(
            ExchangeRate.parse(btc, usd, "62200") * ExchangeRate.parse(usd, bob, "11.88"),
            consolidated.holdings[0].appliedRate,
        )
    }

    @Test
    fun `falla si no existe tasa registrada para la divisa`() {
        val mysteryCurrency = Currency.of("XYZ", 2)
        val money = Money.parseCanonical("10.0", mysteryCurrency)

        assertFailsWith<IllegalArgumentException> {
            converter.convert(money, usd)
        }
    }
}
