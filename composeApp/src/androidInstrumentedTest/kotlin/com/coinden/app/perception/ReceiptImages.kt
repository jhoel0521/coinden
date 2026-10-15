package com.coinden.app.perception

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

/** Dibuja un comprobante inventado (estructura de un pago QR, datos falsos) como imagen. */
object ReceiptImages {
    val QR_PAYMENT =
        listOf(
            "Banco ECONOMICO",
            "Pago QR otros bancos",
            "Beneficiario",
            "POLLOS DEL VALLE",
            "Importe",
            "Bs25.00",
            "Nro. de transaccion: 400000001",
        )

    fun bitmap(lines: List<String> = QR_PAYMENT): Bitmap {
        val bitmap = Bitmap.createBitmap(1080, 120 + lines.size * 90, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 52f
                typeface = Typeface.DEFAULT
            }
        lines.forEachIndexed { i, line -> canvas.drawText(line, 60f, 110f + i * 90f, paint) }
        return bitmap
    }

    fun jpeg(lines: List<String> = QR_PAYMENT): ByteArray =
        ByteArrayOutputStream().use {
            bitmap(lines).compress(Bitmap.CompressFormat.JPEG, 90, it)
            it.toByteArray()
        }
}
