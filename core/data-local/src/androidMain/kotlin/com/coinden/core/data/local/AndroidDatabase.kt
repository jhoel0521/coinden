package com.coinden.core.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.coinden.core.data.local.db.CoinDenDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Base local cifrada en Android: SQLite con SQLCipher (AES-256 de todo el archivo).
 *
 * La passphrase son 32 bytes aleatorios generados en el primer inicio. Se guardan cifrados con
 * una llave AES-GCM no exportable del Android Keystore (su alias es el nombre de la base), en el
 * almacenamiento privado de la app. No se pide cuenta ni contraseña; el PIN opcional es un cambio aparte.
 */
class AndroidDatabase private constructor(
    val driver: SqlDriver,
) {
    val database: CoinDenDatabase = CoinDenDatabase(driver)

    fun close() = driver.close()

    companion object {
        const val DEFAULT_NAME = "coinden.db"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128

        init {
            System.loadLibrary("sqlcipher")
        }

        fun open(
            context: Context,
            name: String = DEFAULT_NAME,
        ): AndroidDatabase {
            val passphrase = passphrase(keyFile(context, name), alias = name)
            // La fábrica limpia la passphrase de memoria después de abrir la base.
            val driver = AndroidSqliteDriver(CoinDenDatabase.Schema, context, name, factory = SupportOpenHelperFactory(passphrase))
            return AndroidDatabase(driver)
        }

        /**
         * Borrado total por decisión de la persona: la base con sus archivos (-wal, -shm, -journal),
         * su passphrase y la entrada del Keystore, juntas. Sin esa llave, una copia que haya quedado
         * no se puede descifrar, y la próxima apertura crea una base y una llave nuevas. La base
         * debe estar cerrada. Borrar lo que ya no existe no falla.
         */
        fun destroy(
            context: Context,
            name: String = DEFAULT_NAME,
        ) {
            context.deleteDatabase(name)
            keyFile(context, name).delete()
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(name)
        }

        private fun keyFile(
            context: Context,
            name: String,
        ) = File(context.filesDir, "$name.key")

        /** Lee la passphrase guardada o crea una nueva; solo la llave del Keystore puede descifrarla. */
        private fun passphrase(
            file: File,
            alias: String,
        ): ByteArray {
            val key = keystoreKey(alias)
            if (file.exists()) {
                val stored = file.readBytes()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, stored, 0, IV_SIZE))
                return cipher.doFinal(stored, IV_SIZE, stored.size - IV_SIZE)
            }
            val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            file.writeBytes(cipher.iv + cipher.doFinal(passphrase))
            return passphrase
        }

        private fun keystoreKey(alias: String): SecretKey {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            generator.init(
                KeyGenParameterSpec
                    .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            return generator.generateKey()
        }
    }
}
