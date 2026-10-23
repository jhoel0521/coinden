package com.coinden.app

import com.coinden.app.capture.AttachmentStore
import com.coinden.app.capture.InMemoryAttachmentStore
import com.coinden.app.currency.CurrencyNames
import com.coinden.app.i18n.LanguageAwarePacks
import com.coinden.core.data.memory.InMemoryBundleRepository
import com.coinden.core.data.memory.InMemoryCategorizationRepository
import com.coinden.core.data.memory.InMemoryCurrencyProfileRepository
import com.coinden.core.data.memory.InMemoryDraftRepository
import com.coinden.core.data.memory.InMemoryPreferencesRepository
import com.coinden.core.data.memory.InMemoryWalletAliasRepository
import com.coinden.core.data.memory.InMemoryWalletRepository
import com.coinden.core.data.memory.UuidIdGenerator
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.interpret.BundleInterpreter
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.port.output.BundleRepositoryPort
import com.coinden.core.domain.port.output.CategorizationRepositoryPort
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.CurrencyCatalogPort
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.DraftInterpreterPort
import com.coinden.core.domain.port.output.DraftRepositoryPort
import com.coinden.core.domain.port.output.PreferencesRepositoryPort
import com.coinden.core.domain.port.output.RegionalSettingsPort
import com.coinden.core.domain.port.output.WalletAliasRepositoryPort
import com.coinden.core.domain.port.output.WalletRepositoryPort
import com.coinden.core.domain.usecase.GetConsolidatedPortfolioUseCase
import com.coinden.core.domain.usecase.bundle.BundleContext
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.category.SpendingByCategoryUseCase
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmReadyDraftsUseCase
import com.coinden.core.domain.usecase.draft.CorrectMovementUseCase
import com.coinden.core.domain.usecase.draft.DiscardDraftUseCase
import com.coinden.core.domain.usecase.draft.DismissCorrectionUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.FindSameMovementQuestionsUseCase
import com.coinden.core.domain.usecase.draft.GetCaptureStatsUseCase
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.GetMovementHistoryUseCase
import com.coinden.core.domain.usecase.draft.GetMovementTimelineUseCase
import com.coinden.core.domain.usecase.draft.GetPendingCorrectionUseCase
import com.coinden.core.domain.usecase.draft.InterpretPerceivedUseCase
import com.coinden.core.domain.usecase.draft.ResolveSameMovementUseCase
import com.coinden.core.domain.usecase.draft.ReviseDraftUseCase
import com.coinden.core.domain.usecase.draft.SetDefaultWalletUseCase
import com.coinden.core.domain.usecase.draft.SettleIdenticalReceiptUseCase
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
    /** Paquetes de captura: lo que la persona manda junto en un momento. */
    val bundles: BundleRepositoryPort,
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
                InMemoryBundleRepository(),
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
    /** Fotos, PDF y audios de las evidencias: cifrados en el teléfono, en memoria en las pruebas. */
    val attachments: AttachmentStore = InMemoryAttachmentStore(),
    /**
     * Los paquetes de idioma y plantillas que la persona descargó. Se suman a los que viajan con
     * la app; vacío significa que todavía no descargó ninguno, y la app funciona igual.
     */
    private val downloadedPacks: () -> List<String> = { emptyList() },
) {
    private val languageState = MutableStateFlow(initialLanguage)

    /** Idioma de la app como flujo: la interfaz cambia al instante al elegir otro. */
    val language: StateFlow<String> = languageState.asStateFlow()
    val languageCode: String get() = languageState.value

    private val ids = UuidIdGenerator()
    private val packs = LanguageAwarePacks({ languageCode }, downloadedPacks)

    val ledger = LedgerContext(storage.wallets, ids, clock, device)
    val categorization = CategorizationContext(storage.categorization, storage.wallets, packs, ids, clock, device)
    val spendingByCategory = SpendingByCategoryUseCase(categorization, storage.profiles, cryptoRates = emptyList())
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

    /**
     * Los paquetes de captura: lo que la persona manda junto se interpreta junto. El intérprete
     * del paquete son las reglas; cuando haya modelo local, entra con el mismo contrato.
     */
    val bundles =
        BundleContext(
            bundles = storage.bundles,
            drafts = drafts,
            interpreter = BundleInterpreter(interpreter),
        )

    val createWallet = CreateWalletUseCase(ledger)
    val setDefaultWallet = SetDefaultWalletUseCase(drafts)
    val captureDraft = CaptureDraftUseCase(drafts, interpreter)

    /** Captura del formulario: guarda la evidencia sin interpretarla; la propuesta la da el formulario. */
    val captureManual =
        CaptureDraftUseCase(
            drafts,
            object : DraftInterpreterPort {
                override suspend fun interpret(
                    evidence: Evidence,
                    wallets: WalletDirectory,
                ): List<MovementProposal> = emptyList()
            },
        )
    val draftInbox = GetDraftInboxUseCase(drafts)
    val reviseDraft = ReviseDraftUseCase(drafts)
    val confirmDraft = ConfirmDraftUseCase(drafts, ledger)
    val findSameMovementQuestions = FindSameMovementQuestionsUseCase(drafts)
    val resolveSameMovement = ResolveSameMovementUseCase(drafts)
    val interpretPerceived = InterpretPerceivedUseCase(drafts, interpreter)
    val discardDraft = DiscardDraftUseCase(drafts)
    val correctMovement = CorrectMovementUseCase(drafts, ledger)
    val pendingCorrection = GetPendingCorrectionUseCase(drafts)
    val dismissCorrection = DismissCorrectionUseCase(drafts)
    private val settleIdenticalReceipt = SettleIdenticalReceiptUseCase(drafts)
    val confirmReadyDrafts = ConfirmReadyDraftsUseCase(drafts, ledger)
    val movementHistory = GetMovementHistoryUseCase(drafts)
    val movementTimeline = GetMovementTimelineUseCase(drafts)
    val captureStats = GetCaptureStatsUseCase(drafts)

    /** Un id global nuevo (UUID), por ejemplo para reintentar una corrección sin duplicarla. */
    fun newId(): String = ids.newId()

    /** Cada comprobante idéntico a un confirmado queda como su evidencia: se devuelve el confirmado. */
    suspend fun settleIdentical(captured: List<Draft>): List<Draft> = captured.map { settleIdenticalReceipt(it.id) ?: it }

    /** Desfase de la zona horaria del teléfono: define el "mismo día" de dos capturas. */
    val utcOffsetMinutes: Int get() = regional.settings().utcOffsetMinutes
    val walletBalances = GetActiveWalletBalancesUseCase(storage.wallets)
    val portfolio = GetConsolidatedPortfolioUseCase(storage.profiles, cryptoRates = emptyList())

    /** El onboarding terminó si hay perfil de moneda y al menos una billetera activa. */
    suspend fun isOnboarded(): Boolean =
        storage.profiles.getProfile(ownerId) != null && storage.wallets.listWallets(ownerId).any { it.isActive }

    /** Cambia el idioma de la app y del intérprete, y lo recuerda. */
    suspend fun chooseLanguage(code: String) {
        languageState.value = code
        storage.preferences.setLanguageCode(code)
    }
}
