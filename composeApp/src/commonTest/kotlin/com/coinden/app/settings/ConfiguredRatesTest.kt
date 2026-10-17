package com.coinden.app.settings

import com.coinden.app.home.HomePresenter
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfiguredRatesTest {
    @Test
    fun `John y Carlos no reciben una cotizacion BTC de ejemplo al quitar su referencia`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val p = WalletSettingsPresenter(c).also { it.load() }
            p.addCurrency("USD")
            p.saveRate("USD", "10")
            p.addCurrency("BTC")
            p.addWallet("Bitcoin", WalletType.CRYPTO, "BTC", "0.001")
            val home = HomePresenter(c).also { it.load() }
            assertEquals(listOf(Money.parseCanonical("0.001", Currency.BTC)), home.state.value.unconverted)
            assertTrue(p.saveRate("BTC", "600000"))
            home.load()
            assertEquals(Money.parseCanonical("1200", Currency.BOB), home.state.value.total)
            assertTrue(p.removeRate("BTC"))
            home.load()
            assertEquals(Money.parseCanonical("600", Currency.BOB), home.state.value.total)
            assertEquals(listOf(Money.parseCanonical("0.001", Currency.BTC)), home.state.value.unconverted)
        }

    @Test
    fun `una referencia extrema guardada no derriba la pantalla al dibujar su cotizacion`() {
        val rate = ExchangeRate.parseCanonical("9000000000000000000/1", Currency.USD, Currency.BOB)
        assertEquals(null, referenceQuote(rate))
        assertEquals(Money.parseCanonical("11.20", Currency.BOB), referenceQuote(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20")))
    }
}
