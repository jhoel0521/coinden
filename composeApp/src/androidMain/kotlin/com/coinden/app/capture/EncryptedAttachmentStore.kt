package com.coinden.app.capture

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Evidencias en archivos `<dir>/<uuid>.bin` cifrados con AES-256-GCM. La llave vive en el Android
 * Keystore (no se puede exportar) y es distinta de la de la base. Cada archivo lleva su IV al
 * inicio; el tipo MIME va cifrado junto con los bytes, así que nada del archivo es legible.
 */
class EncryptedAttachmentStore(
    private val dir: File,
    private val keyAlias: String = KEY_ALIAS,
) : AttachmentStore {
    override suspend fun save(
        bytes: ByteArray,
        mimeType: String,
    ): String =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val mime = mimeType.encodeToByteArray()
            require(mime.size <= MAX_MIME) { "Tipo MIME demasiado largo" }
            val plain = byteArrayOf((mime.size shr 8).toByte(), mime.size.toByte()) + mime + bytes
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            dir.mkdirs()
            fileOf(id).writeBytes(cipher.iv + cipher.doFinal(plain))
            id
        }

    override suspend fun read(id: String): Attachment? =
        withContext(Dispatchers.IO) {
            if (!ID.matches(id)) return@withContext null
            val file = fileOf(id).takeIf { it.isFile } ?: return@withContext null
            val stored = file.readBytes()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, stored, 0, IV_SIZE))
            val plain = cipher.doFinal(stored, IV_SIZE, stored.size - IV_SIZE)
            val mimeSize = ((plain[0].toInt() and 0xFF) shl 8) or (plain[1].toInt() and 0xFF)
            Attachment(plain.copyOfRange(2 + mimeSize, plain.size), plain.decodeToString(2, 2 + mimeSize))
        }

    /** El archivo cifrado de [id] (para comprobar que no es legible). */
    fun fileOf(id: String): File = dir.resolve("$id.bin")

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val KEY_ALIAS = "coinden.attachments"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
        private const val MAX_MIME = 0xFFFF
        private val ID = Regex("[0-9a-f-]{36}")
    }
}
