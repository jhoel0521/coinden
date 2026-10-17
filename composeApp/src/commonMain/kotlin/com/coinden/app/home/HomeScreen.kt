package com.coinden.app.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import coinden.composeapp.generated.resources.home_default
import coinden.composeapp.generated.resources.home_history
import coinden.composeapp.generated.resources.home_language
import coinden.composeapp.generated.resources.home_pending
import coinden.composeapp.generated.resources.home_pending_note
import coinden.composeapp.generated.resources.home_privacy
import coinden.composeapp.generated.resources.home_review
import coinden.composeapp.generated.resources.home_total
import coinden.composeapp.generated.resources.home_unconverted
import coinden.composeapp.generated.resources.home_wallets
import coinden.composeapp.generated.resources.language_en
import coinden.composeapp.generated.resources.language_es
import coinden.composeapp.generated.resources.language_pt
import coinden.composeapp.generated.resources.logo_description
import coinden.composeapp.generated.resources.privacy_note
import coinden.composeapp.generated.resources.settings_title
import coinden.composeapp.generated.resources.spending_title
import com.coinden.app.capture.CaptureBar
import com.coinden.app.capture.CaptureNoticeCard
import com.coinden.app.capture.CapturePresenter
import com.coinden.app.capture.CaptureResultSheet
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.onboarding.label
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.theme.CoinDenTheme
import com.coinden.app.theme.CoinDenWordmark
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Inicio según el tablero "Inicio": patrimonio, borradores por revisar, billeteras y la barra de captura. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    presenter: HomePresenter,
    locale: UserLocale,
    language: String,
    onChooseLanguage: suspend (String) -> Unit,
    capture: CapturePresenter,
    onDictate: () -> Unit,
    onPhoto: () -> Unit,
    onWrite: () -> Unit,
    utcOffsetMinutes: Int,
    onPrivacy: () -> Unit = {},
    onVoiceModels: () -> Unit = {},
    onReview: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSpending: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val state by presenter.state.collectAsState()
    val captureState by capture.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    // Cada captura o confirmación cambia el resultado: Inicio se recarga (pendientes y saldos).
    LaunchedEffect(presenter, captureState.result) { presenter.load() }
    // Mientras Inicio está abierto, lo que la cola procesa actualiza la tarjeta del resultado.
    LaunchedEffect(capture) { capture.followProcessing() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { CoinDenWordmark(stringResource(Res.string.logo_description)) }
                    TextButton(onClick = onPrivacy, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(CoinDenIcon.SHIELD.vector, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(Res.string.home_privacy), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.large, color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(Res.string.home_total), style = MaterialTheme.typography.labelLarge)
                        Text(state.total?.let { MoneyFormatter.format(it, locale) }.orEmpty(), style = CoinDenTheme.amounts.amountLarge)
                        if (state.unconverted.isNotEmpty()) {
                            val apart = state.unconverted.joinToString(" · ") { MoneyFormatter.format(it, locale) }
                            Text(stringResource(Res.string.home_unconverted, apart), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.medium, color = scheme.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.home_pending, state.pendingDrafts), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(Res.string.home_pending_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onReview, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Text(stringResource(Res.string.home_review, state.pendingDrafts))
                            }
                            OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Text(stringResource(Res.string.home_history))
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onSpending, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.spending_title))
                }
            }
            item {
                Text(
                    stringResource(Res.string.home_wallets),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.settings_title))
                }
            }
            items(state.wallets, key = { it.id }) { wallet -> WalletRow(wallet, locale) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        CoinDenIcon.SHIELD.vector,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        stringResource(Res.string.privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(Res.string.home_language),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LANGUAGES.forEach { (code, name) ->
                            FilterChip(
                                selected = language == code,
                                onClick = { scope.launch { onChooseLanguage(code) } },
                                label = { Text(stringResource(name)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
            }
        }
        captureState.notice?.let { CaptureNoticeCard(it, onDismiss = capture::dismissNotice) }
        CaptureBar(listening = captureState.listening, onDictate = onDictate, onPhoto = onPhoto, onWrite = onWrite)
    }
    captureState.result?.let { result ->
        CaptureResultSheet(
            result,
            locale,
            utcOffsetMinutes,
            onConfirm = { draftId -> scope.launch { capture.confirm(draftId) } },
            onResolve = { draftId, same -> scope.launch { capture.resolve(draftId, same) } },
            onDone = capture::dismissResult,
            reading = captureState.reading,
            onVoiceModels = onVoiceModels,
        )
    }
}

@Composable
private fun WalletRow(
    wallet: HomeWallet,
    locale: UserLocale,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(44.dp).background(scheme.secondaryContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    wallet.type.icon().vector,
                    contentDescription = null,
                    tint = scheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(wallet.name, style = MaterialTheme.typography.titleMedium)
                val sub =
                    stringResource(wallet.type.label()) + if (wallet.isDefault) " · " + stringResource(Res.string.home_default) else ""
                Text(sub, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Text(MoneyFormatter.format(wallet.balance, locale), style = CoinDenTheme.amounts.amount)
        }
    }
    Spacer(Modifier.size(0.dp))
}

private fun WalletType.icon(): CoinDenIcon =
    when (this) {
        WalletType.CASH -> CoinDenIcon.CASH
        WalletType.BANK -> CoinDenIcon.BANK
        WalletType.DIGITAL -> CoinDenIcon.QR
        WalletType.CRYPTO -> CoinDenIcon.SPARKLE
        WalletType.CREDIT -> CoinDenIcon.CARD
    }

private val LANGUAGES = listOf("es" to Res.string.language_es, "en" to Res.string.language_en, "pt" to Res.string.language_pt)
