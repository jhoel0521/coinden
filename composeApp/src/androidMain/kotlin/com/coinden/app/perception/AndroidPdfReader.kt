package com.coinden.app.perception

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Lee un PDF en el teléfono: el texto que ya trae (PdfBox, Apache 2.0) o, si es escaneado, sus
 * páginas dibujadas como imágenes para el lector de fotos. El archivo temporal se borra al terminar.
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

    private fun pagesOf(
        pdf: ByteArray,
        maxPages: Int,
    ): PdfReading {
        val temp = File(appContext.cacheDir, "pdf-${UUID.randomUUID()}.pdf")
        try {
            temp.writeBytes(pdf)
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    val images =
                        (0 until minOf(renderer.pageCount, maxPages)).map { index ->
                            renderer.openPage(index).use { page ->
                                val scale = PAGE_WIDTH.toFloat() / page.width
                                val bitmap = Bitmap.createBitmap(PAGE_WIDTH, (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                ByteArrayOutputStream().use {
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                                    bitmap.recycle()
                                    it.toByteArray()
                                }
                            }
                        }
                    return if (images.isEmpty()) PdfReading.Unreadable else PdfReading.Pages(images)
                }
            }
        } finally {
            temp.delete()
        }
    }

    private companion object {
        /** Menos letras y dígitos que esto: el PDF no trae texto útil (escaneado). */
        const val MIN_USEFUL_CHARS = 20
        const val PAGE_WIDTH = 2000
    }
}
