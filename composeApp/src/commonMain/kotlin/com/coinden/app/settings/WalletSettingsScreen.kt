package com.coinden.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.back)) }
        if (state.busy) Text(stringResource(Res.string.settings_loading), modifier = Modifier.padding(horizontal = 16.dp))
        state.error?.let { error ->
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(settingsErrorText(error), color = MaterialTheme.colorScheme.error)
                if (error == SettingsError.STORAGE) {
                    TextButton(
                        onClick = { scope.launch { presenter.load() } },
                        enabled = !state.busy,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(Res.string.settings_retry)) }
                }
            }
        }
        if (state.saved) Text(stringResource(Res.string.settings_saved), modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.headlineSmall) }
            state.wallets.forEach { row ->
                item(key = row.wallet.id) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(row.wallet.name, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(row.wallet.type.label()) + " · " + MoneyFormatter.format(row.balance, locale))
                            if (!row.wallet.isActive) {
                                Text(stringResource(Res.string.settings_archived))
                                TextButton(onClick = {
                                    scope.launch { presenter.reactivate(row.wallet.id) }
                                }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text(stringResource(Res.string.settings_reactivate))
                                }
                            } else {
                                if (row.wallet.id == state.defaultWalletId) {
                                    Text(stringResource(Res.string.home_default))
                                } else {
                                    TextButton(onClick = {
                                        scope.launch { presenter.makeDefault(row.wallet.id) }
                                    }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text(stringResource(Res.string.settings_default))
                                    }
                                }
                                TextButton(
                                    onClick = { archiveId = row.wallet.id },
                                    enabled = !state.busy,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text(stringResource(Res.string.settings_archive, row.wallet.name))
                                }
                            }
                        }
                    }
                }
            }
            if (state.loaded) {
                item {
                    OutlinedButton(
                        onClick = { showWallet = !showWallet },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.settings_add_wallet))
                    }
                }
                if (showWallet) item { NewWalletForm(presenter, state, onAdded = { showWallet = false }) }
                item { Text(stringResource(Res.string.rates_title), style = MaterialTheme.typography.titleLarge) }
                item { Text(stringResource(Res.string.rates_note)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(code, {
                            code = it
                        }, enabled = !state.busy, singleLine = true, label = {
                            Text(stringResource(Res.string.settings_currency))
                        }, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = {
                            scope.launch { if (presenter.addCurrency(code)) code = "" }
                        }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(Res.string.settings_currency_add))
                        }
                    }
                }
                state.currencies.filter { it.code != state.profile?.localCurrency?.code }.forEach { currency ->
                    item(key = "rate-${currency.code}") { ReferenceRateForm(currency, state, presenter, locale, container) }
                }
            }
        }
    }
    archiveId?.let { id ->
        AlertDialog(
            onDismissRequest = { archiveId = null },
            title = { Text(stringResource(Res.string.settings_archive_title)) },
            text = { Text(stringResource(Res.string.settings_archive_note)) },
            confirmButton = {
                TextButton(onClick = {
                    archiveId = null
                    scope.launch { presenter.archive(id) }
                }, enabled = !state.busy) { Text(stringResource(Res.string.settings_archive_action)) }
            },
            dismissButton = { TextButton(onClick = { archiveId = null }) { Text(stringResource(Res.string.dialog_cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, {
            name = it
        }, enabled = !state.busy, label = { Text(stringResource(Res.string.wallet_name)) }, modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WalletType.entries.forEach { choice ->
                FilterChip(selected = type == choice, onClick = {
                    type = choice
                }, enabled = !state.busy, label = { Text(stringResource(choice.label())) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.currencies.forEach { choice ->
                FilterChip(selected = currency == choice.code, onClick = {
                    currency = choice.code
                }, enabled = !state.busy, label = { Text(choice.code) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        OutlinedTextField(opening, {
            opening = it
        }, enabled = !state.busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), label = {
            Text(
                stringResource(Res.string.wallet_opening) + " (" + currency + ")",
            )
        }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                if (presenter.addWallet(name, type, currency, opening)) onAdded()
            }
        }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(Res.string.wallet_add))
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        OutlinedTextField(text, {
            text = it
        }, enabled = !state.busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), label = {
            Text(stringResource(Res.string.rate_label, currency.code, profile.localCurrency.code))
        }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    presenter.saveRate(currency.code, text)
                }
            }, enabled = !state.busy && text.isNotBlank(), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.settings_save))
            }
            OutlinedButton(onClick = {
                scope.launch {
                    presenter.removeRate(currency.code)
                }
            }, enabled = !state.busy && reference != null, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.settings_rate_remove))
            }
        }
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
