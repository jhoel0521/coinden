package com.coinden.core.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Se ejecuta en el emulador: el archivo de la base no se puede leer sin la llave. */
@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "prueba-cifrado.db"

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
        context.filesDir.resolve("$name.key").delete()
    }

    @Test
    fun el_archivo_esta_cifrado_y_se_reabre_con_la_llave_guardada() =
        runBlocking {
            val wallet = Wallet("w1", "carlos", "Dolares colchon secreto", Currency.USD, WalletType.CASH)
            val captured = DraftEvent.Captured("e1", "d1", "carlos", "dev", 1L, 1L, Evidence(EvidenceSource.TEXT, "almuerzo secreto 45 qr"))

            val first = AndroidDatabase.open(context, name)
            LocalWalletRepository(first.database, Dispatchers.IO).saveWallet(wallet)
            LocalDraftRepository(first.database, Dispatchers.IO).appendEvent(captured)
            first.close()

            val bytes = context.getDatabasePath(name).readBytes()
            val text = String(bytes, Charsets.ISO_8859_1)
            assertFalse("no debe ser un SQLite legible", text.startsWith("SQLite format 3"))
            assertFalse("no debe contener el nombre de la billetera", text.contains("colchon secreto"))
            assertFalse("no debe contener la captura", text.contains("almuerzo secreto"))

            val reopened = AndroidDatabase.open(context, name)
            assertEquals(wallet, LocalWalletRepository(reopened.database, Dispatchers.IO).getWallet("w1"))
            assertEquals(listOf(captured), LocalDraftRepository(reopened.database, Dispatchers.IO).eventsFor("d1"))
            reopened.close()
        }
}
