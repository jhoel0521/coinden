package com.coinden.app.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.dialog_cancel
import coinden.composeapp.generated.resources.evidence_file
import coinden.composeapp.generated.resources.form_confirm
import coinden.composeapp.generated.resources.form_discard
import coinden.composeapp.generated.resources.inbox_balances
import coinden.composeapp.generated.resources.inbox_change_pending
import coinden.composeapp.generated.resources.inbox_change_pending_plain
import coinden.composeapp.generated.resources.inbox_confirm_all
import coinden.composeapp.generated.resources.inbox_empty
import coinden.composeapp.generated.resources.inbox_ready
import coinden.composeapp.generated.resources.inbox_reason_assigned_by_order
import coinden.composeapp.generated.resources.inbox_reason_check_data
import coinden.composeapp.generated.resources.inbox_reason_context_for_several
import coinden.composeapp.generated.resources.inbox_reason_contradiction
import coinden.composeapp.generated.resources.inbox_reason_missing_amount
import coinden.composeapp.generated.resources.inbox_reason_missing_wallet
import coinden.composeapp.generated.resources.inbox_reason_model_disagrees
import coinden.composeapp.generated.resources.inbox_reason_not_movement
import coinden.composeapp.generated.resources.inbox_reason_pending_change
import coinden.composeapp.generated.resources.inbox_reason_same_question
import coinden.composeapp.generated.resources.inbox_reason_suggested_wallet
import coinden.composeapp.generated.resources.inbox_reason_unread
import coinden.composeapp.generated.resources.inbox_reason_unverified
import coinden.composeapp.generated.resources.inbox_reason_without_receipt
import coinden.composeapp.generated.resources.inbox_review
import coinden.composeapp.generated.resources.inbox_shortfall
import coinden.composeapp.generated.resources.inbox_summary_title
import coinden.composeapp.generated.resources.inbox_summary_wallet
import coinden.composeapp.generated.resources.inbox_title
import coinden.composeapp.generated.resources.inbox_week
import coinden.composeapp.generated.resources.suggested_tag
import coinden.composeapp.generated.resources.wallet_missing
import com.coinden.app.capture.FormMode
import com.coinden.app.capture.SameMovementPrompt
import com.coinden.app.capture.problemText
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.theme.CoinDenTheme
import com.coinden.app.ui.LoadNotice
import com.coinden.app.ui.ScreenLoad
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.BadgeTone
import com.coinden.app.ui.components.CardTone
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdBadge
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdDialog
import com.coinden.app.ui.components.CdErrorText
import com.coinden.app.ui.components.CdTopBar
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
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
    onBack: (() -> Unit)? = null,
    reload: Any? = null,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    // Como en Inicio: lo confirmado desde la hoja de captura o lo compartido sale o entra de la lista.
    LaunchedEffect(presenter, reload) { presenter.load() }

    val ready = state.load == ScreenLoad.READY
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CdTopBar(stringResource(Res.string.inbox_title), backLabel = stringResource(Res.string.back), onBack = onBack)
        LoadNotice(state.load, state.loaded, onRetry = { scope.launch { presenter.load() } })
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    CdCard(tone = CardTone.Raised) {
                        Text(stringResource(Res.string.inbox_balances), style = MaterialTheme.typography.titleSmall)
                        state.balances.forEach { b ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(b.walletName, modifier = Modifier.weight(1f))
                                Text(
                                    MoneyFormatter.format(b.projected, locale),
                                    style = CoinDenTheme.amounts.amount,
                                    color = if (b.shortfall) scheme.error else scheme.onSurface,
                                )
                            }
                            if (b.shortfall) CdErrorText(stringResource(Res.string.inbox_shortfall))
                        }
                    }
                }
            }
            if (state.review.isNotEmpty()) {
                item { SectionTitle(stringResource(Res.string.inbox_review)) }
                items(state.review, key = { it.card.draftId }) { item ->
                    InboxRow(
                        item,
                        state,
                        locale,
                        utcOffsetMinutes,
                        enabled = ready,
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
                    CdActions(
                        listOf(
                            CdActionSpec(
                                pluralStringResource(Res.plurals.inbox_confirm_all, state.ready.size, state.ready.size),
                                { scope.launch { presenter.askConfirmReady() } },
                                ActionRole.Primary,
                                enabled = ready,
                            ),
                        ),
                    )
                }
                items(state.ready, key = { it.card.draftId }) { item ->
                    InboxRow(
                        item,
                        state,
                        locale,
                        utcOffsetMinutes,
                        enabled = ready,
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
        CdDialog(
            title = stringResource(Res.string.inbox_summary_title, summary.count),
            onDismiss = presenter::cancelSummary,
            confirm = CdActionSpec(stringResource(Res.string.form_confirm), { scope.launch { presenter.confirmReady() } }),
            dismiss = CdActionSpec(stringResource(Res.string.dialog_cancel), presenter::cancelSummary),
        ) {
            summary.byWallet.forEach { (wallet, total) ->
                Text(stringResource(Res.string.inbox_summary_wallet, wallet, MoneyFormatter.format(total, locale)))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

/**
 * Un pendiente en dos líneas: la nota (o lo capturado) y el monto; debajo la billetera y, en
 * "Para revisar", el motivo. Tocarla abre el formulario; el detalle completo está ahí.
 */
@Composable
private fun InboxRow(
    item: InboxItem,
    state: InboxState,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    enabled: Boolean,
    onOpen: () -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onResolve: (otherDraftId: String, same: Boolean) -> Unit,
) {
    val card = item.card
    val scheme = MaterialTheme.colorScheme
    val opens = enabled && (item.opensForm || item.correctionTarget != null)
    CdCard(onClick = if (opens) onOpen else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                card.note ?: card.evidenceText ?: card.perceivedText ?: stringResource(Res.string.evidence_file),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            card.amount?.let { Text(MoneyFormatter.format(it, locale), style = CoinDenTheme.amounts.amount) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val wallet = card.walletName ?: stringResource(Res.string.wallet_missing)
            val suggested = if (card.walletSuggested) " · " + stringResource(Res.string.suggested_tag) else ""
            Text(
                wallet + suggested,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            item.reason?.let { CdBadge(stringResource(it.label()), BadgeTone.Warning) }
        }
        if (item.correctionTarget != null) {
            Text(
                item.targetNote?.let { stringResource(Res.string.inbox_change_pending, it) }
                    ?: stringResource(Res.string.inbox_change_pending_plain),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        card.question?.takeIf { item.correctionTarget == null }?.let { question ->
            SameMovementPrompt(question, locale, utcOffsetMinutes) { same -> onResolve(question.otherDraftId, same) }
        }
        state.problems[card.draftId]?.let { CdErrorText(problemText(it, locale)) }
        if (!item.opensForm && item.correctionTarget == null) {
            CdActions(
                listOf(
                    CdActionSpec(stringResource(Res.string.form_discard), onDiscard, ActionRole.Danger, enabled),
                    CdActionSpec(stringResource(Res.string.form_confirm), onConfirm, ActionRole.Primary, enabled),
                ),
            )
        }
    }
}

private fun InboxReason.label(): StringResource =
    when (this) {
        InboxReason.PENDING_CHANGE -> Res.string.inbox_reason_pending_change
        InboxReason.SAME_QUESTION -> Res.string.inbox_reason_same_question
        InboxReason.UNREAD -> Res.string.inbox_reason_unread
        InboxReason.NOT_A_MOVEMENT -> Res.string.inbox_reason_not_movement
        InboxReason.MISSING_AMOUNT -> Res.string.inbox_reason_missing_amount
        InboxReason.MISSING_WALLET -> Res.string.inbox_reason_missing_wallet
        InboxReason.SUGGESTED_WALLET -> Res.string.inbox_reason_suggested_wallet
        InboxReason.CHECK_DATA -> Res.string.inbox_reason_check_data
        InboxReason.CONTRADICTION -> Res.string.inbox_reason_contradiction
        InboxReason.MODEL_DISAGREES -> Res.string.inbox_reason_model_disagrees
        InboxReason.UNVERIFIED -> Res.string.inbox_reason_unverified
        InboxReason.ASSIGNED_BY_ORDER -> Res.string.inbox_reason_assigned_by_order
        InboxReason.CONTEXT_FOR_SEVERAL -> Res.string.inbox_reason_context_for_several
        InboxReason.WITHOUT_RECEIPT -> Res.string.inbox_reason_without_receipt
    }
