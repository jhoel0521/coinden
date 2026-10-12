package com.coinden.core.domain.model.interpret

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
)

/**
 * Catálogo inicial de plantillas (Bolivia). Solo se usan las que el puerto indica instaladas.
 */
object ReceiptTemplates {
    val YAPE_BO =
        ReceiptTemplate(
            id = "bo-yape-payment",
            markers = listOf("yapeaste"),
            kind = TemplateKind.EXPENSE,
            issuerWalletHint = "yape",
            counterpartyAfterAmount = true,
        )

    val BE_QR_PAYMENT =
        ReceiptTemplate(
            id = "bo-be-qr-payment",
            markers = listOf("pago qr"),
            kind = TemplateKind.EXPENSE,
            amountLabel = "importe",
            counterpartyLabel = "beneficiario",
            noteLabel = "nota",
            accountLabel = "cuenta origen",
        )

    val BE_ECOMMERCE_DEBIT =
        ReceiptTemplate(
            id = "bo-be-ecommerce-debit",
            markers = listOf("debito por compra"),
            kind = TemplateKind.EXPENSE,
            amountLabel = "importe",
            noteLabel = "nota",
            accountLabel = "cuenta",
        )

    val BE_CASH_WITHDRAWAL =
        ReceiptTemplate(
            id = "bo-be-cash-withdrawal",
            markers = listOf("retiro de efectivo"),
            kind = TemplateKind.WITHDRAWAL,
            amountLabel = "monto",
            accountLabel = "cuenta origen",
        )

    val all: List<ReceiptTemplate> = listOf(YAPE_BO, BE_QR_PAYMENT, BE_ECOMMERCE_DEBIT, BE_CASH_WITHDRAWAL)
}
