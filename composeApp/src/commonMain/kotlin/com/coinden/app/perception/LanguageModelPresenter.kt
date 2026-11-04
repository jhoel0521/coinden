package com.coinden.app.perception

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** El modelo de lenguaje del teléfono: lo que la plataforma sabe descargar, verificar y borrar. */
interface LanguageModelManager {
    fun deviceProfile(): DeviceProfile

    /** true si hay un modelo descargado y verificado. */
    fun isInstalled(): Boolean

    /** true si quedó descargado y verificado; false si falló o la huella no coincidió (se borró). */
    suspend fun download(
        model: LanguageModel,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): Boolean

    suspend fun delete()

    /** true la primera vez que se pregunta después de que el modelo instalado no cargó. */
    fun consumeLoadFailure(): Boolean
}

data class LanguageModelState(
    val offer: LanguageModelOffer? = null,
    val installed: Boolean = false,
    /** Descarga en curso y su avance (0..1). */
    val downloading: LanguageModel? = null,
    val progress: Float = 0f,
    val failed: Boolean = false,
    /** El modelo instalado no cargó: se sigue con el respaldo y se avisa una vez. */
    val loadFailed: Boolean = false,
)

/** Descargar y borrar el modelo de lenguaje. La app ofrece según el teléfono; la persona decide. */
class LanguageModelPresenter(
    private val manager: LanguageModelManager,
) {
    private val mutableState = MutableStateFlow(LanguageModelState())
    val state: StateFlow<LanguageModelState> = mutableState.asStateFlow()

    fun load() {
        val offer = LanguageModelRecommender.recommend(manager.deviceProfile())
        val installed = manager.isInstalled()
        val loadFailed = installed && manager.consumeLoadFailure()
        mutableState.update { it.copy(offer = offer, installed = installed, loadFailed = loadFailed) }
    }

    suspend fun download(model: LanguageModel) {
        if (state.value.downloading != null) return
        mutableState.update { it.copy(downloading = model, progress = 0f, failed = false) }
        val ok =
            manager.download(model) { done, total ->
                mutableState.update { it.copy(progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f) }
            }
        mutableState.update { it.copy(downloading = null, failed = !ok) }
        load()
    }

    suspend fun delete() {
        manager.delete()
        mutableState.update { it.copy(failed = false) }
        load()
    }
}
