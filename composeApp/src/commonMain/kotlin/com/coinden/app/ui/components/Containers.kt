package com.coinden.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.theme.CoinDenTheme

/**
 * Dónde está un componente. Los contenedores lo informan a sus hijos: dentro de una tarjeta,
 * una sección, una hoja o un diálogo, los hijos usan el ancho de su padre sin margen propio.
 */
enum class CdContainer { Screen, Card, Section, Sheet, Dialog }

val LocalCdContainer = staticCompositionLocalOf { CdContainer.Screen }

/** Tono de una tarjeta. */
enum class CardTone { Plain, Raised, Accent, Warning, Error }

/** Tarjeta de CoinDen: forma, color y margen interior fijos; sus hijos saben que están dentro. */
@Composable
fun CdCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Plain,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val extra = CoinDenTheme.colors
    val (color, onColor) =
        when (tone) {
            CardTone.Plain -> scheme.surfaceContainerLowest to scheme.onSurface
            CardTone.Raised -> scheme.surfaceContainerHigh to scheme.onSurface
            CardTone.Accent -> scheme.primaryContainer to scheme.onPrimaryContainer
            CardTone.Warning -> extra.warningContainer to extra.onWarningContainer
            CardTone.Error -> scheme.errorContainer to scheme.onErrorContainer
        }
    val border = if (tone == CardTone.Plain) BorderStroke(1.dp, scheme.outlineVariant) else null
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color, contentColor = onColor, border = border) {
        CompositionLocalProvider(LocalCdContainer provides CdContainer.Card) {
            Column(clickable.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

/**
 * Sección: título, contenido y, si hay, el valor anterior ("antes: …") y el error, siempre
 * juntos. Así un aviso nunca queda lejos del grupo al que pertenece.
 */
@Composable
fun CdSection(
    title: String?,
    modifier: Modifier = Modifier,
    before: String? = null,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
        }
        CompositionLocalProvider(LocalCdContainer provides CdContainer.Section) { content() }
        before?.let { CdChangedText(it) }
        error?.let { CdErrorText(it) }
    }
}

/** El valor anterior de un campo que cambia: "antes: 35,00 Bs". */
@Composable
fun CdChangedText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
}

/** Un error en texto, junto a lo que lo causa. */
@Composable
fun CdErrorText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Tipo de aviso. */
enum class NoticeKind { Info, Success, Warning, Error }

/** Aviso con su ícono y, si hay, una acción; se anuncia en los lectores de pantalla. */
@Composable
fun CdNotice(
    kind: NoticeKind,
    text: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    action: CdActionSpec? = null,
) {
    val tone =
        when (kind) {
            NoticeKind.Info -> CardTone.Raised
            NoticeKind.Success -> CardTone.Accent
            NoticeKind.Warning -> CardTone.Warning
            NoticeKind.Error -> CardTone.Error
        }
    val icon =
        when (kind) {
            NoticeKind.Info -> CoinDenIcon.SPARKLE
            NoticeKind.Success -> CoinDenIcon.CHECK_CIRCLE
            NoticeKind.Warning, NoticeKind.Error -> CoinDenIcon.ALERT
        }
    CdCard(modifier.semantics { liveRegion = LiveRegionMode.Polite }, tone = tone) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon.vector, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        action?.let { CdActions(listOf(it)) }
    }
}

/** Diálogo de CoinDen: título, texto y sus acciones (según Material, abajo a la derecha). */
@Composable
fun CdDialog(
    title: String,
    onDismiss: () -> Unit,
    confirm: CdActionSpec,
    dismiss: CdActionSpec? = null,
    text: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            CompositionLocalProvider(LocalCdContainer provides CdContainer.Dialog) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    text?.let { Text(it) }
                    content?.invoke(this)
                }
            }
        },
        confirmButton = {
            CdAction(
                confirm.copy(
                    role =
                        if (confirm.role ==
                            ActionRole.Danger
                        ) {
                            ActionRole.Danger
                        } else {
                            ActionRole.Tertiary
                        },
                ),
            )
        },
        dismissButton = dismiss?.let { { CdAction(it.copy(role = ActionRole.Tertiary)) } },
    )
}

/** Fila con interruptor: toda la fila se toca; el título y el texto explican qué hace. */
@Composable
fun CdSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            text?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Bloque plegable ("Más detalles"): se abre al tocarlo o si ya trae algo. */
@Composable
fun CdCollapsible(
    title: String,
    initiallyOpen: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(initiallyOpen) { mutableStateOf(initiallyOpen) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(if (open) "−" else "+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (open) CompositionLocalProvider(LocalCdContainer provides CdContainer.Section) { content() }
    }
}

/** Hoja inferior de CoinDen; sus hijos saben que están dentro de una hoja. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CdSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        CompositionLocalProvider(LocalCdContainer provides CdContainer.Sheet) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                title?.let { Text(it, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
                content()
            }
        }
    }
}

/** Tono de una insignia. */
enum class BadgeTone { Neutral, Success, Warning, Error }

/** Insignia no interactiva (un motivo, un estado): texto corto con fondo suave, sin depender solo del color. */
@Composable
fun CdBadge(
    text: String,
    tone: BadgeTone = BadgeTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val extra = CoinDenTheme.colors
    val (bg, fg) =
        when (tone) {
            BadgeTone.Neutral -> scheme.secondaryContainer to scheme.onSecondaryContainer
            BadgeTone.Success -> extra.successContainer to extra.onSuccessContainer
            BadgeTone.Warning -> extra.warningContainer to extra.onWarningContainer
            BadgeTone.Error -> scheme.errorContainer to scheme.onErrorContainer
        }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        maxLines = 1,
        modifier = modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
