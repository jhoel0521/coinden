package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
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
    fun `venta de 50 USD por 594 Bs deriva tasa de 11_88`() {
        val rate = transfer(Money(5_000, Currency.USD), Money(59_400, Currency.BOB)).derivedRate()
        assertEquals(Money(1_188, Currency.BOB), rate)
    }

    @Test
    fun `venta de BTC a USD con redondeo half-even`() {
        // 0.00300000 BTC -> 194.99 USD  => 64,996.666... USD por BTC -> 64,996.67
        val rate = transfer(Money(300_000, Currency.BTC), Money(19_499, Currency.USD)).derivedRate()
        assertEquals(Money(6_499_667, Currency.USD), rate)
    }

    @Test
    fun `half-even redondea al par cuando queda exactamente a la mitad`() {
        val x = Currency.of("XHALF", 1)
        val half = transfer(Money(200, Currency.USD), Money(5, x)).derivedRate() // 5*100/200 = 2.5 -> 2
        assertEquals(Money(2, x), half)
        val half2 = transfer(Money(200, Currency.USD), Money(7, x)).derivedRate() // 3.5 -> 4
        assertEquals(Money(4, x), half2)
    }

    @Test
    fun `misma billetera es invalida`() {
        assertFailsWith<IllegalArgumentException> {
            transfer(Money(100, Currency.BOB), Money(100, Currency.BOB), from = "billetera", to = "billetera")
        }
    }

    @Test
    fun `misma moneda con montos distintos es invalida`() {
        assertFailsWith<IllegalArgumentException> {
            transfer(Money(20_000, Currency.BOB), Money(19_500, Currency.BOB))
        }
    }

    @Test
    fun `montos cero o negativos son invalidos`() {
        assertFailsWith<IllegalArgumentException> { transfer(Money(0, Currency.USD), Money(100, Currency.BOB)) }
        assertFailsWith<IllegalArgumentException> { transfer(Money(100, Currency.USD), Money(-100, Currency.BOB)) }
    }

    @Test
    fun `cada pata tiene su signo`() {
        val t = transfer(Money(5_000, Currency.USD), Money(59_400, Currency.BOB))
        assertEquals(Money(-5_000, Currency.USD), t.deltaFor("colchon"))
        assertEquals(Money(59_400, Currency.BOB), t.deltaFor("billetera"))
        assertEquals(null, t.deltaFor("otra"))
    }
}
