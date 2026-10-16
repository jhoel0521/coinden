package com.coinden.app.capture

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coinden.app.perception.ReceiptImages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** Se ejecuta en el emulador con evidencia inventada: se ve y se escucha sin dejar copias descifradas. */
@RunWith(AndroidJUnit4::class)
class EvidenceRenderingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun filesNow(): Set<String> =
        listOf(context.cacheDir, context.filesDir)
            .flatMap { dir ->
                dir
                    .walkTopDown()
                    .filter(File::isFile)
                    .map { it.path }
                    .toList()
            }.toSet()

    /** Un PDF de dos páginas con el comprobante inventado dibujado. */
    private fun pdf(): ByteArray {
        val document = PdfDocument()
        repeat(2) { n ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, n + 1).create())
            val paint = Paint().apply { textSize = 14f }
            ReceiptImages.QR_PAYMENT.forEachIndexed { i, line -> page.canvas.drawText(line, 40f, 60f + i * 24f, paint) }
            document.finishPage(page)
        }
        return ByteArrayOutputStream().use {
            document.writeTo(it)
            document.close()
            it.toByteArray()
        }
    }

    /** Medio segundo de un tono de 440 Hz en WAV PCM de 16 bits. */
    private fun wav(): ByteArray {
        val rate = 16_000
        val samples = rate / 2
        val pcm = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(samples) { i -> pcm.putShort((sin(2 * PI * 440 * i / rate) * 8_000).toInt().toShort()) }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVE".toByteArray())
        header
            .put("fmt ".toByteArray())
            .putInt(16)
            .putShort(1)
            .putShort(1)
        header
            .putInt(rate)
            .putInt(rate * 2)
            .putShort(2)
            .putShort(16)
        header.put("data".toByteArray()).putInt(samples * 2)
        return header.array() + pcm.array()
    }

    @Test
    fun la_foto_el_pdf_y_el_audio_se_muestran_sin_escribir_copias() {
        val before = filesNow()

        val image = EvidenceRendering.image(ReceiptImages.jpeg())
        assertNotNull(image)
        EvidenceRendering.pdf(context, pdf())!!.use { pages ->
            assertEquals(2, pages.pageCount)
            val page = pages.render(1)
            assertNotNull(page)
            assertTrue(page!!.width > 595)
        }
        val player = EvidenceRendering.player(wav())
        assertNotNull(player)
        assertTrue(player!!.duration in 400..600)
        player.release()

        assertEquals(before, filesNow())
    }

    @Test
    fun lo_que_no_es_pdf_ni_audio_no_se_abre() {
        assertNull(EvidenceRendering.pdf(context, "no es un pdf".toByteArray()))
        assertNull(EvidenceRendering.player("tampoco audio".toByteArray()))
    }
}
