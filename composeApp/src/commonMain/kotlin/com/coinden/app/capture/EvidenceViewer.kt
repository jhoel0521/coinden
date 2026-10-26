package com.coinden.app.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coinden.app.AppContainer
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Cómo se muestra un archivo de evidencia. */
enum class EvidenceKind {
    IMAGE,
    PDF,
    AUDIO,
    OTHER,
    ;

    companion object {
        fun of(mimeType: String): EvidenceKind =
            when {
                mimeType.startsWith("image/") -> IMAGE
                mimeType == "application/pdf" -> PDF
                mimeType.startsWith("audio/") -> AUDIO
                else -> OTHER
            }
    }
}

/**
 * Muestra el archivo de una evidencia, ya descifrado en memoria: la foto o el PDF a pantalla
 * completa y el audio con reproducción. No escribe copias descifradas en el teléfono.
 */
interface EvidenceViewerPort {
    @Composable
    fun Show(
        attachment: Attachment,
        modifier: Modifier,
    )
}

data class EvidenceState(
    /** La evidencia original y las unidas, en orden. */
    val items: List<Evidence> = emptyList(),
    val index: Int = 0,
    /** El archivo de la evidencia actual, descifrado en memoria (null si es solo texto o no se pudo leer). */
    val attachment: Attachment? = null,
    val unavailable: Boolean = false,
) {
    val current: Evidence? get() = items.getOrNull(index)
}

/** Recorre la evidencia de un borrador o movimiento. Sin Compose. */
class EvidencePresenter(
    private val container: AppContainer,
    private val draftId: String,
) {
    private val mutableState = MutableStateFlow(EvidenceState())
    val state: StateFlow<EvidenceState> = mutableState.asStateFlow()

    /**
     * Todo lo que se mandó junto, con Quitar y Separar; null si el borrador no vino en un paquete
     * o ya no está pendiente (lo confirmado se corrige como movimiento, no reabriendo el paquete).
     */
    var bundle: BundleEvidenceViewer? = null
        private set

    suspend fun load() {
        val draft = container.drafts.load(draftId) ?: return
        val pending = draft !is Draft.Confirmed && draft !is Draft.Discarded
        val captured = draft.header.bundleId?.let { container.bundles.load(it) }
        bundle = captured?.takeIf { pending }?.let { BundleEvidenceViewer(container, it.id) }
        val items = captured?.evidences?.map { it.evidence } ?: (listOf(draft.header.evidence) + draft.header.extraEvidence)
        mutableState.value = EvidenceState(items = items)
        show(0)
    }

    suspend fun show(index: Int) {
        val evidence = state.value.items.getOrNull(index) ?: return
        val attachment = evidence.attachmentRef?.let { runCatching { container.attachments.read(it) }.getOrNull() }
        mutableState.update {
            it.copy(index = index, attachment = attachment, unavailable = evidence.attachmentRef != null && attachment == null)
        }
    }

    /** Al cerrar el visor: suelta los bytes descifrados. */
    fun close() = mutableState.update { it.copy(attachment = null) }
}
