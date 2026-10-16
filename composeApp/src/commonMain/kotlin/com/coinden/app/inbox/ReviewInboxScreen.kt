package com.coinden.app.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import coinden.composeapp.generated.resources.dialog_cancel
import coinden.composeapp.generated.resources.form_confirm
import coinden.composeapp.generated.resources.form_discard
import coinden.composeapp.generated.resources.inbox_balances
import coinden.composeapp.generated.resources.inbox_change_pending
import coinden.composeapp.generated.resources.inbox_change_pending_plain
import coinden.composeapp.generated.resources.inbox_confirm_all
import coinden.composeapp.generated.resources.inbox_empty
import coinden.composeapp.generated.resources.inbox_open
import coinden.composeapp.generated.resources.inbox_ready
import coinden.composeapp.generated.resources.inbox_review
import coinden.composeapp.generated.resources.inbox_shortfall
import coinden.composeapp.generated.resources.inbox_summary_title
import coinden.composeapp.generated.resources.inbox_summary_wallet
import coinden.composeapp.generated.resources.inbox_title
import coinden.composeapp.generated.resources.inbox_week
import com.coinden.app.capture.DraftCardView
import com.coinden.app.capture.FormMode
import com.coinden.app.capture.SameMovementPrompt
import com.coinden.app.capture.problemText
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.theme.CoinDenTheme
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * La bandeja de la noche: saldos si se confirma lo pendiente, "Para revisar" (se abre el
 * formulario) y "Listos" (se confirman de una vez tras ver el resumen).
 */
@Composable
fun ReviewInboxScreen(
    presenter: ReviewInboxPresenter,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    onOpen: (FormMode) -> Unit,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(presenter) { presenter.load() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(CoinDenIcon.CLOSE.vector, stringResource(Res.string.back)) }
            Text(stringResource(Res.string.inbox_title), style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isEmpty) {
                item {
                    Text(
                        stringResource(Res.string.inbox_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            state.week?.let { week ->
                item {
                    Text(
                        stringResource(Res.string.inbox_week, week.readyAlone, week.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.tertiary,
                    )
                }
            }
            if (state.balances.isNotEmpty()) {
                item {
                    Surface(shape = MaterialTheme.shapes.medium, color = scheme.surfaceContainerHigh) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(Res.string.inbox_balances), style = MaterialTheme.typography.titleSmall)
                            state.balances.forEach { b ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(b.walletName, modifier = Modifier.weight(1f))
                                    Text(
                                        MoneyFormatter.format(b.projected, locale),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (b.shortfall) scheme.error else scheme.onSurface,
                                    )
                                }
                                if (b.shortfall) {
                                    Text(
                                        stringResource(Res.string.inbox_shortfall),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (state.review.isNotEmpty()) {
                item { SectionTitle(stringResource(Res.string.inbox_review)) }
                items(state.review, key = { it.card.draftId }) { item ->
                    InboxCard(
                        item,
                        state,
                        locale,
                        utcOffsetMinutes,
                        onOpen = { presenter.formFor(item)?.let(onOpen) },
                        onConfirm = { scope.launch { presenter.confirm(item.card.draftId) } },
                        onDiscard = { scope.launch { presenter.discard(item.card.draftId) } },
                        onResolve = { other, same -> scope.launch { presenter.resolve(item.card.draftId, other, same)?.let(onOpen) } },
                    )
                }
            }
            if (state.ready.isNotEmpty()) {
                item { SectionTitle(stringResource(Res.string.inbox_ready)) }
                item {
                    Button(
                        onClick = { scope.launch { presenter.askConfirmReady() } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.large,
                    ) { Text(stringResource(Res.string.inbox_confirm_all, state.ready.size), style = MaterialTheme.typography.titleMedium) }
                }
                items(state.ready, key = { it.card.draftId }) { item ->
                    InboxCard(
                        item,
                        state,
                        locale,
                        utcOffsetMinutes,
                        onOpen = { presenter.formFor(item)?.let(onOpen) },
                        onConfirm = { scope.launch { presenter.confirm(item.card.draftId) } },
                        onDiscard = { scope.launch { presenter.discard(item.card.draftId) } },
                        onResolve = { _, _ -> },
                    )
                }
            }
        }
    }

    state.summary?.let { summary ->
        AlertDialog(
            onDismissRequest = presenter::cancelSummary,
            title = { Text(stringResource(Res.string.inbox_summary_title, summary.count)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    summary.byWallet.forEach { (wallet, total) ->
                        Text(stringResource(Res.string.inbox_summary_wallet, wallet, MoneyFormatter.format(total, locale)))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { scope.launch { presenter.confirmReady() } },
                ) { Text(stringResource(Res.string.form_confirm)) }
            },
            dismissButton = { TextButton(onClick = presenter::cancelSummary) { Text(stringResource(Res.string.dialog_cancel)) } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun InboxCard(
    item: InboxItem,
    state: InboxState,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    onOpen: () -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onResolve: (otherDraftId: String, same: Boolean) -> Unit,
) {
    val extra = CoinDenTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (item.correctionTarget != null) {
            Surface(shape = MaterialTheme.shapes.medium, color = extra.warningContainer, contentColor = extra.onWarningContainer) {
                Text(
                    item.targetNote?.let { stringResource(Res.string.inbox_change_pending, it) }
                        ?: stringResource(Res.string.inbox_change_pending_plain),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                )
            }
        }
        Column(Modifier.clickable(enabled = item.opensForm || item.correctionTarget != null, onClick = onOpen)) {
            DraftCardView(item.card, locale)
        }
        item.card.question?.takeIf { item.correctionTarget == null }?.let { question ->
            SameMovementPrompt(question, locale, utcOffsetMinutes) { same -> onResolve(question.otherDraftId, same) }
        }
        state.problems[item.card.draftId]?.let { Text(problemText(it, locale), color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.opensForm || item.correctionTarget != null) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(stringResource(Res.string.inbox_open)) }
            } else {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.form_discard))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(stringResource(Res.string.form_confirm)) }
            }
        }
    }
}
