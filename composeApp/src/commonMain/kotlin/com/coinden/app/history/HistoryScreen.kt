package com.coinden.app.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.category_none
import coinden.composeapp.generated.resources.field_reference
import coinden.composeapp.generated.resources.form_see_evidence
import coinden.composeapp.generated.resources.history_correct
import coinden.composeapp.generated.resources.history_corrected
import coinden.composeapp.generated.resources.history_empty
import coinden.composeapp.generated.resources.history_evidence
import coinden.composeapp.generated.resources.history_title
import coinden.composeapp.generated.resources.spending_category_missing
import com.coinden.app.capture.FormMode
import com.coinden.app.capture.MovementKindView
import com.coinden.app.capture.label
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.format.TimeFormatter
import com.coinden.app.theme.CoinDenTheme
import com.coinden.app.ui.LoadNotice
import com.coinden.app.ui.ScreenLoad
import com.coinden.app.ui.components.BadgeTone
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdBadge
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdTopBar
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Lo confirmado, del más reciente al más antiguo, con su evidencia y Corregir. */
@Composable
fun HistoryScreen(
    presenter: HistoryPresenter,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    onCorrect: (FormMode) -> Unit,
    onEvidence: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(presenter) { presenter.load() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CdTopBar(stringResource(Res.string.history_title), backLabel = stringResource(Res.string.back), onBack = onBack)
        LoadNotice(state.load, state.loaded, onRetry = { scope.launch { presenter.load() } })
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.load == ScreenLoad.READY && state.items.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            items(state.items, key = { it.draftId }) { item ->
                CdCard {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.note ?: stringResource(item.kind.label()),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            val sign = if (item.kind == MovementKindView.EXPENSE) "-" else ""
                            Text(sign + MoneyFormatter.format(item.amount, locale), style = CoinDenTheme.amounts.amount)
                        }
                        val details =
                            listOfNotNull(
                                TimeFormatter.dayMonthTime(item.occurredAtEpochMs, utcOffsetMinutes, locale),
                                item.walletName,
                                if (item.kind == MovementKindView.EXPENSE) categoryLabel(item.category, item.customCategoryName) else null,
                                item.reference?.let { "${stringResource(Res.string.field_reference)} $it" },
                            )
                        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        if (item.corrected) CdBadge(stringResource(Res.string.history_corrected), BadgeTone.Success)
                        val evidence =
                            if (item.evidenceCount > 1) {
                                stringResource(Res.string.history_evidence, item.evidenceCount)
                            } else {
                                stringResource(Res.string.form_see_evidence)
                            }
                        CdActions(
                            listOfNotNull(
                                CdActionSpec(evidence, { onEvidence(item.draftId) }),
                                presenter.correct(item)?.let { mode ->
                                    CdActionSpec(stringResource(Res.string.history_correct), { onCorrect(mode) })
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryLabel(
    category: CategoryRef?,
    customName: String?,
): String =
    when (category) {
        null -> stringResource(Res.string.category_none)
        is CategoryRef.Base -> stringResource(category.category.label())
        is CategoryRef.Custom -> customName ?: stringResource(Res.string.spending_category_missing)
    }
