package com.coinden.core.domain.feature

import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.IdGeneratorPort
import com.coinden.core.domain.port.output.WalletRepositoryPort

/**
 * Repositorio en memoria de billeteras con libro append-only.
 */
class InMemoryWalletRepository : WalletRepositoryPort {
    private val wallets = mutableMapOf<String, Wallet>()
    private val events = mutableListOf<LedgerEvent>()

    override suspend fun saveWallet(wallet: Wallet) {
        wallets[wallet.id] = wallet
    }

    override suspend fun getWallet(walletId: String): Wallet? = wallets[walletId]

    override suspend fun listWallets(ownerId: String): List<Wallet> = wallets.values.filter { it.ownerId == ownerId }

    override suspend fun appendEvent(event: LedgerEvent) {
        check(events.none { it.id == event.id }) { "El libro es append-only: el evento ${event.id} ya existe" }
        events += event
    }

    override suspend fun findEvent(eventId: String): LedgerEvent? = events.firstOrNull { it.id == eventId }

    override suspend fun eventsForWallet(walletId: String): List<LedgerEvent> = events.filter { walletId in it.affectedWalletIds }

    val allEvents: List<LedgerEvent> get() = events.toList()
}

/**
 * Generador determinista de identificadores: "<prefix>-1", "<prefix>-2", ...
 */
class SequentialIdGenerator(
    private val prefix: String = "id",
) : IdGeneratorPort {
    private var next = 1

    override fun newId(): String = "$prefix-${next++}"
}

/**
 * Reloj controlable para pruebas.
 */
class FixedClock(
    var nowEpochMs: Long = 0L,
) : ClockPort {
    override fun nowEpochMs(): Long = nowEpochMs
}

/**
 * Identidad de dispositivo fija para pruebas.
 */
class FixedDevice(
    private val id: String = "device-phone",
) : DeviceIdentityPort {
    override fun deviceId(): String = id
}

/** Hora del día como epoch ms de un día de prueba (día 0). */
fun at(
    hour: Int,
    minute: Int = 0,
): Long = (hour * 60L + minute) * 60_000L
