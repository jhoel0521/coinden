package com.coinden.core.domain.model.wallet

import com.coinden.core.domain.model.Currency

/**
 * Tipo de "bolsillo" real del usuario. Solo [CREDIT] admite saldo negativo por defecto.
 */
enum class WalletType {
    CASH,
    BANK,
    DIGITAL,
    CRYPTO,
    CREDIT,
    ;

    val allowsNegativeByDefault: Boolean get() = this == CREDIT
}

/**
 * Ciclo de vida de una billetera. Las billeteras nunca se borran: solo se archivan.
 */
enum class WalletStatus {
    ACTIVE,
    ARCHIVED,
}

/**
 * Billetera de una sola moneda.
 *
 * El saldo no forma parte de la entidad: se deriva siempre del libro de movimientos
 * ([LedgerProjection]). El [name] lo escribe el usuario y se guarda tal cual.
 */
data class Wallet(
    val id: String,
    val ownerId: String,
    val name: String,
    val currency: Currency,
    val type: WalletType,
    val allowsNegative: Boolean = type.allowsNegativeByDefault,
    val status: WalletStatus = WalletStatus.ACTIVE,
) {
    init {
        require(isValidName(name)) { "El nombre de la billetera no puede estar vacío" }
    }

    val isActive: Boolean get() = status == WalletStatus.ACTIVE

    fun archived(): Wallet = copy(status = WalletStatus.ARCHIVED)

    fun reactivated(): Wallet = copy(status = WalletStatus.ACTIVE)

    fun accepts(currency: Currency): Boolean = this.currency.isSameAs(currency) && this.currency.decimals == currency.decimals

    companion object {
        fun isValidName(name: String): Boolean = name.isNotBlank()
    }
}
