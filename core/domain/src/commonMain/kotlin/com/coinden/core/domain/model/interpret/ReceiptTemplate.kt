package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.wallet.PaymentMethod

/** Qué movimiento describe un comprobante. */
enum class TemplateKind {
    EXPENSE,
    INCOME,
    WITHDRAWAL,
}

/**
 * Plantilla de un comprobante conocido, como datos: si todas sus [markers] aparecen en el texto,
 * sus campos se toman como evidencia. Agregar un banco es agregar una plantilla, no código.
 *
 * Cada etiqueta busca su valor en la misma línea (después de la etiqueta) o en la siguiente.
 * [counterpartyAfterAmount] cubre formatos como Yape, donde el destinatario va en la línea que
 * sigue al monto, sin etiqueta. [issuerWalletHint] es el alias de la billetera del emisor ("yape").
 * Las etiquetas se escriben normalizadas: minúsculas, sin tildes ni puntos ("nro de transaccion").
 */
data class ReceiptTemplate(
    val id: String,
    val markers: List<String>,
    val kind: TemplateKind,
    val amountLabel: String? = null,
    val counterpartyLabel: String? = null,
    val noteLabel: String? = null,
    val accountLabel: String? = null,
    val issuerWalletHint: String? = null,
    val counterpartyAfterAmount: Boolean = false,
    /** Etiqueta del número de transacción, normalizada ("nro de transaccion"). */
    val referenceLabel: String? = null,
    /**
     * Con qué se pagó, cuando el comprobante lo prueba: un comprobante de pago QR es una
     * transferencia, y uno de retiro es efectivo. Sin esto, una frase que dice "en efectivo"
     * se confunde con el pago del comprobante cuando coincide el monto.
     */
    val method: PaymentMethod? = null,
)
