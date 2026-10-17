package com.coinden.app.ui.components

/** Cómo se acomoda un grupo de acciones según cuántas son y si caben en una fila. */
enum class ActionsArrangement { Full, Row, Stack }

/** Un grupo de acciones: 1 a todo el ancho; 2 que caben, lado a lado; si no, apiladas. */
object ActionsLayout {
    fun of(
        count: Int,
        fitsInRow: Boolean,
    ): ActionsArrangement =
        when {
            count <= 1 -> ActionsArrangement.Full
            count == 2 && fitsInRow -> ActionsArrangement.Row
            else -> ActionsArrangement.Stack
        }
}

/** Forma de un grupo de elección única. */
sealed interface ChoiceArrangement {
    /** Una sola fila de segmentos del mismo ancho (como un `btn-group`). */
    data object Segmented : ChoiceArrangement

    /** Una sola fila que se desliza, con la elegida a la vista. */
    data object ScrollRow : ChoiceArrangement

    /** Las [count] más usadas y "Más…", que abre todas en una cuadrícula. */
    data class TopAndMore(
        val count: Int,
    ) : ChoiceArrangement
}

/**
 * Elige la forma de un grupo de elección única. [open]: la lista crece con los datos de la
 * persona (billeteras, monedas). [labelsWidth]: lo que ocupan juntos los textos con su relleno,
 * medido con la letra actual. Nunca deja una opción sola en una fila incompleta.
 */
object ChoiceLayout {
    private const val MAX_SEGMENTS = 4
    private const val MAX_SCROLL_FIXED = 8
    private const val TOP = 4

    fun of(
        count: Int,
        open: Boolean,
        labelsWidth: Float,
        available: Float,
    ): ChoiceArrangement =
        when {
            open -> ChoiceArrangement.ScrollRow
            count > MAX_SCROLL_FIXED -> ChoiceArrangement.TopAndMore(TOP)
            count <= MAX_SEGMENTS && labelsWidth <= available -> ChoiceArrangement.Segmented
            else -> ChoiceArrangement.ScrollRow
        }
}

/** Columnas fijas e iguales de una cuadrícula de opciones: entre 2 y 4. */
object GridLayout {
    /** Relleno de una celda alrededor de su etiqueta (16 dp por lado y un margen). */
    private const val CELL_PADDING = 40f

    /**
     * Columnas iguales en las que entra la etiqueta más larga entera ([widestLabel], en dp). Si se
     * conoce la cantidad de opciones ([count]), usa menos columnas cuando así la última fila no
     * queda con una opción sola (4 opciones: 2 x 2, no 3 + 1).
     */
    fun columns(
        available: Float,
        minCell: Float,
        widestLabel: Float = 0f,
        count: Int = 0,
    ): Int {
        val most = (available / maxOf(minCell, widestLabel + CELL_PADDING)).toInt().coerceIn(2, 4)
        return (most downTo 2).firstOrNull { count % it != 1 } ?: most
    }
}

/** Las opciones más usadas primero (a igual uso, el orden base) y la elegida siempre visible. */
object FrequentChoices {
    fun <T> top(
        all: List<T>,
        usage: Map<T, Int>,
        selected: T?,
        count: Int,
    ): List<T> {
        val ranked =
            all
                .withIndex()
                .sortedWith(
                    compareByDescending<IndexedValue<T>> { usage[it.value] ?: 0 }.thenBy { it.index },
                ).map { it.value }
        val top = ranked.take(count)
        return if (selected == null || selected in top) top else listOf(selected) + top.take(count - 1)
    }
}

/** Cómo se acomodan los mosaicos con ícono. */
sealed interface TileArrangement {
    /** Todos en una fila, del mismo ancho. */
    data object Row : TileArrangement

    /** En columnas iguales, cuando en una fila no le quedan [TileLayout.MIN_TILE] dp a cada uno. */
    data class Grid(
        val columns: Int,
    ) : TileArrangement
}

object TileLayout {
    /** Ancho mínimo de un mosaico: ícono, etiqueta de 2 líneas y relleno. */
    const val MIN_TILE = 64f

    /** Ancho mínimo de una celda cuando los mosaicos pasan a cuadrícula. */
    private const val GRID_CELL = 80f

    /** Relleno lateral de un mosaico (6 dp por lado) y su borde (2 dp por lado). */
    private const val TILE_PADDING = 16f

    /** Separación entre mosaicos. */
    private const val GAP = 8f

    /**
     * Fila si a cada mosaico le quedan [MIN_TILE] dp y su palabra más larga ([widestWord], en dp)
     * entra sin partirse; si no, cuadrícula de columnas iguales.
     */
    fun of(
        count: Int,
        available: Float,
        widestWord: Float = 0f,
    ): TileArrangement {
        val tile = maxOf(MIN_TILE, widestWord + TILE_PADDING)
        return if ((available - GAP * (count - 1)) / count >= tile) {
            TileArrangement.Row
        } else {
            TileArrangement.Grid(GridLayout.columns(available, maxOf(GRID_CELL, tile), count = count))
        }
    }
}
