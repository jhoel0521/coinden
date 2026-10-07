package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CurrencyTest {
    @Test
    fun `soporta divisas ancla y tradicionales con sus decimales exactos`() {
        assertEquals("USD", Currency.USD.code)
        assertEquals(2, Currency.USD.decimals)

        assertEquals("EUR", Currency.EUR.code)
        assertEquals(2, Currency.EUR.decimals)

        assertEquals("CNY", Currency.CNY.code)
        assertEquals(2, Currency.CNY.decimals)

        assertEquals("BTC", Currency.BTC.code)
        assertEquals("₿", Currency.BTC.symbol)
        assertEquals(8, Currency.BTC.decimals) // Satoshis

        assertEquals("CLP", Currency.CLP.code)
        assertEquals(0, Currency.CLP.decimals) // Pesos chilenos sin centavos
    }

    @Test
    fun `permite registrar monedas personalizadas con precision arbitraria de 0 a 8 decimales`() {
        // Token comunitario sin centavos
        val vale = Currency.custom(code = "VALE", symbol = "V", decimals = 0, name = "Token Vale")
        assertEquals("VALE", vale.code)
        assertEquals(0, vale.decimals)
        assertTrue(vale.isCustom)

        // Token con 4 decimales
        val moon = Currency.custom(code = "MOON", symbol = "M", decimals = 4, name = "Moon Token")
        assertEquals("MOON", moon.code)
        assertEquals(4, moon.decimals)
        assertTrue(moon.isCustom)
    }

    @Test
    fun `resuelve codigos de forma insensible a mayusculas`() {
        assertEquals(Currency.BOB, Currency.fromCode("bob"))
        assertEquals(Currency.USD, Currency.fromCode("Usd"))
        assertEquals(Currency.BTC, Currency.fromCode("btc"))
    }

    @Test
    fun `rechaza codigos no validos o vacios`() {
        assertFailsWith<IllegalArgumentException> {
            Currency(code = "US") // Menos de 3 letras
        }
        assertFailsWith<IllegalArgumentException> {
            Currency(code = "DEMASIADOLARGO") // Más de 6 letras
        }
        assertFailsWith<IllegalArgumentException> {
            Currency(code = "")
        }
    }

    @Test
    fun `rechaza decimales fuera del rango de 0 a 8`() {
        assertFailsWith<IllegalArgumentException> {
            Currency(code = "USD", decimals = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            Currency(code = "ETH", decimals = 9) // Superior al rango seguro de Long
        }
    }
}
