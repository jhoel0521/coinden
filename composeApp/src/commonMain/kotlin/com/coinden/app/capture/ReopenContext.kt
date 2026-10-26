package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.usecase.bundle.LastPendingDraftUseCase
import com.coinden.core.domain.usecase.bundle.ReopenBundleUseCase

/** El último borrador pendiente, para ofrecer "Agregar al último" sin mostrar montos. */
data class LastPendingHint(
    val draftId: String,
    val note: String?,
    val capturedAtEpochMs: Long,
)

/**
 * Los accesos para sumarle contexto a algo ya capturado: "Agregar contexto" en la tarjeta, en
 * Revisar y en el formulario, y "Agregar al último" en la hoja de Capturar.
 *
 * Todos terminan en lo mismo: reabrir el paquete de ese borrador y mostrar la página de contexto.
 * Un borrador de antes de los paquetes se convierte en uno al reabrirlo, sin tocar el borrador.
 */
class ReopenContext(
    private val container: AppContainer,
) {
    /**
     * Reabre el paquete del borrador y devuelve su id, o null si ya no se puede: un borrador
     * confirmado o descartado no se reabre, para eso está la corrección de movimientos.
     */
    suspend fun forDraft(draftId: String): String? {
        val bundleId = container.drafts.load(draftId)?.let { bundleIdOf(it) } ?: return null
        return if (ReopenBundleUseCase(container.bundles)(bundleId)) bundleId else null
    }

    /** Lo que ofrece "Agregar al último", o null si no hay nada pendiente. */
    suspend fun lastPending(): LastPendingHint? =
        LastPendingDraftUseCase(container.bundles)(container.ownerId)?.let {
            LastPendingHint(it.header.id, it.header.evidence.text, it.header.capturedAtEpochMs)
        }

    /**
     * El paquete al que pertenece el borrador. Los de la 0.1.0 no tienen uno: su propio id hace
     * de paquete, y se materializa recién si la persona lo reabre.
     */
    private fun bundleIdOf(draft: Draft): String = draft.header.bundleId ?: draft.header.id
}
