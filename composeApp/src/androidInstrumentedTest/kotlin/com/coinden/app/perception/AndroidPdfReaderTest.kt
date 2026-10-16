package com.coinden.app.perception

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/** Se ejecuta en el emulador con PDF inventados generados en la prueba. */
@RunWith(AndroidJUnit4::class)
class AndroidPdfReaderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val reader = AndroidPdfReader(context)

    /** Un PDF con texto, como el que exporta una banca por internet. */
    private fun textPdf(): ByteArray =
        pdf { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f }
            ReceiptImages.QR_PAYMENT.forEachIndexed { i, line -> canvas.drawText(line, 40f, 60f + i * 24f, paint) }
        }

    /** Un PDF escaneado: solo la imagen del comprobante, sin texto. */
    private fun scannedPdf(): ByteArray =
        pdf { canvas ->
            val bitmap = ReceiptImages.bitmap()
            canvas.drawColor(Color.WHITE)
            canvas.scale(595f / bitmap.width, 595f / bitmap.width)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }

    private fun pdf(draw: (android.graphics.Canvas) -> Unit): ByteArray {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        draw(page.canvas)
        document.finishPage(page)
        return ByteArrayOutputStream().use {
            document.writeTo(it)
            document.close()
            it.toByteArray()
        }
    }

    @Test
    fun un_pdf_con_texto_se_lee_sin_dibujarlo() =
        runBlocking {
            val reading = reader.read(textPdf(), maxPages = 5)
            assertTrue(reading is PdfReading.Text)
            val text = (reading as PdfReading.Text).text
            assertTrue(text, text.contains("400000001"))
        }

    @Test
    fun un_pdf_escaneado_devuelve_sus_paginas_y_tesseract_las_lee() =
        runBlocking {
            val before = storedFiles()
            val reading = reader.read(scannedPdf(), maxPages = 5)
            assertTrue(reading is PdfReading.Pages)
            // Las páginas se dibujan en memoria: no aparece ninguna copia del PDF en el teléfono.
            assertEquals(before, storedFiles())
            val pages = (reading as PdfReading.Pages).images
            assertEquals(1, pages.size)
            val text = TesseractImageReader(context) { "es" }.read(pages.single()).orEmpty()
            assertTrue(text, text.contains("400000001"))
        }

    private fun storedFiles(): Set<String> =
        listOf(context.cacheDir, context.filesDir)
            .flatMap { dir ->
                dir
                    .walkTopDown()
                    .filter { it.isFile }
                    .map { it.path }
                    .toList()
            }.toSet()

    @Test
    fun al_iniciar_se_borran_los_pdf_temporales_de_una_version_anterior() {
        val leftover = java.io.File(context.cacheDir, "pdf-anterior.pdf").apply { writeBytes(textPdf()) }

        AndroidPdfReader.clearLeftovers(context)

        assertTrue(!leftover.exists())
    }

    @Test
    fun un_archivo_que_no_es_pdf_no_se_puede_leer() =
        runBlocking {
            assertEquals(PdfReading.Unreadable, reader.read("no soy un pdf".encodeToByteArray(), maxPages = 5))
        }
}
