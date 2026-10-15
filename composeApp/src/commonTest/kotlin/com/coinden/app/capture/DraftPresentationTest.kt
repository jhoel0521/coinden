package com.coinden.app.capture

import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.app.theme.CoinDenIcon
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.wallet.PaymentMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object NoDictation : DictationPort {
    override suspend fun listen(languageCode: String) = DictationOutcome.NothingHeard

    override fun cancel() = Unit
}

/** La confianza se ve con color, ícono y texto; nunca solo con color. */
class DraftPresentationTest {
    @Test
    fun `cada nivel de confianza tiene su tono y su icono`() {
        assertEquals(ConfidenceStyle(Tone.SUCCESS, CoinDenIcon.CHECK_CIRCLE), Confidence.HIGH.style())
        assertEquals(ConfidenceStyle(Tone.WARNING, CoinDenIcon.GAUGE), Confidence.MEDIUM.style())
        assertEquals(ConfidenceStyle(Tone.ERROR, CoinDenIcon.ALERT), Confidence.LOW.style())
    }

    @Test
    fun `los tres niveles se distinguen por icono y por texto, no solo por color`() {
        val levels = Confidence.entries
        assertEquals(levels.size, levels.map { it.style().icon }.toSet().size)
        assertEquals(levels.size, levels.map { it.label() }.toSet().size)
    }

    @Test
    fun `cada medio de pago tiene su icono`() {
        assertEquals(CoinDenIcon.CASH, PaymentMethod.CASH.icon())
        assertEquals(CoinDenIcon.QR, PaymentMethod.TRANSFER.icon())
        assertEquals(CoinDenIcon.CARD, PaymentMethod.CARD.icon())
    }

    private suspend fun cardFor(text: String): DraftCard {
        val container = testContainer()
        seedCarlos(container)
        val presenter = CapturePresenter(container, NoDictation)
        presenter.shared(SharedContent.Text(text))
        return presenter.state.value.result!!
            .cards
            .single()
    }

    @Test
    fun `la tarjeta trae el medio de pago dicho`() =
        runTest {
            assertEquals(PaymentMethod.TRANSFER, cardFor("almuerzo 45 qr").paymentMethod)
            assertEquals(null, cardFor("almuerzo 45 bnb").paymentMethod)
        }
}
