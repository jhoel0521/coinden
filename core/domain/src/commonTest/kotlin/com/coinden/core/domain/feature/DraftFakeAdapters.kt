package com.coinden.core.domain.feature

import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.port.output.DraftInterpreterPort
import com.coinden.core.domain.port.output.DraftRepositoryPort
import com.coinden.core.domain.port.output.WalletAliasRepositoryPort

/**
 * Historial de borradores en memoria, append-only.
 */
class InMemoryDraftRepository : DraftRepositoryPort {
    private val events = mutableListOf<DraftEvent>()

    override suspend fun appendEvent(event: DraftEvent) {
        check(events.none { it.id == event.id }) { "El historial es append-only: el evento ${event.id} ya existe" }
        events += event
    }

    override suspend fun eventsFor(draftId: String): List<DraftEvent> = events.filter { it.draftId == draftId }

    override suspend fun draftIdsFor(ownerId: String): List<String> = events.filter { it.ownerId == ownerId }.map { it.draftId }.distinct()

    val allEvents: List<DraftEvent> get() = events.toList()
}

/**
 * Alias de billetera en memoria.
 */
class InMemoryWalletAliasRepository : WalletAliasRepositoryPort {
    private val byOwner = mutableMapOf<String, MutableMap<String, Set<String>>>()

    override suspend fun setAliases(
        ownerId: String,
        walletId: String,
        aliases: Set<String>,
    ) {
        byOwner.getOrPut(ownerId) { mutableMapOf() }[walletId] = aliases
    }

    override suspend fun aliasesFor(ownerId: String): Map<String, String> =
        byOwner[ownerId].orEmpty().flatMap { (walletId, aliases) -> aliases.map { it to walletId } }.toMap()

    private val defaults = mutableMapOf<String, String?>()

    override suspend fun setDefaultWallet(
        ownerId: String,
        walletId: String?,
    ) {
        defaults[ownerId] = walletId
    }

    override suspend fun defaultWalletFor(ownerId: String): String? = defaults[ownerId]
}

/**
 * Intérprete de prueba: devuelve propuestas fijas según el texto de la evidencia (o la referencia
 * del adjunto). Simula a la IA local sin red ni modelos.
 */
class ScriptedInterpreter(
    private val script: Map<String, List<MovementProposal>> = emptyMap(),
    private val failingKeys: Set<String> = emptySet(),
) : DraftInterpreterPort {
    var lastDirectory: WalletDirectory? = null
        private set

    override suspend fun interpret(
        evidence: Evidence,
        wallets: WalletDirectory,
    ): List<MovementProposal> {
        lastDirectory = wallets
        val key = evidence.text ?: evidence.attachmentRef ?: ""
        if (key in failingKeys) throw IllegalStateException("Intérprete caído para '$key'")
        return script[key].orEmpty()
    }
}
