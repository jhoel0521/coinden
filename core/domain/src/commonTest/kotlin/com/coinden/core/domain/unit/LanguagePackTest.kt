package com.coinden.core.domain.unit

import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.merged
import com.coinden.core.domain.model.wallet.PaymentMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguagePackTest {
    @Test
    fun `cada paquete trae sus marcadores de lo recibido en un cambio`() {
        assertEquals(setOf("got", "received", "for"), LanguagePacks.ENGLISH.receivedMarkers)
        assertEquals(setOf("me dieron", "me dio", "recibi", "por"), LanguagePacks.SPANISH.receivedMarkers)
        assertEquals(setOf("me deram", "recebi", "por"), LanguagePacks.PORTUGUESE.receivedMarkers)
    }

    @Test
    fun `al unir paquetes se unen sus marcadores de lo recibido`() {
        val merged = listOf(LanguagePacks.ENGLISH, LanguagePacks.SPANISH).merged()

        assertTrue(merged.receivedMarkers.containsAll(setOf("got", "for", "me dieron", "por")))
        assertEquals(7, merged.receivedMarkers.size)
    }

    @Test
    fun `cada paquete trae sus palabras de medio de pago`() {
        val cash = PaymentMethod.CASH
        val transfer = PaymentMethod.TRANSFER
        val card = PaymentMethod.CARD
        assertEquals(
            mapOf("cash" to cash, "qr" to transfer, "transfer" to transfer, "card" to card, "debit" to card),
            LanguagePacks.ENGLISH.paymentMethodWords,
        )
        assertEquals(
            mapOf("efectivo" to cash, "qr" to transfer, "transferencia" to transfer, "tarjeta" to card, "debito" to card),
            LanguagePacks.SPANISH.paymentMethodWords,
        )
        assertEquals(
            mapOf(
                "dinheiro" to cash,
                "pix" to transfer,
                "qr" to transfer,
                "transferencia" to transfer,
                "ted" to transfer,
                "cartao" to card,
                "debito" to card,
            ),
            LanguagePacks.PORTUGUESE.paymentMethodWords,
        )
    }

    @Test
    fun `al unir paquetes se unen sus palabras de medio de pago`() {
        val merged = listOf(LanguagePacks.ENGLISH, LanguagePacks.PORTUGUESE).merged()
        assertEquals(PaymentMethod.TRANSFER, merged.paymentMethodWords["pix"])
        assertEquals(PaymentMethod.CASH, merged.paymentMethodWords["cash"])
    }
}
