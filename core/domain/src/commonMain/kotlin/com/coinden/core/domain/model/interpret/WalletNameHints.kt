package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.wallet.Wallet

/**
 * Apodos deducidos del nombre de cada billetera, sin que el usuario configure nada:
 * "BNB Bs" -> "bnb", "Banco Económico" -> "economico", "Dólares colchón" -> "colchon".
 *
 * Del nombre normalizado se descartan las palabras de moneda ("bs", "usd", "dolares"), de medio de
 * pago ("qr", "efectivo"), las genéricas del paquete ("banco", "cuenta") y las de menos de 3 letras.
 * Una palabra que comparten dos billeteras activas no identifica a ninguna.
 */
object WalletNameHints {
    private const val MIN_LENGTH = 3

    /** Palabra normalizada -> id de billetera. */
    fun derive(
        wallets: List<Wallet>,
        pack: LanguagePack,
        locale: UserLocale,
    ): Map<String, String> {
        val owners = mutableMapOf<String, MutableSet<String>>()
        wallets.filter { it.isActive }.forEach { wallet ->
            TextNormalizer
                .normalize(wallet.name)
                .split(' ')
                .filter { it.length >= MIN_LENGTH && !isNoise(it, pack, locale) }
                .forEach { owners.getOrPut(it) { mutableSetOf() } += wallet.id }
        }
        return owners.filterValues { it.size == 1 }.mapValues { it.value.single() }
    }

    private fun isNoise(
        word: String,
        pack: LanguagePack,
        locale: UserLocale,
    ): Boolean =
        word in pack.genericWalletWords ||
            word in pack.paymentMethodWords ||
            word in pack.currencyWords ||
            MoneyScanner.scan("1 $word", locale, pack.currencyWords).firstOrNull()?.currency != null
}
