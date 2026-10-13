package com.coinden.app

import com.coinden.core.data.memory.InMemoryCategorizationRepository
import com.coinden.core.data.memory.InMemoryCurrencyProfileRepository
import com.coinden.core.data.memory.InMemoryDraftRepository
import com.coinden.core.data.memory.InMemoryWalletAliasRepository
import com.coinden.core.data.memory.InMemoryWalletRepository
import com.coinden.core.data.memory.StaticInstalledPacks
import com.coinden.core.data.memory.UuidIdGenerator
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.port.output.CategorizationRepositoryPort
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.DraftRepositoryPort
import com.coinden.core.domain.port.output.RegionalSettingsPort
import com.coinden.core.domain.port.output.WalletAliasRepositoryPort
import com.coinden.core.domain.port.output.WalletRepositoryPort
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.SetDefaultWalletUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext

/** Los puertos de almacenamiento: la base local cifrada en la app, en memoria en las pruebas. */
class AppStorage(
    val wallets: WalletRepositoryPort,
    val drafts: DraftRepositoryPort,
    val aliases: WalletAliasRepositoryPort,
    val categorization: CategorizationRepositoryPort,
    val profiles: CurrencyProfileRepositoryPort,
) {
    companion object {
        fun inMemory() =
            AppStorage(
                InMemoryWalletRepository(),
                InMemoryDraftRepository(),
                InMemoryWalletAliasRepository(),
                InMemoryCategorizationRepository(),
                InMemoryCurrencyProfileRepository(),
            )
    }
}

/**
 * Inyección de dependencias manual: arma el dominio real con el almacenamiento recibido.
 * [ownerId] es el usuario local, estable entre sesiones (lo guarda el almacenamiento).
 */
class AppContainer(
    storage: AppStorage,
    val ownerId: String,
    val regional: RegionalSettingsPort,
    device: DeviceIdentityPort,
    clock: ClockPort,
) {
    private val ids = UuidIdGenerator()
    private val packs = StaticInstalledPacks()

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
    val setWalletAliases = SetWalletAliasesUseCase(drafts)
    val setDefaultWallet = SetDefaultWalletUseCase(drafts)
    val captureDraft = CaptureDraftUseCase(drafts, interpreter)
    val draftInbox = GetDraftInboxUseCase(drafts)
}
