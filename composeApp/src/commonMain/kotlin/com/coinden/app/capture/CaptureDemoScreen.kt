package com.coinden.app.capture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.capture_button
import coinden.composeapp.generated.resources.capture_empty
import coinden.composeapp.generated.resources.capture_field_label
import coinden.composeapp.generated.resources.capture_hint
import coinden.composeapp.generated.resources.capture_subtitle
import coinden.composeapp.generated.resources.capture_title
import coinden.composeapp.generated.resources.drafts_title
import coinden.composeapp.generated.resources.logo_description
import coinden.composeapp.generated.resources.privacy_badge
import coinden.composeapp.generated.resources.privacy_note
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.theme.CoinDenWordmark
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Demostración de captura con el estilo del tablero "Captura rápida": escribir, capturar y ver el borrador. */
@Composable
fun CaptureDemoScreen(
    presenter: CaptureDemoPresenter,
    locale: UserLocale,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scheme = MaterialTheme.colorScheme

    // El borrador nuevo queda arriba: la lista vuelve al inicio al capturar.
    LaunchedEffect(state.cards.firstOrNull()?.draftId) { listState.animateScrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinDenWordmark(stringResource(Res.string.logo_description))
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Icon(
                        CoinDenIcon.LOCK.vector,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(Res.string.privacy_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(Res.string.capture_title), style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
                Text(
                    stringResource(Res.string.capture_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.text,
                    onValueChange = presenter::onTextChange,
                    label = { Text(stringResource(Res.string.capture_field_label)) },
                    placeholder = { Text(stringResource(Res.string.capture_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    textStyle = MaterialTheme.typography.titleMedium,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = scheme.surfaceContainerLowest,
                            focusedContainerColor = scheme.surfaceContainerLowest,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { scope.launch { presenter.onCapture() } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
                ) {
                    Icon(CoinDenIcon.PLUS.vector, contentDescription = null, modifier = Modifier.size(22.dp))
                    Text(
                        stringResource(Res.string.capture_button),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        item {
            Text(
                stringResource(if (state.cards.isEmpty()) Res.string.capture_empty else Res.string.drafts_title),
                style = if (state.cards.isEmpty()) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleLarge,
                color = if (state.cards.isEmpty()) scheme.onSurfaceVariant else scheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        items(state.cards, key = { it.draftId }) { card -> DraftCardView(card, locale) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(CoinDenIcon.SHIELD.vector, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Text(stringResource(Res.string.privacy_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}
