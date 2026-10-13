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
 * una llave AES-GCM no exportable del Android Keystore, en el almacenamiento privado de la app.
 * No se pide cuenta ni contraseña; el PIN opcional es un cambio aparte.
 */
class AndroidDatabase private constructor(
    val driver: SqlDriver,
) {
    val database: CoinDenDatabase = CoinDenDatabase(driver)

    fun close() = driver.close()

    companion object {
        const val DEFAULT_NAME = "coinden.db"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "coinden.db"
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
            val passphrase = passphrase(File(context.filesDir, "$name.key"))
            // La fábrica limpia la passphrase de memoria después de abrir la base.
            val driver = AndroidSqliteDriver(CoinDenDatabase.Schema, context, name, factory = SupportOpenHelperFactory(passphrase))
            return AndroidDatabase(driver)
        }

        /** Lee la passphrase guardada o crea una nueva; solo la llave del Keystore puede descifrarla. */
        private fun passphrase(file: File): ByteArray {
            val key = keystoreKey()
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

        private fun keystoreKey(): SecretKey {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            generator.init(
                KeyGenParameterSpec
                    .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            return generator.generateKey()
        }
    }
}
