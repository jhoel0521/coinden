package com.coinden.core.domain.model.wallet

import com.coinden.core.domain.model.Money

/**
 * Evento inmutable del libro de movimientos (append-only, listo para sincronizar).
 *
 * Todo evento lleva un identificador global, el dispositivo que lo creó, el momento
 * en que ocurrió y el momento en que se registró. Corregir un evento no lo edita:
 * se registra un [Annulment] que lo referencia.
 */
sealed class LedgerEvent {
    abstract val id: String
    abstract val deviceId: String
    abstract val occurredAtEpochMs: Long
    abstract val recordedAtEpochMs: Long

    /** Billeteras cuyo saldo toca este evento. */
    abstract val affectedWalletIds: Set<String>

    /** Efecto de este evento sobre el saldo de [walletId], o null si no la toca. */
    open fun deltaFor(walletId: String): Money? = null

    data class OpeningBalance(
        override val id: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val walletId: String,
        val amount: Money,
    ) : LedgerEvent() {
        override val affectedWalletIds: Set<String> get() = setOf(walletId)

        override fun deltaFor(walletId: String): Money? = amount.takeIf { walletId == this.walletId }
    }

    data class Adjustment(
        override val id: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val walletId: String,
        val amount: Money,
    ) : LedgerEvent() {
        init {
            require(!amount.isZero) { "Un ajuste no puede ser cero" }
        }

        override val affectedWalletIds: Set<String> get() = setOf(walletId)

        override fun deltaFor(walletId: String): Money? = amount.takeIf { walletId == this.walletId }
    }

    data class Expense(
        override val id: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val walletId: String,
        val amount: Money,
    ) : LedgerEvent() {
        init {
            require(amount.isPositive) { "Un gasto debe ser estrictamente positivo" }
        }

        override val affectedWalletIds: Set<String> get() = setOf(walletId)

        override fun deltaFor(walletId: String): Money? = (-amount).takeIf { walletId == this.walletId }
    }

    data class Income(
        override val id: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val walletId: String,
        val amount: Money,
    ) : LedgerEvent() {
        init {
            require(amount.isPositive) { "Un ingreso debe ser estrictamente positivo" }
        }

        override val affectedWalletIds: Set<String> get() = setOf(walletId)

        override fun deltaFor(walletId: String): Money? = amount.takeIf { walletId == this.walletId }
    }

    /**
     * Transferencia con dos patas: sale [amountOut] de [fromWalletId] y entra [amountIn]
     * en [toWalletId]. Entre monedas distintas, la tasa se deriva con [derivedRate].
     */
    data class Transfer(
        override val id: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val fromWalletId: String,
        val toWalletId: String,
        val amountOut: Money,
        val amountIn: Money,
    ) : LedgerEvent() {
        init {
            require(fromWalletId != toWalletId) { "Origen y destino deben ser billeteras distintas" }
            require(amountOut.isPositive && amountIn.isPositive) { "Los montos de una transferencia deben ser positivos" }
            require(!isSameCurrency(amountOut, amountIn) || amountOut.amountInMinorUnits == amountIn.amountInMinorUnits) {
                "En la misma moneda, el monto que sale y el que entra deben coincidir"
            }
        }

        override val affectedWalletIds: Set<String> get() = setOf(fromWalletId, toWalletId)

        override fun deltaFor(walletId: String): Money? =
            when (walletId) {
                fromWalletId -> -amountOut
                toWalletId -> amountIn
                else -> null
            }

        /**
         * Cuánto de la moneda de destino se obtuvo por UNA unidad mayor de la moneda de origen,
         * con aritmética entera y redondeo half-even. Ej.: 50.00 USD -> 594.00 BOB = 11.88 BOB.
         */
        fun derivedRate(): Money {
            val numerator = amountIn.amountInMinorUnits * Money.scaleFactor(amountOut.currency.decimals)
            return Money(divideHalfEven(numerator, amountOut.amountInMinorUnits), amountIn.currency)
        }

        companion object {
            fun isSameCurrency(
                a: Money,
                b: Money,
            ): Boolean = a.currency.isSameAs(b.currency) && a.currency.decimals == b.currency.decimals
        }
    }

    /**
     * Anula a [targetId]: el evento original deja de contar en su propio punto
     * cronológico, como si no hubiera ocurrido.
     */
    data class Annulment(
        override val id: String,
        override val deviceId: String,
        override val occurredAtEpochMs: Long,
        override val recordedAtEpochMs: Long,
        val targetId: String,
        override val affectedWalletIds: Set<String>,
    ) : LedgerEvent()
}

private fun divideHalfEven(
    numerator: Long,
    denominator: Long,
): Long {
    val quotient = numerator / denominator
    val twiceRemainder = 2 * (numerator % denominator)
    return when {
        twiceRemainder > denominator -> quotient + 1
        twiceRemainder == denominator && quotient % 2 != 0L -> quotient + 1
        else -> quotient
    }
}
