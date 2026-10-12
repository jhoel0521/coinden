package com.coinden.core.domain.model.draft

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.wallet.Wallet

/** Por dónde llegó una captura. */
enum class EvidenceSource {
    TEXT,
    VOICE,
    PHOTO,
    MANUAL,
}

/**
 * Evidencia original de una captura. Se guarda siempre, aunque nadie la entienda.
 *
 * [text] es lo escrito o transcrito; [attachmentRef] referencia un audio o una foto
 * guardados fuera del dominio.
 */
data class Evidence(
    val source: EvidenceSource,
    val text: String? = null,
    val attachmentRef: String? = null,
)

/**
 * Monto tal como lo propuso un intérprete: decimal canónico ("45", "45.50") y, si la conoce,
 * la moneda. Si falta la moneda, se toma la de la billetera al evaluar.
 */
data class ProposedAmount(
    val value: String,
    val currency: Currency? = null,
)

/** Billetera indicada por su id o por una pista de texto ("qr", "meru") que se resuelve con alias. */
sealed interface WalletRef {
    data class ById(val walletId: String) : WalletRef

    data class ByHint(val hint: String) : WalletRef
}

/** Intenciones reconocibles que no son movimientos de dinero (se guardan como No soportado). */
enum class NonMovementIntent {
    RECURRING_PAYMENT,
    LOAN,
    SAVINGS_GOAL,
    CATEGORY,
    WALLET_SETUP,
    ANNULMENT,
    QUERY,
    OTHER,
}

/**
 * Lo que un intérprete (IA local, reglas de texto, OCR o formulario) entendió de una evidencia.
 *
 * El tipo ES el movimiento: no hay enum de "kind". Cualquier campo puede faltar; el dominio
 * decide con [DraftEvaluator] si alcanza para un borrador Listo.
 */
sealed interface MovementProposal {
    data class Expense(
        val amount: ProposedAmount? = null,
        val wallet: WalletRef? = null,
        val occurredAtEpochMs: Long? = null,
        val note: String? = null,
    ) : MovementProposal

    data class Income(
        val amount: ProposedAmount? = null,
        val wallet: WalletRef? = null,
        val occurredAtEpochMs: Long? = null,
        val note: String? = null,
    ) : MovementProposal

    /** Entre dos billeteras: misma moneda (cajero, pagar la tarjeta) o cambio de moneda. */
    data class Transfer(
        val out: ProposedAmount? = null,
        val from: WalletRef? = null,
        val into: ProposedAmount? = null,
        val to: WalletRef? = null,
        val occurredAtEpochMs: Long? = null,
        val note: String? = null,
    ) : MovementProposal

    /** "En la billetera tengo 140": el saldo real, no la diferencia. */
    data class AdjustTo(
        val target: ProposedAmount? = null,
        val wallet: WalletRef? = null,
        val occurredAtEpochMs: Long? = null,
        val note: String? = null,
    ) : MovementProposal

    data class NotAMovement(
        val intent: NonMovementIntent,
        val note: String? = null,
    ) : MovementProposal

    data class Unknown(
        val note: String? = null,
    ) : MovementProposal
}

/** Datos que faltan para que un borrador esté Listo. */
enum class DraftField {
    KIND,
    AMOUNT,
    WALLET,
    TO_AMOUNT,
    TO_WALLET,
}

/** Contradicciones que impiden que un borrador esté Listo. */
enum class DraftIssue {
    INVALID_AMOUNT,
    CURRENCY_MISMATCH,
    UNKNOWN_WALLET,
    UNKNOWN_WALLET_HINT,
    WALLET_ARCHIVED,
    SAME_WALLET,
    INCONSISTENT_AMOUNTS,
}

/**
 * Instantánea de las billeteras de un usuario y sus alias: el "esquema" que se le da al
 * intérprete y con el que el dominio resuelve las propuestas.
 */
data class WalletDirectory(
    val wallets: List<Wallet>,
    val aliases: Map<String, String> = emptyMap(),
) {
    fun byId(walletId: String): Wallet? = wallets.firstOrNull { it.id == walletId }

    fun byHint(hint: String): Wallet? = aliases[normalizeAlias(hint)]?.let(::byId)

    companion object {
        fun normalizeAlias(alias: String): String = alias.trim().lowercase()
    }
}
