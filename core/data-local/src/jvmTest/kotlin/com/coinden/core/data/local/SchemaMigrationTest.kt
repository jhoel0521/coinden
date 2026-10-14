package com.coinden.core.data.local

import com.coinden.core.data.local.db.CoinDenDatabase
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Una base creada con el esquema 1 se migra al abrirla, sin perder sus datos. */
class SchemaMigrationTest {
    private val file = File.createTempFile("coinden-v1-", ".db").also { it.delete() }

    @AfterTest
    fun cleanUp() {
        file.delete()
    }

    @Test
    fun `la version 1 se migra a la 2 conservando las billeteras`() =
        runTest {
            assertEquals(2L, CoinDenDatabase.Schema.version)

            // Base como la dejaba local-persistence: esquema 1, sin app_settings
            val v1 = JvmDatabase.driver(file.path)
            val wallet = Wallet("w1", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
            LocalWalletRepository(JvmDatabase.open(v1), Dispatchers.Unconfined).saveWallet(wallet)
            v1.execute(null, "DROP TABLE app_settings", 0)
            v1.execute(null, "PRAGMA user_version = 1", 0)
            v1.close()

            val v2 = JvmDatabase.open(JvmDatabase.driver(file.path))
            assertEquals(wallet, LocalWalletRepository(v2, Dispatchers.Unconfined).getWallet("w1"))
            val preferences = LocalPreferencesRepository(v2, Dispatchers.Unconfined)
            preferences.setLanguageCode("es")
            assertEquals("es", preferences.languageCode())
        }
}
