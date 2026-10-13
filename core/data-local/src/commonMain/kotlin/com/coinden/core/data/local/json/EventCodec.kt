package com.coinden.core.data.local.json

import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.wallet.LedgerEvent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/** Un evento listo para el sobre de la tabla: tipo, versión del formato y contenido JSON. */
data class StoredEvent(
    val type: String,
    val version: Long,
    val payload: String,
)

/**
 * Formato versionado de los eventos. Leer devuelve null si el tipo no se conoce o la versión
 * es más nueva que la soportada: el evento se conserva en disco y solo se omite al leer.
 */
object EventCodec {
    const val VERSION = 1L

    private fun <T> store(
        type: String,
        serializer: KSerializer<T>,
        dto: T,
    ) = StoredEvent(type, VERSION, CoinDenJson.encodeToString(serializer, dto))

    private fun <T> load(
        version: Long,
        payload: String,
        serializer: KSerializer<T>,
    ): T? = if (version > VERSION) null else CoinDenJson.decodeFromString(serializer, payload)

    // --- Libro ---------------------------------------------------------------

    fun encodeLedger(event: LedgerEvent): StoredEvent =
        when (event) {
            is LedgerEvent.OpeningBalance ->
                store(
                    LedgerTypes.OPENING_BALANCE,
                    WalletAmountDto.serializer(),
                    WalletAmountDto(event.common(), event.walletId, event.amount.toDto()),
                )
            is LedgerEvent.Adjustment ->
                store(
                    LedgerTypes.ADJUSTMENT,
                    WalletAmountDto.serializer(),
                    WalletAmountDto(event.common(), event.walletId, event.amount.toDto()),
                )
            is LedgerEvent.Expense ->
                store(
                    LedgerTypes.EXPENSE,
                    WalletAmountDto.serializer(),
                    WalletAmountDto(event.common(), event.walletId, event.amount.toDto()),
                )
            is LedgerEvent.Income ->
                store(
                    LedgerTypes.INCOME,
                    WalletAmountDto.serializer(),
                    WalletAmountDto(event.common(), event.walletId, event.amount.toDto()),
                )
            is LedgerEvent.Transfer ->
                store(
                    LedgerTypes.TRANSFER,
                    TransferDto.serializer(),
                    TransferDto(event.common(), event.fromWalletId, event.toWalletId, event.amountOut.toDto(), event.amountIn.toDto()),
                )
            is LedgerEvent.Annulment ->
                store(
                    LedgerTypes.ANNULMENT,
                    AnnulmentDto.serializer(),
                    AnnulmentDto(event.common(), event.targetId, event.affectedWalletIds.toList()),
                )
        }

    fun decodeLedger(
        type: String,
        version: Long,
        payload: String,
    ): LedgerEvent? =
        when (type) {
            LedgerTypes.OPENING_BALANCE ->
                load(version, payload, WalletAmountDto.serializer())?.let {
                    LedgerEvent.OpeningBalance(
                        it.event.id,
                        it.event.deviceId,
                        it.event.occurredAt,
                        it.event.recordedAt,
                        it.walletId,
                        it.amount.toDomain(),
                    )
                }
            LedgerTypes.ADJUSTMENT ->
                load(version, payload, WalletAmountDto.serializer())?.let {
                    LedgerEvent.Adjustment(
                        it.event.id,
                        it.event.deviceId,
                        it.event.occurredAt,
                        it.event.recordedAt,
                        it.walletId,
                        it.amount.toDomain(),
                    )
                }
            LedgerTypes.EXPENSE ->
                load(version, payload, WalletAmountDto.serializer())?.let {
                    LedgerEvent.Expense(
                        it.event.id,
                        it.event.deviceId,
                        it.event.occurredAt,
                        it.event.recordedAt,
                        it.walletId,
                        it.amount.toDomain(),
                    )
                }
            LedgerTypes.INCOME ->
                load(version, payload, WalletAmountDto.serializer())?.let {
                    LedgerEvent.Income(
                        it.event.id,
                        it.event.deviceId,
                        it.event.occurredAt,
                        it.event.recordedAt,
                        it.walletId,
                        it.amount.toDomain(),
                    )
                }
            LedgerTypes.TRANSFER ->
                load(version, payload, TransferDto.serializer())?.let {
                    LedgerEvent.Transfer(
                        it.event.id,
                        it.event.deviceId,
                        it.event.occurredAt,
                        it.event.recordedAt,
                        it.fromWalletId,
                        it.toWalletId,
                        it.amountOut.toDomain(),
                        it.amountIn.toDomain(),
                    )
                }
            LedgerTypes.ANNULMENT ->
                load(version, payload, AnnulmentDto.serializer())?.let {
                    LedgerEvent.Annulment(
                        it.event.id,
                        it.event.deviceId,
                        it.event.occurredAt,
                        it.event.recordedAt,
                        it.targetId,
                        it.affectedWalletIds.toSet(),
                    )
                }
            else -> null
        }

    private fun LedgerEvent.common() = EventFieldsDto(id, deviceId, occurredAtEpochMs, recordedAtEpochMs)

    // --- Borradores ----------------------------------------------------------

    fun encodeDraft(event: DraftEvent): StoredEvent {
        val draft = DraftFieldsDto(event.id, event.draftId, event.ownerId, event.deviceId, event.occurredAtEpochMs, event.recordedAtEpochMs)
        return when (event) {
            is DraftEvent.Captured -> store(DraftTypes.CAPTURED, CapturedDto.serializer(), CapturedDto(draft, event.evidence.toDto()))
            is DraftEvent.Interpreted ->
                store(
                    DraftTypes.INTERPRETED,
                    ProposalEventDto.serializer(),
                    ProposalEventDto(draft, event.proposal.toDto()),
                )
            is DraftEvent.Revised ->
                store(
                    DraftTypes.REVISED,
                    ProposalEventDto.serializer(),
                    ProposalEventDto(draft, event.proposal.toDto()),
                )
            is DraftEvent.Confirmed ->
                store(DraftTypes.CONFIRMED, ConfirmedDto.serializer(), ConfirmedDto(draft, event.ledgerEventId, event.movement.toDto()))
            is DraftEvent.Discarded -> store(DraftTypes.DISCARDED, DiscardedDto.serializer(), DiscardedDto(draft))
        }
    }

    fun decodeDraft(
        type: String,
        version: Long,
        payload: String,
    ): DraftEvent? =
        when (type) {
            DraftTypes.CAPTURED ->
                load(version, payload, CapturedDto.serializer())?.let {
                    with(it.draft) { DraftEvent.Captured(id, draftId, ownerId, deviceId, occurredAt, recordedAt, it.evidence.toDomain()) }
                }
            DraftTypes.INTERPRETED ->
                load(version, payload, ProposalEventDto.serializer())?.let {
                    with(
                        it.draft,
                    ) { DraftEvent.Interpreted(id, draftId, ownerId, deviceId, occurredAt, recordedAt, it.proposal.toDomain()) }
                }
            DraftTypes.REVISED ->
                load(version, payload, ProposalEventDto.serializer())?.let {
                    with(it.draft) { DraftEvent.Revised(id, draftId, ownerId, deviceId, occurredAt, recordedAt, it.proposal.toDomain()) }
                }
            DraftTypes.CONFIRMED ->
                load(version, payload, ConfirmedDto.serializer())?.let {
                    with(it.draft) {
                        DraftEvent.Confirmed(
                            id,
                            draftId,
                            ownerId,
                            deviceId,
                            occurredAt,
                            recordedAt,
                            it.ledgerEventId,
                            it.movement.toDomain(),
                        )
                    }
                }
            DraftTypes.DISCARDED ->
                load(version, payload, DiscardedDto.serializer())?.let {
                    with(it.draft) { DraftEvent.Discarded(id, draftId, ownerId, deviceId, occurredAt, recordedAt) }
                }
            else -> null
        }

    // --- Clasificaciones -----------------------------------------------------

    fun encodeClassification(event: ClassificationEvent): StoredEvent =
        store(
            ClassificationTypes.CLASSIFIED,
            ClassificationDto.serializer(),
            ClassificationDto(
                event.id,
                event.movementId,
                event.ownerId,
                event.deviceId,
                event.occurredAtEpochMs,
                event.recordedAtEpochMs,
                event.description,
                event.category?.toDto(),
                event.origin?.name,
            ),
        )

    fun decodeClassification(
        type: String,
        version: Long,
        payload: String,
    ): ClassificationEvent? {
        if (type != ClassificationTypes.CLASSIFIED) return null
        return load(version, payload, ClassificationDto.serializer())?.toDomain()
    }
}

/** Tipos de evento guardados en el sobre. Cambiar un nombre rompe la lectura de datos guardados. */
object LedgerTypes {
    const val OPENING_BALANCE = "ledger.opening_balance"
    const val ADJUSTMENT = "ledger.adjustment"
    const val EXPENSE = "ledger.expense"
    const val INCOME = "ledger.income"
    const val TRANSFER = "ledger.transfer"
    const val ANNULMENT = "ledger.annulment"
}

object DraftTypes {
    const val CAPTURED = "draft.captured"
    const val INTERPRETED = "draft.interpreted"
    const val REVISED = "draft.revised"
    const val CONFIRMED = "draft.confirmed"
    const val DISCARDED = "draft.discarded"
}

object ClassificationTypes {
    const val CLASSIFIED = "classification.classified"
}

// --- DTOs de eventos (versión 1) ------------------------------------------------

@Serializable
internal data class EventFieldsDto(
    val id: String,
    val deviceId: String,
    val occurredAt: Long,
    val recordedAt: Long,
)

@Serializable
internal data class WalletAmountDto(
    val event: EventFieldsDto,
    val walletId: String,
    val amount: MoneyDto,
)

@Serializable
internal data class TransferDto(
    val event: EventFieldsDto,
    val fromWalletId: String,
    val toWalletId: String,
    val amountOut: MoneyDto,
    val amountIn: MoneyDto,
)

@Serializable
internal data class AnnulmentDto(
    val event: EventFieldsDto,
    val targetId: String,
    val affectedWalletIds: List<String>,
)

@Serializable
internal data class DraftFieldsDto(
    val id: String,
    val draftId: String,
    val ownerId: String,
    val deviceId: String,
    val occurredAt: Long,
    val recordedAt: Long,
)

@Serializable
internal data class CapturedDto(
    val draft: DraftFieldsDto,
    val evidence: EvidenceDto,
)

@Serializable
internal data class ProposalEventDto(
    val draft: DraftFieldsDto,
    val proposal: ProposalDto,
)

@Serializable
internal data class ConfirmedDto(
    val draft: DraftFieldsDto,
    val ledgerEventId: String,
    val movement: ReadyMovementDto,
)

@Serializable
internal data class DiscardedDto(
    val draft: DraftFieldsDto,
)
