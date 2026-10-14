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

    /** Medios de pago con los que puede salir dinero de este tipo de billetera. */
    val acceptedMethods: Set<PaymentMethod>
        get() =
            when (this) {
                CASH -> setOf(PaymentMethod.CASH)
                BANK -> setOf(PaymentMethod.TRANSFER, PaymentMethod.CARD)
                DIGITAL -> setOf(PaymentMethod.TRANSFER)
                CRYPTO -> setOf(PaymentMethod.TRANSFER)
                CREDIT -> setOf(PaymentMethod.CARD)
            }
}

/**
 * Medio de pago: de dónde puede salir el dinero, no por qué canal. Un medio no identifica una
 * billetera: filtra las candidatas. "QR", "Pix", banca por internet o ventanilla son [TRANSFER].
 */
enum class PaymentMethod {
    /** Efectivo, en la moneda de la billetera. */
    CASH,

    /** Transferencia desde una cuenta: QR, Pix, banca por internet o ventanilla. */
    TRANSFER,

    /** Tarjeta de débito o de crédito. */
    CARD,
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
