package com.coinden.app.perception

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.coinden.app.download.DownloadResult
import com.coinden.app.download.VerifiedDownload
import java.io.File

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
    ): DownloadResult = VerifiedDownload.to(fileOf(model), url, model.sha256, model.sizeBytes, onProgress)
}
