package com.coinden.core.domain.feature

/**
 * Comprobantes de prueba con la ESTRUCTURA real de los que comparten los usuarios (orden de
 * líneas, etiquetas y formatos de monto y fecha), pero con datos INVENTADOS: nombres, cuentas,
 * NIT, PIN y números de transacción no corresponden a nadie. Nunca se versionan datos reales.
 *
 * La cuenta 1111222233 es la cuenta (inventada) de la billetera "BE Bs" de Carlos.
 */
object AnonymizedReceipts {
    const val CARLOS_BE_ACCOUNT = "1111222233"

    /** Bolivia: pago con Yape. */
    val YAPE_PAYMENT =
        """
        yape
        TARIJA VIÑEDOS
        ¡Yapeaste!
        Bs 36
        Mariela Quispe Rojas
        03 oct. 2026 | 12:54 p. m.
        DATOS DE LA TRANSACCIÓN
        Cuenta 70000001
        Destino Banco Ganadero
        Nro. de transacción 900000001
        Realizado por Carlos Mamani Flores
        Con el respaldo del BCP
        """.trimIndent()

    /** Bolivia: Banco Económico, pago QR a otro banco. */
    val BE_QR_CHICKEN =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Procesada
        Beneficiario
        POLLOS DEL VALLE
        Banco destino: BANCO DE CREDITO - BCP
        Cuenta destino: 20000000000001
        NIT o carnet: 1234567
        Importe
        Bs25.00
        Nota: POLLOS DEL VALLE
        Remitente
        CARLOS MAMANI FLORES
        Cuenta origen: CA: $CARLOS_BE_ACCOUNT (Bs)
        Otros
        Fecha y hora: 10/Oct/2026 14:22:50
        Nro. de transacción: 400000001
        Código de autorización: 100001
        """.trimIndent()

    /** Bolivia: Banco Económico, pago QR a una farmacia. */
    val BE_QR_PHARMACY =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Procesada
        Beneficiario
        FARMACIA EJEMPLO S.A.
        Banco destino: BANCO GANADERO
        Cuenta destino: 1300000001
        NIT o carnet: 1000000001
        Importe
        Bs31.70
        Nota: Pago de productos
        Remitente
        CARLOS MAMANI FLORES
        Cuenta origen: CA: $CARLOS_BE_ACCOUNT (Bs)
        Otros
        Fecha y hora: 11/Oct/2026 10:04:28
        Nro. de transacción: 400000002
        Código de autorización: 100002
        """.trimIndent()

    /** Bolivia: Banco Económico, débito por compra en comercio electrónico (monto con signo). */
    val BE_ECOMMERCE_DEBIT =
        """
        Banco ECONOMICO
        DEBITO POR COMPRA EN COMERCIO ELECTRONIC
        Completado
        Cuenta
        CA: $CARLOS_BE_ACCOUNT (Bs)
        Titular de la cuenta: CARLOS MAMANI FLORES
        Importe
        -Bs118.71
        Nota: StreamingPremium650-2530000US
        Fecha y Hora: 09/Oct/2026 18:49:22
        Nro. de transacción : 1000000001
        """.trimIndent()

    /** Bolivia: Banco Económico, retiro de efectivo móvil. */
    val BE_CASH_WITHDRAWAL =
        """
        Banco ECONOMICO
        Retiro de Efectivo
        Efectivo Móvil pagado
        PIN para el ATM
        0000
        Nro. de cédula de identidad: 0000000
        Monto
        Bs10.00
        Cuenta
        CARLOS MAMANI FLORES
        Cuenta origen: CA: $CARLOS_BE_ACCOUNT (Bs)
        Número de pago: 90000001
        Otros
        Nro. de transacción: 8000001
        Código de autorización: 700001
        Fecha y hora: 11/Oct/2026 19:03:38
        """.trimIndent()

    /** Argentina: banco desconocido (inventado), transferencia en pesos. */
    val AR_UNKNOWN_TRANSFER =
        """
        Banco Imaginario
        ¡Transferiste!
        Monto
        $ 8.000,00
        Para: Kiosco Don Pepe
        Desde: Cuenta 4444555566
        11/10/2026 09:15
        Operación N° 123456
        """.trimIndent()

    /** Brasil: comprobante Pix inventado. */
    val BR_PIX =
        """
        Comprovante
        Pix enviado
        Valor
        R$ 45,90
        Para
        Padaria Pão Quente
        Data 10/10/2026 - 14:22
        ID da transação E00000000000000000001
        """.trimIndent()

    /** EEUU: pago inventado en inglés. */
    val US_PAYMENT =
        """
        Payment confirmation
        You paid
        Amount ${'$'}25.00
        To Corner Coffee Shop
        Oct 10, 2026 2:22 PM
        Confirmation 000111222
        """.trimIndent()
}
