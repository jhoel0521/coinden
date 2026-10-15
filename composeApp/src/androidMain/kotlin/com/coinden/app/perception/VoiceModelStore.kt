package com.coinden.app.perception

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Resultado de bajar un modelo. */
sealed interface DownloadResult {
    data class Ready(
        val file: File,
    ) : DownloadResult

    /** La huella no coincide: el archivo se borró y no se usa. */
    data object Corrupted : DownloadResult

    data object Failed : DownloadResult
}

/**
 * Modelos de voz descargados en el almacenamiento privado. Solo se descargan cuando la persona lo
 * pide, desde la dirección pública del catálogo, sin enviar nada suyo; antes de usarse se verifica
 * su huella. También dice cómo es el teléfono para recomendar uno.
 */
class VoiceModelStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "models").apply { mkdirs() }

    fun fileOf(model: VoiceModel): File = File(dir, model.fileName)

    /** Modelos ya descargados y verificados, del más preciso al más chico. */
    fun installed(): List<VoiceModel> = VoiceModels.all.filter { fileOf(it).isFile }.reversed()

    fun delete(model: VoiceModel) {
        fileOf(model).delete()
    }

    fun deviceProfile(): DeviceProfile {
        val memory = ActivityManager.MemoryInfo().also { (appContext.getSystemService(ActivityManager::class.java)).getMemoryInfo(it) }
        return DeviceProfile(
            totalMemoryBytes = memory.totalMem,
            is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty(),
            freeStorageBytes = StatFs(dir.absolutePath).availableBytes,
        )
    }

    suspend fun download(
        model: VoiceModel,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
        url: String = model.url,
    ): DownloadResult =
        withContext(Dispatchers.IO) {
            val temp = File(dir, "${model.fileName}.part")
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                val digest = MessageDigest.getInstance("SHA-256")
                connection.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var total = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            total += n
                            onProgress(total, model.sizeBytes)
                        }
                    }
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                if (hash != model.sha256) {
                    temp.delete()
                    DownloadResult.Corrupted
                } else {
                    val target = fileOf(model)
                    target.delete()
                    if (temp.renameTo(target)) DownloadResult.Ready(target) else DownloadResult.Failed
                }
            }.getOrElse {
                temp.delete()
                DownloadResult.Failed
            }
        }
}
