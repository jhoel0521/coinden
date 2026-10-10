package com.coinden.core.domain.model.wallet

import com.coinden.core.domain.model.Money

/**
 * Proyección pura del libro de una billetera: saldo, historial vigente y punto más bajo.
 *
 * Los eventos se ordenan por (ocurrencia, registro, id) para obtener siempre el mismo orden,
 * sin importar en qué dispositivo o en qué secuencia llegaron.
 */
class LedgerProjection(
    val wallet: Wallet,
    events: List<LedgerEvent>,
) {
    private val annulledIds: Set<String> =
        events.filterIsInstance<LedgerEvent.Annulment>().map { it.targetId }.toSet()

    /** Eventos que mueven dinero en esta billetera y no fueron anulados, en orden cronológico. */
    val effectiveEvents: List<LedgerEvent> =
        events
            .filter { it !is LedgerEvent.Annulment && it.id !in annulledIds && it.deltaFor(wallet.id) != null }
            .sortedWith(CHRONOLOGICAL)

    val balance: Money

    /** Saldo acumulado más bajo de toda la historia (cero si nunca bajó de cero). */
    val lowestBalance: Money

    /** Primer momento en que el saldo acumulado quedó negativo, o null si nunca ocurrió. */
    val firstNegativeAtEpochMs: Long?

    init {
        var running = 0L
        var lowest = 0L
        var firstNegative: Long? = null
        for (event in effectiveEvents) {
            running += event.deltaFor(wallet.id)!!.amountInMinorUnits
            if (running < lowest) lowest = running
            if (running < 0 && firstNegative == null) firstNegative = event.occurredAtEpochMs
        }
        balance = Money(running, wallet.currency)
        lowestBalance = Money(lowest, wallet.currency)
        firstNegativeAtEpochMs = firstNegative
    }

    /** Saldo acumulado después de cada evento vigente, en orden cronológico. */
    val runningBalances: List<Pair<LedgerEvent, Money>> by lazy {
        var running = 0L
        effectiveEvents.map { event ->
            running += event.deltaFor(wallet.id)!!.amountInMinorUnits
            event to Money(running, wallet.currency)
        }
    }

    fun isAnnulled(eventId: String): Boolean = eventId in annulledIds

    /** Faltante máximo como monto positivo (cero si nunca hubo saldo negativo). */
    val shortfall: Money get() = -lowestBalance

    /** Billetera que no admite negativos pero cuyo libro tiene algún punto negativo. */
    val isUnbalanced: Boolean get() = !wallet.allowsNegative && lowestBalance.isNegative

    companion object {
        val CHRONOLOGICAL: Comparator<LedgerEvent> =
            compareBy<LedgerEvent>({ it.occurredAtEpochMs }, { it.recordedAtEpochMs }, { it.id })
    }
}
