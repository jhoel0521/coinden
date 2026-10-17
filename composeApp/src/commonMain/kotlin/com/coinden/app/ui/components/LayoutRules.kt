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
    fun columns(
        available: Float,
        minCell: Float,
    ): Int = (available / minCell).toInt().coerceIn(2, 4)
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
