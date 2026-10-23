package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.usecase.bundle.ExcludeEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.SplitEvidenceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Una evidencia del paquete tal como se ve en la revisión.
 *
 * [pendingRead] es un archivo que la cola todavía no leyó; [unreadable], uno que no se pudo leer.
 * Son distintos a propósito: el primero es cuestión de esperar, el segundo pide completarlo a mano.
 */
data class BundleEvidenceItem(
    val evidenceId: String,
    val source: EvidenceSource,
    val role: EvidenceRole,
    val text: String?,
    val excluded: Boolean,
    val pendingRead: Boolean,
    val unreadable: Boolean,
)

/**
 * El visor de todo lo que la persona mandó junto, para la revisión de la noche.
 *
 * Muestra cada evidencia con lo que se leyó de ella, en el orden en que llegó, y lo que se quitó
 * sigue a la vista, marcado: nada se borra. Desde acá se puede quitar lo que sobra o separar lo
 * que era otro pago.
 */
class BundleEvidenceViewer(
    private val container: AppContainer,
    private val bundleId: String,
) {
    private val mutable = MutableStateFlow<List<BundleEvidenceItem>>(emptyList())
    val state: StateFlow<List<BundleEvidenceItem>> = mutable.asStateFlow()

    suspend fun load() {
        refresh()
    }

    /** "No es de este pago": queda guardada, deja de interpretarse. */
    suspend fun remove(evidenceId: String) {
        ExcludeEvidenceUseCase(container.bundles)(bundleId, evidenceId)
        refresh()
    }

    /** "Era otro pago": sale a su propio paquete. Devuelve el paquete nuevo, o null si no se pudo. */
    suspend fun split(evidenceId: String): String? {
        val nuevo = SplitEvidenceUseCase(container.bundles)(bundleId, evidenceId)
        refresh()
        return nuevo
    }

    private suspend fun refresh() {
        val bundle = container.bundles.load(bundleId)
        mutable.value =
            bundle?.evidences.orEmpty().map {
                BundleEvidenceItem(
                    evidenceId = it.id,
                    source = it.evidence.source,
                    role = it.role,
                    text = it.text,
                    excluded = it.excluded,
                    pendingRead = it.needsReading,
                    unreadable = it.unreadable,
                )
            }
    }
}
