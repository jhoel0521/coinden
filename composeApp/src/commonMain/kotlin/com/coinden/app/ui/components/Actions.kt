package com.coinden.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Papel de una acción. Principal: la que avanza (una por grupo). Secundaria: tonal. Terciaria:
 * solo texto. Peligro: borra o descarta; usa contorno de error y no depende solo del color.
 */
enum class ActionRole { Primary, Secondary, Tertiary, Danger }

/** Una acción como dato, para que un grupo decida cómo acomodarla con sus hermanas. */
data class CdActionSpec(
    val text: String,
    val onClick: () -> Unit,
    val role: ActionRole = ActionRole.Secondary,
    val enabled: Boolean = true,
    val icon: ImageVector? = null,
)

/** Una acción de CoinDen: altura mínima de 48 dp (56 si es principal) y forma fija. */
@Composable
fun CdAction(
    spec: CdActionSpec,
    modifier: Modifier = Modifier,
) {
    val height = if (spec.role == ActionRole.Primary) 56.dp else 48.dp
    val shaped = modifier.heightIn(min = height)
    val shape = MaterialTheme.shapes.extraLarge
    val content: @Composable () -> Unit = {
        spec.icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" ")
        }
        Text(
            spec.text,
            style = if (spec.role == ActionRole.Primary) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    when (spec.role) {
        ActionRole.Primary -> Button(spec.onClick, shaped, spec.enabled, shape = shape) { content() }
        ActionRole.Secondary -> FilledTonalButton(spec.onClick, shaped, spec.enabled, shape = shape) { content() }
        ActionRole.Tertiary -> TextButton(spec.onClick, shaped, spec.enabled, shape = shape) { content() }
        ActionRole.Danger ->
            OutlinedButton(
                spec.onClick,
                shaped,
                spec.enabled,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) { content() }
    }
}

/** Atajo para una acción suelta. */
@Composable
fun CdAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: ActionRole = ActionRole.Secondary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) = CdAction(CdActionSpec(text, onClick, role, enabled, icon), modifier)

/**
 * Grupo de acciones hermanas: una ocupa todo el ancho; dos que caben, lado a lado y del mismo
 * ancho; si no caben, o son tres o más, se apilan con la principal al final ([ActionsLayout]).
 */
@Composable
fun CdActions(
    actions: List<CdActionSpec>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Lo que necesita cada acción: su texto, su ícono y el relleno del botón.
        val needed =
            with(density) {
                actions
                    .sumOf { a ->
                        val text =
                            measurer
                                .measure(a.text, style)
                                .size.width
                                .toDp()
                                .value
                        (text + 48f + (if (a.icon != null) 26f else 0f)).toDouble()
                    }.toFloat() + 8f * (actions.size - 1)
            }
        when (ActionsLayout.of(actions.size, fitsInRow = needed <= maxWidth.value)) {
            ActionsArrangement.Full -> CdAction(actions.single(), Modifier.fillMaxWidth())
            ActionsArrangement.Row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { CdAction(it, Modifier.weight(1f)) }
                }
            ActionsArrangement.Stack ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // La principal va al final, donde termina la lectura.
                    (actions.filter { it.role != ActionRole.Primary } + actions.filter { it.role == ActionRole.Primary })
                        .forEach { CdAction(it, Modifier.fillMaxWidth()) }
                }
        }
    }
}

/** Botón de ícono con su descripción obligatoria, de 48 dp. */
@Composable
fun CdIconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) { Icon(icon, contentDescription = label) }
}

/** Una acción de texto en una cabecera (por ejemplo "Billeteras y tasas" junto a su título). */
@Composable
fun CdHeaderAction(
    title: String,
    action: CdActionSpec?,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(end = 8.dp))
        action?.let { CdAction(it.copy(role = ActionRole.Tertiary)) }
    }
}
