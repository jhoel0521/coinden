package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.wallet.PaymentMethod

/**
 * Paquete de idioma: las palabras clave que el intérprete reconoce, como datos. Agregar un idioma
 * es agregar un paquete, no código. Todas las palabras se guardan normalizadas (ver [TextNormalizer]).
 */
data class LanguagePack(
    val code: String,
    val amountLabels: Set<String>,
    val counterpartyLabels: Set<String>,
    val accountLabels: Set<String>,
    val noteLabels: Set<String>,
    val expenseWords: Set<String>,
    val incomeWords: Set<String>,
    val withdrawalWords: Set<String>,
    val exchangeWords: Set<String>,
    val notePrepositions: Set<String>,
    val conjunctions: Set<String>,
    /** Palabra que introduce la tasa en un cambio ("a 11.88", "at 11.88"). */
    val rateMarkers: Set<String>,
    val months: Map<String, Int>,
    /** Palabra de moneda -> código ISO; null = la moneda del locale ("pesos"). */
    val currencyWords: Map<String, String?>,
    /** Palabras típicas de cada categoría base ("almuerzo" -> FOOD), normalizadas. */
    val categoryWords: Map<String, BaseCategory> = emptyMap(),
    /**
     * Marcadores de lo recibido en un cambio ("me dieron 112 bs", "got 1190 bob"); pueden tener
     * varias palabras. Solo cuentan dentro de un cambio y delante de un monto.
     */
    val receivedMarkers: Set<String> = emptySet(),
    /** Palabras de medio de pago ("qr", "pix", "efectivo"): filtran billeteras, no las nombran. */
    val paymentMethodWords: Map<String, PaymentMethod> = emptyMap(),
    /** Palabras genéricas de un nombre de billetera ("banco", "cuenta"): no sirven como apodo. */
    val genericWalletWords: Set<String> = emptySet(),
    /** Etiquetas del número de transacción en comprobantes sin plantilla, normalizadas. */
    val referenceLabels: Set<String> = emptySet(),
)

/** Une varios paquetes activos en uno solo para buscar en todos a la vez. */
fun List<LanguagePack>.merged(): LanguagePack =
    LanguagePack(
        code = joinToString("+") { it.code },
        amountLabels = flatMap { it.amountLabels }.toSet(),
        counterpartyLabels = flatMap { it.counterpartyLabels }.toSet(),
        accountLabels = flatMap { it.accountLabels }.toSet(),
        noteLabels = flatMap { it.noteLabels }.toSet(),
        expenseWords = flatMap { it.expenseWords }.toSet(),
        incomeWords = flatMap { it.incomeWords }.toSet(),
        withdrawalWords = flatMap { it.withdrawalWords }.toSet(),
        exchangeWords = flatMap { it.exchangeWords }.toSet(),
        notePrepositions = flatMap { it.notePrepositions }.toSet(),
        conjunctions = flatMap { it.conjunctions }.toSet(),
        rateMarkers = flatMap { it.rateMarkers }.toSet(),
        months = fold(emptyMap()) { acc, pack -> acc + pack.months },
        currencyWords = fold(emptyMap()) { acc, pack -> acc + pack.currencyWords },
        categoryWords = fold(emptyMap()) { acc, pack -> acc + pack.categoryWords },
        receivedMarkers = flatMap { it.receivedMarkers }.toSet(),
        paymentMethodWords = fold(emptyMap()) { acc, pack -> acc + pack.paymentMethodWords },
        genericWalletWords = flatMap { it.genericWalletWords }.toSet(),
        referenceLabels = flatMap { it.referenceLabels }.toSet(),
    )
