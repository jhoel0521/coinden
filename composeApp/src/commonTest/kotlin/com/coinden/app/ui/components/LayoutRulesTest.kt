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
        assertEquals(ChoiceArrangement.Segmented, ChoiceLayout.of(count = 2, open = false, widestLabel = 70f, available = 340f))
    }

    @Test
    fun `cuatro medios que no caben con letra grande pasan a fila deslizable`() {
        assertEquals(ChoiceArrangement.Segmented, ChoiceLayout.of(count = 4, open = false, widestLabel = 30f, available = 340f))
        assertEquals(ChoiceArrangement.ScrollRow, ChoiceLayout.of(count = 4, open = false, widestLabel = 60f, available = 340f))
    }

    @Test
    fun `los segmentados son del mismo ancho y la etiqueta mas larga tiene que entrar en su parte`() {
        // "Español", "English", "Português" con la letra al máximo: el total cabe, pero "Português" no entra en un tercio.
        assertEquals(ChoiceArrangement.ScrollRow, ChoiceLayout.of(count = 3, open = false, widestLabel = 89f, available = 371f))
        assertEquals(ChoiceArrangement.Segmented, ChoiceLayout.of(count = 3, open = false, widestLabel = 60f, available = 371f))
    }

    @Test
    fun `las billeteras son una lista abierta y se deslizan`() {
        assertEquals(ChoiceArrangement.ScrollRow, ChoiceLayout.of(count = 2, open = true, widestLabel = 50f, available = 340f))
    }

    @Test
    fun `cinco a ocho fijas se deslizan y mas de ocho muestran las mas usadas`() {
        assertEquals(ChoiceArrangement.ScrollRow, ChoiceLayout.of(count = 6, open = false, widestLabel = 30f, available = 340f))
        assertEquals(ChoiceArrangement.TopAndMore(4), ChoiceLayout.of(count = 13, open = false, widestLabel = 70f, available = 340f))
    }

    @Test
    fun `la cuadricula usa entre 2 y 4 columnas iguales`() {
        assertEquals(2, GridLayout.columns(available = 200f, minCell = 96f))
        assertEquals(3, GridLayout.columns(available = 340f, minCell = 96f))
        assertEquals(4, GridLayout.columns(available = 600f, minCell = 96f))
    }

    @Test
    fun `la cuadricula da a cada celda el ancho de la etiqueta mas larga sin cortarla`() {
        // "Tarjeta de crédito" mide ~150 dp: en 340 dp entran 2 columnas, no 3.
        assertEquals(2, GridLayout.columns(available = 340f, minCell = 96f, widestLabel = 150f))
        assertEquals(3, GridLayout.columns(available = 340f, minCell = 96f, widestLabel = 40f))
    }

    @Test
    fun `las mas usadas primero y la elegida siempre visible`() {
        val all = listOf("comida", "super", "transporte", "salud", "ocio", "personas")
        val usage = mapOf("transporte" to 9, "comida" to 7, "salud" to 3)

        assertEquals(listOf("transporte", "comida", "salud", "super"), FrequentChoices.top(all, usage, selected = null, count = 4))
        assertEquals(listOf("personas", "transporte", "comida", "salud"), FrequentChoices.top(all, usage, selected = "personas", count = 4))
    }

    @Test
    fun `los mosaicos con icono van en una fila si a cada uno le quedan 64 dp`() {
        // Medio de pago: 4 mosaicos en 328 dp.
        assertEquals(TileArrangement.Row, TileLayout.of(count = 4, available = 328f))
        // Cinco en una pantalla ancha (tableta o apaisado).
        assertEquals(TileArrangement.Row, TileLayout.of(count = 5, available = 400f))
    }

    @Test
    fun `si no caben los mosaicos pasan a una cuadricula de columnas iguales`() {
        // Tipos de billetera: 5 mosaicos en 288 dp (dentro de una tarjeta) dejan menos de 64 dp a cada uno.
        assertEquals(TileArrangement.Grid(columns = 3), TileLayout.of(count = 5, available = 288f))
    }

    @Test
    fun `si la palabra mas larga no entra en el mosaico pasan a cuadricula sin partir palabras`() {
        // Letra 1,3: "Transferencia" mide ~104 dp y no entra en 4 mosaicos de 328 dp.
        assertEquals(TileArrangement.Grid(columns = 2), TileLayout.of(count = 4, available = 328f, widestWord = 96f))
        assertEquals(TileArrangement.Row, TileLayout.of(count = 4, available = 328f, widestWord = 58f))
    }

    @Test
    fun `la cuadricula no deja una opcion sola en la ultima fila`() {
        // 4 opciones donde entrarían 3 columnas: 2 x 2, no 3 + 1.
        assertEquals(2, GridLayout.columns(available = 328f, minCell = 96f, count = 4))
        // 5 en 3 columnas: 3 + 2 está bien.
        assertEquals(3, GridLayout.columns(available = 328f, minCell = 96f, count = 5))
        // 10 monedas en 4 columnas: 4 + 4 + 2.
        assertEquals(4, GridLayout.columns(available = 400f, minCell = 96f, count = 10))
    }

    @Test
    fun `con mas de 5 opciones con icono la fila muestra 3 y Mas`() {
        assertEquals(4, TileLayout.shown(count = 4))
        assertEquals(5, TileLayout.shown(count = 5))
        // 13 categorías: 3 más usadas y el mosaico "Más…".
        assertEquals(3, TileLayout.shown(count = 13))
    }

    @Test
    fun `si la ultima fila quedaria con una sola opcion se reparten las filas`() {
        // 13 categorías en 3 columnas: 3 + 3 + 3 + 2 + 2, no 3 + 3 + 3 + 3 + 1.
        assertEquals(listOf(3, 3, 3, 2, 2), GridLayout.rows(count = 13, columns = 3))
        assertEquals(listOf(4, 4, 3, 2), GridLayout.rows(count = 13, columns = 4))
        assertEquals(listOf(4, 4, 2), GridLayout.rows(count = 10, columns = 4))
        assertEquals(listOf(2, 2), GridLayout.rows(count = 4, columns = 2))
    }

    @Test
    fun `en 2 columnas la ultima opcion sola ocupa la fila entera`() {
        // Letra 1,3: 13 categorías en 2 columnas. La fila de 1 se marca para ocupar todo el ancho.
        assertEquals(listOf(2, 2, 2, 2, 2, 2, 1), GridLayout.rows(count = 13, columns = 2))
        assertEquals(true, GridLayout.lastSpansRow(count = 13, columns = 2))
        assertEquals(false, GridLayout.lastSpansRow(count = 13, columns = 3))
        assertEquals(false, GridLayout.lastSpansRow(count = 12, columns = 2))
    }

    @Test
    fun `cada campo le da al teclado una tecla que hace algo`() {
        assertEquals(ImeKey.Done, ImeRule.of(hasAction = true, search = false))
        assertEquals(ImeKey.Search, ImeRule.of(hasAction = false, search = true))
        assertEquals(ImeKey.Next, ImeRule.of(hasAction = false, search = false))
    }

    @Test
    fun `la barra tiene cinco pestanas con Capturar al centro`() {
        assertEquals(
            listOf(NavTab.HOME, NavTab.REVIEW, NavTab.CAPTURE, NavTab.MOVEMENTS, NavTab.PROFILE),
            NavTabs.order,
        )
        assertEquals(NavTab.CAPTURE, NavTabs.order[NavTabs.order.size / 2])
        // Capturar abre la captura sin quedar elegida: las pestañas elegibles son las otras cuatro.
        assertEquals(false, NavTabs.selectable(NavTab.CAPTURE))
        assertEquals(true, NavTabs.selectable(NavTab.PROFILE))
    }
}
