package com.coinden.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/** Las reglas de cómo se acomodan los componentes según sus hermanos, puras y sin Compose. */
class LayoutRulesTest {
    @Test
    fun `una accion ocupa todo el ancho`() {
        assertEquals(ActionsArrangement.Full, ActionsLayout.of(count = 1, fitsInRow = true))
    }

    @Test
    fun `dos acciones que caben van lado a lado`() {
        assertEquals(ActionsArrangement.Row, ActionsLayout.of(count = 2, fitsInRow = true))
    }

    @Test
    fun `dos que no caben o tres se apilan`() {
        assertEquals(ActionsArrangement.Stack, ActionsLayout.of(count = 2, fitsInRow = false))
        assertEquals(ActionsArrangement.Stack, ActionsLayout.of(count = 3, fitsInRow = true))
    }

    @Test
    fun `gasto e ingreso son segmentados`() {
        assertEquals(ChoiceArrangement.Segmented, ChoiceLayout.of(count = 2, open = false, labelsWidth = 160f, available = 340f))
    }

    @Test
    fun `cuatro medios que no caben con letra grande pasan a fila deslizable`() {
        assertEquals(ChoiceArrangement.Segmented, ChoiceLayout.of(count = 4, open = false, labelsWidth = 300f, available = 340f))
        assertEquals(ChoiceArrangement.ScrollRow, ChoiceLayout.of(count = 4, open = false, labelsWidth = 420f, available = 340f))
    }

    @Test
    fun `las billeteras son una lista abierta y se deslizan`() {
        assertEquals(ChoiceArrangement.ScrollRow, ChoiceLayout.of(count = 2, open = true, labelsWidth = 100f, available = 340f))
    }

    @Test
    fun `cinco a ocho fijas se deslizan y mas de ocho muestran las mas usadas`() {
        assertEquals(ChoiceArrangement.ScrollRow, ChoiceLayout.of(count = 6, open = false, labelsWidth = 200f, available = 340f))
        assertEquals(ChoiceArrangement.TopAndMore(4), ChoiceLayout.of(count = 13, open = false, labelsWidth = 900f, available = 340f))
    }

    @Test
    fun `la cuadricula usa entre 2 y 4 columnas iguales`() {
        assertEquals(2, GridLayout.columns(available = 200f, minCell = 96f))
        assertEquals(3, GridLayout.columns(available = 340f, minCell = 96f))
        assertEquals(4, GridLayout.columns(available = 600f, minCell = 96f))
    }

    @Test
    fun `las mas usadas primero y la elegida siempre visible`() {
        val all = listOf("comida", "super", "transporte", "salud", "ocio", "personas")
        val usage = mapOf("transporte" to 9, "comida" to 7, "salud" to 3)

        assertEquals(listOf("transporte", "comida", "salud", "super"), FrequentChoices.top(all, usage, selected = null, count = 4))
        assertEquals(listOf("personas", "transporte", "comida", "salud"), FrequentChoices.top(all, usage, selected = "personas", count = 4))
    }
}
