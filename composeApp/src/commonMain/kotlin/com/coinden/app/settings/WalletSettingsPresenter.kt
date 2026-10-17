package com.coinden.app.settings

import com.coinden.app.AppContainer
import com.coinden.app.onboarding.defaultWalletFor
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.currentRate
import com.coinden.core.domain.model.interpret.AmountReader
import com.coinden.core.domain.model.wallet.LedgerProjection
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.wallet.ArchiveWalletUseCase
import com.coinden.core.domain.usecase.wallet.ReactivateWalletUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

enum class SettingsError { INVALID_WALLET, INVALID_RATE, INVALID_CURRENCY, LAST_WALLET, NOT_FOUND, STORAGE }

/** Una cotización guardada puede desbordar al mostrar una unidad mayor; no debe cerrar la UI. */
fun referenceQuote(rate: ExchangeRate): Money? =
    try {
        rate.quoteFor(Money.parseCanonical("1", rate.from))
    } catch (_: ArithmeticException) {
        null
    }

data class SettingsWallet(
    val wallet: Wallet,
    val balance: Money,
)

data class WalletSettingsState(
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val wallets: List<SettingsWallet> = emptyList(),
    val profile: UserCurrencyProfile? = null,
    val currencies: List<Currency> = emptyList(),
    val defaultWalletId: String? = null,
    val error: SettingsError? = null,
    val saved: Boolean = false,
)

/** Administración local. Las operaciones de dinero y las políticas siguen en el dominio. */
class WalletSettingsPresenter(
    private val container: AppContainer,
) {
    private val mutableState = MutableStateFlow(WalletSettingsState())
    val state = mutableState.asStateFlow()

    suspend fun load() {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, saved = false, error = null) }
        try {
            refresh()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.update { it.copy(error = SettingsError.STORAGE) }
        } finally {
            mutableState.update { it.copy(busy = false) }
        }
    }

    private suspend fun profile() = container.storage.profiles.getProfile(container.ownerId) ?: invalid(SettingsError.STORAGE)

    private suspend fun refresh() {
        val profile = profile()
        val wallets = container.storage.wallets.listWallets(container.ownerId)
        val active = wallets.filter { it.isActive }
        var default = container.storage.aliases.defaultWalletFor(container.ownerId)
        if (active.none { it.id == default }) {
            default = defaultWalletFor(active, profile.localCurrency)?.id
            default?.let { container.setDefaultWallet(container.ownerId, it) }
        }
        val rows = wallets.map { SettingsWallet(it, LedgerProjection(it, container.storage.wallets.eventsForWallet(it.id)).balance) }
        val currencies = (listOf(profile.localCurrency) + profile.currencies + wallets.map { it.currency }).distinctBy { it.code }
        mutableState.update {
            it.copy(
                loaded = true,
                wallets = rows,
                profile = profile,
                currencies = currencies,
                defaultWalletId = default,
            )
        }
    }

    suspend fun addWallet(
        name: String,
        type: WalletType,
        currencyCode: String,
        openingText: String,
    ): Boolean =
        change {
            val currency = state.value.currencies.firstOrNull { it.code == currencyCode } ?: invalid(SettingsError.INVALID_CURRENCY)
            val signed = openingText.trim().ifBlank { "0" }
            val unsigned = if (signed.startsWith('-') || signed.startsWith('+')) signed.drop(1) else signed
            val numeric =
                AmountReader.read(unsigned, container.regional.settings().locale)?.canonical
                    ?: invalid(SettingsError.INVALID_WALLET)
            val canonical = if (signed.startsWith('-')) "-$numeric" else numeric
            val opening =
                try {
                    Money.parseCanonical(canonical, currency)
                } catch (
                    _: IllegalArgumentException,
                ) {
                    invalid(SettingsError.INVALID_WALLET)
                }
            if (!Wallet.isValidName(name) || (opening.isNegative && !type.allowsNegativeByDefault)) invalid(SettingsError.INVALID_WALLET)
            val result = container.createWallet(container.ownerId, name, currency, type, opening)
            if (result !is WalletOperationResult.WalletCreated) invalid(SettingsError.INVALID_WALLET)
        }

    suspend fun archive(id: String): Boolean =
        change {
            val wallet = owned(id)
            if (!wallet.isActive) return@change
            val active =
                container.storage.wallets
                    .listWallets(container.ownerId)
                    .filter { it.isActive && it.id != id }
            if (active.isEmpty()) invalid(SettingsError.LAST_WALLET)
            if (container.storage.aliases.defaultWalletFor(container.ownerId) == id) {
                defaultWalletFor(active, profile().localCurrency)?.let { container.setDefaultWallet(container.ownerId, it.id) }
            }
            if (ArchiveWalletUseCase(container.storage.wallets)(id) is WalletOperationResult.Rejected) invalid(SettingsError.NOT_FOUND)
        }

    suspend fun reactivate(id: String): Boolean =
        change {
            owned(id)
            if (ReactivateWalletUseCase(container.storage.wallets)(id) is WalletOperationResult.Rejected) invalid(SettingsError.NOT_FOUND)
        }

    suspend fun makeDefault(id: String): Boolean =
        change {
            if (!owned(id).isActive) invalid(SettingsError.NOT_FOUND)
            container.setDefaultWallet(container.ownerId, id)
        }

    suspend fun addCurrency(code: String): Boolean =
        change {
            val normalized = code.trim().uppercase()
            val current = profile()
            if ((listOf(current.localCurrency) + current.currencies).any { it.code == normalized }) return@change
            val currency =
                container.catalog.find(normalized) ?: Currency.BTC.takeIf { normalized == it.code }
                    ?: invalid(SettingsError.INVALID_CURRENCY)
            container.storage.profiles.saveProfile(current.copy(currencies = current.currencies + currency))
        }

    suspend fun saveRate(
        code: String,
        text: String,
    ): Boolean =
        change {
            val profile = profile()
            val currency =
                state.value.currencies.firstOrNull { it.code == code && it.code != profile.localCurrency.code }
                    ?: invalid(SettingsError.INVALID_CURRENCY)
            val canonical = AmountReader.read(text, container.regional.settings().locale)?.canonical ?: invalid(SettingsError.INVALID_RATE)
            val rate =
                try {
                    ExchangeRate.parse(currency, profile.localCurrency, canonical)
                } catch (
                    _: IllegalArgumentException,
                ) {
                    invalid(SettingsError.INVALID_RATE)
                }
            val updated = ExchangeRateStrategy.ManualDaily(rate, container.clock.nowEpochMs())
            container.storage.profiles.saveProfile(
                profile.copy(
                    currencies = (profile.currencies + currency).distinctBy { it.code },
                    referenceRates = profile.referenceRates + (code to updated),
                ),
            )
        }

    suspend fun removeRate(code: String): Boolean =
        change {
            val profile = profile()
            if (code == profile.localCurrency.code ||
                state.value.currencies.none { it.code == code }
            ) {
                invalid(SettingsError.INVALID_CURRENCY)
            }
            val legacy = profile.strategy.currentRate
            val strategy =
                if (legacy.from.code == code && legacy.to.isSameAs(profile.localCurrency)) {
                    ExchangeRateStrategy.Fixed(ExchangeRate.identity(profile.localCurrency))
                } else {
                    profile.strategy
                }
            container.storage.profiles.saveProfile(profile.copy(referenceRates = profile.referenceRates - code, strategy = strategy))
        }

    private suspend fun owned(id: String): Wallet =
        container.storage.wallets
            .getWallet(id)
            ?.takeIf { it.ownerId == container.ownerId }
            ?: invalid(SettingsError.NOT_FOUND)

    private suspend fun change(action: suspend () -> Unit): Boolean {
        if (state.value.busy || !state.value.loaded) return false
        mutableState.update { it.copy(busy = true, saved = false, error = null) }
        return try {
            // Una navegación no debe cortar una operación entre sus escrituras locales.
            withContext(NonCancellable) {
                action()
                refresh()
            }
            mutableState.update { it.copy(saved = true) }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (validation: Validation) {
            mutableState.update { it.copy(error = validation.error) }
            false
        } catch (_: Exception) {
            mutableState.update { it.copy(error = SettingsError.STORAGE) }
            false
        } finally {
            mutableState.update { it.copy(busy = false) }
        }
    }

    private class Validation(
        val error: SettingsError,
    ) : RuntimeException()

    private fun invalid(error: SettingsError): Nothing = throw Validation(error)
}
