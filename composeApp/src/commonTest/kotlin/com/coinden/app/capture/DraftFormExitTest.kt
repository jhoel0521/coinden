package com.coinden.app.capture

import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DraftFormExitTest {
    @Test
    fun `Carlos sale sin aviso si no edita o revierte sus cambios`() =
        runTest {
            val c = testContainer()
            seedCarlos(c)
            val p = DraftFormPresenter(c).also { it.load() }
            assertEquals(FormExit.LEAVE, p.requestExit())
            p.onAmountChange("42")
            assertEquals(FormExit.ASK, p.requestExit())
            p.onAmountChange("")
            assertEquals(FormExit.LEAVE, p.requestExit())
        }

    @Test
    fun `John puede seguir editando o salir sin guardar su correccion`() =
        runTest {
            val c = testContainer(language = "en", country = "US", ownerId = "john")
            seedCarlos(c)
            val new =
                DraftFormPresenter(c).also {
                    it.load()
                    it.onAmountChange("12")
                }
            val id =
                kotlin.test
                    .assertNotNull(new.save())
                    .cards
                    .single()
                    .draftId
            c.confirmDraft(id)
            val before = c.walletBalances(c.ownerId)
            val p = DraftFormPresenter(c, FormMode.Correct(id)).also { it.load() }
            p.onNoteChange("edited")
            assertEquals(FormExit.ASK, p.requestExit())
            assertEquals("edited", p.state.value.note)
            assertEquals(before, c.walletBalances(c.ownerId))
        }

    @Test
    fun `Carlos conserva la edicion al volver de evidencia y cargar de nuevo`() =
        runTest {
            val c = testContainer()
            seedCarlos(c)
            val new =
                DraftFormPresenter(c).also {
                    it.load()
                    it.onAmountChange("12")
                }
            val id =
                kotlin.test
                    .assertNotNull(new.save())
                    .cards
                    .single()
                    .draftId
            val p = DraftFormPresenter(c, FormMode.Review(id)).also { it.load() }
            p.onAmountChange("42")
            p.load()
            assertEquals("42", p.state.value.amount)
        }
}
