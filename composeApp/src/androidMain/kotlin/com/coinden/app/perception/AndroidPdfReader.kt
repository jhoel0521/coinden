package com.coinden.app.perception

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Lee un PDF en el teléfono: el texto que ya trae (PdfBox, Apache 2.0) o, si es escaneado, sus
 * páginas dibujadas en memoria como imágenes para el lector de fotos. Nada descifrado se escribe en el teléfono.
 */
class AndroidPdfReader(
    context: Context,
) : PdfTextReader {
    private val appContext = context.applicationContext

    override suspend fun read(
        pdf: ByteArray,
        maxPages: Int,
    ): PdfReading =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = textOf(pdf)
                if (text.count { it.isLetterOrDigit() } >= MIN_USEFUL_CHARS) PdfReading.Text(text) else pagesOf(pdf, maxPages)
            }.getOrDefault(PdfReading.Unreadable)
        }

    private fun textOf(pdf: ByteArray): String {
        PDFBoxResourceLoader.init(appContext)
        return PDDocument.load(pdf).use { document ->
            PDFTextStripper().apply { sortByPosition = true }.getText(document).trim()
        }
    }

    /** Dibuja las primeras páginas en memoria (pdfbox): nunca escribe el PDF descifrado en el teléfono. */
    private fun pagesOf(
        pdf: ByteArray,
        maxPages: Int,
    ): PdfReading {
        PDFBoxResourceLoader.init(appContext)
        return PDDocument.load(pdf).use { document ->
            val renderer = PDFRenderer(document)
            val images =
                (0 until minOf(document.numberOfPages, maxPages)).map { index ->
                    val width = document.getPage(index).mediaBox.width
                    val bitmap = renderer.renderImage(index, PAGE_WIDTH / width, ImageType.RGB)
                    ByteArrayOutputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                        bitmap.recycle()
                        it.toByteArray()
                    }
                }
            if (images.isEmpty()) PdfReading.Unreadable else PdfReading.Pages(images)
        }
    }

    companion object {
        /** Menos letras y dígitos que esto: el PDF no trae texto útil (escaneado). */
        private const val MIN_USEFUL_CHARS = 20
        private const val PAGE_WIDTH = 2000f

        /** Borra las copias temporales que dejaba una versión anterior al leer un PDF escaneado. */
        fun clearLeftovers(context: Context) {
            context.cacheDir.listFiles { file -> file.name.startsWith("pdf-") && file.name.endsWith(".pdf") }?.forEach { it.delete() }
        }
    }
}
