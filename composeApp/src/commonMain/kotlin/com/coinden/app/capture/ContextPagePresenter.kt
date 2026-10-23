package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.usecase.bundle.AttachEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.AttachResult
import com.coinden.core.domain.usecase.bundle.CloseBundleUseCase
import com.coinden.core.domain.usecase.bundle.ExcludeEvidenceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Una evidencia en la página: qué es, qué dice y si la persona la quitó. */
data class ContextItem(
    val evidenceId: String,
    val source: EvidenceSource,
    val role: EvidenceRole,
    val text: String?,
    val excluded: Boolean,
)

/**
 * Lo que la página muestra. [showsAmounts] es siempre false a propósito: esta página no interpreta
 * nada, así que no hay montos que ocultar y no hace falta desbloquear la app para verla.
 */
data class ContextPageState(
    val items: List<ContextItem> = emptyList(),
    val canRecord: Boolean = true,
    val closed: Boolean = false,
) {
    val showsAmounts: Boolean get() = false
}

/**
 * La página de contexto rápido: todo lo que la persona mandó junto, con la opción de sumar un
 * audio, una nota o una foto, y de quitar lo que no es de ese pago.
 *
 * Tocar Listo o salir cierra el paquete, que es lo que lo manda a la cola. No hay temporizadores:
 * salir por cualquier motivo cierra.
 */
class ContextPagePresenter(
    private val container: AppContainer,
    private val bundleId: String,
    private val canRecord: () -> Boolean = { true },
) {
    private val mutable = MutableStateFlow(ContextPageState())
    val state: StateFlow<ContextPageState> = mutable.asStateFlow()

    suspend fun load() {
        refresh()
    }

    suspend fun addAudio(evidence: Evidence): Boolean = attach(evidence, EvidenceRole.CONTEXT)

    suspend fun addPhoto(evidence: Evidence): Boolean = attach(evidence, EvidenceRole.of(evidence))

    /** Una nota escrita por la persona: explica, nunca es un hecho del comprobante. */
    suspend fun addNote(text: String): Boolean {
        val limpio = text.trim()
        if (limpio.isEmpty()) return false
        return attach(Evidence(EvidenceSource.TEXT, limpio), EvidenceRole.CONTEXT)
    }

    /** Quitar: queda guardada y a la vista, marcada, pero no se interpreta. */
    suspend fun remove(evidenceId: String) {
        ExcludeEvidenceUseCase(container.bundles)(bundleId, evidenceId)
        refresh()
    }

    suspend fun done() = close()

    /** Salir de la página, por el motivo que sea, cierra el paquete. */
    suspend fun onLeave() = close()

    private suspend fun close() {
        if (!mutable.value.closed) CloseBundleUseCase(container.bundles)(bundleId)
        refresh()
    }

    private suspend fun attach(
        evidence: Evidence,
        role: EvidenceRole,
    ): Boolean {
        val resultado = AttachEvidenceUseCase(container.bundles)(bundleId, evidence, role)
        refresh()
        return resultado is AttachResult.Attached
    }

    private suspend fun refresh() {
        val bundle = container.bundles.load(bundleId)
        mutable.value =
            ContextPageState(
                items =
                    bundle?.evidences.orEmpty().map {
                        ContextItem(it.id, it.evidence.source, it.role, it.text, it.excluded)
                    },
                canRecord = canRecord(),
                closed = bundle?.open == false,
            )
    }
}
