package com.coinden.core.domain.model.draft

import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.CategorySuggestion
import com.coinden.core.domain.model.wallet.WalletError

/** Motivos de rechazo de una operación sobre borradores. Sin textos: la presentación los traduce. */
sealed interface DraftError {
    data object NotFound : DraftError

    data object TerminalState : DraftError

    data class NotReady(
        val missing: Set<DraftField>,
        val issues: Set<DraftIssue>,
    ) : DraftError

    /** El ajuste pide el saldo que la billetera ya tiene. */
    data object NothingToAdjust : DraftError

    /** El libro de movimientos rechazó el movimiento (por ejemplo, saldo insuficiente). */
    data class Wallet(
        val error: WalletError,
    ) : DraftError

    /** Solo se corrige un movimiento confirmado. */
    data object NotConfirmed : DraftError

    /** Los ajustes a un saldo objetivo no se corrigen: se registra otro ajuste. */
    data object NotCorrectable : DraftError

    data class AliasAlreadyUsed(
        val alias: String,
        val walletId: String,
    ) : DraftError
}

sealed interface DraftResult {
    data class Ok(
        val draft: Draft,
    ) : DraftResult

    data class Rejected(
        val error: DraftError,
    ) : DraftResult
}

/** Saldo de una billetera si se confirmaran todos sus borradores Listos. */
data class ProjectedBalance(
    val walletId: String,
    val current: Money,
    val projected: Money,
    val hasShortfall: Boolean,
)

/**
 * La bandeja de revisión: pendientes, proyección por billetera y, para los borradores de gasto,
 * la categoría sugerida (id del borrador -> sugerencia). La categoría no afecta la confianza del borrador.
 */
data class DraftInbox(
    val pending: List<Draft>,
    val projections: List<ProjectedBalance>,
    val suggestions: Map<String, CategorySuggestion> = emptyMap(),
    /**
     * Los que se pueden confirmar de una vez ("Listos"): Listos de confianza ALTA, sin pregunta de
     * mismo movimiento ni cambio pendiente, en el orden de la bandeja. Todo lo demás se revisa.
     */
    val batchReady: List<String> = emptyList(),
    /** Preguntas de mismo movimiento de los pendientes. */
    val questions: List<SameMovementQuestion> = emptyList(),
)
