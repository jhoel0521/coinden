package com.coinden.app.capture

import com.coinden.app.DemoSeed
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.SuggestionReason
import com.coinden.core.domain.model.draft.Confidence
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Carlos (Bolivia) prueba la demo de captura con frases reales de su día. */
class CaptureDemoPresenterTest {
    private suspend fun capture(text: String): DraftCard {
        val container = testContainer()
        DemoSeed.seed(container)
        val presenter = CaptureDemoPresenter(container)
        presenter.onTextChange(text)
        presenter.onCapture()
        val state = presenter.state.value
        assertEquals("", state.text, "el campo se limpia tras capturar")
        return state.cards.single()
    }

    @Test
    fun `gasto hormiga sin billetera queda en efectivo sugerida con confianza baja y sin categoria`() =
        runTest {
            val card = capture("page 3 bs para trasmporte")

            assertEquals(DraftStatusView.READY, card.status)
            assertEquals(MovementKindView.EXPENSE, card.kind)
            assertEquals(Money.parseCanonical("3", Currency.BOB), card.amount)
            assertEquals("Billetera", card.walletName)
            assertTrue(card.walletSuggested)
            assertEquals(Confidence.LOW, card.confidence)
            assertNull(card.category)
        }

    @Test
    fun `gasto con alias va al banco con confianza media y comida por almuerzo`() =
        runTest {
            val card = capture("almuerzo 45 qr")

            assertEquals(MovementKindView.EXPENSE, card.kind)
            assertEquals(Money.parseCanonical("45", Currency.BOB), card.amount)
            assertEquals("BNB Bs", card.walletName)
            assertFalse(card.walletSuggested)
            assertEquals(Confidence.MEDIUM, card.confidence)
            assertEquals(CategoryRef.Base(BaseCategory.FOOD), card.category?.ref)
            assertEquals(SuggestionReason.PackWord("almuerzo"), card.category?.reason)
        }

    @Test
    fun `frase sin monto queda capturada sin propuesta`() =
        runTest {
            val card = capture("hola")

            assertEquals(DraftStatusView.CAPTURED, card.status)
            assertNull(card.kind)
            assertNull(card.amount)
            assertNull(card.walletName)
            assertEquals(Confidence.LOW, card.confidence)
            assertNull(card.category)
        }

    @Test
    fun `texto vacio no captura nada`() =
        runTest {
            val container = testContainer()
            DemoSeed.seed(container)
            val presenter = CaptureDemoPresenter(container)

            presenter.onTextChange("   ")
            presenter.onCapture()

            assertTrue(presenter.state.value.cards.isEmpty())
        }
}
