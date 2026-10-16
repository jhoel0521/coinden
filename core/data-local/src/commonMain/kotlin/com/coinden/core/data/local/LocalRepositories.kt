package com.coinden.core.data.local

import com.coinden.core.data.local.db.CoinDenDatabase
import com.coinden.core.data.local.json.EventCodec
import com.coinden.core.data.local.json.StateCodec
import com.coinden.core.data.local.json.StoredEvent
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/*
 * Repositorios persistentes sobre la base local (SQLDelight). Cumplen los mismos contratos que
 * los de memoria: los eventos solo se agregan (un id repetido se rechaza) y se leen en el orden
 * en que se guardaron. Los drivers son síncronos: cada operación corre en [dispatcher].
 */

private object Streams {
    const val LEDGER = "ledger"
    const val DRAFT = "draft"
    const val CLASSIFICATION = "classification"
}

/** Escribe en el hilo de la base. Lo que devuelve el driver no se expone: los puertos devuelven Unit. */
private suspend fun write(
    dispatcher: CoroutineDispatcher,
    block: () -> Unit,
) = withContext(dispatcher) { block() }

/** Agrega un evento con sus referencias en una transacción; rechaza un id ya guardado. */
private fun CoinDenDatabase.append(
    id: String,
    stream: String,
    ownerId: String?,
    deviceId: String,
    occurredAt: Long,
    recordedAt: Long,
    stored: StoredEvent,
    refs: Set<String>,
) = transaction {
    check(coinDenQueries.eventExists(id).executeAsOne() == 0L) { "Los eventos son de solo agregar: el evento $id ya existe" }
    coinDenQueries.insertEvent(id, stream, ownerId, deviceId, occurredAt, recordedAt, stored.type, stored.version, stored.payload)
    refs.forEach { coinDenQueries.insertEventRef(id, it) }
}

class LocalWalletRepository(
    private val db: CoinDenDatabase,
    private val dispatcher: CoroutineDispatcher,
) : WalletRepositoryPort {
    private val q get() = db.coinDenQueries

    override suspend fun saveWallet(wallet: Wallet) =
        write(dispatcher) {
            q.upsertWallet(wallet.id, wallet.ownerId, StateCodec.encodeWallet(wallet))
        }

    override suspend fun getWallet(walletId: String): Wallet? =
        withContext(dispatcher) { q.walletById(walletId).executeAsOneOrNull()?.let(StateCodec::decodeWallet) }

    override suspend fun listWallets(ownerId: String): List<Wallet> =
        withContext(dispatcher) { q.walletsByOwner(ownerId).executeAsList().map(StateCodec::decodeWallet) }

    override suspend fun appendEvent(event: LedgerEvent) =
        withContext(dispatcher) {
            db.append(
                event.id,
                Streams.LEDGER,
                null,
                event.deviceId,
                event.occurredAtEpochMs,
                event.recordedAtEpochMs,
                EventCodec.encodeLedger(event),
                event.affectedWalletIds,
            )
        }

    /** Todos en una transacción: si uno falla, no queda ninguno. */
    override suspend fun appendEvents(events: List<LedgerEvent>) =
        withContext(dispatcher) {
            db.transaction {
                events.forEach { event ->
                    db.append(
                        event.id,
                        Streams.LEDGER,
                        null,
                        event.deviceId,
                        event.occurredAtEpochMs,
                        event.recordedAtEpochMs,
                        EventCodec.encodeLedger(event),
                        event.affectedWalletIds,
                    )
                }
            }
        }

    override suspend fun findEvent(eventId: String): LedgerEvent? =
        withContext(dispatcher) {
            q.eventById(eventId, Streams.LEDGER).executeAsOneOrNull()?.let { EventCodec.decodeLedger(it.type, it.version, it.payload) }
        }

    override suspend fun eventsForWallet(walletId: String): List<LedgerEvent> =
        withContext(dispatcher) {
            q.eventsByRef(walletId, Streams.LEDGER).executeAsList().mapNotNull { EventCodec.decodeLedger(it.type, it.version, it.payload) }
        }
}

class LocalDraftRepository(
    private val db: CoinDenDatabase,
    private val dispatcher: CoroutineDispatcher,
) : DraftRepositoryPort {
    private val q get() = db.coinDenQueries

    override suspend fun appendEvent(event: DraftEvent) =
        withContext(dispatcher) {
            db.append(
                event.id,
                Streams.DRAFT,
                event.ownerId,
                event.deviceId,
                event.occurredAtEpochMs,
                event.recordedAtEpochMs,
                EventCodec.encodeDraft(event),
                setOf(event.draftId),
            )
        }

    override suspend fun eventsFor(draftId: String): List<DraftEvent> =
        withContext(dispatcher) {
            q.eventsByRef(draftId, Streams.DRAFT).executeAsList().mapNotNull { EventCodec.decodeDraft(it.type, it.version, it.payload) }
        }

    override suspend fun draftIdsFor(ownerId: String): List<String> =
        withContext(dispatcher) {
            q.refsByOwner(Streams.DRAFT, ownerId).executeAsList()
        }
}

class LocalWalletAliasRepository(
    private val db: CoinDenDatabase,
    private val dispatcher: CoroutineDispatcher,
) : WalletAliasRepositoryPort {
    private val q get() = db.coinDenQueries

    override suspend fun setAliases(
        ownerId: String,
        walletId: String,
        aliases: Set<String>,
    ) = withContext(dispatcher) {
        db.transaction {
            q.deleteAliasesOfWallet(ownerId, walletId)
            aliases.forEach { q.insertAlias(ownerId, it, walletId) }
        }
    }

    override suspend fun aliasesFor(ownerId: String): Map<String, String> =
        withContext(dispatcher) { q.aliasesByOwner(ownerId).executeAsList().associate { it.alias to it.wallet_id } }

    override suspend fun setDefaultWallet(
        ownerId: String,
        walletId: String?,
    ) = write(dispatcher) { q.upsertDefaultWallet(ownerId, walletId) }

    override suspend fun defaultWalletFor(ownerId: String): String? =
        withContext(dispatcher) { q.defaultWallet(ownerId).executeAsOneOrNull()?.wallet_id }
}

class LocalCategorizationRepository(
    private val db: CoinDenDatabase,
    private val dispatcher: CoroutineDispatcher,
) : CategorizationRepositoryPort {
    private val q get() = db.coinDenQueries

    override suspend fun appendClassification(event: ClassificationEvent) =
        withContext(dispatcher) {
            db.append(
                event.id,
                Streams.CLASSIFICATION,
                event.ownerId,
                event.deviceId,
                event.occurredAtEpochMs,
                event.recordedAtEpochMs,
                EventCodec.encodeClassification(event),
                setOf(event.movementId),
            )
        }

    override suspend fun classificationsFor(movementId: String): List<ClassificationEvent> =
        withContext(dispatcher) {
            q.eventsByRef(movementId, Streams.CLASSIFICATION).executeAsList().mapNotNull {
                EventCodec.decodeClassification(it.type, it.version, it.payload)
            }
        }

    override suspend fun classificationsOf(ownerId: String): List<ClassificationEvent> =
        withContext(dispatcher) {
            q.eventsByOwner(Streams.CLASSIFICATION, ownerId).executeAsList().mapNotNull {
                EventCodec.decodeClassification(it.type, it.version, it.payload)
            }
        }

    override suspend fun saveRule(rule: CategoryRule) =
        write(dispatcher) {
            q.upsertRule(rule.ownerId, rule.key, StateCodec.encodeRule(rule))
        }

    override suspend fun rulesOf(ownerId: String): List<CategoryRule> =
        withContext(dispatcher) { q.rulesByOwner(ownerId).executeAsList().map(StateCodec::decodeRule) }

    override suspend fun saveCustomCategory(category: CustomCategory) =
        write(dispatcher) { q.upsertCustomCategory(category.id, category.ownerId, StateCodec.encodeCustomCategory(category)) }

    override suspend fun customCategoriesOf(ownerId: String): List<CustomCategory> =
        withContext(dispatcher) { q.customCategoriesByOwner(ownerId).executeAsList().map(StateCodec::decodeCustomCategory) }
}

class LocalCurrencyProfileRepository(
    private val db: CoinDenDatabase,
    private val dispatcher: CoroutineDispatcher,
) : CurrencyProfileRepositoryPort {
    private val q get() = db.coinDenQueries

    override suspend fun saveProfile(profile: UserCurrencyProfile) =
        write(dispatcher) { q.upsertProfile(profile.userId, StateCodec.encodeProfile(profile)) }

    override suspend fun getProfile(userId: String): UserCurrencyProfile? =
        withContext(dispatcher) { q.profileByUser(userId).executeAsOneOrNull()?.let(StateCodec::decodeProfile) }
}

class LocalPreferencesRepository(
    private val db: CoinDenDatabase,
    private val dispatcher: CoroutineDispatcher,
) : PreferencesRepositoryPort {
    override suspend fun languageCode(): String? = withContext(dispatcher) { db.coinDenQueries.settingValue(LANGUAGE).executeAsOneOrNull() }

    override suspend fun setLanguageCode(code: String) = write(dispatcher) { db.coinDenQueries.upsertSetting(LANGUAGE, code) }

    override suspend fun googleReaderEnabled(): Boolean = flag(GOOGLE_READER)

    override suspend fun setGoogleReaderEnabled(enabled: Boolean) =
        write(dispatcher) {
            db.coinDenQueries.upsertSetting(GOOGLE_READER, enabled.toString())
        }

    override suspend fun privacyNoticeSeen(): Boolean = flag(PRIVACY_SEEN)

    override suspend fun setPrivacyNoticeSeen() = write(dispatcher) { db.coinDenQueries.upsertSetting(PRIVACY_SEEN, "true") }

    /** Banderas guardadas como "true"/"false"; sin fila es falso (lo guardado antes no las trae). */
    private suspend fun flag(key: String): Boolean =
        withContext(dispatcher) { db.coinDenQueries.settingValue(key).executeAsOneOrNull() == "true" }

    private companion object {
        const val LANGUAGE = "language"
        const val GOOGLE_READER = "google_reader"
        const val PRIVACY_SEEN = "privacy_seen"
    }
}

/**
 * Identidad estable del dispositivo y del usuario local: UUID creados la primera vez y guardados
 * en la misma base cifrada (si la base se pierde, es un dispositivo nuevo).
 */
class LocalIdentity(
    private val db: CoinDenDatabase,
    private val newId: () -> String,
) {
    fun deviceId(): String = getOrCreate("device_id")

    fun ownerId(): String = getOrCreate("owner_id")

    private fun getOrCreate(name: String): String =
        db.transactionWithResult {
            db.coinDenQueries.identityValue(name).executeAsOneOrNull() ?: newId().also { db.coinDenQueries.insertIdentity(name, it) }
        }
}
