package com.coinden.app.capture

/** Archivo de una evidencia: bytes y tipo MIME ("image/jpeg", "application/pdf", "audio/ogg"). */
class Attachment(
    val bytes: ByteArray,
    val mimeType: String,
)

/**
 * Evidencias en archivo (fotos, PDF, audios), fuera de la base. En Android se guardan cifradas
 * con una llave del Keystore; el dominio solo conoce el id que devuelve [save].
 */
interface AttachmentStore {
    suspend fun save(
        bytes: ByteArray,
        mimeType: String,
    ): String

    suspend fun read(id: String): Attachment?
}

/** Almacén en memoria para las pruebas en la JVM. */
class InMemoryAttachmentStore : AttachmentStore {
    private val files = mutableMapOf<String, Attachment>()
    private var next = 0

    override suspend fun save(
        bytes: ByteArray,
        mimeType: String,
    ): String = "attachment-${next++}".also { files[it] = Attachment(bytes.copyOf(), mimeType) }

    override suspend fun read(id: String): Attachment? = files[id]?.let { Attachment(it.bytes.copyOf(), it.mimeType) }
}
