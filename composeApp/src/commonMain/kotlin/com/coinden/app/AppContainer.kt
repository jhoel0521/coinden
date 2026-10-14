package com.coinden.app

import com.coinden.app.currency.CurrencyNames
import com.coinden.app.i18n.LanguageAwarePacks
import com.coinden.core.data.memory.InMemoryCategorizationRepository
import com.coinden.core.data.memory.InMemoryCurrencyProfileRepository
import com.coinden.core.data.memory.InMemoryDraftRepository
import com.coinden.core.data.memory.InMemoryPreferencesRepository
import com.coinden.core.data.memory.InMemoryWalletAliasRepository
import com.coinden.core.data.memory.InMemoryWalletRepository
import com.coinden.core.data.memory.UuidIdGenerator
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.port.output.CategorizationRepositoryPort
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.CurrencyCatalogPort
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.DraftRepositoryPort
import com.coinden.core.domain.port.output.PreferencesRepositoryPort
import com.coinden.core.domain.port.output.RegionalSettingsPort
import com.coinden.core.domain.port.output.WalletAliasRepositoryPort
import com.coinden.core.domain.port.output.WalletRepositoryPort
import com.coinden.core.domain.usecase.GetConsolidatedPortfolioUseCase
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.SetDefaultWalletUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetActiveWalletBalancesUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Los puertos de almacenamiento: la base local cifrada en la app, en memoria en las pruebas. */
class AppStorage(
    val wallets: WalletRepositoryPort,
    val drafts: DraftRepositoryPort,
    val aliases: WalletAliasRepositoryPort,
    val categorization: CategorizationRepositoryPort,
    val profiles: CurrencyProfileRepositoryPort,
    val preferences: PreferencesRepositoryPort,
) {
    companion object {
        fun inMemory() =
            AppStorage(
                InMemoryWalletRepository(),
                InMemoryDraftRepository(),
                InMemoryWalletAliasRepository(),
                InMemoryCategorizationRepository(),
                InMemoryCurrencyProfileRepository(),
                InMemoryPreferencesRepository(),
            )
    }
}

/**
 * Inyección de dependencias manual: arma el dominio real con el almacenamiento recibido.
 * [ownerId] es el usuario local, estable entre sesiones (lo guarda el almacenamiento).
 * [initialLanguage] es el idioma con el que arranca: la preferencia guardada o el del teléfono.
 */
class AppContainer(
    val storage: AppStorage,
    val ownerId: String,
    val regional: RegionalSettingsPort,
    device: DeviceIdentityPort,
    val clock: ClockPort,
    val catalog: CurrencyCatalogPort,
    val currencyNames: CurrencyNames,
    initialLanguage: String = "es",
) {
    private val languageState = MutableStateFlow(initialLanguage)

    /** Idioma de la app como flujo: la interfaz cambia al instante al elegir otro. */
    val language: StateFlow<String> = languageState.asStateFlow()
    val languageCode: String get() = languageState.value

    private val ids = UuidIdGenerator()
    private val packs = LanguageAwarePacks({ languageCode })

    val ledger = LedgerContext(storage.wallets, ids, clock, device)
    val categorization = CategorizationContext(storage.categorization, storage.wallets, packs, ids, clock, device)
    val drafts =
        DraftContext(
            drafts = storage.drafts,
            wallets = storage.wallets,
            aliases = storage.aliases,
            ids = ids,
            clock = clock,
            device = device,
            categorization = categorization,
        )
    val interpreter = TextDraftInterpreter(packs, regional)

    val createWallet = CreateWalletUseCase(ledger)
    val setDefaultWallet = SetDefaultWalletUseCase(drafts)
    val captureDraft = CaptureDraftUseCase(drafts, interpreter)
    val draftInbox = GetDraftInboxUseCase(drafts)
    val walletBalances = GetActiveWalletBalancesUseCase(storage.wallets)
    val portfolio = GetConsolidatedPortfolioUseCase(storage.profiles)

    /** El onboarding terminó si hay perfil de moneda y al menos una billetera activa. */
    suspend fun isOnboarded(): Boolean =
        storage.profiles.getProfile(ownerId) != null && storage.wallets.listWallets(ownerId).any { it.isActive }

    /** Cambia el idioma de la app y del intérprete, y lo recuerda. */
    suspend fun chooseLanguage(code: String) {
        languageState.value = code
        storage.preferences.setLanguageCode(code)
    }
}
