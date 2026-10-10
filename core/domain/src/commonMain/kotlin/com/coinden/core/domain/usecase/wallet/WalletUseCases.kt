package com.coinden.core.domain.usecase.wallet

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletOperationResult.Rejected
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.IdGeneratorPort
import com.coinden.core.domain.port.output.WalletRepositoryPort

/**
 * Dependencias comunes para crear eventos del libro con identidad, dispositivo y fechas.
 */
class LedgerContext(
    val repository: WalletRepositoryPort,
    val ids: IdGeneratorPort,
    val clock: ClockPort,
    val device: DeviceIdentityPort,
) {
    internal val guard = LedgerGuard(repository)
}

/**
 * Caso de Uso: crear una billetera de una sola moneda con su saldo inicial.
 */
class CreateWalletUseCase(
    private val ctx: LedgerContext,
) {
    suspend operator fun invoke(
        ownerId: String,
        name: String,
        currency: Currency,
        type: WalletType,
        openingBalance: Money,
        allowsNegative: Boolean = type.allowsNegativeByDefault,
        occurredAtEpochMs: Long? = null,
    ): WalletOperationResult {
        if (!Wallet.isValidName(name)) return Rejected(WalletError.InvalidName)
        val wallet = Wallet(ctx.ids.newId(), ownerId, name, currency, type, allowsNegative)
        if (!wallet.accepts(openingBalance.currency)) return Rejected(WalletError.CurrencyMismatch)

        val now = ctx.clock.nowEpochMs()
        val opening =
            LedgerEvent.OpeningBalance(
                id = ctx.ids.newId(),
                deviceId = ctx.device.deviceId(),
                occurredAtEpochMs = occurredAtEpochMs ?: now,
                recordedAtEpochMs = now,
                walletId = wallet.id,
                amount = openingBalance,
            )
        ctx.guard.check(listOf(wallet), opening)?.let { return Rejected(it) }

        ctx.repository.saveWallet(wallet)
        ctx.repository.appendEvent(opening)
        return WalletOperationResult.WalletCreated(wallet, opening)
    }
}

/**
 * Caso de Uso: archivar una billetera. Nunca se borra; conserva todo su historial.
 */
class ArchiveWalletUseCase(
    private val repository: WalletRepositoryPort,
) {
    suspend operator fun invoke(walletId: String): WalletOperationResult {
        val wallet = repository.getWallet(walletId) ?: return Rejected(WalletError.NotFound)
        val archived = wallet.archived()
        repository.saveWallet(archived)
        return WalletOperationResult.WalletUpdated(archived)
    }
}

/**
 * Caso de Uso: reactivar una billetera archivada.
 */
class ReactivateWalletUseCase(
    private val repository: WalletRepositoryPort,
) {
    suspend operator fun invoke(walletId: String): WalletOperationResult {
        val wallet = repository.getWallet(walletId) ?: return Rejected(WalletError.NotFound)
        val active = wallet.reactivated()
        repository.saveWallet(active)
        return WalletOperationResult.WalletUpdated(active)
    }
}

/**
 * Tipos de movimiento simple sobre una sola billetera.
 */
enum class MovementKind {
    EXPENSE,
    INCOME,
    ADJUSTMENT,
}

/**
 * Caso de Uso: registrar un gasto, ingreso o ajuste en una billetera activa.
 */
class RegisterMovementUseCase(
    private val ctx: LedgerContext,
) {
    suspend operator fun invoke(
        walletId: String,
        kind: MovementKind,
        amount: Money,
        occurredAtEpochMs: Long? = null,
    ): WalletOperationResult {
        val wallet = ctx.repository.getWallet(walletId) ?: return Rejected(WalletError.NotFound)
        if (!wallet.isActive) return Rejected(WalletError.WalletArchived)
        if (!wallet.accepts(amount.currency)) return Rejected(WalletError.CurrencyMismatch)
        val validAmount =
            when (kind) {
                MovementKind.EXPENSE, MovementKind.INCOME -> amount.isPositive
                MovementKind.ADJUSTMENT -> !amount.isZero
            }
        if (!validAmount) return Rejected(WalletError.InvalidAmount)

        val now = ctx.clock.nowEpochMs()
        val id = ctx.ids.newId()
        val device = ctx.device.deviceId()
        val occurred = occurredAtEpochMs ?: now
        val event =
            when (kind) {
                MovementKind.EXPENSE -> LedgerEvent.Expense(id, device, occurred, now, walletId, amount)
                MovementKind.INCOME -> LedgerEvent.Income(id, device, occurred, now, walletId, amount)
                MovementKind.ADJUSTMENT -> LedgerEvent.Adjustment(id, device, occurred, now, walletId, amount)
            }
        ctx.guard.check(listOf(wallet), event)?.let { return Rejected(it) }

        ctx.repository.appendEvent(event)
        return WalletOperationResult.EventRecorded(event)
    }
}

/**
 * Caso de Uso: transferir entre dos billeteras, en la misma moneda o cambiando de moneda.
 */
class RegisterTransferUseCase(
    private val ctx: LedgerContext,
) {
    suspend operator fun invoke(
        fromWalletId: String,
        toWalletId: String,
        amountOut: Money,
        amountIn: Money,
        occurredAtEpochMs: Long? = null,
    ): WalletOperationResult {
        if (fromWalletId == toWalletId) return Rejected(WalletError.SameWallet)
        val from = ctx.repository.getWallet(fromWalletId) ?: return Rejected(WalletError.NotFound)
        val to = ctx.repository.getWallet(toWalletId) ?: return Rejected(WalletError.NotFound)
        if (!from.isActive || !to.isActive) return Rejected(WalletError.WalletArchived)
        if (!from.accepts(amountOut.currency) || !to.accepts(amountIn.currency)) {
            return Rejected(WalletError.CurrencyMismatch)
        }
        if (!amountOut.isPositive || !amountIn.isPositive) return Rejected(WalletError.InvalidAmount)
        if (LedgerEvent.Transfer.isSameCurrency(amountOut, amountIn) &&
            amountOut.amountInMinorUnits != amountIn.amountInMinorUnits
        ) {
            return Rejected(WalletError.InconsistentTransferAmounts)
        }

        val now = ctx.clock.nowEpochMs()
        val transfer =
            LedgerEvent.Transfer(
                id = ctx.ids.newId(),
                deviceId = ctx.device.deviceId(),
                occurredAtEpochMs = occurredAtEpochMs ?: now,
                recordedAtEpochMs = now,
                fromWalletId = fromWalletId,
                toWalletId = toWalletId,
                amountOut = amountOut,
                amountIn = amountIn,
            )
        ctx.guard.check(listOf(from, to), transfer)?.let { return Rejected(it) }

        ctx.repository.appendEvent(transfer)
        return WalletOperationResult.EventRecorded(transfer)
    }
}

/**
 * Caso de Uso: corregir un movimiento anulándolo. El original nunca se edita ni se borra.
 */
class AnnulMovementUseCase(
    private val ctx: LedgerContext,
) {
    suspend operator fun invoke(eventId: String): WalletOperationResult {
        val target = ctx.repository.findEvent(eventId) ?: return Rejected(WalletError.NotFound)
        if (target is LedgerEvent.Annulment) return Rejected(WalletError.CannotAnnulAnnulment)

        val wallets = target.affectedWalletIds.map { ctx.repository.getWallet(it) ?: return Rejected(WalletError.NotFound) }
        if (wallets.any { !it.isActive }) return Rejected(WalletError.WalletArchived)
        val alreadyAnnulled =
            ctx.repository
                .eventsForWallet(wallets.first().id)
                .any { it is LedgerEvent.Annulment && it.targetId == eventId }
        if (alreadyAnnulled) return Rejected(WalletError.AlreadyAnnulled)

        val now = ctx.clock.nowEpochMs()
        val annulment =
            LedgerEvent.Annulment(
                id = ctx.ids.newId(),
                deviceId = ctx.device.deviceId(),
                occurredAtEpochMs = now,
                recordedAtEpochMs = now,
                targetId = eventId,
                affectedWalletIds = target.affectedWalletIds,
            )
        ctx.guard.check(wallets, annulment)?.let { return Rejected(it) }

        ctx.repository.appendEvent(annulment)
        return WalletOperationResult.EventRecorded(annulment)
    }
}
