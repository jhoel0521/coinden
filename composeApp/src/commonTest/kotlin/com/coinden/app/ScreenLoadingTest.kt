package com.coinden.app

import com.coinden.app.capture.ConfirmProblem
import com.coinden.app.capture.MovementKindView
import com.coinden.app.history.HistoryItem
import com.coinden.app.history.HistoryPresenter
import com.coinden.app.history.HistoryState
import com.coinden.app.home.HomePresenter
import com.coinden.app.home.HomeState
import com.coinden.app.inbox.InboxSnapshot
import com.coinden.app.inbox.InboxState
import com.coinden.app.inbox.ReviewInboxPresenter
import com.coinden.app.ui.ScreenLoad
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScreenLoadingTest {
    private class Probe(
        val load: suspend () -> Unit,
        val status: () -> ScreenLoad,
        val loaded: () -> Boolean,
        val marker: () -> String,
    )

    private fun probe(
        screen: String,
        query: suspend () -> Int,
    ): Probe {
        val c = testContainer()
        return when (screen) {
            "inicio" -> {
                val p = HomePresenter(c, query = { HomeState(pendingDrafts = query()) })
                Probe(p::load, { p.state.value.load }, { p.state.value.loaded }, {
                    p.state.value.pendingDrafts
                        .toString()
                })
            }
            "historial" -> {
                val p =
                    HistoryPresenter(c, query = {
                        val n = query()
                        HistoryState(
                            items =
                                if (n == 0) {
                                    emptyList()
                                } else {
                                    listOf(
                                        HistoryItem(
                                            draftId = "id",
                                            kind = MovementKindView.EXPENSE,
                                            note = n.toString(),
                                            amount = Money.parseCanonical("1", Currency.USD),
                                            walletName = "wallet",
                                            occurredAtEpochMs = 0,
                                            category = null,
                                            reference = null,
                                            evidenceCount = 1,
                                            corrected = false,
                                        ),
                                    )
                                },
                        )
                    })
                Probe(p::load, { p.state.value.load }, { p.state.value.loaded }, {
                    p.state.value.items
                        .firstOrNull()
                        ?.note ?: "0"
                })
            }
            else -> {
                val p =
                    ReviewInboxPresenter(c, query = {
                        val n = query()
                        InboxSnapshot(InboxState(problems = if (n == 0) emptyMap() else mapOf(n.toString() to ConfirmProblem.NotReady)))
                    })
                Probe(p::load, { p.state.value.load }, { p.state.value.loaded }, {
                    p.state.value.problems.keys
                        .firstOrNull() ?: "0"
                })
            }
        }
    }

    @Test
    fun `Carlos distingue carga, fallo, datos anteriores y reintento en las tres pantallas`() =
        runTest {
            for (screen in listOf("inicio", "historial", "bandeja")) {
                var fail = false
                var value = 1
                val gate = CompletableDeferred<Unit>()
                val p =
                    probe(screen) {
                        gate.await()
                        if (fail) error("disk")
                        value
                    }
                assertFalse(p.loaded(), screen)
                val loading = launch { p.load() }
                runCurrent()
                assertEquals(ScreenLoad.LOADING, p.status(), screen)
                gate.complete(Unit)
                loading.join()
                assertTrue(p.loaded(), screen)
                assertEquals(ScreenLoad.READY, p.status(), screen)
                fail = true
                p.load()
                assertEquals(ScreenLoad.ERROR, p.status(), screen)
                assertTrue(p.loaded(), screen)
                assertEquals("1", p.marker(), screen)
                fail = false
                value = 2
                p.load()
                assertEquals(ScreenLoad.READY, p.status(), screen)
                assertEquals("2", p.marker(), screen)
            }
        }

    @Test
    fun `Carlos no ve vacio como resultado de un fallo inicial y puede recuperar lista vacia`() =
        runTest {
            for (screen in listOf("inicio", "historial", "bandeja")) {
                var fail = true
                val p =
                    probe(screen) {
                        if (fail) error("disk")
                        0
                    }
                p.load()
                assertEquals(ScreenLoad.ERROR, p.status(), screen)
                assertFalse(p.loaded(), screen)
                fail = false
                p.load()
                assertEquals(ScreenLoad.READY, p.status(), screen)
                assertTrue(p.loaded(), screen)
                assertEquals("0", p.marker(), screen)
            }
        }

    @Test
    fun `respuesta vieja no pisa datos recientes de Carlos en ninguna pantalla`() =
        runTest {
            for (screen in listOf("inicio", "historial", "bandeja")) {
                val first = CompletableDeferred<Int>()
                var calls = 0
                val p = probe(screen) { if (++calls == 1) first.await() else 2 }
                val old = launch { p.load() }
                runCurrent()
                p.load()
                first.complete(1)
                old.join()
                assertEquals("2", p.marker(), screen)
                assertEquals(ScreenLoad.READY, p.status(), screen)
            }
        }

    @Test
    fun `cancelar una carga no se presenta como error de disco`() =
        runTest {
            for (screen in listOf("inicio", "historial", "bandeja")) {
                val p = probe(screen) { throw CancellationException("closed") }
                assertFailsWith<CancellationException> { p.load() }
                assertEquals(ScreenLoad.LOADING, p.status(), screen)
            }
        }
}
