package com.coinden.app.capture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.amount_missing
import coinden.composeapp.generated.resources.category_by_rule
import coinden.composeapp.generated.resources.category_by_word
import coinden.composeapp.generated.resources.category_custom
import coinden.composeapp.generated.resources.category_none
import coinden.composeapp.generated.resources.draft_not_counted
import coinden.composeapp.generated.resources.evidence_audio
import coinden.composeapp.generated.resources.evidence_extra
import coinden.composeapp.generated.resources.evidence_file
import coinden.composeapp.generated.resources.evidence_quote
import coinden.composeapp.generated.resources.evidence_read
import coinden.composeapp.generated.resources.evidence_unread
import coinden.composeapp.generated.resources.field_category
import coinden.composeapp.generated.resources.field_method
import coinden.composeapp.generated.resources.field_reference
import coinden.composeapp.generated.resources.field_wallet
import coinden.composeapp.generated.resources.kind_unknown
import coinden.composeapp.generated.resources.method_card
import coinden.composeapp.generated.resources.method_cash
import coinden.composeapp.generated.resources.method_transfer
import coinden.composeapp.generated.resources.suggested_tag
import coinden.composeapp.generated.resources.suggestion_default
import coinden.composeapp.generated.resources.suggestion_only
import coinden.composeapp.generated.resources.suggestion_several
import coinden.composeapp.generated.resources.wallet_missing
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.theme.CoinDenTheme
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.SuggestionReason
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.WalletSuggestion
import com.coinden.core.domain.model.draft.WalletSuggestionReason
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.PaymentMethod
import org.jetbrains.compose.resources.stringResource

/** Tarjeta de un borrador, según el tablero "Tarjeta de borrador" del diseño. */
@Composable
fun DraftCardView(
    card: DraftCard,
    locale: UserLocale,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(card.status.label()), scheme.secondaryContainer, scheme.onSecondaryContainer)
                Spacer(Modifier.weight(1f))
                if (card.status != DraftStatusView.CAPTURED) {
                    Text(
                        stringResource(card.kind?.label() ?: Res.string.kind_unknown),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            card.evidenceText?.let {
                Text(
                    stringResource(Res.string.evidence_quote, it),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (card.extraEvidence > 0) {
                Text(
                    stringResource(Res.string.evidence_extra, card.extraEvidence),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            card.attachmentRef?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        CoinDenIcon.LOCK.vector,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    val stored =
                        stringResource(
                            if (card.evidenceSource ==
                                EvidenceSource.VOICE
                            ) {
                                Res.string.evidence_audio
                            } else {
                                Res.string.evidence_file
                            },
                        )
                    val unread = if (card.perceivedText == null) " " + stringResource(Res.string.evidence_unread) else ""
                    Text(stored + unread, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            card.perceivedText?.let {
                Text(
                    stringResource(Res.string.evidence_read),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.evidence_quote, it),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = scheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (card.status != DraftStatusView.CAPTURED) {
                Text(
                    card.amount?.let { MoneyFormatter.format(it, locale) } ?: stringResource(Res.string.amount_missing),
                    style = CoinDenTheme.amounts.amountLarge,
                    color = scheme.onSurface,
                )
                FieldRow(CoinDenIcon.WALLET, stringResource(Res.string.field_wallet)) {
                    val name = card.walletName ?: stringResource(Res.string.wallet_missing)
                    if (card.walletSuggested) SuggestedValue(name) else Value(name)
                }
                card.walletSuggestion?.let {
                    Text(
                        suggestionText(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 114.dp),
                    )
                }
                card.paymentMethod?.let { method ->
                    FieldRow(method.icon(), stringResource(Res.string.field_method)) { Value(stringResource(method.label())) }
                }
                FieldRow(CoinDenIcon.SPARKLE, stringResource(Res.string.field_category)) { Value(categoryText(card.category)) }
                card.reference?.let { reference ->
                    FieldRow(CoinDenIcon.HASH, stringResource(Res.string.field_reference)) { Value(reference) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfidenceChip(card)
            }
            if (card.status != DraftStatusView.CAPTURED) {
                Text(
                    stringResource(Res.string.draft_not_counted),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConfidenceChip(card: DraftCard) {
    val style = card.confidence.style()
    val extra = CoinDenTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) =
        when (style.tone) {
            Tone.SUCCESS -> extra.successContainer to extra.onSuccessContainer
            Tone.WARNING -> extra.warningContainer to extra.onWarningContainer
            Tone.ERROR -> scheme.errorContainer to scheme.onErrorContainer
        }
    Pill(stringResource(card.confidence.label()), bg, fg, style.icon)
}

@Composable
private fun Pill(
    text: String,
    background: Color,
    content: Color,
    icon: CoinDenIcon? = null,
) {
    Row(
        modifier =
            Modifier
                .heightIn(min = 32.dp)
                .background(background, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let { Icon(it.vector, contentDescription = null, tint = content, modifier = Modifier.size(16.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
private fun FieldRow(
    icon: CoinDenIcon,
    label: String,
    value: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon.vector, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.width(84.dp))
        value()
    }
}

@Composable
private fun Value(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
}

/** Lo sugerido no depende del color: borde punteado y la palabra "sugerida". */
@Composable
private fun SuggestedValue(text: String) {
    val scheme = MaterialTheme.colorScheme
    val dash = scheme.primary
    Row(
        modifier =
            Modifier
                .drawBehind {
                    drawRoundRect(
                        color = dash,
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                    )
                }.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        Text(stringResource(Res.string.suggested_tag), style = MaterialTheme.typography.labelMedium, color = scheme.primary)
    }
}

@Composable
private fun suggestionText(suggestion: WalletSuggestion): String =
    when (suggestion.reason) {
        WalletSuggestionReason.DEFAULT_WALLET -> stringResource(Res.string.suggestion_default)
        WalletSuggestionReason.ONLY_CANDIDATE -> stringResource(Res.string.suggestion_only)
        WalletSuggestionReason.RECOMMENDED_AMONG_SEVERAL -> stringResource(Res.string.suggestion_several, suggestion.candidates)
    }

@Composable
private fun categoryText(category: CategoryView?): String {
    if (category == null) return stringResource(Res.string.category_none)
    val name =
        when (val ref = category.ref) {
            is CategoryRef.Base -> stringResource(ref.category.label())
            is CategoryRef.Custom -> stringResource(Res.string.category_custom)
        }
    return when (val reason = category.reason) {
        is SuggestionReason.PackWord -> stringResource(Res.string.category_by_word, name, reason.word)
        is SuggestionReason.UserRule -> stringResource(Res.string.category_by_rule, name, reason.key)
        null -> name
    }
}

fun PaymentMethod.label() =
    when (this) {
        PaymentMethod.CASH -> Res.string.method_cash
        PaymentMethod.TRANSFER -> Res.string.method_transfer
        PaymentMethod.CARD -> Res.string.method_card
    }
