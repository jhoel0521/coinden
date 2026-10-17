package com.coinden.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.dialog_cancel
import coinden.composeapp.generated.resources.error_invalid_wallet
import coinden.composeapp.generated.resources.home_default
import coinden.composeapp.generated.resources.rate_label
import coinden.composeapp.generated.resources.rate_later_set
import coinden.composeapp.generated.resources.rates_note
import coinden.composeapp.generated.resources.rates_title
import coinden.composeapp.generated.resources.settings_add_wallet
import coinden.composeapp.generated.resources.settings_archive
import coinden.composeapp.generated.resources.settings_archive_action
import coinden.composeapp.generated.resources.settings_archive_note
import coinden.composeapp.generated.resources.settings_archive_title
import coinden.composeapp.generated.resources.settings_archived
import coinden.composeapp.generated.resources.settings_currency
import coinden.composeapp.generated.resources.settings_currency_add
import coinden.composeapp.generated.resources.settings_currency_error
import coinden.composeapp.generated.resources.settings_default
import coinden.composeapp.generated.resources.settings_last_wallet
import coinden.composeapp.generated.resources.settings_loading
import coinden.composeapp.generated.resources.settings_not_found
import coinden.composeapp.generated.resources.settings_quote_overflow
import coinden.composeapp.generated.resources.settings_rate_current
import coinden.composeapp.generated.resources.settings_rate_error
import coinden.composeapp.generated.resources.settings_rate_remove
import coinden.composeapp.generated.resources.settings_reactivate
import coinden.composeapp.generated.resources.settings_retry
import coinden.composeapp.generated.resources.settings_save
import coinden.composeapp.generated.resources.settings_saved
import coinden.composeapp.generated.resources.settings_storage_error
import coinden.composeapp.generated.resources.settings_title
import coinden.composeapp.generated.resources.wallet_add
import coinden.composeapp.generated.resources.wallet_name
import coinden.composeapp.generated.resources.wallet_opening
import com.coinden.app.AppContainer
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.onboarding.label
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.BadgeTone
import com.coinden.app.ui.components.CardTone
import com.coinden.app.ui.components.CdAction
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdBadge
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdChoice
import com.coinden.app.ui.components.CdChoiceGrid
import com.coinden.app.ui.components.CdDialog
import com.coinden.app.ui.components.CdField
import com.coinden.app.ui.components.CdNotice
import com.coinden.app.ui.components.CdOption
import com.coinden.app.ui.components.CdTopBar
import com.coinden.app.ui.components.NoticeKind
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.currentRate
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun WalletSettingsScreen(
    presenter: WalletSettingsPresenter,
    container: AppContainer,
    locale: UserLocale,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showWallet by remember { mutableStateOf(false) }
    var archiveId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    LaunchedEffect(presenter) { presenter.load() }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CdTopBar(stringResource(Res.string.settings_title), stringResource(Res.string.back), onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (state.busy) item { CdNotice(NoticeKind.Info, stringResource(Res.string.settings_loading)) }
            state.error?.let { error ->
                item {
                    CdNotice(
                        NoticeKind.Error,
                        settingsErrorText(error),
                        action =
                            if (error == SettingsError.STORAGE) {
                                CdActionSpec(
                                    stringResource(Res.string.settings_retry),
                                    { scope.launch { presenter.load() } },
                                    enabled = !state.busy,
                                )
                            } else {
                                null
                            },
                    )
                }
            }
            if (state.saved) item { CdNotice(NoticeKind.Success, stringResource(Res.string.settings_saved)) }
            state.wallets.forEach { row ->
                item(key = row.wallet.id) {
                    CdCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(row.wallet.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            when {
                                !row.wallet.isActive -> CdBadge(stringResource(Res.string.settings_archived), BadgeTone.Neutral)
                                row.wallet.id == state.defaultWalletId ->
                                    CdBadge(
                                        stringResource(Res.string.home_default),
                                        BadgeTone.Success,
                                    )
                            }
                        }
                        Text(stringResource(row.wallet.type.label()) + " · " + MoneyFormatter.format(row.balance, locale))
                        val actions =
                            if (!row.wallet.isActive) {
                                listOf(
                                    CdActionSpec(
                                        stringResource(Res.string.settings_reactivate),
                                        { scope.launch { presenter.reactivate(row.wallet.id) } },
                                        enabled = !state.busy,
                                    ),
                                )
                            } else {
                                listOfNotNull(
                                    CdActionSpec(
                                        stringResource(Res.string.settings_archive, row.wallet.name),
                                        { archiveId = row.wallet.id },
                                        ActionRole.Danger,
                                        enabled = !state.busy,
                                    ),
                                    if (row.wallet.id != state.defaultWalletId) {
                                        CdActionSpec(
                                            stringResource(Res.string.settings_default),
                                            { scope.launch { presenter.makeDefault(row.wallet.id) } },
                                            enabled = !state.busy,
                                        )
                                    } else {
                                        null
                                    },
                                )
                            }
                        CdActions(actions)
                    }
                }
            }
            if (state.loaded) {
                item {
                    CdAction(
                        stringResource(Res.string.settings_add_wallet),
                        { showWallet = !showWallet },
                        role = ActionRole.Tertiary,
                        enabled = !state.busy,
                        icon = CoinDenIcon.PLUS.vector,
                    )
                }
                if (showWallet) item { NewWalletForm(presenter, state, onAdded = { showWallet = false }) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(Res.string.rates_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(Res.string.rates_note), style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    CdCard(tone = CardTone.Raised) {
                        CdField(code, { code = it }, label = stringResource(Res.string.settings_currency), enabled = !state.busy)
                        CdActions(
                            listOf(
                                CdActionSpec(
                                    stringResource(Res.string.settings_currency_add),
                                    { scope.launch { if (presenter.addCurrency(code)) code = "" } },
                                    enabled = !state.busy && code.isNotBlank(),
                                    icon = CoinDenIcon.PLUS.vector,
                                ),
                            ),
                        )
                    }
                }
                state.currencies.filter { it.code != state.profile?.localCurrency?.code }.forEach { currency ->
                    item(key = "rate-${currency.code}") { ReferenceRateForm(currency, state, presenter, locale, container) }
                }
            }
        }
    }
    archiveId?.let { id ->
        CdDialog(
            title = stringResource(Res.string.settings_archive_title),
            text = stringResource(Res.string.settings_archive_note),
            onDismiss = { archiveId = null },
            confirm =
                CdActionSpec(
                    stringResource(Res.string.settings_archive_action),
                    {
                        archiveId = null
                        scope.launch { presenter.archive(id) }
                    },
                    ActionRole.Danger,
                    enabled = !state.busy,
                ),
            dismiss = CdActionSpec(stringResource(Res.string.dialog_cancel), { archiveId = null }),
        )
    }
}

@Composable
private fun NewWalletForm(
    presenter: WalletSettingsPresenter,
    state: WalletSettingsState,
    onAdded: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(WalletType.CASH) }
    var currency by remember { mutableStateOf(state.profile!!.localCurrency.code) }
    var opening by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    CdCard(tone = CardTone.Raised) {
        CdField(name, { name = it }, label = stringResource(Res.string.wallet_name), enabled = !state.busy)
        CdChoiceGrid(WalletType.entries.map { CdOption(it, stringResource(it.label())) }, selected = type, onSelect = { type = it })
        if (state.currencies.size > 1) {
            CdChoice(
                state.currencies.map { CdOption(it.code, it.code) },
                selected = currency,
                onSelect = { currency = it },
                enabled = !state.busy,
            )
        }
        CdField(
            opening,
            { opening = it },
            label = stringResource(Res.string.wallet_opening) + " (" + currency + ")",
            suffix = currency,
            keyboardType = KeyboardType.Decimal,
            enabled = !state.busy,
        )
        CdActions(
            listOf(
                CdActionSpec(
                    stringResource(Res.string.wallet_add),
                    { scope.launch { if (presenter.addWallet(name, type, currency, opening)) onAdded() } },
                    enabled = !state.busy,
                    icon = CoinDenIcon.PLUS.vector,
                ),
            ),
        )
    }
}

@Composable
private fun ReferenceRateForm(
    currency: Currency,
    state: WalletSettingsState,
    presenter: WalletSettingsPresenter,
    locale: UserLocale,
    container: AppContainer,
) {
    val profile = state.profile ?: return
    val reference = profile.referenceRateFor(currency)
    var text by remember(currency.code, reference) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    CdCard {
        Text(
            currency.name ?: container.currencyNames.name(currency.code, container.languageCode) ?: currency.code,
            style = MaterialTheme.typography.titleMedium,
        )
        if (reference == null) {
            Text(stringResource(Res.string.rate_later_set))
        } else {
            val equivalent = referenceQuote(reference.currentRate)
            Text(
                if (equivalent != null) {
                    stringResource(Res.string.settings_rate_current, currency.code, MoneyFormatter.format(equivalent, locale))
                } else {
                    stringResource(Res.string.settings_quote_overflow)
                },
            )
        }
        CdField(
            text,
            { text = it },
            label = stringResource(Res.string.rate_label, currency.code, profile.localCurrency.code),
            keyboardType = KeyboardType.Decimal,
            enabled = !state.busy,
        )
        CdActions(
            listOf(
                CdActionSpec(
                    stringResource(Res.string.settings_rate_remove),
                    { scope.launch { presenter.removeRate(currency.code) } },
                    ActionRole.Danger,
                    enabled = !state.busy && reference != null,
                ),
                CdActionSpec(
                    stringResource(Res.string.settings_save),
                    { scope.launch { presenter.saveRate(currency.code, text) } },
                    ActionRole.Primary,
                    enabled = !state.busy && text.isNotBlank(),
                ),
            ),
        )
    }
}

@Composable
private fun settingsErrorText(error: SettingsError): String =
    when (error) {
        SettingsError.INVALID_WALLET -> stringResource(Res.string.error_invalid_wallet)
        SettingsError.INVALID_RATE -> stringResource(Res.string.settings_rate_error)
        SettingsError.INVALID_CURRENCY -> stringResource(Res.string.settings_currency_error)
        SettingsError.LAST_WALLET -> stringResource(Res.string.settings_last_wallet)
        SettingsError.NOT_FOUND -> stringResource(Res.string.settings_not_found)
        SettingsError.STORAGE -> stringResource(Res.string.settings_storage_error)
    }
