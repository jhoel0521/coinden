package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExchangeRateTest {
    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private fun usd(canonical: String) = Money.parseCanonical(canonical, Currency.USD)

    private fun eur(canonical: String) = Money.parseCanonical(canonical, Currency.EUR)

    private fun btc(canonical: String) = Money.parseCanonical(canonical, Currency.BTC)

    private val usdToBs = ExchangeRate.of(usd("1.00"), bs("11.88"))

    @Test
    fun `conversion con la tasa del paralelo`() {
        assertEquals(bs("594.00"), usd("50.00") * usdToBs)
    }

    @Test
    fun `cuanto tengo en todas mis monedas - bs mas usd por tasa`() {
        val total = bs("100.00") + (usd("50.00") * usdToBs)
        assertEquals(bs("694.00"), total)
        // sin tasa no se puede: monedas distintas
        assertFailsWith<IllegalArgumentException> { bs("100.00") + usd("50.00") }
        // la tasa debe corresponder a la moneda del monto
        assertFailsWith<IllegalArgumentException> { bs("100.00") * usdToBs }
    }

    @Test
    fun `conversion encadenada BTC a USD a BOB con un solo redondeo`() {
        val btcToUsd = ExchangeRate.of(btc("1"), usd("62200.00"))
        val btcToBs = btcToUsd * usdToBs
        assertEquals(bs("110840.40"), btc("0.15") * btcToBs)
        assertEquals(Currency.BTC, btcToBs.from)
        assertEquals(Currency.BOB, btcToBs.to)
    }

    @Test
    fun `half-even al centavo`() {
        // 0.05 USD * 0.25 = 0.0125 EUR -> 0.01 (empate al par)
        assertEquals(eur("0.01"), usd("0.05") * ExchangeRate.of(usd("1.00"), eur("0.25")))
        // 0.07 USD * 0.25 = 0.0175 EUR -> 0.02 (empate al par)
        assertEquals(eur("0.02"), usd("0.07") * ExchangeRate.of(usd("1.00"), eur("0.25")))
    }

    @Test
    fun `tasa definida por una operacion real`() {
        val venta = ExchangeRate.of(usd("50.00"), bs("594.00"))
        assertEquals(usdToBs, venta)
        assertEquals(bs("11.88"), venta.quoteFor(usd("1.00")))
    }

    @Test
    fun `tasa inversa`() {
        val usdToBsManual = ExchangeRate.of(usd("1.00"), bs("11.20"))
        assertEquals(usd("100.00"), bs("1120.00") * usdToBsManual.inverse())
        assertEquals(usdToBsManual, usdToBsManual.inverse().inverse())
    }

    @Test
    fun `parse lee decimales sin coma flotante`() {
        val ars = Currency.ARS
        val rate = ExchangeRate.parse(Currency.USD, ars, "1420.50")
        assertEquals(Money.parseCanonical("1420.50", ars), rate.quoteFor(usd("1.00")))
        assertEquals(ExchangeRate.of(usd("2.00"), Money.parseCanonical("2841.00", ars)), rate)

        // mas decimales que la moneda destino: 0.923456 EUR por USD
        val precise = ExchangeRate.parse(Currency.USD, Currency.EUR, "0.923456")
        assertEquals(eur("923.46"), usd("1000.00") * precise)
    }

    @Test
    fun `parse rechaza textos no canonicos, cero y monedas iguales`() {
        listOf("", "11,88", "-11.88", "abc", "11.", ".5", "0", "0.000").forEach { raw ->
            assertFailsWith<IllegalArgumentException>("deberia rechazar '$raw'") {
                ExchangeRate.parse(Currency.USD, Currency.BOB, raw)
            }
        }
        assertFailsWith<IllegalArgumentException> { ExchangeRate.parse(Currency.USD, Currency.USD, "1") }
    }

    @Test
    fun `of rechaza monedas iguales y montos no positivos`() {
        assertFailsWith<IllegalArgumentException> { ExchangeRate.of(usd("1.00"), usd("1.00")) }
        assertFailsWith<IllegalArgumentException> { ExchangeRate.of(usd("0.00"), bs("11.88")) }
        assertFailsWith<IllegalArgumentException> { ExchangeRate.of(usd("1.00"), bs("-11.88")) }
    }

    @Test
    fun `composicion incompatible rechazada`() {
        val eurToBs = ExchangeRate.of(eur("1.00"), bs("12.90"))
        assertFailsWith<IllegalArgumentException> { usdToBs * eurToBs }
    }

    @Test
    fun `identidad no cambia el monto`() {
        val identity = ExchangeRate.identity(Currency.USD)
        assertTrue(identity.isIdentity)
        assertEquals(usd("9330.00"), usd("9330.00") * identity)
    }

    @Test
    fun `desbordamiento explicito al convertir`() {
        val huge = Money.ofMinorUnits(Long.MAX_VALUE / 10, Currency.USD)
        assertFailsWith<ArithmeticException> { huge * usdToBs }
    }

    @Test
    fun `representacion canonica de la tasa`() {
        assertEquals("1 USD = 11.88 BOB", usdToBs.toString())
    }

    // --- Forma canónica para persistir: fracción exacta de unidades mínimas ---

    @Test
    fun `la forma canonica es la fraccion reducida de unidades minimas`() {
        // 11.00 USD -> 112.00 BOB: 11200 / 1100 centavos = 112/11
        assertEquals("112/11", ExchangeRate.of(usd("11"), bs("112")).toCanonical())
        assertEquals("297/25", ExchangeRate.parse(Currency.USD, Currency.BOB, "11.88").toCanonical())
        assertEquals("1/1", ExchangeRate.identity(Currency.BOB).toCanonical())
    }

    @Test
    fun `ida y vuelta exacta de tasas decimales, fraccionarias, inversas e identidad`() {
        val vale = Currency(code = "VALE", decimals = 0, isCustom = true)
        val rates =
            listOf(
                ExchangeRate.parse(Currency.USD, Currency.BOB, "11.88"),
                ExchangeRate.of(usd("11"), bs("112")),
                // 1 USD = 1/3 de VALE: no tiene decimal exacto
                ExchangeRate.of(usd("3"), Money.parseCanonical("1", vale)),
                ExchangeRate.of(usd("11"), bs("112")).inverse(),
                ExchangeRate.of(btc("1"), usd("62345.67")),
                ExchangeRate.identity(Currency.USD),
            )
        rates.forEach { rate ->
            assertEquals(rate, ExchangeRate.parseCanonical(rate.toCanonical(), rate.from, rate.to))
        }
    }

    @Test
    fun `la forma canonica se reduce al leerla`() {
        assertEquals(ExchangeRate.of(usd("11"), bs("112")), ExchangeRate.parseCanonical("224/22", Currency.USD, Currency.BOB))
    }

    @Test
    fun `rechaza formas canonicas invalidas`() {
        listOf("", "11.88", "112", "/11", "112/", "-112/11", "112/-11", "0/11", "112/0", "a/b", "1 12/11").forEach { text ->
            assertFailsWith<IllegalArgumentException>(text) { ExchangeRate.parseCanonical(text, Currency.USD, Currency.BOB) }
        }
        // una moneda consigo misma solo admite la identidad
        assertFailsWith<IllegalArgumentException> { ExchangeRate.parseCanonical("112/11", Currency.BOB, Currency.BOB) }
        assertEquals(ExchangeRate.identity(Currency.BOB), ExchangeRate.parseCanonical("1/1", Currency.BOB, Currency.BOB))
    }
}
