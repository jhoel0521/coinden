package com.coinden.app.onboarding

import com.coinden.app.AppContainer
import com.coinden.app.currency.FrequentCurrencies
import com.coinden.app.i18n.AppLanguage
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.interpret.AmountReader
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class OnboardingStep { WELCOME, CURRENCIES, WALLETS }

/** Errores del onboarding como datos: la pantalla los traduce. */
sealed interface OnboardingError {
    data class InvalidRate(
        val code: String,
    ) : OnboardingError

    data object InvalidCustomCurrency : OnboardingError

    data object InvalidWallet : OnboardingError

    data object NoWallets : OnboardingError
}

/** Otra moneda que la persona usa, con su tasa escrita o "La pongo después". */
data class CurrencyChoice(
    val currency: Currency,
    val rateText: String = "",
    val later: Boolean = false,
)

/** Billetera por crear al terminar; [opening] ya validado. */
data class WalletDraft(
    val name: String,
    val type: WalletType,
    val opening: Money,
)

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val language: String,
    val localCurrency: Currency,
    val others: List<CurrencyChoice> = emptyList(),
    val frequent: List<Currency> = emptyList(),
    val searchResults: List<Currency> = emptyList(),
    val wallets: List<WalletDraft> = emptyList(),
    val errors: Set<OnboardingError> = emptySet(),
) {
    /** Monedas que ofrece el paso de billeteras: la principal y las elegidas. */
    val walletCurrencies: List<Currency> get() = listOf(localCurrency) + others.map { it.currency }
}

/**
 * Primer inicio: idioma, monedas que usa y billeteras. No usa Compose: se prueba en la JVM.
 * Nada se guarda hasta [finish], salvo el idioma, que aplica al instante.
 */
class OnboardingPresenter(
    private val container: AppContainer,
    deviceLanguage: String,
    private val deviceCountry: String,
) {
    private val locale get() = container.regional.settings().locale
    private val mutableState: MutableStateFlow<OnboardingState>

    init {
        val local = container.catalog.forRegion(deviceCountry) ?: Currency.USD
        mutableState =
            MutableStateFlow(
                OnboardingState(
                    language = AppLanguage.proposedFor(deviceLanguage),
                    localCurrency = local,
                    frequent = FrequentCurrencies.forRegion(deviceCountry, local, container.catalog),
                ),
            )
    }

    val state: StateFlow<OnboardingState> = mutableState.asStateFlow()

    suspend fun chooseLanguage(code: String) {
        container.chooseLanguage(code)
        mutableState.update { it.copy(language = code) }
    }

    fun setLocalCurrency(currency: Currency) {
        mutableState.update { s ->
            s.copy(
                localCurrency = currency,
                others = s.others.filterNot { it.currency.isSameAs(currency) },
                frequent = FrequentCurrencies.forRegion(deviceCountry, currency, container.catalog),
            )
        }
    }

    fun toggleCurrency(currency: Currency) {
        mutableState.update { s ->
            val selected = s.others.any { it.currency.isSameAs(currency) }
            s.copy(others = if (selected) s.others.filterNot { it.currency.isSameAs(currency) } else s.others + CurrencyChoice(currency))
        }
    }

    fun setRate(
        code: String,
        text: String,
    ) = updateChoice(code) { it.copy(rateText = text, later = false) }

    fun setRateLater(code: String) = updateChoice(code) { it.copy(rateText = "", later = true) }

    fun search(query: String) {
        mutableState.update {
            it.copy(
                searchResults = FrequentCurrencies.search(query, it.language, container.catalog, container.currencyNames),
            )
        }
    }

    /** Moneda propia o cripto: código de 3 a 6 letras, nombre, símbolo y de 0 a 8 decimales. */
    fun addCustomCurrency(
        code: String,
        name: String,
        symbol: String,
        decimalsText: String,
    ): Boolean {
        val currency =
            runCatching {
                Currency(
                    code = code.trim().uppercase(),
                    symbol = symbol.trim().ifEmpty { null },
                    decimals = decimalsText.trim().toInt(),
                    name = name.trim().ifEmpty { null },
                    isCustom = true,
                )
            }.getOrNull()
        if (currency == null || state.value.walletCurrencies.any { it.isSameAs(currency) }) {
            mutableState.update { it.copy(errors = setOf(OnboardingError.InvalidCustomCurrency)) }
            return false
        }
        mutableState.update { it.copy(others = it.others + CurrencyChoice(currency), errors = emptySet()) }
        return true
    }

    /** Avanza un paso; en el de monedas valida las tasas escritas. */
    fun next(): Boolean {
        val s = state.value
        if (s.step == OnboardingStep.CURRENCIES) {
            val invalid = s.others.filter { !it.later && it.rateText.isNotBlank() && rateOf(it) == null }
            if (invalid.isNotEmpty()) {
                mutableState.update { it.copy(errors = invalid.map { c -> OnboardingError.InvalidRate(c.currency.code) }.toSet()) }
                return false
            }
        }
        val nextStep =
            when (s.step) {
                OnboardingStep.WELCOME -> OnboardingStep.CURRENCIES
                OnboardingStep.CURRENCIES -> OnboardingStep.WALLETS
                OnboardingStep.WALLETS -> return false
            }
        mutableState.update { it.copy(step = nextStep, errors = emptySet()) }
        return true
    }

    /** La pantalla de [step] está visible (navegación o botón Atrás del sistema). */
    fun show(step: OnboardingStep) {
        mutableState.update { it.copy(step = step, errors = emptySet()) }
    }

    fun back() {
        mutableState.update { s ->
            val previous =
                when (s.step) {
                    OnboardingStep.WALLETS -> OnboardingStep.CURRENCIES
                    OnboardingStep.CURRENCIES, OnboardingStep.WELCOME -> OnboardingStep.WELCOME
                }
            s.copy(step = previous, errors = emptySet())
        }
    }

    /** Agrega una billetera a la lista; el saldo se escribe con el formato del locale ("1.234,50"). */
    fun addWallet(
        name: String,
        type: WalletType,
        currency: Currency,
        openingText: String,
    ): Boolean {
        val opening = parseAmount(openingText.ifBlank { "0" }, currency)
        if (!Wallet.isValidName(name) || opening == null || opening.isNegative) {
            mutableState.update { it.copy(errors = setOf(OnboardingError.InvalidWallet)) }
            return false
        }
        mutableState.update { it.copy(wallets = it.wallets + WalletDraft(name.trim(), type, opening), errors = emptySet()) }
        return true
    }

    fun removeWallet(index: Int) {
        mutableState.update { it.copy(wallets = it.wallets.filterIndexed { i, _ -> i != index }) }
    }

    /** Guarda el perfil y crea las billeteras en orden; elige sola la billetera por defecto. */
    suspend fun finish(): Boolean {
        val s = state.value
        if (s.wallets.isEmpty()) {
            mutableState.update { it.copy(errors = setOf(OnboardingError.NoWallets)) }
            return false
        }
        val owner = container.ownerId
        val now = container.clock.nowEpochMs()
        container.storage.profiles.saveProfile(
            UserCurrencyProfile(
                userId = owner,
                localCurrency = s.localCurrency,
                strategy = ExchangeRateStrategy.Fixed(ExchangeRate.identity(s.localCurrency)),
                currencies = s.others.map { it.currency },
                referenceRates =
                    s.others
                        .mapNotNull { c -> rateOf(c)?.let { c.currency.code to ExchangeRateStrategy.ManualDaily(it, now) } }
                        .toMap(),
            ),
        )
        val created =
            s.wallets.map { w ->
                val result = container.createWallet(owner, w.name, w.opening.currency, w.type, w.opening)
                (result as WalletOperationResult.WalletCreated).wallet
            }
        defaultWalletFor(created, s.localCurrency)?.let { container.setDefaultWallet(owner, it.id) }
        return true
    }

    private fun rateOf(choice: CurrencyChoice): ExchangeRate? {
        if (choice.later || choice.rateText.isBlank()) return null
        val canonical = AmountReader.read(choice.rateText.trim(), locale)?.canonical ?: return null
        return runCatching { ExchangeRate.parse(choice.currency, state.value.localCurrency, canonical) }.getOrNull()
    }

    private fun parseAmount(
        text: String,
        currency: Currency,
    ): Money? {
        val canonical = AmountReader.read(text.trim(), locale)?.canonical ?: return null
        return runCatching { Money.parseCanonical(canonical, currency) }.getOrNull()
    }

    private fun updateChoice(
        code: String,
        change: (CurrencyChoice) -> CurrencyChoice,
    ) {
        mutableState.update { s -> s.copy(others = s.others.map { if (it.currency.code == code) change(it) else it }) }
    }
}

/** La billetera por defecto: el efectivo en la moneda local; si no hay, la primera creada. */
fun defaultWalletFor(
    wallets: List<Wallet>,
    localCurrency: Currency,
): Wallet? = wallets.firstOrNull { it.type == WalletType.CASH && it.accepts(localCurrency) } ?: wallets.firstOrNull()
