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

/**
 * Puerto de Salida (SPI) del intérprete de intenciones: el modelo de lenguaje que corre **solo en
 * el teléfono**. Entiende qué quiso decir la persona; el dominio verifica lo que propone.
 *
 * Devuelve el mismo contrato que las reglas, así que el dominio no sabe cuál de los dos habló.
 * Un `null` significa "no hubo propuesta utilizable": no hay modelo, no entra en este teléfono, no
 * llegó a tiempo o su respuesta no cumplió el formato cerrado. En todos esos casos manda el
 * respaldo determinista, y la app sigue funcionando igual.
 *
 * [categories] y [wallets] van cerrados a propósito: el modelo elige de ahí y no puede inventar.
 */
interface IntentInterpreterPort {
    suspend fun interpret(
        texts: List<BundleText>,
        wallets: WalletDirectory,
        categories: Set<String>,
    ): List<BundlePayment>?
}

/**
 * Puerto de Salida (SPI) del motor que corre el modelo en el teléfono.
 *
 * El adaptador es quien fija el prompt de sistema, la temperatura 0 y aplica [grammar] durante la
 * decodificación, de modo que la respuesta no pueda salirse del formato. El dominio solo le pasa
 * datos y lee lo que vuelve.
 *
 * Devuelve `null` cuando no hay modelo, no entra en este teléfono, no llegó a tiempo o falló:
 * nunca lanza, porque quedarse sin modelo no es un error, es el caso normal de la mitad de los
 * teléfonos.
 */
interface LocalModelPort {
    suspend fun complete(
        input: String,
        grammar: String,
    ): String?
}
