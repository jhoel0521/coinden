package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.inbox.ReviewInboxPresenter
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.CaptureStats
import com.coinden.core.domain.model.draft.CaptureTip
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Carlos dicta con poca información y la tarjeta le enseña qué decir, con sus propias palabras. */
class CaptureCoachingTest {
    private suspend fun carlos(): AppContainer = testContainer().also { seedCarlos(it) }

    private suspend fun AppContainer.say(phrase: String): Draft = captureDraft(ownerId, Evidence(EvidenceSource.VOICE, phrase)).single()

    private suspend fun AppContainer.card(draft: Draft) = DraftCards(this).of(listOf(draft)).single()

    @Test
    fun `sin billetera dicha la tarjeta aconseja decirla con un ejemplo de sus billeteras`() =
        runTest {
            val container = carlos()

            val card = container.card(container.say("gasté 10 en power"))

            assertEquals(setOf(CaptureTip.SAY_WALLET), card.tips)
            assertEquals("power 10 billetera", card.tipExample)
        }

    @Test
    fun `con un alias el ejemplo usa el alias`() =
        runTest {
            val container = carlos()
            val bnb =
                container.drafts
                    .directory(container.ownerId)
                    .wallets
                    .single { it.name == "BNB Bs" }
                    .id
            container.setDefaultWallet(container.ownerId, bnb)
            com.coinden.core.domain.usecase.draft
                .SetWalletAliasesUseCase(container.drafts)(container.ownerId, bnb, setOf("bnb"))

            assertEquals("power 10 bnb", container.card(container.say("gasté 10 en power")).tipExample)
        }

    @Test
    fun `sin monto aconseja decir el monto y una captura completa no aconseja nada`() =
        runTest {
            val container = carlos()

            assertEquals(setOf(CaptureTip.SAY_AMOUNT), container.card(container.say("almuerzo bnb")).tips)
            val complete = container.card(container.say("pagué 25 de almuerzo bnb"))
            assertEquals(emptySet(), complete.tips)
            assertNull(complete.tipExample)
        }

    @Test
    fun `la bandeja cuenta las capturas de la semana que quedaron listas sin revisar`() =
        runTest {
            val container = carlos()
            container.say("pagué 25 de almuerzo bnb")
            container.say("pagué 10 de taxi bnb")
            container.say("page 3 bs para trasmporte")
            val inbox = ReviewInboxPresenter(container)

            inbox.load()

            assertEquals(CaptureStats(total = 3, readyAlone = 2), inbox.state.value.week)
        }
}
