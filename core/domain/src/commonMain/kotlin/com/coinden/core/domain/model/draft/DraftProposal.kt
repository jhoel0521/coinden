package com.coinden.core.domain.model.draft

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType

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
    data class ById(
        val walletId: String,
    ) : WalletRef

    data class ByHint(
        val hint: String,
    ) : WalletRef
}

/** Por qué el intérprete sugirió una billetera que la frase no nombró. Sin textos: la presentación lo explica. */
enum class WalletSuggestionReason {
    /** La billetera por defecto: la frase no nombró billetera, medio ni otra moneda. */
    DEFAULT_WALLET,

    /** La única billetera activa que acepta el medio de pago o la moneda dichos. */
    ONLY_CANDIDATE,

    /** Una recomendada entre varias candidatas: hay que revisarla. */
    RECOMMENDED_AMONG_SEVERAL,
}

/** Motivo de la sugerencia y cuántas billeteras eran candidatas. */
data class WalletSuggestion(
    val reason: WalletSuggestionReason,
    val candidates: Int,
)

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
 * De dónde salió un campo propuesto. Todo en un borrador es una recomendación; el origen
 * dice cuánto respaldo tiene.
 */
enum class FieldOrigin {
    /** Lo dice la evidencia: una etiqueta o una plantilla del comprobante. */
    EVIDENCE,

    /** Lo dijo o escribió el usuario. */
    STATED,

    /** Lo corrigió el usuario en la revisión. */
    CORRECTED,

    /** Lo dedujo una regla de lectura (un verbo con error de tipeo, un "$" resuelto por el locale). */
    INFERRED,

    /** Lo propuso una regla por defecto: nadie lo dijo (la billetera por defecto). */
    SUGGESTED,
    ;

    val confidence: Confidence
        get() =
            when (this) {
                EVIDENCE, STATED, CORRECTED -> Confidence.HIGH
                INFERRED -> Confidence.MEDIUM
                SUGGESTED -> Confidence.LOW
            }
}

/** Confianza de un borrador, de menor a mayor (así ordena la bandeja). */
enum class Confidence {
    LOW,
    MEDIUM,
    HIGH,
}

/**
 * Lo que un intérprete (IA local, reglas de texto, OCR o formulario) entendió de una evidencia.
 *
 * El tipo ES el movimiento: no hay enum de "kind". Cualquier campo puede faltar; el dominio
 * decide con [DraftEvaluator] si alcanza para un borrador Listo. [origins] indica de dónde salió
 * cada campo presente; un campo sin entrada cuenta como [FieldOrigin.STATED].
 */
sealed interface MovementProposal {
    val origins: Map<DraftField, FieldOrigin>

    /** Campos que la propuesta trae (el tipo de un movimiento siempre está presente). */
    val presentFields: Set<DraftField>

    data class Expense(
        val amount: ProposedAmount? = null,
        val wallet: WalletRef? = null,
        val occurredAtEpochMs: Long? = null,
        val note: String? = null,
        override val origins: Map<DraftField, FieldOrigin> = emptyMap(),
        /** Medio de pago dicho ("qr", "efectivo"): contexto para elegir la billetera en la revisión. */
        val paymentMethod: PaymentMethod? = null,
        /** Por qué se sugirió la billetera (null si la dijo el usuario o no hay). */
        val walletSuggestion: WalletSuggestion? = null,
    ) : MovementProposal {
        override val presentFields: Set<DraftField> get() = singleWalletFields(amount, wallet, occurredAtEpochMs)
    }

    data class Income(
        val amount: ProposedAmount? = null,
        val wallet: WalletRef? = null,
        val occurredAtEpochMs: Long? = null,
        val note: String? = null,
        override val origins: Map<DraftField, FieldOrigin> = emptyMap(),
        /** Medio de pago dicho ("qr", "efectivo"): contexto para elegir la billetera en la revisión. */
        val paymentMethod: PaymentMethod? = null,
        /** Por qué se sugirió la billetera (null si la dijo el usuario o no hay). */
        val walletSuggestion: WalletSuggestion? = null,
    ) : MovementProposal {
        override val presentFields: Set<DraftField> get() = singleWalletFields(amount, wallet, occurredAtEpochMs)
    }

    /** Entre dos billeteras: misma moneda (cajero, pagar la tarjeta) o cambio de moneda. */
    data class Transfer(
        val out: ProposedAmount? = null,
        val from: WalletRef? = null,
        val into: ProposedAmount? = null,
        val to: WalletRef? = null,
        val occurredAtEpochMs: Long? = null,
        val note: String? = null,
        override val origins: Map<DraftField, FieldOrigin> = emptyMap(),
        /** Medio de pago dicho para el origen. */
        val paymentMethod: PaymentMethod? = null,
    ) : MovementProposal {
        override val presentFields: Set<DraftField>
            get() =
                singleWalletFields(out, from, occurredAtEpochMs) +
                    listOfNotNull(into?.let { DraftField.TO_AMOUNT }, to?.let { DraftField.TO_WALLET })
    }

    /** "En la billetera tengo 140": el saldo real, no la diferencia. */
    data class AdjustTo(
        val target: ProposedAmount? = null,
        val wallet: WalletRef? = null,
        val occurredAtEpochMs: Long? = null,
        val note: String? = null,
        override val origins: Map<DraftField, FieldOrigin> = emptyMap(),
    ) : MovementProposal {
        override val presentFields: Set<DraftField> get() = singleWalletFields(target, wallet, occurredAtEpochMs)
    }

    data class NotAMovement(
        val intent: NonMovementIntent,
        val note: String? = null,
    ) : MovementProposal {
        override val origins: Map<DraftField, FieldOrigin> get() = emptyMap()
        override val presentFields: Set<DraftField> get() = emptySet()
    }

    data class Unknown(
        val note: String? = null,
    ) : MovementProposal {
        override val origins: Map<DraftField, FieldOrigin> get() = emptyMap()
        override val presentFields: Set<DraftField> get() = emptySet()
    }
}

private fun singleWalletFields(
    amount: ProposedAmount?,
    wallet: WalletRef?,
    occurredAtEpochMs: Long?,
): Set<DraftField> =
    setOfNotNull(
        DraftField.KIND,
        amount?.let { DraftField.AMOUNT },
        amount?.currency?.let { DraftField.CURRENCY },
        wallet?.let { DraftField.WALLET },
        occurredAtEpochMs?.let { DraftField.OCCURRED_AT },
    )

/**
 * Confianza determinista de una propuesta: la del campo presente más débil. Sin campos
 * presentes (no se entendió o no es un movimiento) es BAJA.
 */
val MovementProposal.confidence: Confidence
    get() =
        presentFields
            .map { (origins[it] ?: FieldOrigin.STATED).confidence }
            .minOrNull() ?: Confidence.LOW

/** La misma propuesta revisada por el usuario: todos sus campos presentes pasan a corregidos. */
fun MovementProposal.markedCorrected(): MovementProposal {
    val corrected = presentFields.associateWith { FieldOrigin.CORRECTED }
    return when (this) {
        is MovementProposal.Expense -> copy(origins = corrected, walletSuggestion = null)
        is MovementProposal.Income -> copy(origins = corrected, walletSuggestion = null)
        is MovementProposal.Transfer -> copy(origins = corrected)
        is MovementProposal.AdjustTo -> copy(origins = corrected)
        is MovementProposal.NotAMovement, is MovementProposal.Unknown -> this
    }
}

/** Campos de una propuesta: los que pueden faltar y los que pueden tener un origen. */
enum class DraftField {
    KIND,
    AMOUNT,
    CURRENCY,
    WALLET,
    TO_AMOUNT,
    TO_WALLET,
    OCCURRED_AT,
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
    /** Billetera sugerida para capturas que no nombran ninguna (ver [resolveDefault]). */
    val defaultWalletId: String? = null,
) {
    fun byId(walletId: String): Wallet? = wallets.firstOrNull { it.id == walletId }

    fun byHint(hint: String): Wallet? = aliases[normalizeAlias(hint)]?.let(::byId)

    companion object {
        fun normalizeAlias(alias: String): String = alias.trim().lowercase()

        /**
         * La elección explícita si la billetera sigue activa; si no, la única billetera activa
         * de efectivo; si no, ninguna.
         */
        fun resolveDefault(
            wallets: List<Wallet>,
            chosenWalletId: String?,
        ): String? {
            val active = wallets.filter { it.isActive }
            active.firstOrNull { it.id == chosenWalletId }?.let { return it.id }
            return active.singleOrNull { it.type == WalletType.CASH }?.id
        }
    }
}
