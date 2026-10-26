package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.interpret.WalletNameHints
import com.coinden.core.domain.model.interpret.merged
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.support.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals

/** Los apodos salen del nombre de cada billetera: el usuario no configura nada. */
class WalletNameHintsTest {
    private val es = listOf(TestPacks.english, TestPacks.spanish).merged()
    private val bolivia = testLocale("es", "BO")

    private fun wallet(
        id: String,
        name: String,
        currency: Currency = Currency.BOB,
        type: WalletType = WalletType.BANK,
    ) = Wallet(id, "carlos", name, currency, type)

    private fun hints(vararg wallets: Wallet) = WalletNameHints.derive(wallets.toList(), es, bolivia)

    @Test
    fun `la sigla del banco sin la moneda`() {
        assertEquals(mapOf("bnb" to "bnb"), hints(wallet("bnb", "BNB Bs")))
    }

    @Test
    fun `las palabras genericas no son apodos`() {
        assertEquals(mapOf("economico" to "be"), hints(wallet("be", "Banco Económico")))
        assertEquals(mapOf("sueldo" to "s"), hints(wallet("s", "Mi cuenta sueldo")))
    }

    @Test
    fun `una palabra compartida por dos billeteras no identifica a ninguna`() {
        val colchon = wallet("colchon", "Dólares colchón", Currency.USD, WalletType.CASH)
        val banco = wallet("dolares-banco", "Dólares banco", Currency.USD)
        assertEquals(mapOf("colchon" to "colchon"), hints(colchon, banco))
    }

    @Test
    fun `monedas y medios de pago no son apodos`() {
        assertEquals(emptyMap(), hints(wallet("x", "QR Bs USD")))
        assertEquals(mapOf("yape" to "yape"), hints(wallet("yape", "Yape efectivo", type = WalletType.DIGITAL)))
    }

    @Test
    fun `las billeteras archivadas no cuentan`() {
        val vieja = wallet("vieja", "BNB viejo").archived()
        val nueva = wallet("bnb", "BNB Bs")
        // "bnb" solo la usa la activa; "viejo" es de una archivada
        assertEquals(mapOf("bnb" to "bnb"), hints(vieja, nueva))
    }

    @Test
    fun `palabras muy cortas no son apodos`() {
        // "caja" es genérica; "de" y "la" son muy cortas
        assertEquals(mapOf("casa" to "c"), hints(wallet("c", "Caja de la casa")))
    }
}
