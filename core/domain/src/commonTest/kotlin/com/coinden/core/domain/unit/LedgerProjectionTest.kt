package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.LedgerProjection
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerProjectionTest {
    private val cash = Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH)

    private fun bs(cents: Long) = Money(cents, Currency.BOB)

    private fun opening(
        cents: Long,
        time: Long = at(8),
    ) = LedgerEvent.OpeningBalance("e-open", "d", time, time, cash.id, bs(cents))

    private fun expense(
        id: String,
        cents: Long,
        time: Long,
        recorded: Long = time,
    ) = LedgerEvent.Expense(id, "d", time, recorded, cash.id, bs(cents))

    private fun income(
        id: String,
        cents: Long,
        time: Long,
    ) = LedgerEvent.Income(id, "d", time, time, cash.id, bs(cents))

    private fun annul(
        id: String,
        target: String,
    ) = LedgerEvent.Annulment(id, "d", at(21), at(21), target, setOf(cash.id))

    @Test
    fun `saldo es la suma de los movimientos`() {
        val p = LedgerProjection(cash, listOf(opening(15_000), expense("g", 300, at(12)), income("i", 5_000, at(18))))
        assertEquals(bs(19_700), p.balance)
    }

    @Test
    fun `un movimiento anulado no cuenta`() {
        val p = LedgerProjection(cash, listOf(opening(15_000), expense("g", 4_500, at(13)), annul("a", "g")))
        assertEquals(bs(15_000), p.balance)
        assertTrue(p.isAnnulled("g"))
        assertEquals(listOf("e-open"), p.effectiveEvents.map { it.id })
    }

    @Test
    fun `ajuste negativo baja el saldo y ajuste cero es invalido`() {
        val adj = LedgerEvent.Adjustment("adj", "d", at(20), at(20), cash.id, bs(-700))
        assertEquals(bs(14_000), LedgerProjection(cash, listOf(opening(14_700), adj)).balance)
        assertFailsWith<IllegalArgumentException> { adj.copy(amount = bs(0)) }
    }

    @Test
    fun `gasto e ingreso deben ser positivos`() {
        assertFailsWith<IllegalArgumentException> { expense("g", 0, at(12)) }
        assertFailsWith<IllegalArgumentException> { expense("g", -500, at(12)) }
        assertFailsWith<IllegalArgumentException> { income("i", 0, at(12)) }
    }

    @Test
    fun `transferencia se proyecta solo con la pata de esta billetera`() {
        val t = LedgerEvent.Transfer("t", "d", at(10), at(10), "colchon", cash.id, Money(5_000, Currency.USD), bs(59_400))
        assertEquals(bs(60_400), LedgerProjection(cash, listOf(opening(1_000), t)).balance)
    }

    @Test
    fun `sin saldo negativo no hay faltante`() {
        val p = LedgerProjection(cash, listOf(opening(1_000), expense("g", 300, at(12))))
        assertEquals(bs(0), p.lowestBalance)
        assertNull(p.firstNegativeAtEpochMs)
        assertFalse(p.isUnbalanced)
    }

    @Test
    fun `detecta el hueco negativo aunque el saldo final sea positivo`() {
        // 10 Bs previos, almuerzo 45 Bs a las 13:00, retiro de 200 Bs a las 14:00
        val p = LedgerProjection(cash, listOf(opening(1_000), income("cajero", 20_000, at(14)), expense("almuerzo", 4_500, at(13))))
        assertEquals(bs(16_500), p.balance)
        assertEquals(bs(-3_500), p.lowestBalance)
        assertEquals(bs(3_500), p.shortfall)
        assertEquals(at(13), p.firstNegativeAtEpochMs)
        assertTrue(p.isUnbalanced)
    }

    @Test
    fun `anular un ingreso del que dependen gastos deja un hueco`() {
        val events = listOf(opening(0), income("sueldo", 20_000, at(9)), expense("g", 18_000, at(12)), annul("a", "sueldo"))
        val p = LedgerProjection(cash, events)
        assertEquals(bs(-18_000), p.lowestBalance)
        assertEquals(at(12), p.firstNegativeAtEpochMs)
    }

    @Test
    fun `credito con negativo no esta descuadrado`() {
        val credit = cash.copy(id = "visa", type = WalletType.CREDIT, allowsNegative = true)
        val g = LedgerEvent.Expense("g", "d", at(12), at(12), credit.id, bs(25_000))
        val p = LedgerProjection(credit, listOf(g))
        assertEquals(bs(-25_000), p.balance)
        assertFalse(p.isUnbalanced)
    }

    @Test
    fun `orden determinista por ocurrencia, registro e id`() {
        val a = expense("b-id", 100, at(12), recorded = at(21))
        val b = expense("a-id", 100, at(12), recorded = at(21))
        val c = expense("c-id", 100, at(12), recorded = at(20))
        val first = LedgerProjection(cash, listOf(opening(1_000), a, b, c)).effectiveEvents.map { it.id }
        val second = LedgerProjection(cash, listOf(c, b, opening(1_000), a)).effectiveEvents.map { it.id }
        assertEquals(listOf("e-open", "c-id", "a-id", "b-id"), first)
        assertEquals(first, second)
    }
}
