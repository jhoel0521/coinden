package com.coinden.core.domain.model.draft

/**
 * Por qué dos borradores pueden ser el mismo movimiento. Ordenados del más seguro al menos:
 * los tres primeros son una captura repetida; el último, una coincidencia que hay que preguntar.
 */
enum class SameMovementReason {
    /** El mismo archivo (foto, PDF o audio), por su huella. */
    SAME_FILE,

    /** La misma referencia del comprobante y el mismo monto. */
    SAME_REFERENCE,

    /** El mismo texto de un comprobante de varias líneas. */
    SAME_TEXT,

    /** Mismo tipo, mismo monto y moneda, mismo día local. */
    SAME_DAY_AMOUNT,
    ;

    val isRepeatedCapture: Boolean get() = this != SAME_DAY_AMOUNT
}

/** "¿[draftId] es el mismo movimiento que [otherDraftId]?" La pregunta va en el capturado después. */
data class SameMovementQuestion(
    val draftId: String,
    val otherDraftId: String,
    val reason: SameMovementReason,
)

/** Clave para comparar referencias: mayúsculas, solo letras y dígitos ("E-0001 2" -> "E00012"). */
object ReferenceKey {
    fun of(reference: String): String = reference.uppercase().filter { it.isLetterOrDigit() }
}

/**
 * Combina dos propuestas del mismo movimiento ("son el mismo"). Monto, moneda, billetera y momento
 * leídos de un comprobante ([FieldOrigin.EVIDENCE]) ganan a los dichos o sugeridos; si no, gana el
 * borrador que queda. La nota dicha por la persona gana a la del comprobante. Medio de pago y
 * referencia se toman de quien los tenga.
 */
object ProposalMerge {
    fun combine(
        kept: MovementProposal?,
        absorbed: MovementProposal?,
    ): MovementProposal? {
        if (kept == null) return absorbed
        if (absorbed == null) return kept
        val pick = Picker(kept, absorbed)
        return when {
            kept is MovementProposal.Expense && absorbed is MovementProposal.Expense -> {
                val amount = pick(DraftField.AMOUNT, kept.amount, absorbed.amount, DraftField.CURRENCY)
                val wallet = pick(DraftField.WALLET, kept.wallet, absorbed.wallet)
                val occurredAt = pick(DraftField.OCCURRED_AT, kept.occurredAtEpochMs, absorbed.occurredAtEpochMs)
                kept.copy(
                    amount = amount,
                    wallet = wallet,
                    occurredAtEpochMs = occurredAt,
                    note = pick.note(kept.note, absorbed.note),
                    origins = pick.origins,
                    paymentMethod = kept.paymentMethod ?: absorbed.paymentMethod,
                    walletSuggestion = if (pick.tookAbsorbed(DraftField.WALLET)) absorbed.walletSuggestion else kept.walletSuggestion,
                    reference = kept.reference ?: absorbed.reference,
                )
            }
            kept is MovementProposal.Income && absorbed is MovementProposal.Income -> {
                val amount = pick(DraftField.AMOUNT, kept.amount, absorbed.amount, DraftField.CURRENCY)
                val wallet = pick(DraftField.WALLET, kept.wallet, absorbed.wallet)
                val occurredAt = pick(DraftField.OCCURRED_AT, kept.occurredAtEpochMs, absorbed.occurredAtEpochMs)
                kept.copy(
                    amount = amount,
                    wallet = wallet,
                    occurredAtEpochMs = occurredAt,
                    note = pick.note(kept.note, absorbed.note),
                    origins = pick.origins,
                    paymentMethod = kept.paymentMethod ?: absorbed.paymentMethod,
                    walletSuggestion = if (pick.tookAbsorbed(DraftField.WALLET)) absorbed.walletSuggestion else kept.walletSuggestion,
                    reference = kept.reference ?: absorbed.reference,
                )
            }
            kept is MovementProposal.Transfer && absorbed is MovementProposal.Transfer -> {
                val out = pick(DraftField.AMOUNT, kept.out, absorbed.out, DraftField.CURRENCY)
                val from = pick(DraftField.WALLET, kept.from, absorbed.from)
                val into = pick(DraftField.TO_AMOUNT, kept.into, absorbed.into)
                val to = pick(DraftField.TO_WALLET, kept.to, absorbed.to)
                val occurredAt = pick(DraftField.OCCURRED_AT, kept.occurredAtEpochMs, absorbed.occurredAtEpochMs)
                kept.copy(
                    out = out,
                    from = from,
                    into = into,
                    to = to,
                    occurredAtEpochMs = occurredAt,
                    note = pick.note(kept.note, absorbed.note),
                    origins = pick.origins,
                    paymentMethod = kept.paymentMethod ?: absorbed.paymentMethod,
                    reference = kept.reference ?: absorbed.reference,
                )
            }
            else -> kept
        }
    }

    /** Elige campo por campo y arma los orígenes de lo elegido. */
    private class Picker(
        private val kept: MovementProposal,
        private val absorbed: MovementProposal,
    ) {
        val origins = mutableMapOf<DraftField, FieldOrigin>().apply { kept.origins[DraftField.KIND]?.let { put(DraftField.KIND, it) } }
        private val fromAbsorbed = mutableSetOf<DraftField>()

        operator fun <T> invoke(
            field: DraftField,
            keptValue: T?,
            absorbedValue: T?,
            vararg linked: DraftField,
        ): T? {
            val useAbsorbed =
                absorbedValue != null &&
                    (keptValue == null || (absorbed.origins[field] == FieldOrigin.EVIDENCE && kept.origins[field] != FieldOrigin.EVIDENCE))
            val source = if (useAbsorbed) absorbed else kept
            if (useAbsorbed) fromAbsorbed += field
            (listOf(field) + linked).forEach { f -> source.origins[f]?.let { origins[f] = it } }
            return if (useAbsorbed) absorbedValue else keptValue
        }

        fun tookAbsorbed(field: DraftField) = field in fromAbsorbed

        /** La nota de quien habló (sin campos de comprobante) gana; si no hay, la del otro. */
        fun note(
            keptNote: String?,
            absorbedNote: String?,
        ): String? =
            when {
                !kept.isReceipt && keptNote != null -> keptNote
                !absorbed.isReceipt && absorbedNote != null -> absorbedNote
                else -> keptNote ?: absorbedNote
            }

        private val MovementProposal.isReceipt: Boolean get() = origins.values.any { it == FieldOrigin.EVIDENCE }
    }
}
