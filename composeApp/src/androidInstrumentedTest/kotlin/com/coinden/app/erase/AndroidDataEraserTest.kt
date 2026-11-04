package com.coinden.app.erase

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coinden.app.capture.EncryptedAttachmentStore
import com.coinden.core.data.local.AndroidDatabase
import com.coinden.core.data.local.LocalWalletRepository
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore

/**
 * Se ejecuta en el emulador, con carpetas, base y alias propios: los datos reales de la app no se
 * tocan. Carlos borra todo y no queda nada suyo; Ana conserva sus modelos y paquetes.
 */
@RunWith(AndroidJUnit4::class)
class AndroidDataEraserTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val root = context.cacheDir.resolve("prueba-borrado")
    private val files = root.resolve("files")
    private val cache = root.resolve("cache")
    private val name = "prueba-borrado-app.db"
    private val alias = "coinden.attachments.prueba-borrado"
    private var cancelled = 0

    private fun eraser(closeDatabase: () -> Unit = {}) =
        AndroidDataEraser(context, files, cache, name, alias, cancelWork = { cancelled++ }, closeDatabase = closeDatabase)

    private fun keystoreHas(alias: String) = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(alias)

    /** El teléfono de Carlos: una base con su billetera, fotos cifradas, cola, temporales y descargas públicas. */
    private fun carlosPhone(): AndroidDatabase =
        runBlocking {
            val db = AndroidDatabase.open(context, name)
            LocalWalletRepository(db.database, Dispatchers.IO)
                .saveWallet(Wallet("w1", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK))
            val photos = EncryptedAttachmentStore(files.resolve("attachments"), alias)
            repeat(5) { photos.save("comprobante $it".encodeToByteArray(), "image/jpeg") }
            files.resolve("queue-failures.tsv").writeText("d1\t1\t0")
            cache.mkdirs()
            cache.resolve("pdf-123.pdf").writeText("pdf")
            cache.resolve("contexto-456.m4a").writeText("audio")
            for (public in listOf("models/whisper.bin", "packs/es.json", "tesseract/tessdata/spa.traineddata")) {
                files.resolve(public).apply { parentFile!!.mkdirs() }.writeText("publico")
            }
            db
        }

    @After
    fun cleanUp() {
        AndroidDatabase.destroy(context, name)
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
        root.deleteRecursively()
    }

    private fun assertNothingPersonalLeft() {
        assertFalse("queda la base", context.getDatabasePath(name).exists())
        assertFalse("queda la llave", context.filesDir.resolve("$name.key").exists())
        assertFalse("queda la entrada del Keystore de la base", keystoreHas(name))
        assertFalse("quedan los adjuntos", files.resolve("attachments").exists())
        assertFalse("queda la llave de los adjuntos", keystoreHas(alias))
        assertFalse("queda la cola", files.resolve("queue-failures.tsv").exists())
        assertEquals("quedan temporales", emptyList<String>(), cache.list().orEmpty().toList())
        assertFalse("queda el marcador", files.resolve(AndroidDataEraser.MARKER).exists())
    }

    private fun assertPublicKept() {
        for (public in listOf("models/whisper.bin", "packs/es.json", "tesseract/tessdata/spa.traineddata")) {
            assertTrue("se borró $public", files.resolve(public).isFile)
        }
    }

    @Test
    fun carlos_borra_todo_y_no_queda_nada_suyo_pero_si_los_modelos_y_paquetes() =
        runBlocking {
            val db = carlosPhone()
            var closed = false
            eraser { db.close().also { closed = true } }.eraseAll()

            assertTrue("la base no se cerró", closed)
            assertEquals(1, cancelled)
            assertNothingPersonalLeft()
            assertPublicKept()
        }

    @Test
    fun un_borrado_cortado_despues_de_la_base_se_completa_al_siguiente_inicio() {
        carlosPhone().close()
        // Se cortó después de marcar y de borrar la base, antes de las fotos.
        File(files, AndroidDataEraser.MARKER).createNewFile()
        AndroidDatabase.destroy(context, name)
        assertTrue(files.resolve("attachments").exists())

        assertTrue(eraser().finishPending())

        assertNothingPersonalLeft()
        assertPublicKept()
    }

    @Test
    fun sin_marcador_el_inicio_no_borra_nada() {
        carlosPhone().close()
        assertFalse(eraser().finishPending())
        assertTrue(context.getDatabasePath(name).exists())
        assertTrue(files.resolve("attachments").exists())
    }

    @Test
    fun los_temporales_que_se_borran_son_solo_los_de_la_persona() {
        assertTrue(AndroidDataEraser.isLeftover("pdf-1.pdf"))
        assertTrue(AndroidDataEraser.isLeftover("contexto-2.m4a"))
        assertFalse(AndroidDataEraser.isLeftover("whisper.bin"))
    }
}
