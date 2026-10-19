package com.coinden.app.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import coinden.composeapp.generated.resources.welcome_language
import coinden.composeapp.generated.resources.welcome_no_account
import coinden.composeapp.generated.resources.welcome_start
import coinden.composeapp.generated.resources.welcome_subtitle
import com.coinden.app.currency.CurrencyNames
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.theme.CoinDenTheme
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.BrandLogo
import com.coinden.app.ui.components.CardTone
import com.coinden.app.ui.components.CdAction
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdChoice
import com.coinden.app.ui.components.CdErrorText
import com.coinden.app.ui.components.CdField
import com.coinden.app.ui.components.CdIconAction
import com.coinden.app.ui.components.CdListOption
import com.coinden.app.ui.components.CdMultiChoiceGrid
import com.coinden.app.ui.components.CdOption
import com.coinden.app.ui.components.CdSection
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val STEPS = 3

/**
 * Esqueleto común de los pasos: contenido desplazable y acciones abajo ([CdActions]). Mientras el
 * teclado está abierto, las acciones se ocultan para que se vea lo que se escribe y sus resultados.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepScaffold(
    step: Int,
    title: String?,
    subtitle: String? = null,
    primaryText: String,
    onPrimary: () -> Unit,
    onBack: (() -> Unit)?,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        LazyColumn(
            state = listState,
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
            if (title != null || subtitle != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        title?.let { Text(it, style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface) }
                        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
                    }
                }
            }
            content()
        }
        if (!WindowInsets.isImeVisible) {
            CdActions(
                listOfNotNull(
                    onBack?.let { CdActionSpec(stringResource(Res.string.back), it) },
                    CdActionSpec(primaryText, onPrimary, ActionRole.Primary),
                ),
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

/** Paso 1: bienvenida en los tres idiomas y elección de idioma. */
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
        // El nombre y el lema van debajo del logo, como texto.
        title = null,
        primaryText = stringResource(Res.string.welcome_start),
        onPrimary = onStart,
        onBack = null,
    ) {
        item { BrandLogo() }
        item { Text(stringResource(Res.string.welcome_subtitle), style = MaterialTheme.typography.bodyLarge) }
        item {
            CdSection(stringResource(Res.string.welcome_language)) {
                CdChoice(
                    languages.map { (code, label) -> CdOption(code, stringResource(label)) },
                    selected = state.language,
                    onSelect = { code -> scope.launch { presenter.chooseLanguage(code) } },
                )
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
    val listState = rememberLazyListState()
    // Al buscar, el buscador sube arriba de todo: sus resultados quedan a la vista aunque el teclado esté abierto.
    LaunchedEffect(query.isNotBlank()) {
        if (query.isNotBlank()) {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == SEARCH_KEY }
                ?.index
                ?.let { listState.animateScrollToItem(it) }
        }
    }

    fun label(c: Currency) = "${c.code} · ${names.name(c.code, state.language) ?: c.name ?: c.code}"

    StepScaffold(
        step = 2,
        title = stringResource(Res.string.currencies_title),
        primaryText = stringResource(Res.string.next),
        onPrimary = { if (presenter.next()) onNext() },
        onBack = onBack,
        listState = listState,
    ) {
        item {
            CdSection(stringResource(Res.string.currencies_main)) {
                CdActions(listOf(CdActionSpec(label(state.localCurrency), { pickingMain = !pickingMain })))
            }
        }
        item {
            CdSection(stringResource(Res.string.currencies_others)) {
                CdMultiChoiceGrid(
                    (state.frequent + state.others.map { it.currency }).distinctBy { it.code }.map { CdOption(it, it.code) },
                    selected = state.others.map { it.currency }.toSet(),
                    onToggle = presenter::toggleCurrency,
                )
            }
        }
        item(key = SEARCH_KEY) {
            CdField(
                query,
                {
                    query = it
                    presenter.search(it)
                },
                label = stringResource(Res.string.currencies_search),
                search = true,
            )
        }
        if (query.isNotBlank()) {
            state.searchResults.take(8).forEach { c ->
                item(key = "search-${c.code}") {
                    CdListOption(label(c), onClick = {
                        if (pickingMain) presenter.setLocalCurrency(c) else presenter.toggleCurrency(c)
                        pickingMain = false
                        query = ""
                        presenter.search("")
                    })
                }
            }
        }
        item {
            CdAction(
                stringResource(Res.string.currencies_add_custom),
                { showCustom = !showCustom },
                role = ActionRole.Tertiary,
                icon = CoinDenIcon.PLUS.vector,
            )
        }
        if (showCustom) {
            item {
                val error =
                    if (OnboardingError.InvalidCustomCurrency in state.errors) stringResource(Res.string.error_custom_currency) else null
                CustomCurrencyForm(presenter, error) { showCustom = false }
            }
        }
        if (state.others.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.rates_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.rates_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            state.others.forEach { choice ->
                item(key = "rate-${choice.currency.code}") {
                    val code = choice.currency.code
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (choice.later) {
                            Text(
                                "$code: " + stringResource(Res.string.rate_later_set),
                                modifier = Modifier.weight(1f),
                                color = scheme.onSurfaceVariant,
                            )
                            CdAction(stringResource(Res.string.rate_set), { presenter.setRate(code, "") }, role = ActionRole.Tertiary)
                        } else {
                            val invalid = state.errors.any { it is OnboardingError.InvalidRate && it.code == code }
                            CdField(
                                choice.rateText,
                                { presenter.setRate(code, it) },
                                label = stringResource(Res.string.rate_label, code, state.localCurrency.code),
                                error = if (invalid) stringResource(Res.string.error_invalid_rate, code) else null,
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                            )
                            CdAction(stringResource(Res.string.rate_later), { presenter.setRateLater(code) }, role = ActionRole.Tertiary)
                        }
                    }
                }
            }
        }
    }
}

private const val SEARCH_KEY = "search"

/** Moneda propia o cripto: el error se muestra en el mismo formulario, a la vista. */
@Composable
private fun CustomCurrencyForm(
    presenter: OnboardingPresenter,
    error: String?,
    onAdded: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var decimals by remember { mutableStateOf("2") }
    val add = { if (presenter.addCustomCurrency(code, name, symbol, decimals)) onAdded() }
    CdCard(tone = CardTone.Raised) {
        CdSection(title = null, error = error) {
            CdField(code, { code = it }, label = stringResource(Res.string.custom_code))
            CdField(name, { name = it }, label = stringResource(Res.string.custom_name))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CdField(symbol, { symbol = it }, label = stringResource(Res.string.custom_symbol), modifier = Modifier.weight(1f))
                CdField(
                    decimals,
                    { decimals = it },
                    label = stringResource(Res.string.custom_decimals),
                    keyboardType = KeyboardType.Number,
                    onImeAction = add,
                    modifier = Modifier.weight(1f),
                )
            }
            CdActions(
                listOf(
                    CdActionSpec(
                        stringResource(Res.string.custom_add),
                        add,
                    ),
                ),
            )
        }
    }
}

/** Paso 3: billeteras con nombre, tipo, moneda y saldo de hoy; sin apodos que configurar. */
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
    val addWallet = {
        if (presenter.addWallet(name, type, currency, opening)) {
            name = ""
            opening = ""
        }
    }

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
                CdCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(wallet.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(wallet.type.label()),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Text(MoneyFormatter.format(wallet.opening, locale), style = CoinDenTheme.amounts.amount)
                        CdIconAction(
                            CoinDenIcon.CLOSE.vector,
                            stringResource(Res.string.wallet_remove, wallet.name),
                            { presenter.removeWallet(index) },
                        )
                    }
                }
            }
        }
        item {
            val error = if (OnboardingError.InvalidWallet in state.errors) stringResource(Res.string.error_invalid_wallet) else null
            CdCard(tone = CardTone.Raised) {
                CdSection(title = null, error = error) {
                    CdField(
                        name,
                        { name = it },
                        label = stringResource(Res.string.wallet_name),
                        placeholder = stringResource(Res.string.wallet_name_hint),
                    )
                    CdChoice(
                        WalletType.entries.map {
                            CdOption(
                                it,
                                stringResource(it.label()),
                                icon = it.icon().vector,
                            )
                        },
                        selected = type,
                        onSelect = {
                            type =
                                it
                        },
                    )
                    if (state.walletCurrencies.size > 1) {
                        CdChoice(state.walletCurrencies.map { CdOption(it.code, it.code) }, selected = currency.code, onSelect = { code ->
                            state.walletCurrencies.firstOrNull { it.code == code }?.let { currency = it }
                        })
                    }
                    CdField(
                        opening,
                        { opening = it },
                        label = stringResource(Res.string.wallet_opening) + " (${currency.code})",
                        suffix = currency.code,
                        keyboardType = KeyboardType.Decimal,
                        onImeAction = addWallet,
                    )
                    CdActions(
                        listOf(
                            CdActionSpec(
                                stringResource(Res.string.wallet_add),
                                addWallet,
                                icon = CoinDenIcon.PLUS.vector,
                            ),
                        ),
                    )
                }
            }
        }
        item { Text(stringResource(Res.string.wallets_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        if (OnboardingError.NoWallets in state.errors) item { CdErrorText(stringResource(Res.string.error_no_wallets)) }
        item { Spacer(Modifier.heightIn(min = 8.dp)) }
    }
}

/** Ícono de cada tipo de billetera (Inicio, onboarding y ajustes). */
fun WalletType.icon(): CoinDenIcon =
    when (this) {
        WalletType.CASH -> CoinDenIcon.CASH
        WalletType.BANK -> CoinDenIcon.BANK
        WalletType.DIGITAL -> CoinDenIcon.QR
        WalletType.CRYPTO -> CoinDenIcon.SPARKLE
        WalletType.CREDIT -> CoinDenIcon.CARD
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
