package com.coinden.core.domain.port.output

import com.coinden.core.domain.model.bundle.BundleEvent
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.bundle.ReviewReason
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory

/**
 * Puerto de Salida (SPI) para el historial de los paquetes de captura. Es append-only:
 * [appendEvent] nunca reemplaza ni borra un evento.
 */
interface BundleRepositoryPort {
    suspend fun appendEvent(event: BundleEvent)

    suspend fun eventsFor(bundleId: String): List<BundleEvent>

    suspend fun bundleIdsFor(ownerId: String): List<String>
}

/** El texto de una evidencia del paquete, marcado con su papel, tal como entra al intérprete. */
data class BundleText(
    val evidenceId: String,
    val role: EvidenceRole,
    val source: EvidenceSource,
    val text: String,
)

/** Un pago que el intérprete encontró en el paquete. [paymentKey] lo identifica entre interpretaciones. */
data class BundlePayment(
    val paymentKey: String,
    val proposal: MovementProposal,
    val evidenceIds: Set<String>,
    val reviewReasons: Set<ReviewReason> = emptySet(),
)

/**
 * Puerto de Salida (SPI) del intérprete del paquete: reglas en el teléfono o, más adelante, un
 * modelo de lenguaje local con el mismo contrato. Nunca decide: el dominio valida y la persona confirma.
 */
interface BundleInterpreterPort {
    suspend fun interpret(
        texts: List<BundleText>,
        wallets: WalletDirectory,
    ): List<BundlePayment>
}
