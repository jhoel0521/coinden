package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.InMemoryWalletRepository
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletStatus
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletTest {
    private fun wallet(
        type: WalletType = WalletType.CASH,
        name: String = "Billetera",
    ) = Wallet(id = "w-1", ownerId = "carlos", name = name, currency = Currency.BOB, type = type)

    @Test
    fun `nombre vacio o solo espacios es invalido`() {
        assertFailsWith<IllegalArgumentException> { wallet(name = "") }
        assertFailsWith<IllegalArgumentException> { wallet(name = "   ") }
        assertFalse(Wallet.isValidName("  "))
    }

    @Test
    fun `solo credito admite negativo por defecto`() {
        assertTrue(wallet(WalletType.CREDIT).allowsNegative)
        WalletType.entries.filter { it != WalletType.CREDIT }.forEach {
            assertFalse(wallet(it).allowsNegative, "$it no deberia admitir negativo por defecto")
        }
    }

    @Test
    fun `la politica de negativos se puede indicar explicitamente`() {
        val cash = wallet(WalletType.CASH).copy(allowsNegative = true)
        assertTrue(cash.allowsNegative)
    }

    @Test
    fun `archivar y reactivar cambian solo el estado`() {
        val active = wallet()
        val archived = active.archived()
        assertEquals(WalletStatus.ARCHIVED, archived.status)
        assertFalse(archived.isActive)
        assertEquals(active, archived.reactivated())
    }

    @Test
    fun `acepta solo montos en su moneda`() {
        assertTrue(wallet().accepts(Currency.BOB))
        assertFalse(wallet().accepts(Currency.USD))
    }

    @Test
    fun `el repositorio en memoria nunca reemplaza un evento existente`() =
        runTest {
            val repo = InMemoryWalletRepository()
            val event = LedgerEvent.Income("e-1", "d", 0, 0, "w-1", Money(100, Currency.BOB))
            repo.appendEvent(event)
            assertFailsWith<IllegalStateException> { repo.appendEvent(event.copy(amount = Money(999, Currency.BOB))) }
            assertEquals(listOf(event), repo.allEvents)
        }
}
