package com.coinden.core.domain.usecase.draft

import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.DraftInbox
import com.coinden.core.domain.model.draft.DraftProjection
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.DraftResult.Rejected
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProjectedBalance
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.wallet.LedgerProjection
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.DraftInterpreterPort
import com.coinden.core.domain.port.output.DraftRepositoryPort
import com.coinden.core.domain.port.output.IdGeneratorPort
import com.coinden.core.domain.port.output.WalletAliasRepositoryPort
import com.coinden.core.domain.port.output.WalletRepositoryPort
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.MovementKind
import com.coinden.core.domain.usecase.wallet.RegisterMovementUseCase
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase

/**
 * Dependencias comunes de los casos de uso de borradores.
 */
class DraftContext(
    val drafts: DraftRepositoryPort,
    val wallets: WalletRepositoryPort,
    val aliases: WalletAliasRepositoryPort,
    val ids: IdGeneratorPort,
    val clock: ClockPort,
    val device: DeviceIdentityPort,
) {
    suspend fun directory(ownerId: String): WalletDirectory = WalletDirectory(wallets.listWallets(ownerId), aliases.aliasesFor(ownerId))

    /** Estado actual del borrador, reproducido desde su historial con el directorio vigente. */
    suspend fun load(draftId: String): Draft? {
        val events = drafts.eventsFor(draftId)
        val ownerId = events.firstOrNull()?.ownerId ?: return null
        return DraftProjection.project(events, directory(ownerId))
    }

    /** Agrega una transición con identidad, dispositivo y hora, y devuelve el nuevo estado. */
    suspend fun record(
        draft: Draft,
        build: (id: String, deviceId: String, now: Long) -> DraftEvent,
    ): Draft {
        val now = clock.nowEpochMs()
        drafts.appendEvent(build(ids.newId(), device.deviceId(), now))
        return load(draft.id)!!
    }
}

/**
 * Caso de Uso: definir los alias con los que el usuario nombra una billetera al capturar ("qr", "efectivo").
 * Devuelve null si se guardaron, o el error.
 */
class SetWalletAliasesUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(
        ownerId: String,
        walletId: String,
        aliases: Set<String>,
    ): DraftError? {
        val wallet = ctx.wallets.getWallet(walletId)
        if (wallet == null || wallet.ownerId != ownerId) return DraftError.NotFound
        val normalized = aliases.map(WalletDirectory::normalizeAlias).filter { it.isNotEmpty() }.toSet()
        val existing = ctx.aliases.aliasesFor(ownerId)
        normalized.firstOrNull { existing[it] != null && existing[it] != walletId }?.let {
            return DraftError.AliasAlreadyUsed(it, existing.getValue(it))
        }
        ctx.aliases.setAliases(ownerId, walletId, normalized)
        return null
    }
}

/**
 * Caso de Uso: capturar en segundos. Nunca falla y nunca pierde la evidencia.
 *
 * Cada propuesta del intérprete genera su propio borrador con la misma evidencia
 * ("almuerzo 45 y micro 3" -> dos borradores). Si el intérprete falla o no propone nada,
 * el borrador queda Capturado para completarlo en la revisión.
 */
class CaptureDraftUseCase(
    private val ctx: DraftContext,
    private val interpreter: DraftInterpreterPort,
) {
    suspend operator fun invoke(
        ownerId: String,
        evidence: Evidence,
        capturedAtEpochMs: Long? = null,
    ): List<Draft> {
        val capturedAt = capturedAtEpochMs ?: ctx.clock.nowEpochMs()
        val first = capture(ownerId, evidence, capturedAt)

        val directory = ctx.directory(ownerId)
        val proposals = runCatching { interpreter.interpret(evidence, directory) }.getOrDefault(emptyList())
        if (proposals.isEmpty()) return listOf(ctx.load(first)!!)

        return proposals.mapIndexed { index, proposal ->
            val draftId = if (index == 0) first else capture(ownerId, evidence, capturedAt)
            val now = ctx.clock.nowEpochMs()
            ctx.drafts.appendEvent(
                DraftEvent.Interpreted(ctx.ids.newId(), draftId, ownerId, ctx.device.deviceId(), capturedAt, now, proposal),
            )
            ctx.load(draftId)!!
        }
    }

    private suspend fun capture(
        ownerId: String,
        evidence: Evidence,
        capturedAt: Long,
    ): String {
        val draftId = ctx.ids.newId()
        val now = ctx.clock.nowEpochMs()
        ctx.drafts.appendEvent(DraftEvent.Captured(ctx.ids.newId(), draftId, ownerId, ctx.device.deviceId(), capturedAt, now, evidence))
        return draftId
    }
}

/**
 * Caso de Uso: completar o corregir un borrador reemplazando su propuesta entera
 * (también convierte un No soportado en un movimiento).
 */
class ReviseDraftUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(
        draftId: String,
        proposal: MovementProposal,
    ): DraftResult {
        val draft = ctx.load(draftId) ?: return Rejected(DraftError.NotFound)
        if (draft.isTerminal) return Rejected(DraftError.TerminalState)
        val revised =
            ctx.record(draft) { id, device, now ->
                DraftEvent.Revised(id, draftId, draft.header.ownerId, device, now, now, proposal)
            }
        return DraftResult.Ok(revised)
    }
}

/**
 * Caso de Uso: descartar un borrador. No se borra: queda Descartado con su evidencia e historial.
 */
class DiscardDraftUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(draftId: String): DraftResult {
        val draft = ctx.load(draftId) ?: return Rejected(DraftError.NotFound)
        if (draft.isTerminal) return Rejected(DraftError.TerminalState)
        val discarded =
            ctx.record(draft) { id, device, now ->
                DraftEvent.Discarded(id, draftId, draft.header.ownerId, device, now, now)
            }
        return DraftResult.Ok(discarded)
    }
}

/**
 * Caso de Uso: confirmar un borrador Listo en el libro de movimientos.
 *
 * Delega en `wallet-management` (control estricto de saldo). El id del movimiento se deriva
 * del borrador ([ledgerEventIdFor]): si el libro ya lo tiene (otro dispositivo lo confirmó),
 * solo se marca Confirmado y no se registra un segundo movimiento.
 */
class ConfirmDraftUseCase(
    private val ctx: DraftContext,
    ledger: LedgerContext,
) {
    private val registerMovement = RegisterMovementUseCase(ledger)
    private val registerTransfer = RegisterTransferUseCase(ledger)

    suspend operator fun invoke(draftId: String): DraftResult {
        val draft = ctx.load(draftId) ?: return Rejected(DraftError.NotFound)
        val ready =
            when (draft) {
                is Draft.Ready -> draft
                is Draft.Incomplete -> return Rejected(DraftError.NotReady(draft.missing, draft.issues))
                is Draft.Captured -> return Rejected(DraftError.NotReady(emptySet(), emptySet()))
                is Draft.Unsupported -> return Rejected(DraftError.NotReady(emptySet(), emptySet()))
                is Draft.Confirmed, is Draft.Discarded -> return Rejected(DraftError.TerminalState)
            }

        val ledgerEventId = ledgerEventIdFor(draftId)
        if (ctx.wallets.findEvent(ledgerEventId) == null) {
            register(ready.movement, ledgerEventId)?.let { return Rejected(it) }
        }

        val confirmed =
            ctx.record(ready) { id, device, now ->
                DraftEvent.Confirmed(id, draftId, ready.header.ownerId, device, now, now, ledgerEventId, ready.movement)
            }
        return DraftResult.Ok(confirmed)
    }

    /** Registra el movimiento en el libro; devuelve el error si lo rechaza. */
    private suspend fun register(
        movement: ReadyMovement,
        eventId: String,
    ): DraftError? {
        val result =
            when (movement) {
                is ReadyMovement.Expense ->
                    registerMovement(movement.walletId, MovementKind.EXPENSE, movement.amount, movement.occurredAtEpochMs, eventId)
                is ReadyMovement.Income ->
                    registerMovement(movement.walletId, MovementKind.INCOME, movement.amount, movement.occurredAtEpochMs, eventId)
                is ReadyMovement.Transfer ->
                    registerTransfer(
                        movement.fromWalletId,
                        movement.toWalletId,
                        movement.out,
                        movement.into,
                        movement.occurredAtEpochMs,
                        eventId,
                    )
                is ReadyMovement.AdjustTo -> {
                    val delta = movement.target - currentBalance(movement.walletId)
                    if (delta.isZero) return DraftError.NothingToAdjust
                    registerMovement(movement.walletId, MovementKind.ADJUSTMENT, delta, movement.occurredAtEpochMs, eventId)
                }
            }
        return (result as? WalletOperationResult.Rejected)?.let { DraftError.Wallet(it.error) }
    }

    private suspend fun currentBalance(walletId: String): Money {
        val wallet = ctx.wallets.getWallet(walletId)!!
        return LedgerProjection(wallet, ctx.wallets.eventsForWallet(walletId)).balance
    }

    companion object {
        fun ledgerEventIdFor(draftId: String): String = "draft:$draftId"
    }
}

/**
 * Caso de Uso: la bandeja de la noche. Lista los pendientes y proyecta el saldo de cada billetera
 * si se confirmaran sus borradores Listos; avisa (sin bloquear) cuando quedaría un faltante.
 */
class GetDraftInboxUseCase(
    private val ctx: DraftContext,
) {
    suspend operator fun invoke(ownerId: String): DraftInbox {
        val directory = ctx.directory(ownerId)
        val pending =
            ctx.drafts
                .draftIdsFor(ownerId)
                .mapNotNull { DraftProjection.project(ctx.drafts.eventsFor(it), directory) }
                .filterNot { it.isTerminal }
                .sortedBy { it.header.capturedAtEpochMs }

        val projected = mutableMapOf<String, Money>()
        val current = mutableMapOf<String, Money>()

        suspend fun balanceOf(walletId: String): Money =
            projected.getOrPut(walletId) {
                val wallet = directory.byId(walletId)!!
                LedgerProjection(wallet, ctx.wallets.eventsForWallet(walletId)).balance.also { current[walletId] = it }
            }

        for (draft in pending.filterIsInstance<Draft.Ready>()) {
            when (val m = draft.movement) {
                is ReadyMovement.Expense -> projected[m.walletId] = balanceOf(m.walletId) - m.amount
                is ReadyMovement.Income -> projected[m.walletId] = balanceOf(m.walletId) + m.amount
                is ReadyMovement.Transfer -> {
                    projected[m.fromWalletId] = balanceOf(m.fromWalletId) - m.out
                    projected[m.toWalletId] = balanceOf(m.toWalletId) + m.into
                }
                is ReadyMovement.AdjustTo -> {
                    val before = balanceOf(m.walletId)
                    projected[m.walletId] = before + (m.target - current.getValue(m.walletId))
                }
            }
        }

        val projections =
            projected.map { (walletId, balance) ->
                val wallet = directory.byId(walletId)!!
                ProjectedBalance(walletId, current.getValue(walletId), balance, !wallet.allowsNegative && balance.isNegative)
            }
        return DraftInbox(pending, projections)
    }
}
