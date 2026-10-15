package com.coinden.app.perception

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Se ejecuta en el emulador: ML Kit lee con su modelo incluido en la app, sin descargar nada. */
@RunWith(AndroidJUnit4::class)
class MlKitImageReaderTest {
    @Test
    fun lee_el_numero_de_transaccion_con_el_modelo_incluido() =
        runBlocking {
            val reader = MlKitImageReader()
            try {
                val read = reader.read(ReceiptImages.jpeg())
                assertNotNull(read)
                assertTrue(read!!, read.contains("400000001"))
            } finally {
                reader.close()
            }
        }
}
