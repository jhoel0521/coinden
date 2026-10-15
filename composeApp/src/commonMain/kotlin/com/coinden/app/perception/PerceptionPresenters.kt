package com.coinden.app.perception

import com.coinden.app.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Modelos de voz del teléfono: lo que la plataforma sabe descargar, verificar y borrar. */
interface VoiceModelManager {
    fun deviceProfile(): DeviceProfile

    fun installed(): List<VoiceModel>

    /** true si quedó descargado y verificado; false si falló o la huella no coincidió (se borró). */
    suspend fun download(
        model: VoiceModel,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): Boolean

    fun delete(model: VoiceModel)
}

data class VoiceModelRow(
    val model: VoiceModel,
    val installed: Boolean,
    val recommended: Boolean,
)

data class VoiceModelsState(
    val recommendation: VoiceRecommendation? = null,
    val models: List<VoiceModelRow> = emptyList(),
    /** Descarga en curso y su avance (0..1). */
    val downloading: VoiceModel? = null,
    val progress: Float = 0f,
    val failed: VoiceModel? = null,
)

/** Elegir, descargar y borrar modelos de voz. La app recomienda según el teléfono; la persona decide. */
class VoiceModelsPresenter(
    private val manager: VoiceModelManager,
) {
    private val mutableState = MutableStateFlow(VoiceModelsState())
    val state: StateFlow<VoiceModelsState> = mutableState.asStateFlow()

    fun load() {
        val recommendation = ModelRecommender.recommend(manager.deviceProfile())
        val installed = manager.installed().toSet()
        mutableState.update {
            it.copy(
                recommendation = recommendation,
                models =
                    VoiceModels.all.map { model ->
                        VoiceModelRow(model, model in installed, recommendation == VoiceRecommendation.Download(model))
                    },
            )
        }
    }

    suspend fun download(model: VoiceModel) {
        if (state.value.downloading != null) return
        mutableState.update { it.copy(downloading = model, progress = 0f, failed = null) }
        val ok =
            manager.download(model) { done, total ->
                mutableState.update { it.copy(progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f) }
            }
        mutableState.update { it.copy(downloading = null, failed = if (ok) null else model) }
        load()
    }

    fun delete(model: VoiceModel) {
        manager.delete(model)
        load()
    }
}

data class PrivacyState(
    val googleReader: Boolean = false,
)

/** Pantalla de privacidad: qué sale del teléfono y la elección del lector de Google. */
class PrivacyPresenter(
    private val container: AppContainer,
) {
    private val mutableState = MutableStateFlow(PrivacyState())
    val state: StateFlow<PrivacyState> = mutableState.asStateFlow()

    suspend fun load() {
        mutableState.value = PrivacyState(container.storage.preferences.googleReaderEnabled())
    }

    fun onGoogleReaderChange(enabled: Boolean) = mutableState.update { it.copy(googleReader = enabled) }

    /** Guarda la elección y recuerda que la persona ya vio la pantalla. */
    suspend fun onContinue() {
        container.storage.preferences.setGoogleReaderEnabled(state.value.googleReader)
        container.storage.preferences.setPrivacyNoticeSeen()
    }
}
