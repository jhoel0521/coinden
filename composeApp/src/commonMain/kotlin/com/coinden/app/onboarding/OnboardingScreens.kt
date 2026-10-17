package com.coinden.app.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import coinden.composeapp.generated.resources.currencies_add_custom
import coinden.composeapp.generated.resources.currencies_main
import coinden.composeapp.generated.resources.currencies_others
import coinden.composeapp.generated.resources.currencies_search
import coinden.composeapp.generated.resources.currencies_title
import coinden.composeapp.generated.resources.custom_add
import coinden.composeapp.generated.resources.custom_code
import coinden.composeapp.generated.resources.custom_decimals
import coinden.composeapp.generated.resources.custom_name
import coinden.composeapp.generated.resources.custom_symbol
import coinden.composeapp.generated.resources.error_custom_currency
import coinden.composeapp.generated.resources.error_invalid_rate
import coinden.composeapp.generated.resources.error_invalid_wallet
import coinden.composeapp.generated.resources.error_no_wallets
import coinden.composeapp.generated.resources.finish
import coinden.composeapp.generated.resources.language_en
import coinden.composeapp.generated.resources.language_es
import coinden.composeapp.generated.resources.language_pt
import coinden.composeapp.generated.resources.logo_description
import coinden.composeapp.generated.resources.next
import coinden.composeapp.generated.resources.rate_label
import coinden.composeapp.generated.resources.rate_later
import coinden.composeapp.generated.resources.rate_later_set
import coinden.composeapp.generated.resources.rate_set
import coinden.composeapp.generated.resources.rates_note
import coinden.composeapp.generated.resources.rates_title
import coinden.composeapp.generated.resources.step_counter
import coinden.composeapp.generated.resources.wallet_add
import coinden.composeapp.generated.resources.wallet_name
import coinden.composeapp.generated.resources.wallet_name_hint
import coinden.composeapp.generated.resources.wallet_opening
import coinden.composeapp.generated.resources.wallet_remove
import coinden.composeapp.generated.resources.wallet_type_bank
import coinden.composeapp.generated.resources.wallet_type_cash
import coinden.composeapp.generated.resources.wallet_type_credit
import coinden.composeapp.generated.resources.wallet_type_crypto
import coinden.composeapp.generated.resources.wallet_type_digital
import coinden.composeapp.generated.resources.wallets_hint
import coinden.composeapp.generated.resources.wallets_subtitle
import coinden.composeapp.generated.resources.wallets_title
import coinden.composeapp.generated.resources.welcome_greeting
import coinden.composeapp.generated.resources.welcome_language
import coinden.composeapp.generated.resources.welcome_no_account
import coinden.composeapp.generated.resources.welcome_start
import coinden.composeapp.generated.resources.welcome_subtitle
import com.coinden.app.currency.CurrencyNames
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.theme.CoinDenLogo
import com.coinden.app.theme.CoinDenTheme
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val STEPS = 3

/** Esqueleto común de los pasos: contenido desplazable y botones abajo. */
@Composable
private fun StepScaffold(
    step: Int,
    title: String,
    subtitle: String? = null,
    primaryText: String,
    onPrimary: () -> Unit,
    onBack: (() -> Unit)?,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    stringResource(Res.string.step_counter, step, STEPS),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
                }
            }
            content()
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            onBack?.let {
                OutlinedButton(onClick = it, modifier = Modifier.heightIn(min = 56.dp), shape = MaterialTheme.shapes.extraLarge) {
                    Text(stringResource(Res.string.back))
                }
            }
            Button(onClick = onPrimary, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = MaterialTheme.shapes.extraLarge) {
                Text(primaryText, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

/** Paso 1: bienvenida en los tres idiomas y elección de idioma. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    presenter: OnboardingPresenter,
    onStart: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val languages = listOf("es" to Res.string.language_es, "en" to Res.string.language_en, "pt" to Res.string.language_pt)
    StepScaffold(
        step = 1,
        title = stringResource(Res.string.welcome_greeting),
        primaryText = stringResource(Res.string.welcome_start),
        onPrimary = onStart,
        onBack = null,
    ) {
        item { CoinDenLogo(stringResource(Res.string.logo_description), size = 72.dp) }
        item { Text(stringResource(Res.string.welcome_subtitle), style = MaterialTheme.typography.bodyLarge) }
        item { Text(stringResource(Res.string.welcome_language), style = MaterialTheme.typography.titleMedium) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { (code, label) ->
                    FilterChip(
                        selected = state.language == code,
                        onClick = { scope.launch { presenter.chooseLanguage(code) } },
                        label = { Text(stringResource(label)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        item {
            Text(
                stringResource(Res.string.welcome_no_account),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** Paso 2: moneda principal, otras monedas (frecuentes, búsqueda, propias) y tasas opcionales. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrenciesScreen(
    presenter: OnboardingPresenter,
    names: CurrencyNames,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scheme = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var pickingMain by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }

    fun label(c: Currency) = "${c.code} · ${names.name(c.code, state.language) ?: c.name ?: c.code}"

    StepScaffold(
        step = 2,
        title = stringResource(Res.string.currencies_title),
        primaryText = stringResource(Res.string.next),
        onPrimary = { if (presenter.next()) onNext() },
        onBack = onBack,
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.currencies_main), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { pickingMain = !pickingMain }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(label(state.localCurrency), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.currencies_others), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (state.frequent + state.others.map { it.currency }).distinctBy { it.code }.forEach { c ->
                        FilterChip(
                            selected = state.others.any { it.currency.code == c.code },
                            onClick = { presenter.toggleCurrency(c) },
                            label = { Text(c.code) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    presenter.search(it)
                },
                label = { Text(stringResource(Res.string.currencies_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotBlank()) {
            state.searchResults.take(8).forEach { c ->
                item(key = "search-${c.code}") {
                    TextButton(
                        onClick = {
                            if (pickingMain) presenter.setLocalCurrency(c) else presenter.toggleCurrency(c)
                            pickingMain = false
                            query = ""
                            presenter.search("")
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(label(c), modifier = Modifier.fillMaxWidth()) }
                }
            }
        }
        item {
            TextButton(onClick = { showCustom = !showCustom }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(CoinDenIcon.PLUS.vector, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.currencies_add_custom), modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (showCustom) item { CustomCurrencyForm(presenter) { showCustom = false } }
        if (state.others.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.rates_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.rates_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            state.others.forEach { choice ->
                item(key = "rate-${choice.currency.code}") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (choice.later) {
                            Text(
                                "${choice.currency.code}: " + stringResource(Res.string.rate_later_set),
                                modifier = Modifier.weight(1f),
                                color = scheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = { presenter.setRate(choice.currency.code, "") },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(stringResource(Res.string.rate_set)) }
                        } else {
                            OutlinedTextField(
                                value = choice.rateText,
                                onValueChange = { presenter.setRate(choice.currency.code, it) },
                                label = { Text(stringResource(Res.string.rate_label, choice.currency.code, state.localCurrency.code)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { presenter.setRateLater(choice.currency.code) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(Res.string.rate_later))
                            }
                        }
                    }
                }
            }
        }
        state.errors.forEach { error ->
            item {
                when (error) {
                    is OnboardingError.InvalidRate -> ErrorText(stringResource(Res.string.error_invalid_rate, error.code))
                    OnboardingError.InvalidCustomCurrency -> ErrorText(stringResource(Res.string.error_custom_currency))
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun CustomCurrencyForm(
    presenter: OnboardingPresenter,
    onAdded: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var decimals by remember { mutableStateOf("2") }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(code, {
                code = it
            }, label = { Text(stringResource(Res.string.custom_code)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(name, {
                name = it
            }, label = { Text(stringResource(Res.string.custom_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(symbol, {
                    symbol = it
                }, label = { Text(stringResource(Res.string.custom_symbol)) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    decimals,
                    { decimals = it },
                    label = { Text(stringResource(Res.string.custom_decimals)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = { if (presenter.addCustomCurrency(code, name, symbol, decimals)) onAdded() },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.custom_add))
            }
        }
    }
}

/** Paso 3: billeteras con nombre, tipo, moneda y saldo de hoy; sin apodos que configurar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletsScreen(
    presenter: OnboardingPresenter,
    locale: UserLocale,
    onFinished: () -> Unit,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(WalletType.CASH) }
    var currency by remember(state.localCurrency) { mutableStateOf(state.localCurrency) }
    var opening by remember { mutableStateOf("") }

    StepScaffold(
        step = 3,
        title = stringResource(Res.string.wallets_title),
        subtitle = stringResource(Res.string.wallets_subtitle),
        primaryText = stringResource(Res.string.finish),
        onPrimary = { scope.launch { if (presenter.finish()) onFinished() } },
        onBack = onBack,
    ) {
        state.wallets.forEachIndexed { index, wallet ->
            item(key = "wallet-$index-${wallet.name}") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = scheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(wallet.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(wallet.type.label()),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Text(MoneyFormatter.format(wallet.opening, locale), style = CoinDenTheme.amounts.amount)
                        IconButton(onClick = { presenter.removeWallet(index) }) {
                            Icon(
                                CoinDenIcon.CLOSE.vector,
                                contentDescription = stringResource(Res.string.wallet_remove, wallet.name),
                                tint = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = scheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text(stringResource(Res.string.wallet_name)) },
                        placeholder = { Text(stringResource(Res.string.wallet_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WalletType.entries.forEach { t ->
                            FilterChip(selected = type == t, onClick = {
                                type = t
                            }, label = { Text(stringResource(t.label())) }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                    if (state.walletCurrencies.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.walletCurrencies.forEach { c ->
                                FilterChip(selected = currency.code == c.code, onClick = {
                                    currency = c
                                }, label = { Text(c.code) }, modifier = Modifier.heightIn(min = 48.dp))
                            }
                        }
                    }
                    OutlinedTextField(
                        opening,
                        { opening = it },
                        label = { Text(stringResource(Res.string.wallet_opening) + " (${currency.code})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = {
                            if (presenter.addWallet(name, type, currency, opening)) {
                                name = ""
                                opening = ""
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(CoinDenIcon.PLUS.vector, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(Res.string.wallet_add), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
        item { Text(stringResource(Res.string.wallets_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        state.errors.forEach { error ->
            item {
                when (error) {
                    OnboardingError.InvalidWallet -> ErrorText(stringResource(Res.string.error_invalid_wallet))
                    OnboardingError.NoWallets -> ErrorText(stringResource(Res.string.error_no_wallets))
                    else -> Unit
                }
            }
        }
        item { Spacer(Modifier.heightIn(min = 8.dp)) }
    }
}

/** Nombre traducido de cada tipo de billetera; un `when` exhaustivo: un tipo nuevo sin texto no compila. */
fun WalletType.label(): StringResource =
    when (this) {
        WalletType.CASH -> Res.string.wallet_type_cash
        WalletType.BANK -> Res.string.wallet_type_bank
        WalletType.DIGITAL -> Res.string.wallet_type_digital
        WalletType.CRYPTO -> Res.string.wallet_type_crypto
        WalletType.CREDIT -> Res.string.wallet_type_credit
    }
