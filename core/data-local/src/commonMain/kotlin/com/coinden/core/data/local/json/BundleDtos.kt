package com.coinden.core.data.local.json

import com.coinden.core.domain.model.bundle.BundleEvent
import com.coinden.core.domain.model.bundle.EvidenceRole
import kotlinx.serialization.Serializable

/**
 * Tipos de los eventos del paquete de captura.
 *
 * Van en la misma tabla `events` que todo lo demás, con tipos propios: el esquema de la base no
 * cambia, así que una base escrita por la 0.1.0 se sigue leyendo igual.
 */
object BundleTypes {
    const val OPENED = "bundle.opened"
    const val EVIDENCE_ATTACHED = "bundle.evidence_attached"
    const val EVIDENCE_EXCLUDED = "bundle.evidence_excluded"
    const val EVIDENCE_PERCEIVED = "bundle.evidence_perceived"
    const val CLOSED = "bundle.closed"
    const val REOPENED = "bundle.reopened"
    const val EVIDENCE_SPLIT = "bundle.evidence_split"
}

/**
 * El `bundleId` viene del ref de la tabla, así que no se repite acá. El dueño sí: la consulta por
 * ref no lo devuelve, y un evento tiene que poder reconstruirse entero con lo que guardó.
 */
@Serializable
internal data class BundleOpenedDto(
    val fields: EventFieldsDto,
    val ownerId: String,
)

@Serializable
internal data class EvidenceAttachedDto(
    val fields: EventFieldsDto,
    val ownerId: String,
    val evidenceId: String,
    val evidence: EvidenceDto,
    val role: String,
)

@Serializable
internal data class EvidenceExcludedDto(
    val fields: EventFieldsDto,
    val ownerId: String,
    val evidenceId: String,
)

@Serializable
internal data class EvidencePerceivedDto(
    val fields: EventFieldsDto,
    val ownerId: String,
    val evidenceId: String,
    /** null: el lector no sacó texto. Se guarda igual, para no volver a intentarlo. */
    val text: String? = null,
    val engine: String,
)

@Serializable
internal data class BundleClosedDto(
    val fields: EventFieldsDto,
    val ownerId: String,
)

@Serializable
internal data class BundleReopenedDto(
    val fields: EventFieldsDto,
    val ownerId: String,
)

@Serializable
internal data class EvidenceSplitDto(
    val fields: EventFieldsDto,
    val ownerId: String,
    val evidenceId: String,
    val newBundleId: String,
)

internal fun EvidenceRole.toDto() = name

/** Un papel que esta app no conoce se lee como contexto: suma, pero no manda sobre los hechos. */
internal fun String.toEvidenceRole() = EvidenceRole.entries.firstOrNull { it.name == this } ?: EvidenceRole.CONTEXT

internal fun BundleEvent.fields() = EventFieldsDto(id, deviceId, occurredAtEpochMs, recordedAtEpochMs)
