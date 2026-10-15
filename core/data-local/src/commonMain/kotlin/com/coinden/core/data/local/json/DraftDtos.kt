package com.coinden.core.data.local.json

import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryOrigin
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.NonMovementIntent
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.draft.WalletSuggestion
import com.coinden.core.domain.model.draft.WalletSuggestionReason
import com.coinden.core.domain.model.wallet.PaymentMethod
import kotlinx.serialization.Serializable

/*
 * DTOs de borradores y clasificaciones (versión 1). Las variantes de un tipo sellado se guardan
 * con un campo "kind" y sus campos opcionales; los enums por su nombre.
 */

@Serializable
internal data class EvidenceDto(
    val source: String,
    val text: String? = null,
    val attachmentRef: String? = null,
)

internal fun Evidence.toDto() = EvidenceDto(source.name, text, attachmentRef)

internal fun EvidenceDto.toDomain() = Evidence(EvidenceSource.valueOf(source), text, attachmentRef)

@Serializable
internal data class ProposedAmountDto(
    val value: String,
    val currency: CurrencyDto? = null,
)

internal fun ProposedAmount.toDto() = ProposedAmountDto(value, currency?.toDto())

internal fun ProposedAmountDto.toDomain() = ProposedAmount(value, currency?.toDomain())

/** Billetera por id o por pista: solo uno de los dos campos está presente. */
@Serializable
internal data class WalletRefDto(
    val id: String? = null,
    val hint: String? = null,
)

internal fun WalletRef.toDto() =
    when (this) {
        is WalletRef.ById -> WalletRefDto(id = walletId)
        is WalletRef.ByHint -> WalletRefDto(hint = hint)
    }

internal fun WalletRefDto.toDomain(): WalletRef = id?.let { WalletRef.ById(it) } ?: WalletRef.ByHint(requireNotNull(hint))

@Serializable
internal data class ProposalDto(
    val kind: String,
    val amount: ProposedAmountDto? = null,
    val wallet: WalletRefDto? = null,
    val into: ProposedAmountDto? = null,
    val to: WalletRefDto? = null,
    val occurredAt: Long? = null,
    val note: String? = null,
    val intent: String? = null,
    val origins: Map<String, String> = emptyMap(),
    /** Medio de pago dicho (opcional: los datos guardados antes no lo traen). */
    val paymentMethod: String? = null,
    /** Motivo de la billetera sugerida (opcional: los datos guardados antes no lo traen). */
    val walletSuggestion: WalletSuggestionDto? = null,
)

@Serializable
internal data class WalletSuggestionDto(
    val reason: String,
    val candidates: Int,
)

private fun WalletSuggestion.toDto() = WalletSuggestionDto(reason.name, candidates)

private fun WalletSuggestionDto.toDomain() = WalletSuggestion(WalletSuggestionReason.valueOf(reason), candidates)

private object ProposalKinds {
    const val EXPENSE = "expense"
    const val INCOME = "income"
    const val TRANSFER = "transfer"
    const val ADJUST_TO = "adjust_to"
    const val NOT_A_MOVEMENT = "not_a_movement"
    const val UNKNOWN = "unknown"
}

private fun Map<DraftField, FieldOrigin>.toDto() = entries.associate { (field, origin) -> field.name to origin.name }

private fun Map<String, String>.toOrigins() =
    entries.associate { (field, origin) -> DraftField.valueOf(field) to FieldOrigin.valueOf(origin) }

internal fun MovementProposal.toDto(): ProposalDto =
    when (this) {
        is MovementProposal.Expense ->
            ProposalDto(
                ProposalKinds.EXPENSE,
                amount?.toDto(),
                wallet?.toDto(),
                occurredAt = occurredAtEpochMs,
                note = note,
                origins = origins.toDto(),
                paymentMethod = paymentMethod?.name,
                walletSuggestion = walletSuggestion?.toDto(),
            )
        is MovementProposal.Income ->
            ProposalDto(
                ProposalKinds.INCOME,
                amount?.toDto(),
                wallet?.toDto(),
                occurredAt = occurredAtEpochMs,
                note = note,
                origins = origins.toDto(),
                paymentMethod = paymentMethod?.name,
                walletSuggestion = walletSuggestion?.toDto(),
            )
        is MovementProposal.Transfer ->
            ProposalDto(
                ProposalKinds.TRANSFER,
                out?.toDto(),
                from?.toDto(),
                into?.toDto(),
                to?.toDto(),
                occurredAtEpochMs,
                note,
                origins = origins.toDto(),
                paymentMethod = paymentMethod?.name,
            )
        is MovementProposal.AdjustTo ->
            ProposalDto(
                ProposalKinds.ADJUST_TO,
                target?.toDto(),
                wallet?.toDto(),
                occurredAt = occurredAtEpochMs,
                note = note,
                origins = origins.toDto(),
            )
        is MovementProposal.NotAMovement -> ProposalDto(ProposalKinds.NOT_A_MOVEMENT, note = note, intent = intent.name)
        is MovementProposal.Unknown -> ProposalDto(ProposalKinds.UNKNOWN, note = note)
    }

private fun ProposalDto.method(): PaymentMethod? = paymentMethod?.let { PaymentMethod.valueOf(it) }

internal fun ProposalDto.toDomain(): MovementProposal =
    when (kind) {
        ProposalKinds.EXPENSE ->
            MovementProposal.Expense(
                amount?.toDomain(),
                wallet?.toDomain(),
                occurredAt,
                note,
                origins.toOrigins(),
                method(),
                walletSuggestion?.toDomain(),
            )
        ProposalKinds.INCOME ->
            MovementProposal.Income(
                amount?.toDomain(),
                wallet?.toDomain(),
                occurredAt,
                note,
                origins.toOrigins(),
                method(),
                walletSuggestion?.toDomain(),
            )
        ProposalKinds.TRANSFER ->
            MovementProposal.Transfer(
                amount?.toDomain(),
                wallet?.toDomain(),
                into?.toDomain(),
                to?.toDomain(),
                occurredAt,
                note,
                origins.toOrigins(),
                method(),
            )
        ProposalKinds.ADJUST_TO -> MovementProposal.AdjustTo(amount?.toDomain(), wallet?.toDomain(), occurredAt, note, origins.toOrigins())
        ProposalKinds.NOT_A_MOVEMENT -> MovementProposal.NotAMovement(NonMovementIntent.valueOf(requireNotNull(intent)), note)
        ProposalKinds.UNKNOWN -> MovementProposal.Unknown(note)
        else -> error("Tipo de propuesta desconocido: $kind")
    }

@Serializable
internal data class ReadyMovementDto(
    val kind: String,
    val amount: MoneyDto,
    val walletId: String,
    val into: MoneyDto? = null,
    val toWalletId: String? = null,
    val occurredAt: Long,
    val note: String? = null,
)

internal fun ReadyMovement.toDto(): ReadyMovementDto =
    when (this) {
        is ReadyMovement.Expense ->
            ReadyMovementDto(
                ProposalKinds.EXPENSE,
                amount.toDto(),
                walletId,
                occurredAt = occurredAtEpochMs,
                note = note,
            )
        is ReadyMovement.Income ->
            ReadyMovementDto(
                ProposalKinds.INCOME,
                amount.toDto(),
                walletId,
                occurredAt = occurredAtEpochMs,
                note = note,
            )
        is ReadyMovement.Transfer ->
            ReadyMovementDto(ProposalKinds.TRANSFER, out.toDto(), fromWalletId, into.toDto(), toWalletId, occurredAtEpochMs, note)
        is ReadyMovement.AdjustTo ->
            ReadyMovementDto(
                ProposalKinds.ADJUST_TO,
                target.toDto(),
                walletId,
                occurredAt = occurredAtEpochMs,
                note = note,
            )
    }

internal fun ReadyMovementDto.toDomain(): ReadyMovement =
    when (kind) {
        ProposalKinds.EXPENSE -> ReadyMovement.Expense(amount.toDomain(), walletId, occurredAt, note)
        ProposalKinds.INCOME -> ReadyMovement.Income(amount.toDomain(), walletId, occurredAt, note)
        ProposalKinds.TRANSFER ->
            ReadyMovement.Transfer(
                amount.toDomain(),
                walletId,
                requireNotNull(into).toDomain(),
                requireNotNull(toWalletId),
                occurredAt,
                note,
            )
        ProposalKinds.ADJUST_TO -> ReadyMovement.AdjustTo(amount.toDomain(), walletId, occurredAt, note)
        else -> error("Tipo de movimiento desconocido: $kind")
    }

/** Categoría base por nombre o propia por id: solo uno de los dos campos está presente. */
@Serializable
internal data class CategoryRefDto(
    val base: String? = null,
    val customId: String? = null,
)

internal fun CategoryRef.toDto() =
    when (this) {
        is CategoryRef.Base -> CategoryRefDto(base = category.name)
        is CategoryRef.Custom -> CategoryRefDto(customId = id)
    }

internal fun CategoryRefDto.toDomain(): CategoryRef =
    base?.let { CategoryRef.Base(BaseCategory.valueOf(it)) } ?: CategoryRef.Custom(requireNotNull(customId))

@Serializable
internal data class ClassificationDto(
    val id: String,
    val movementId: String,
    val ownerId: String,
    val deviceId: String,
    val occurredAt: Long,
    val recordedAt: Long,
    val description: String? = null,
    val category: CategoryRefDto? = null,
    val origin: String? = null,
)

internal fun ClassificationDto.toDomain() =
    ClassificationEvent(
        id,
        movementId,
        ownerId,
        deviceId,
        occurredAt,
        recordedAt,
        description,
        category?.toDomain(),
        origin?.let { CategoryOrigin.valueOf(it) },
    )
