package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TransferTest {
    private fun transfer(
        out: Money,
        into: Money,
        from: String = "colchon",
        to: String = "billetera",
    ) = LedgerEvent.Transfer("t-1", "d", 0, 0, from, to, out, into)

    @Test
    fun `la tasa derivada es la tasa exacta de la operacion`() {
        val rate = transfer(Money.ofMinorUnits(5_000, Currency.USD), Money.ofMinorUnits(59_400, Currency.BOB)).derivedRate()
        assertEquals(ExchangeRate.of(Money.ofMinorUnits(5_000, Currency.USD), Money.ofMinorUnits(59_400, Currency.BOB)), rate)
        assertEquals(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.88"), rate)
        assertEquals(Money.ofMinorUnits(1_188, Currency.BOB), rate.quoteFor(Money.ofMinorUnits(100, Currency.USD)))
    }

    @Test
    fun `cotizacion de BTC a USD con redondeo half-even solo al mostrar`() {
        // 0.00300000 BTC -> 194.99 USD  => 64,996.666... USD por BTC -> 64,996.67
        val rate = transfer(Money.ofMinorUnits(300_000, Currency.BTC), Money.ofMinorUnits(19_499, Currency.USD)).derivedRate()
        assertEquals(Money.ofMinorUnits(6_499_667, Currency.USD), rate.quoteFor(Money.ofMinorUnits(100_000_000, Currency.BTC)))
        // la tasa es exacta: aplicada a la operacion original devuelve exactamente lo recibido
        assertEquals(Money.ofMinorUnits(19_499, Currency.USD), Money.ofMinorUnits(300_000, Currency.BTC) * rate)
    }

    @Test
    fun `half-even redondea al par cuando queda exactamente a la mitad`() {
        val x = Currency.of("XHALF", 1)
        val oneUsd = Money.ofMinorUnits(100, Currency.USD)
        val half = transfer(Money.ofMinorUnits(200, Currency.USD), Money.ofMinorUnits(5, x)).derivedRate() // 2.5 -> 2
        assertEquals(Money.ofMinorUnits(2, x), half.quoteFor(oneUsd))
        val half2 = transfer(Money.ofMinorUnits(200, Currency.USD), Money.ofMinorUnits(7, x)).derivedRate() // 3.5 -> 4
        assertEquals(Money.ofMinorUnits(4, x), half2.quoteFor(oneUsd))
    }

    @Test
    fun `en la misma moneda la tasa derivada es la identidad`() {
        val rate = transfer(Money.ofMinorUnits(20_000, Currency.BOB), Money.ofMinorUnits(20_000, Currency.BOB)).derivedRate()
        assertEquals(ExchangeRate.identity(Currency.BOB), rate)
    }

    @Test
    fun `misma billetera es invalida`() {
        assertFailsWith<IllegalArgumentException> {
            transfer(Money.ofMinorUnits(100, Currency.BOB), Money.ofMinorUnits(100, Currency.BOB), from = "billetera", to = "billetera")
        }
    }

    @Test
    fun `misma moneda con montos distintos es invalida`() {
        assertFailsWith<IllegalArgumentException> {
            transfer(Money.ofMinorUnits(20_000, Currency.BOB), Money.ofMinorUnits(19_500, Currency.BOB))
        }
    }

    @Test
    fun `montos cero o negativos son invalidos`() {
        assertFailsWith<IllegalArgumentException> { transfer(Money.ofMinorUnits(0, Currency.USD), Money.ofMinorUnits(100, Currency.BOB)) }
        assertFailsWith<IllegalArgumentException> {
            transfer(
                Money.ofMinorUnits(100, Currency.USD),
                Money.ofMinorUnits(-100, Currency.BOB),
            )
        }
    }

    @Test
    fun `cada pata tiene su signo`() {
        val t = transfer(Money.ofMinorUnits(5_000, Currency.USD), Money.ofMinorUnits(59_400, Currency.BOB))
        assertEquals(Money.ofMinorUnits(-5_000, Currency.USD), t.deltaFor("colchon"))
        assertEquals(Money.ofMinorUnits(59_400, Currency.BOB), t.deltaFor("billetera"))
        assertEquals(null, t.deltaFor("otra"))
    }
}
