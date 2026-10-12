package com.coinden.core.domain.port.output

import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory

/**
 * Puerto de Salida (SPI) para el historial de borradores. Es append-only: [appendEvent] nunca
 * reemplaza ni borra un evento.
 */
interface DraftRepositoryPort {
    suspend fun appendEvent(event: DraftEvent)

    suspend fun eventsFor(draftId: String): List<DraftEvent>

    suspend fun draftIdsFor(ownerId: String): List<String>
}

/**
 * Puerto de Salida (SPI) del intérprete de capturas: IA local, reglas de texto, OCR o formulario.
 *
 * Funciona como una herramienta MCP: recibe la evidencia y el "esquema" disponible (billeteras,
 * monedas y alias) y devuelve propuestas tipadas. Nunca decide: el dominio valida y el usuario confirma.
 */
interface DraftInterpreterPort {
    suspend fun interpret(
        evidence: Evidence,
        wallets: WalletDirectory,
    ): List<MovementProposal>
}

/**
 * Puerto de Salida (SPI) de los alias de billetera definidos por el usuario ("qr" -> BNB Bs).
 * Son metadatos: se guardan como estado actual.
 */
interface WalletAliasRepositoryPort {
    suspend fun setAliases(
        ownerId: String,
        walletId: String,
        aliases: Set<String>,
    )

    /** Alias normalizado -> id de billetera. */
    suspend fun aliasesFor(ownerId: String): Map<String, String>

    /** Billetera elegida para las capturas que no nombran billetera (null = sin elección). */
    suspend fun setDefaultWallet(
        ownerId: String,
        walletId: String?,
    )

    suspend fun defaultWalletFor(ownerId: String): String?
}
