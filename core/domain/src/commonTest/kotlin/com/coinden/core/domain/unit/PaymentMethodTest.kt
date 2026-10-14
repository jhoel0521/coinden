package com.coinden.core.domain.unit

import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentMethodTest {
    @Test
    fun `cada tipo de billetera acepta los medios de donde puede salir su dinero`() {
        assertEquals(setOf(PaymentMethod.CASH), WalletType.CASH.acceptedMethods)
        assertEquals(setOf(PaymentMethod.TRANSFER, PaymentMethod.CARD), WalletType.BANK.acceptedMethods)
        assertEquals(setOf(PaymentMethod.TRANSFER), WalletType.DIGITAL.acceptedMethods)
        assertEquals(setOf(PaymentMethod.CARD), WalletType.CREDIT.acceptedMethods)
        assertEquals(setOf(PaymentMethod.TRANSFER), WalletType.CRYPTO.acceptedMethods)
    }

    @Test
    fun `una transferencia por QR o Pix nunca sale del efectivo`() {
        val acceptsTransfer = WalletType.entries.filter { PaymentMethod.TRANSFER in it.acceptedMethods }
        assertEquals(listOf(WalletType.BANK, WalletType.DIGITAL, WalletType.CRYPTO), acceptsTransfer)
    }
}
