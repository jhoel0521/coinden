package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.CurrencyConverter
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
            usdToLocalRates =
                mapOf(
                    "USD" to 1.0,
                    "BOB" to 11.88,
                    "ARS" to 1420.0,
                    "EUR" to 0.92,
                ),
            cryptoToUsdRates =
                mapOf(
                    "BTC" to 62200.0,
                ),
        )

    @Test
    fun `convierte directamente entre misma divisa sin alterar monto`() {
        val original = Money.fromDecimal(50.0, usd)
        val converted = converter.convert(original, usd)
        assertEquals(original, converted)
    }

    @Test
    fun `convierte USD a moneda local usando la tasa directa`() {
        val hundredUsd = Money.fromDecimal(100.0, usd)
        val inBob = converter.convert(hundredUsd, bob)

        // 100 * 11.88 = 1188.00 BOB
        assertEquals(118800L, inBob.amountInMinorUnits)
        assertEquals("1188.00 BOB", inBob.toFormattedString())
    }

    @Test
    fun `convierte Bitcoin a USD usando el precio spot`() {
        val btcAmount = Money.fromDecimal(0.15, btc)
        val inUsd = converter.convert(btcAmount, usd)

        // 0.15 * 62,200 = 9330.00 USD
        assertEquals(933000L, inUsd.amountInMinorUnits)
        assertEquals("9330.00 USD", inUsd.toFormattedString())
    }

    @Test
    fun `aplica la formula transitiva universal BTC hacia BOB transitando por USD`() {
        val btcAmount = Money.fromDecimal(0.15, btc)
        val inBob = converter.convert(btcAmount, bob)

        // 0.15 BTC -> 9,330 USD -> (9,330 * 11.88) = 110,840.40 BOB
        assertEquals(11084040L, inBob.amountInMinorUnits)
        assertEquals("110840.40 BOB", inBob.toFormattedString())
    }

    @Test
    fun `consolida cartera multiactivo sumando correctamente montos unificados y desglosados`() {
        // 0.15 BTC equivale a 110,840.40 BOB
        // 100 USD equivale a 1,188.00 BOB
        val holdings =
            listOf(
                Money.fromDecimal(0.15, btc),
                Money.fromDecimal(100.0, usd),
            )

        val consolidated = converter.consolidate(holdings, bob)

        // Total: 110,840.40 + 1,188.00 = 112,028.40 BOB
        assertEquals(11202840L, consolidated.totalUnifiedAmount.amountInMinorUnits)
        assertEquals("112028.40 BOB", consolidated.totalUnifiedAmount.toFormattedString())
        assertEquals(2, consolidated.holdings.size)
    }

    @Test
    fun `falla si no existe tasa registrada para la divisa`() {
        val mysteryCurrency = Currency.of("XYZ", 2)
        val money = Money.fromDecimal(10.0, mysteryCurrency)

        assertFailsWith<IllegalArgumentException> {
            converter.convert(money, usd)
        }
    }
}
