package com.coinden.app.perception

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Se ejecuta en el emulador: Tesseract lee un comprobante inventado sin red. */
@RunWith(AndroidJUnit4::class)
class TesseractImageReaderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val reader = TesseractImageReader(context) { "es" }

    @Test
    fun lee_el_importe_y_el_numero_de_transaccion() =
        runBlocking {
            val read = reader.read(ReceiptImages.jpeg())
            assertNotNull(read)
            val text = read!!
            assertTrue(text, text.contains("400000001"))
            assertTrue(text, text.contains("Importe"))
            assertTrue(text, text.contains("25.00"))
        }

    @Test
    fun una_imagen_en_blanco_no_tiene_texto() =
        runBlocking {
            assertNull(reader.read(ReceiptImages.jpeg(emptyList())))
        }
}
