package com.coinden.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Una opción de un grupo: su valor, su texto y, si hay, una marca ("sugerida"). */
data class CdOption<T>(
    val value: T,
    val label: String,
    val mark: String? = null,
)

/**
 * Elección única. Decide su forma con [ChoiceLayout]: segmentados si son pocas y caben; una fila
 * deslizable si la lista es abierta o no cabe; y, si son muchas, las más usadas ([usage]) con
 * "Más…" ([moreLabel]), que abre todas en una cuadrícula. Nunca deja una opción sola en una fila.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CdChoice(
    options: List<CdOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    open: Boolean = false,
    usage: Map<T, Int> = emptyMap(),
    moreLabel: String? = null,
    enabled: Boolean = true,
) {
    if (options.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    var showAll by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val labelsWidth =
            with(density) {
                options
                    .sumOf {
                        (
                            measurer
                                .measure(it.label, style)
                                .size.width
                                .toDp()
                                .value + 40f
                        ).toDouble()
                    }.toFloat()
            }
        val arrangement =
            if (moreLabel == null && !open && options.size > 8) {
                ChoiceArrangement.ScrollRow
            } else {
                ChoiceLayout.of(options.size, open, labelsWidth, maxWidth.value)
            }
        when (arrangement) {
            ChoiceArrangement.Segmented ->
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option.value == selected,
                            onClick = { onSelect(option.value) },
                            enabled = enabled,
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            label = { Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            ChoiceArrangement.ScrollRow -> ScrollRow(options, selected, onSelect, enabled)
            is ChoiceArrangement.TopAndMore -> {
                val values = options.map { it.value }
                val top = FrequentChoices.top(values, usage, selected, arrangement.count)
                val shown = top.mapNotNull { v -> options.firstOrNull { it.value == v } }
                ScrollRow(shown, selected, onSelect, enabled, more = moreLabel?.let { label -> label to { showAll = true } })
            }
        }
    }
    if (showAll) {
        CdSheet(onDismiss = { showAll = false }, title = moreLabel) {
            CdChoiceGrid(options, selected, onSelect = {
                onSelect(it)
                showAll = false
            })
        }
    }
}

/** Una fila que se desliza; al aparecer deja la opción elegida a la vista. */
@Composable
private fun <T> ScrollRow(
    options: List<CdOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    enabled: Boolean,
    more: Pair<String, () -> Unit>? = null,
) {
    val state = rememberLazyListState()
    val index = options.indexOfFirst { it.value == selected }
    LaunchedEffect(index) { if (index > 0) state.animateScrollToItem(index) }
    LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(options, key = { i, o -> "$i-${o.value}" }) { _, option ->
            Pill(option, option.value == selected, Modifier, enabled = enabled) { onSelect(option.value) }
        }
        more?.let { (label, open) ->
            item(key = "more") { Pill(CdOption(Unit, label), selected = false, Modifier, emphasized = true, enabled = enabled) { open() } }
        }
    }
}

/**
 * Cuadrícula de columnas fijas e iguales ([GridLayout]): cada opción mide lo mismo y la última
 * fila se alinea con las demás, sin opciones estiradas ni sueltas.
 */
@Composable
fun <T> CdChoiceGrid(
    options: List<CdOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    minCell: Float = 96f,
) {
    val widest = widestLabel(options)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = GridLayout.columns(maxWidth.value, minCell, widest)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option -> Pill(option, option.value == selected, Modifier.weight(1f)) { onSelect(option.value) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Elección múltiple en una cuadrícula de columnas fijas (por ejemplo, las monedas que se usan). */
@Composable
fun <T> CdMultiChoiceGrid(
    options: List<CdOption<T>>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
    minCell: Float = 72f,
) {
    val widest = widestLabel(options)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = GridLayout.columns(maxWidth.value, minCell, widest)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option -> Pill(option, option.value in selected, Modifier.weight(1f)) { onToggle(option.value) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Ancho en dp de la etiqueta más larga, en negrita (como se ve elegida). */
@Composable
private fun <T> widestLabel(options: List<CdOption<T>>): Float {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val density = LocalDensity.current
    return with(density) {
        options.maxOfOrNull {
            measurer
                .measure(it.label, style)
                .size.width
                .toDp()
                .value
        } ?: 0f
    }
}

/** Una opción de una lista (por ejemplo, un resultado de búsqueda): fila de 48 dp a todo el ancho. */
@Composable
fun CdListOption(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = false, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** La pastilla de una opción: elegida con relleno, si no con borde; 48 dp de alto. */
@Composable
private fun <T> Pill(
    option: CdOption<T>,
    selected: Boolean,
    modifier: Modifier,
    emphasized: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier =
            modifier
                .heightIn(
                    min = 48.dp,
                ).selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) scheme.secondaryContainer else scheme.surface,
        contentColor =
            if (selected) {
                scheme.onSecondaryContainer
            } else if (emphasized) {
                scheme.primary
            } else {
                scheme.onSurface
            },
        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                option.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected || emphasized) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            option.mark?.let { Text("  $it", style = MaterialTheme.typography.labelSmall, color = scheme.tertiary) }
        }
    }
}
