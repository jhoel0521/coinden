package com.coinden.core.data.memory

import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.category.CategoryRule
import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.port.output.CategorizationRepositoryPort
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort
import com.coinden.core.domain.port.output.DraftRepositoryPort
import com.coinden.core.domain.port.output.PreferencesRepositoryPort
import com.coinden.core.domain.port.output.WalletAliasRepositoryPort
import com.coinden.core.domain.port.output.WalletRepositoryPort
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
 * Adaptadores en memoria de PRODUCCIÓN, temporales hasta local-persistence: la app funciona,
 * pero los datos se pierden al cerrarla. Cumplen las mismas reglas que exige el dominio
 * (libros e historiales de solo agregar) y serializan el acceso con un Mutex.
 */

class InMemoryWalletRepository : WalletRepositoryPort {
    private val mutex = Mutex()
    private val wallets = mutableMapOf<String, Wallet>()
    private val events = mutableListOf<LedgerEvent>()

    override suspend fun saveWallet(wallet: Wallet) = mutex.withLock { wallets[wallet.id] = wallet }

    override suspend fun getWallet(walletId: String): Wallet? = mutex.withLock { wallets[walletId] }

    override suspend fun listWallets(ownerId: String): List<Wallet> = mutex.withLock { wallets.values.filter { it.ownerId == ownerId } }

    override suspend fun appendEvent(event: LedgerEvent) =
        mutex.withLock {
            check(events.none { it.id == event.id }) { "El libro es de solo agregar: el evento ${event.id} ya existe" }
            events += event
        }

    /** Todos o ninguno: se validan los ids antes de agregar. */
    override suspend fun appendEvents(events: List<LedgerEvent>) =
        mutex.withLock {
            val ids = events.map { it.id }
            check(
                ids.distinct().size == ids.size && this.events.none { it.id in ids },
            ) { "El libro es de solo agregar: un evento ya existe" }
            this.events += events
        }

    override suspend fun findEvent(eventId: String): LedgerEvent? = mutex.withLock { events.firstOrNull { it.id == eventId } }

    override suspend fun eventsForWallet(walletId: String): List<LedgerEvent> =
        mutex.withLock { events.filter { walletId in it.affectedWalletIds } }
}

class InMemoryDraftRepository : DraftRepositoryPort {
    private val mutex = Mutex()
    private val events = mutableListOf<DraftEvent>()

    override suspend fun appendEvent(event: DraftEvent) =
        mutex.withLock {
            check(events.none { it.id == event.id }) { "El historial de borradores es de solo agregar: ${event.id} ya existe" }
            events += event
        }

    override suspend fun eventsFor(draftId: String): List<DraftEvent> = mutex.withLock { events.filter { it.draftId == draftId } }

    override suspend fun draftIdsFor(ownerId: String): List<String> =
        mutex.withLock { events.filter { it.ownerId == ownerId }.map { it.draftId }.distinct() }
}

class InMemoryWalletAliasRepository : WalletAliasRepositoryPort {
    private val mutex = Mutex()
    private val byOwner = mutableMapOf<String, MutableMap<String, Set<String>>>()
    private val defaults = mutableMapOf<String, String?>()

    override suspend fun setAliases(
        ownerId: String,
        walletId: String,
        aliases: Set<String>,
    ) = mutex.withLock { byOwner.getOrPut(ownerId) { mutableMapOf() }[walletId] = aliases }

    override suspend fun aliasesFor(ownerId: String): Map<String, String> =
        mutex.withLock { byOwner[ownerId].orEmpty().flatMap { (walletId, aliases) -> aliases.map { it to walletId } }.toMap() }

    override suspend fun setDefaultWallet(
        ownerId: String,
        walletId: String?,
    ) = mutex.withLock { defaults[ownerId] = walletId }

    override suspend fun defaultWalletFor(ownerId: String): String? = mutex.withLock { defaults[ownerId] }
}

class InMemoryCategorizationRepository : CategorizationRepositoryPort {
    private val mutex = Mutex()
    private val classifications = mutableListOf<ClassificationEvent>()
    private val rules = mutableMapOf<Pair<String, String>, CategoryRule>()
    private val custom = mutableMapOf<String, CustomCategory>()

    override suspend fun appendClassification(event: ClassificationEvent) =
        mutex.withLock {
            check(classifications.none { it.id == event.id }) { "Las clasificaciones son de solo agregar: ${event.id} ya existe" }
            classifications += event
        }

    override suspend fun classificationsFor(movementId: String): List<ClassificationEvent> =
        mutex.withLock { classifications.filter { it.movementId == movementId } }

    override suspend fun classificationsOf(ownerId: String): List<ClassificationEvent> =
        mutex.withLock { classifications.filter { it.ownerId == ownerId } }

    override suspend fun saveRule(rule: CategoryRule) = mutex.withLock { rules[rule.ownerId to rule.key] = rule }

    override suspend fun rulesOf(ownerId: String): List<CategoryRule> = mutex.withLock { rules.values.filter { it.ownerId == ownerId } }

    override suspend fun saveCustomCategory(category: CustomCategory) = mutex.withLock { custom[category.id] = category }

    override suspend fun customCategoriesOf(ownerId: String): List<CustomCategory> =
        mutex.withLock { custom.values.filter { it.ownerId == ownerId } }
}

class InMemoryCurrencyProfileRepository : CurrencyProfileRepositoryPort {
    private val mutex = Mutex()
    private val profiles = mutableMapOf<String, UserCurrencyProfile>()

    override suspend fun saveProfile(profile: UserCurrencyProfile) = mutex.withLock { profiles[profile.userId] = profile }

    override suspend fun getProfile(userId: String): UserCurrencyProfile? = mutex.withLock { profiles[userId] }
}

class InMemoryPreferencesRepository : PreferencesRepositoryPort {
    private val mutex = Mutex()
    private var language: String? = null

    override suspend fun languageCode(): String? = mutex.withLock { language }

    override suspend fun setLanguageCode(code: String) = mutex.withLock { language = code }

    private var googleReader = false
    private var privacySeen = false
    private var appLock = false

    override suspend fun googleReaderEnabled(): Boolean = mutex.withLock { googleReader }

    override suspend fun setGoogleReaderEnabled(enabled: Boolean) = mutex.withLock { googleReader = enabled }

    override suspend fun appLockEnabled(): Boolean = mutex.withLock { appLock }

    override suspend fun setAppLockEnabled(enabled: Boolean) = mutex.withLock { appLock = enabled }

    override suspend fun privacyNoticeSeen(): Boolean = mutex.withLock { privacySeen }

    override suspend fun setPrivacyNoticeSeen() = mutex.withLock { privacySeen = true }
}
