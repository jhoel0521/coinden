package com.coinden.app.capture

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Se ejecuta en el emulador: una foto guardada no es un JPEG legible y se lee de vuelta igual. */
@RunWith(AndroidJUnit4::class)
class EncryptedAttachmentStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = context.filesDir.resolve("prueba-adjuntos")
    private val store = EncryptedAttachmentStore(dir, keyAlias = "coinden.attachments.prueba")

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun la_foto_guardada_no_es_un_jpeg_legible_y_se_lee_de_vuelta() =
        runBlocking {
            // Firma JPEG (FF D8 FF E0) y un texto que no debe verse en el archivo
            val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + "BNB 45,00 secreto".encodeToByteArray()

            val id = store.save(jpeg, "image/jpeg")

            val raw = store.fileOf(id).readBytes()
            assertFalse("no empieza con la firma JPEG", raw[0] == 0xFF.toByte() && raw[1] == 0xD8.toByte())
            assertFalse("el texto no se ve", raw.decodeToString(throwOnInvalidSequence = false).contains("secreto"))
            assertFalse("el tipo no se ve", raw.decodeToString(throwOnInvalidSequence = false).contains("image/jpeg"))

            val read = store.read(id)!!
            assertArrayEquals(jpeg, read.bytes)
            assertEquals("image/jpeg", read.mimeType)
        }

    @Test
    fun un_pdf_y_un_audio_tambien_se_guardan_y_un_id_ajeno_no_se_lee() =
        runBlocking {
            val pdf = "%PDF-1.7 comprobante".encodeToByteArray()
            val audio = "OggS nota de voz".encodeToByteArray()

            val pdfId = store.save(pdf, "application/pdf")
            val audioId = store.save(audio, "audio/ogg")

            assertArrayEquals(pdf, store.read(pdfId)!!.bytes)
            assertEquals("audio/ogg", store.read(audioId)!!.mimeType)
            assertNull(store.read("../coinden.db"))
            assertNull(store.read("00000000-0000-0000-0000-000000000000"))
        }
}
