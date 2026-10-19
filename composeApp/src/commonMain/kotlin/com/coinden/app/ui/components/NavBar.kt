package com.coinden.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Lo que muestra una pestaña: su texto, su ícono y, si hay, cuántos pendientes. */
class CdNavItem(
    val tab: NavTab,
    val label: String,
    val icon: ImageVector,
    val badge: Int? = null,
)

/**
 * Barra de abajo con [NavTabs.order]. La elegida lleva un fondo, el ícono en el color principal y
 * la etiqueta en negrita (no solo color). Capturar, al centro, es un círculo destacado que no queda
 * elegido: abre la captura con [onCapture].
 */
@Composable
fun CdNavBar(
    items: List<CdNavItem>,
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().background(scheme.surface)) {
        HorizontalDivider(color = scheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            NavTabs.order.forEach { tab ->
                val item = items.firstOrNull { it.tab == tab } ?: return@forEach
                if (tab == NavTab.CAPTURE) {
                    CaptureTab(item, onCapture, Modifier.weight(1f))
                } else {
                    NavTabCell(item, tab == selected, Modifier.weight(1f)) { onSelect(tab) }
                }
            }
        }
    }
}

@Composable
private fun NavTabCell(
    item: CdNavItem,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.heightIn(min = 56.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .background(if (selected) scheme.secondaryContainer else scheme.surface, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            item.badge?.takeIf { it > 0 }?.let { count ->
                Box(
                    Modifier
                        .align(
                            Alignment.TopEnd,
                        ).offset(x = 4.dp, y = (-2).dp)
                        .background(scheme.error, CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (count > 99) "99+" else count.toString(), style = MaterialTheme.typography.labelSmall, color = scheme.onError)
                }
            }
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CaptureTab(
    item: CdNavItem,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.heightIn(min = 56.dp).selectable(selected = false, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        // Ocupa el mismo alto que los íconos de las otras pestañas (las etiquetas quedan alineadas)
        // y el círculo sobresale hacia arriba, destacado.
        Box(Modifier.height(32.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier.requiredSize(48.dp).offset(y = (-8).dp).background(scheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.icon, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(26.dp))
            }
        }
        Text(item.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = scheme.primary, maxLines = 1)
    }
}
