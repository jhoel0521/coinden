package com.coinden.app.erase

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.coinden.app.MainActivity
import com.coinden.app.capture.EncryptedAttachmentStore
import com.coinden.core.data.local.AndroidDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore

/**
 * Borrado total en Android, en el orden del diseño: marcar, cancelar el trabajo en segundo plano,
 * cerrar y borrar la base con su llave, y luego adjuntos, cola y temporales. La base y la llave van
 * primero: si se corta a mitad, lo que quede ya no se puede leer, y el marcador `erase.pending`
 * hace que el siguiente inicio termine el borrado antes de abrir la base.
 *
 * No se tocan `models/`, `packs/` ni `tesseract/`: son datos públicos descargados, no de la persona.
 * Las pruebas usan otras carpetas, otro nombre de base y otro alias para no tocar los de la app.
 */
class AndroidDataEraser(
    private val context: Context,
    private val filesDir: File = context.filesDir,
    private val cacheDir: File = context.cacheDir,
    private val databaseName: String = AndroidDatabase.DEFAULT_NAME,
    private val attachmentsKeyAlias: String = EncryptedAttachmentStore.KEY_ALIAS,
    private val cancelWork: () -> Unit = {
        WorkManager.getInstance(context).run {
            cancelAllWork().result.get()
            pruneWork().result.get()
        }
    },
    private val closeDatabase: () -> Unit = {},
) : DataEraser {
    private val marker get() = File(filesDir, MARKER)

    /** Lo de la persona fuera de la base. */
    private fun personalFiles(): List<File> =
        listOf(
            File(filesDir, "$databaseName.key"),
            File(filesDir, ATTACHMENTS),
            File(filesDir, QUEUE_FAILURES),
            File(filesDir, "$QUEUE_FAILURES.tmp"),
        ) + cacheDir.listFiles { file -> isLeftover(file.name) }.orEmpty()

    /** Si un borrado quedó a medias (batería, cierre forzado), lo termina. Va antes de abrir la base. */
    fun finishPending(): Boolean {
        if (!marker.exists()) return false
        runCatching(cancelWork)
        eraseFiles()
        return true
    }

    override suspend fun eraseAll() =
        withContext(Dispatchers.IO) {
            filesDir.mkdirs()
            check(marker.exists() || marker.createNewFile()) { "no se pudo marcar el borrado" }
            cancelWork()
            closeDatabase()
            eraseFiles()
        }

    private fun eraseFiles() {
        AndroidDatabase.destroy(context, databaseName)
        KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(attachmentsKeyAlias)
        personalFiles().forEach { it.deleteRecursively() }
        val left = personalFiles().filter { it.exists() } + listOf(context.getDatabasePath(databaseName)).filter { it.exists() }
        check(left.isEmpty()) { "quedaron ${left.size} archivos" }
        marker.delete()
    }

    /** Vuelve a la bienvenida como recién instalada: relanza la pantalla principal y termina el proceso. */
    override fun restart() {
        context.startActivity(Intent.makeRestartActivityTask(ComponentName(context, MainActivity::class.java)))
        Runtime.getRuntime().exit(0)
    }

    companion object {
        const val MARKER = "erase.pending"
        private const val ATTACHMENTS = "attachments"
        private const val QUEUE_FAILURES = "queue-failures.tsv"
        private const val KEYSTORE = "AndroidKeyStore"

        /** Temporales que pueden quedar en la caché: PDF descifrados y audios de contexto. */
        fun isLeftover(name: String): Boolean =
            (name.startsWith("pdf-") && name.endsWith(".pdf")) || (name.startsWith("contexto-") && name.endsWith(".m4a"))
    }
}
