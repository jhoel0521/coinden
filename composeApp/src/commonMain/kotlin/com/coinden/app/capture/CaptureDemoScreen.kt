package com.coinden.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.amount_missing
import coinden.composeapp.generated.resources.app_title
import coinden.composeapp.generated.resources.capture_button
import coinden.composeapp.generated.resources.capture_empty
import coinden.composeapp.generated.resources.capture_hint
import coinden.composeapp.generated.resources.category_by_rule
import coinden.composeapp.generated.resources.category_by_word
import coinden.composeapp.generated.resources.category_custom
import coinden.composeapp.generated.resources.category_none
import coinden.composeapp.generated.resources.demo_notice
import coinden.composeapp.generated.resources.kind_unknown
import coinden.composeapp.generated.resources.wallet_missing
import coinden.composeapp.generated.resources.wallet_suggested
import com.coinden.app.format.MoneyFormatter
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.SuggestionReason
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Pantalla de demostración: escribir una frase, capturarla y ver el borrador que resulta. */
@Composable
fun CaptureDemoScreen(
    presenter: CaptureDemoPresenter,
    locale: UserLocale,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.app_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(Res.string.demo_notice), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.text,
                onValueChange = presenter::onTextChange,
                placeholder = { Text(stringResource(Res.string.capture_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { scope.launch { presenter.onCapture() } }) {
                Text(stringResource(Res.string.capture_button))
            }
        }
        if (state.cards.isEmpty()) {
            Text(stringResource(Res.string.capture_empty), style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.cards, key = { it.draftId }) { card -> DraftCardView(card, locale) }
        }
    }
}

@Composable
private fun DraftCardView(
    card: DraftCard,
    locale: UserLocale,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            card.evidenceText?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            Text(stringResource(card.status.label()), style = MaterialTheme.typography.titleSmall)
            if (card.status != DraftStatusView.CAPTURED) {
                Text(stringResource(card.kind?.label() ?: Res.string.kind_unknown))
                Text(card.amount?.let { MoneyFormatter.format(it, locale) } ?: stringResource(Res.string.amount_missing))
                Text(walletText(card))
                Text(categoryText(card.category))
            }
            Text(stringResource(card.confidence.label()), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun walletText(card: DraftCard): String {
    val name = card.walletName ?: return stringResource(Res.string.wallet_missing)
    return if (card.walletSuggested) stringResource(Res.string.wallet_suggested, name) else name
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
    }
}
