package com.coinden.app

import com.coinden.core.data.memory.InMemoryCategorizationRepository
import com.coinden.core.data.memory.InMemoryDraftRepository
import com.coinden.core.data.memory.InMemoryWalletAliasRepository
import com.coinden.core.data.memory.InMemoryWalletRepository
import com.coinden.core.data.memory.StaticInstalledPacks
import com.coinden.core.data.memory.UuidIdGenerator
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.RegionalSettingsPort
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.SetDefaultWalletUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext

/**
 * Inyección de dependencias manual: arma el dominio real con los adaptadores en memoria.
 * Los datos viven mientras la app está abierta (la persistencia llega con local-persistence).
 */
class AppContainer(
    val regional: RegionalSettingsPort,
    device: DeviceIdentityPort,
    clock: ClockPort,
) {
    private val ids = UuidIdGenerator()
    private val walletRepository = InMemoryWalletRepository()
    private val packs = StaticInstalledPacks()

    /** Único usuario local del dispositivo; con UUID para que pueda sincronizarse mañana. */
    val ownerId: String = ids.newId()

    val ledger = LedgerContext(walletRepository, ids, clock, device)
    val categorization = CategorizationContext(InMemoryCategorizationRepository(), walletRepository, packs, ids, clock, device)
    val drafts =
        DraftContext(
            drafts = InMemoryDraftRepository(),
            wallets = walletRepository,
            aliases = InMemoryWalletAliasRepository(),
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
}
