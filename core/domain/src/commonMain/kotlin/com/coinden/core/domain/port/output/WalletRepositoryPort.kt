package com.coinden.core.domain.port.output

import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet

/**
 * Puerto de Salida (SPI) para billeteras y su libro de movimientos.
 *
 * El libro es append-only: [appendEvent] solo agrega, nunca reemplaza ni borra.
 * Los metadatos de la billetera ([Wallet]) sí se guardan como estado actual.
 */
interface WalletRepositoryPort {
    suspend fun saveWallet(wallet: Wallet)

    suspend fun getWallet(walletId: String): Wallet?

    suspend fun listWallets(ownerId: String): List<Wallet>

    suspend fun appendEvent(event: LedgerEvent)

    /**
     * Agrega varios eventos juntos (una anulación y su reemplazo): o quedan todos o ninguno.
     * Un adaptador con base de datos lo hace en una transacción.
     */
    suspend fun appendEvents(events: List<LedgerEvent>) = events.forEach { appendEvent(it) }

    suspend fun findEvent(eventId: String): LedgerEvent?

    /**
     * Devuelve todos los eventos que afectan a la billetera, incluidas las
     * transferencias en las que participa y las anulaciones que las referencian.
     */
    suspend fun eventsForWallet(walletId: String): List<LedgerEvent>
}
