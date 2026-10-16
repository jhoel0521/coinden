package com.coinden.core.domain.model.draft

import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.CategoryRef

/** Tipo de un movimiento confirmado, para mostrar que cambió (gasto -> ingreso). */
enum class MovementType { EXPENSE, INCOME, TRANSFER, ADJUSTMENT }

val ReadyMovement.type: MovementType
    get() =
        when (this) {
            is ReadyMovement.Expense -> MovementType.EXPENSE
            is ReadyMovement.Income -> MovementType.INCOME
            is ReadyMovement.Transfer -> MovementType.TRANSFER
            is ReadyMovement.AdjustTo -> MovementType.ADJUSTMENT
        }

/** Campos que una corrección puede cambiar, en el orden en que se muestran. */
enum class CorrectionField { KIND, AMOUNT, WALLET, TO_AMOUNT, TO_WALLET, OCCURRED_AT, NOTE, CATEGORY, REFERENCE }

/**
 * Un campo que cambia con su valor anterior y el nuevo, como datos (sin texto): la presentación
 * los formatea. Un monto incluye su moneda.
 */
sealed interface FieldChange {
    val field: CorrectionField

    data class Kind(
        val before: MovementType,
        val after: MovementType,
    ) : FieldChange {
        override val field get() = CorrectionField.KIND
    }

    /** [large]: el monto nuevo es el doble o más, o la mitad o menos, del anterior. */
    data class Amount(
        val before: Money,
        val after: Money,
        val large: Boolean,
    ) : FieldChange {
        override val field get() = CorrectionField.AMOUNT
    }

    data class Wallet(
        val before: String,
        val after: String,
    ) : FieldChange {
        override val field get() = CorrectionField.WALLET
    }

    data class ToAmount(
        val before: Money?,
        val after: Money?,
    ) : FieldChange {
        override val field get() = CorrectionField.TO_AMOUNT
    }

    data class ToWallet(
        val before: String?,
        val after: String?,
    ) : FieldChange {
        override val field get() = CorrectionField.TO_WALLET
    }

    data class OccurredAt(
        val before: Long,
        val after: Long,
    ) : FieldChange {
        override val field get() = CorrectionField.OCCURRED_AT
    }

    data class Note(
        val before: String?,
        val after: String?,
    ) : FieldChange {
        override val field get() = CorrectionField.NOTE
    }

    data class Category(
        val before: CategoryRef?,
        val after: CategoryRef?,
    ) : FieldChange {
        override val field get() = CorrectionField.CATEGORY
    }

    data class Reference(
        val before: String?,
        val after: String?,
    ) : FieldChange {
        override val field get() = CorrectionField.REFERENCE
    }
}

/** Quién pidió la corrección: la persona a mano o un comprobante (que la persona aprobó). */
enum class CorrectionOrigin { MANUAL, EVIDENCE }

/** Compara un movimiento confirmado con su corrección, de forma pura. */
object MovementDiff {
    private val MONEY_FIELDS =
        setOf(
            CorrectionField.KIND,
            CorrectionField.AMOUNT,
            CorrectionField.WALLET,
            CorrectionField.TO_AMOUNT,
            CorrectionField.TO_WALLET,
            CorrectionField.OCCURRED_AT,
        )

    fun between(
        before: ReadyMovement,
        beforeCategory: CategoryRef?,
        after: ReadyMovement,
        afterCategory: CategoryRef?,
    ): List<FieldChange> {
        val b = Facts.of(before)
        val a = Facts.of(after)
        return listOfNotNull(
            FieldChange.Kind(before.type, after.type).takeIf { it.before != it.after },
            FieldChange.Amount(b.amount, a.amount, isLargeAmountChange(b.amount, a.amount)).takeIf { b.amount != a.amount },
            FieldChange.Wallet(b.walletId, a.walletId).takeIf { b.walletId != a.walletId },
            FieldChange.ToAmount(b.toAmount, a.toAmount).takeIf { b.toAmount != a.toAmount },
            FieldChange.ToWallet(b.toWalletId, a.toWalletId).takeIf { b.toWalletId != a.toWalletId },
            FieldChange.OccurredAt(before.occurredAtEpochMs, after.occurredAtEpochMs).takeIf { it.before != it.after },
            FieldChange.Note(before.note, after.note).takeIf { it.before != it.after },
            FieldChange.Category(beforeCategory, afterCategory).takeIf { it.before != it.after },
            FieldChange.Reference(b.reference, a.reference).takeIf { it.before != it.after },
        )
    }

    /** Si alguno de los cambios obliga a anular el movimiento y registrar otro en el libro. */
    fun touchesMoney(changes: List<FieldChange>): Boolean = changes.any { it.field in MONEY_FIELDS }

    /** El doble o más, o la mitad o menos, en la misma moneda. Otra moneda no se compara. */
    fun isLargeAmountChange(
        before: Money,
        after: Money,
    ): Boolean {
        if (before.currency != after.currency || before.isZero) return false
        return after >= before + before || after + after <= before
    }

    private class Facts(
        val amount: Money,
        val walletId: String,
        val toAmount: Money? = null,
        val toWalletId: String? = null,
        val reference: String? = null,
    ) {
        companion object {
            fun of(m: ReadyMovement): Facts =
                when (m) {
                    is ReadyMovement.Expense -> Facts(m.amount, m.walletId, reference = m.reference)
                    is ReadyMovement.Income -> Facts(m.amount, m.walletId, reference = m.reference)
                    is ReadyMovement.Transfer -> Facts(m.out, m.fromWalletId, m.into, m.toWalletId, m.reference)
                    is ReadyMovement.AdjustTo -> Facts(m.target, m.walletId)
                }
        }
    }
}

/** Lo que le pasó a un movimiento confirmado, en orden: para mostrarlo en su historial. */
sealed interface TimelineEntry {
    val atEpochMs: Long

    data class Confirmed(
        override val atEpochMs: Long,
        val movement: ReadyMovement,
    ) : TimelineEntry

    /**
     * Una corrección aprobada. Desde un comprobante, [read] es lo que leyó el comprobante (antes de
     * que la persona lo corrigiera en el formulario) y [evidence] el comprobante.
     */
    data class Corrected(
        override val atEpochMs: Long,
        val origin: CorrectionOrigin,
        val changes: List<FieldChange>,
        val read: ReadyMovement? = null,
        val evidence: Evidence? = null,
    ) : TimelineEntry

    /** Se sumó evidencia sin cambiar datos ("son el mismo", cambio descartado o comprobante idéntico). */
    data class EvidenceAdded(
        override val atEpochMs: Long,
        val evidence: Evidence,
    ) : TimelineEntry
}
