package com.coinden.core.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * Se ejecuta en el emulador: el borrado total elimina la base, sus archivos y su llave juntos, y
 * la base nueva nace con otra llave. Usa su propio nombre (y su propia entrada del Keystore).
 */
@RunWith(AndroidJUnit4::class)
class DestroyDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "prueba-borrado.db"
    private val keyFile get() = context.filesDir.resolve("$name.key")

    private fun keystoreHas(alias: String) = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(alias)

    @After
    fun cleanUp() {
        AndroidDatabase.destroy(context, name)
    }

    @Test
    fun borrar_todo_elimina_la_base_sus_archivos_y_su_llave_y_la_nueva_tiene_otra_llave() =
        runBlocking {
            val wallet = Wallet("w1", "carlos", "Dolares colchon secreto", Currency.USD, WalletType.CASH)
            val first = AndroidDatabase.open(context, name)
            LocalWalletRepository(first.database, Dispatchers.IO).saveWallet(wallet)
            first.close()
            val oldDatabase = context.getDatabasePath(name).readBytes()
            val oldKey = keyFile.readBytes()
            assertTrue(keystoreHas(name))

            AndroidDatabase.destroy(context, name)

            for (suffix in listOf("", "-wal", "-shm", "-journal")) {
                assertFalse("queda $name$suffix", context.getDatabasePath("$name$suffix").exists())
            }
            assertFalse("queda la llave", keyFile.exists())
            assertFalse("queda la entrada del Keystore", keystoreHas(name))

            // Volver a empezar: una base vacía con una llave distinta.
            val second = AndroidDatabase.open(context, name)
            assertNull(LocalWalletRepository(second.database, Dispatchers.IO).getWallet("w1"))
            second.close()
            assertFalse("la llave nueva es igual a la vieja", oldKey.contentEquals(keyFile.readBytes()))

            // Una copia de la base vieja, incluso con su archivo de llave, ya no se puede abrir.
            context.getDatabasePath(name).writeBytes(oldDatabase)
            keyFile.writeBytes(oldKey)
            val reopened =
                runCatching {
                    val old = AndroidDatabase.open(context, name)
                    try {
                        LocalWalletRepository(old.database, Dispatchers.IO).getWallet("w1")
                    } finally {
                        old.close()
                    }
                }
            assertTrue("la copia vieja se pudo leer", reopened.isFailure)
        }

    @Test
    fun borrar_dos_veces_no_falla() {
        AndroidDatabase.destroy(context, name)
        AndroidDatabase.destroy(context, name)
        assertFalse(context.getDatabasePath(name).exists())
    }
}
