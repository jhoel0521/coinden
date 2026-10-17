package com.coinden.app.capture

import com.coinden.app.AppStorage
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.port.output.DraftRepositoryPort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DraftFormWriteTest {
    private class ControlledDrafts(
        private val base: DraftRepositoryPort,
    ) : DraftRepositoryPort by base {
        var failCapture = false
        var failRevision = false
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun appendEvent(event: DraftEvent) {
            if (event is DraftEvent.Captured && failCapture) error("capture disk")
            if (event is DraftEvent.Revised) {
                gate?.await()
                if (failRevision) error("revision disk")
            }
            base.appendEvent(event)
        }
    }

    private suspend fun fixture(): Pair<com.coinden.app.AppContainer, ControlledDrafts> {
        val base = AppStorage.inMemory()
        val drafts = ControlledDrafts(base.drafts)
        val c =
            testContainer(storage = AppStorage(base.wallets, drafts, base.aliases, base.categorization, base.profiles, base.preferences))
        seedCarlos(c)
        return c to drafts
    }

    @Test
    fun `Carlos toca dos veces guardar y solo crea un borrador sin cambiar saldos`() =
        runTest {
            val (c, drafts) = fixture()
            val p =
                DraftFormPresenter(c).also {
                    it.load()
                    it.onAmountChange("12")
                }
            val before = c.walletBalances(c.ownerId)
            val gate = CompletableDeferred<Unit>()
            drafts.gate = gate
            val first = launch { p.save() }
            runCurrent()
            kotlin.test.assertTrue(p.state.value.busy)
            p.onAmountChange("99")
            assertEquals("12", p.state.value.amount)
            val second = launch { p.save() }
            runCurrent()
            val count = drafts.draftIdsFor(c.ownerId).size
            gate.complete(Unit)
            first.join()
            second.join()
            assertEquals(1, count)
            assertNull(p.save())
            kotlin.test.assertTrue(p.state.value.completed)
            assertEquals(before, c.walletBalances(c.ownerId))
        }

    @Test
    fun `Carlos reintenta revision fallida sobre la misma captura`() =
        runTest {
            val (c, drafts) = fixture()
            val p =
                DraftFormPresenter(c).also {
                    it.load()
                    it.onAmountChange("12")
                    it.onNoteChange("almuerzo")
                }
            drafts.failRevision = true
            assertNull(p.save())
            kotlin.test.assertTrue(p.state.value.storageError)
            assertFalse(p.state.value.busy)
            val id = drafts.draftIdsFor(c.ownerId).single()
            assertEquals("almuerzo", p.state.value.note)
            drafts.failRevision = false
            assertNotNull(p.save())
            assertEquals(listOf(id), drafts.draftIdsFor(c.ownerId))
        }

    @Test
    fun `Carlos conserva campos al fallar antes de capturar y puede reintentar`() =
        runTest {
            val (c, drafts) = fixture()
            val p =
                DraftFormPresenter(c).also {
                    it.load()
                    it.onAmountChange("12")
                }
            drafts.failCapture = true
            assertNull(p.save())
            assertEquals("12", p.state.value.amount)
            assertEquals(0, drafts.draftIdsFor(c.ownerId).size)
            drafts.failCapture = false
            assertNotNull(p.save())
            assertEquals(1, drafts.draftIdsFor(c.ownerId).size)
        }

    @Test
    fun `Sophie no descarta mientras confirma ni repite navegacion al terminar`() =
        runTest {
            val (c, drafts) = fixture()
            val new =
                DraftFormPresenter(c).also {
                    it.load()
                    it.onAmountChange("12")
                }
            val id = assertNotNull(new.save()).cards.single().draftId
            val p = DraftFormPresenter(c, FormMode.Review(id)).also { it.load() }
            val gate = CompletableDeferred<Unit>()
            drafts.gate = gate
            val confirming = launch { p.confirm() }
            runCurrent()
            val discarded = p.discard()
            gate.complete(Unit)
            confirming.join()
            assertFalse(discarded)
            assertFalse(p.confirm())
        }
}
