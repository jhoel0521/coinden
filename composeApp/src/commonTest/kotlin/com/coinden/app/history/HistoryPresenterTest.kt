package com.coinden.app.history

import com.coinden.app.AppContainer
import com.coinden.app.capture.DraftFormPresenter
import com.coinden.app.capture.FormMode
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** El historial de Carlos: lo vigente primero lo más reciente, y Corregir abre el formulario. */
class HistoryPresenterTest {
    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun carlos(): Pair<AppContainer, HistoryPresenter> {
        val container = testContainer()
        seedCarlos(container)
        return container to HistoryPresenter(container)
    }

    private suspend fun AppContainer.confirmed(phrase: String): String {
        val draft = captureDraft(ownerId, Evidence(EvidenceSource.VOICE, phrase)).single()
        assertIs<DraftResult.Ok>(confirmDraft(draft.id))
        return draft.id
    }

    @Test
    fun `lo mas reciente primero, y corregir a mano deja vigente solo lo corregido`() =
        runTest {
            val (container, history) = carlos()
            val lunch = container.confirmed("pagué 35 de almuerzo bnb")
            val taxi = container.confirmed("pagué 10 de taxi bnb")
            history.load()
            assertEquals(
                listOf(taxi, lunch),
                history.state.value.items
                    .map { it.draftId },
            )

            val mode =
                history.correct(
                    history.state.value.items
                        .single { it.draftId == lunch },
                )
            assertEquals(FormMode.Correct(lunch), mode)
            val form = DraftFormPresenter(container, mode!!).also { it.load() }
            form.onAmountChange("32")
            assertTrue(form.confirm())
            history.load()

            val fixed =
                history.state.value.items
                    .single { it.draftId == lunch }
            assertEquals(bs("32"), fixed.amount)
            assertTrue(fixed.corrected)
            assertEquals(2, history.state.value.items.size)
            assertFalse(
                history.state.value.items
                    .single { it.draftId == taxi }
                    .corrected,
            )
        }
}
