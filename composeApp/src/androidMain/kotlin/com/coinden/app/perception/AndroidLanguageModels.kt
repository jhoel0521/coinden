package com.coinden.app.perception

import android.content.Context
import com.coinden.app.download.DownloadResult
import com.coinden.app.download.VerifiedDownload
import com.coinden.app.intent.InstalledLanguageModel
import java.io.File

/**
 * El modelo de lenguaje en el almacenamiento privado (`filesDir/models/`). Se baja solo cuando la
 * persona lo pide, desde la dirección pública del catálogo y sin enviar nada suyo; antes de
 * usarse se verifica su huella con el mismo [VerifiedDownload] que los modelos de voz.
 */
class AndroidLanguageModels(
    context: Context,
    private val installedModel: InstalledLanguageModel,
) : LanguageModelManager {
    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "models").apply { mkdirs() }
    private val file = File(dir, LanguageModels.LOCAL_FILE_NAME)

    override fun deviceProfile() = readDeviceProfile(appContext, dir)

    override fun isInstalled(): Boolean = file.isFile

    override suspend fun download(
        model: LanguageModel,
        onProgress: (Long, Long) -> Unit,
    ): Boolean = VerifiedDownload.to(file, model.url, model.sha256, model.sizeBytes, onProgress) is DownloadResult.Ready

    override suspend fun delete() {
        installedModel.release()
        file.delete()
    }

    override fun consumeLoadFailure(): Boolean = installedModel.consumeLoadFailure()
}
