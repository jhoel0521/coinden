package com.coinden.core.domain.usecase.bundle

import com.coinden.core.domain.model.bundle.BundleEvent
import com.coinden.core.domain.model.bundle.BundleEvidence
import com.coinden.core.domain.model.bundle.BundleProjection
import com.coinden.core.domain.model.bundle.CaptureBundle
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.port.output.BundleInterpreterPort
import com.coinden.core.domain.port.output.BundleRepositoryPort
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.usecase.draft.DraftContext

/** Resultado de agregar una evidencia a un paquete. */
sealed interface AttachResult {
    data class Attached(
        val evidenceId: String,
    ) : AttachResult

    /** El mismo archivo (misma huella) ya está en el paquete: no se agrega otra vez. */
    data object AlreadyInBundle : AttachResult

    /** El paquete está cerrado o no existe: hay que reabrirlo primero. */
    data object NotOpen : AttachResult
}

/** Dependencias comunes de los casos de uso de paquetes. */
class BundleContext(
    val bundles: BundleRepositoryPort,
    val drafts: DraftContext,
    val interpreter: BundleInterpreterPort,
) {
    /**
     * El paquete con ese id. Un borrador de antes de los paquetes es su propio paquete virtual,
     * cerrado, con su evidencia: se lee sin escribir nada (actualizar no reescribe datos).
     */
    suspend fun load(bundleId: String): CaptureBundle? {
        BundleProjection.project(bundles.eventsFor(bundleId))?.let { return it }
        val legacy = drafts.load(bundleId) ?: return null
        if (legacy.header.bundleId != null) return null
        return legacyBundle(legacy)
    }

    /** Los borradores que produjo el paquete, en el orden en que se capturaron. */
    suspend fun draftsOf(bundle: CaptureBundle): List<Draft> =
        drafts.drafts
            .draftIdsFor(bundle.ownerId)
            .mapNotNull { drafts.load(it) }
            .filter { it.header.bundleId == bundle.id || (it.id == bundle.id && it.header.bundleId == null) }
            .sortedBy { it.header.capturedAtEpochMs }

    suspend fun record(build: (id: String, deviceId: String, now: Long) -> BundleEvent) {
        val now = drafts.clock.nowEpochMs()
        bundles.appendEvent(build(drafts.ids.newId(), drafts.device.deviceId(), now))
    }

    companion object {
        /** Id de la evidencia original de un borrador de antes de los paquetes. */
        fun legacyEvidenceId(draftId: String): String = "$draftId:0"

        fun legacyBundle(draft: Draft): CaptureBundle {
            val header = draft.header
            val original =
                BundleEvidence(
                    legacyEvidenceId(draft.id),
                    header.evidence,
                    EvidenceRole.of(header.evidence),
                    header.capturedAtEpochMs,
                    perceivedText = header.perceivedText,
                )
            val extra =
                header.extraEvidence.mapIndexed { index, evidence ->
                    BundleEvidence("${draft.id}:${index + 1}", evidence, EvidenceRole.of(evidence), header.capturedAtEpochMs)
                }
            return CaptureBundle(draft.id, header.ownerId, listOf(original) + extra, open = false, header.capturedAtEpochMs)
        }
    }
}

/** Caso de Uso: abrir un paquete con lo que la persona compartió o capturó. Devuelve su id. */
class StartBundleUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(
        ownerId: String,
        evidences: List<Evidence>,
    ): String {
        val bundleId = ctx.drafts.ids.newId()
        ctx.record { id, device, now -> BundleEvent.Opened(id, bundleId, ownerId, device, now, now) }
        val attach = AttachEvidenceUseCase(ctx)
        evidences.forEach { attach(bundleId, it) }
        return bundleId
    }
}

/** Caso de Uso: sumar una evidencia a un paquete abierto, sin repetir un archivo que ya está. */
class AttachEvidenceUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(
        bundleId: String,
        evidence: Evidence,
        role: EvidenceRole = EvidenceRole.of(evidence),
    ): AttachResult {
        val bundle = ctx.load(bundleId)?.takeIf { it.open } ?: return AttachResult.NotOpen
        if (bundle.hasFingerprint(evidence.fingerprint)) return AttachResult.AlreadyInBundle
        val evidenceId = ctx.drafts.ids.newId()
        ctx.record {
            id,
            device,
            now,
            ->
            BundleEvent.EvidenceAttached(id, bundleId, bundle.ownerId, device, now, now, evidenceId, evidence, role)
        }
        return AttachResult.Attached(evidenceId)
    }
}

/** Caso de Uso: quitar una evidencia ("no es de este pago"). Queda guardada, pero no se interpreta. */
class ExcludeEvidenceUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(
        bundleId: String,
        evidenceId: String,
    ): Boolean {
        val bundle = ctx.load(bundleId) ?: return false
        if (bundle.active.none { it.id == evidenceId }) return false
        ctx.record { id, device, now -> BundleEvent.EvidenceExcluded(id, bundleId, bundle.ownerId, device, now, now, evidenceId) }
        if (!bundle.open) InterpretBundleUseCase(ctx)(bundleId)
        return true
    }
}

/**
 * Caso de Uso: cerrar el paquete (Listo o salir de la página). Si ya no queda nada por leer, se
 * interpreta en el acto; si no, la cola lo interpreta al terminar de leer.
 */
class CloseBundleUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(bundleId: String): List<Draft> {
        val bundle = ctx.load(bundleId) ?: return emptyList()
        if (bundle.open) ctx.record { id, device, now -> BundleEvent.Closed(id, bundleId, bundle.ownerId, device, now, now) }
        return InterpretBundleUseCase(ctx)(bundleId)
    }
}

/**
 * Caso de Uso: reabrir un paquete para agregar o quitar. Solo si le queda algún borrador pendiente
 * (o todavía no produjo ninguno). Un borrador de antes de los paquetes se convierte en paquete aquí.
 */
class ReopenBundleUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(bundleId: String): Boolean {
        val bundle = ctx.load(bundleId) ?: return false
        val drafts = ctx.draftsOf(bundle)
        if (drafts.isNotEmpty() && drafts.all { it.isTerminal }) return false
        if (bundle.open) return true
        if (ctx.bundles.eventsFor(bundleId).isEmpty()) {
            // Borrador de antes de los paquetes: se materializa con sus evidencias, sin tocar el borrador.
            ctx.record { id, device, now -> BundleEvent.Opened(id, bundleId, bundle.ownerId, device, bundle.openedAtEpochMs, now) }
            bundle.evidences.forEach { item ->
                ctx.record { id, device, now ->
                    BundleEvent.EvidenceAttached(
                        id,
                        bundleId,
                        bundle.ownerId,
                        device,
                        bundle.openedAtEpochMs,
                        now,
                        item.id,
                        item.evidence,
                        item.role,
                    )
                }
                item.perceivedText?.let { text ->
                    ctx.record { id, device, now ->
                        BundleEvent.EvidencePerceived(
                            id,
                            bundleId,
                            bundle.ownerId,
                            device,
                            bundle.openedAtEpochMs,
                            now,
                            item.id,
                            text,
                            "legacy",
                        )
                    }
                }
            }
            return true
        }
        ctx.record { id, device, now -> BundleEvent.Reopened(id, bundleId, bundle.ownerId, device, now, now) }
        return true
    }
}

/** Caso de Uso: separar una evidencia en un paquete propio ("era otro pago"). Devuelve el paquete nuevo. */
class SplitEvidenceUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(
        bundleId: String,
        evidenceId: String,
    ): String? {
        val bundle = ctx.load(bundleId) ?: return null
        val item = bundle.active.firstOrNull { it.id == evidenceId } ?: return null
        val newBundleId = ctx.drafts.ids.newId()
        ctx.record { id, device, now -> BundleEvent.EvidenceSplit(id, bundleId, bundle.ownerId, device, now, now, evidenceId, newBundleId) }
        ctx.record { id, device, now -> BundleEvent.Opened(id, newBundleId, bundle.ownerId, device, now, now) }
        ctx.record { id, device, now ->
            BundleEvent.EvidenceAttached(
                id,
                newBundleId,
                bundle.ownerId,
                device,
                now,
                now,
                ctx.drafts.ids.newId(),
                item.evidence,
                item.role,
            )
        }
        val moved = ctx.load(newBundleId)!!.evidences.single()
        item.perceivedText?.let { text ->
            ctx.record {
                id,
                device,
                now,
                ->
                BundleEvent.EvidencePerceived(id, newBundleId, bundle.ownerId, device, now, now, moved.id, text, "split")
            }
        }
        CloseBundleUseCase(ctx)(newBundleId)
        if (!bundle.open) InterpretBundleUseCase(ctx)(bundleId)
        return newBundleId
    }
}

/**
 * Caso de Uso: guardar lo que la cola leyó de un archivo del paquete ([text] null: no se pudo leer).
 * Cuando el paquete cerrado ya no tiene nada por leer, se interpreta.
 */
class PerceiveBundleEvidenceUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(
        bundleId: String,
        evidenceId: String,
        text: String?,
        engine: String,
    ): List<Draft> {
        val bundle = ctx.load(bundleId) ?: return emptyList()
        if (bundle.evidences.none { it.id == evidenceId && it.needsReading }) return emptyList()
        val read = text?.trim()?.takeIf { it.isNotEmpty() }
        ctx.record {
            id,
            device,
            now,
            ->
            BundleEvent.EvidencePerceived(id, bundleId, bundle.ownerId, device, now, now, evidenceId, read, engine)
        }
        return InterpretBundleUseCase(ctx)(bundleId)
    }
}

/** Caso de Uso: los paquetes cerrados con archivos sin leer, para la cola. */
class PendingBundlesUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(ownerId: String): List<CaptureBundle> =
        ctx.bundles
            .bundleIdsFor(ownerId)
            .mapNotNull { ctx.load(it) }
            .filter { !it.open && it.active.any { item -> item.needsReading } }
            .sortedBy { it.openedAtEpochMs }
}

/**
 * Caso de Uso: interpretar el paquete en una sola pasada y reconciliar sus borradores.
 *
 * - Un borrador pendiente con la misma clave de pago recibe la interpretación nueva, salvo que la
 *   persona ya lo haya revisado a mano: lo corregido no se pisa.
 * - Un borrador confirmado no cambia y su pago no produce otro borrador.
 * - Un pago nuevo produce un borrador nuevo; un pendiente que ya no corresponde a ningún pago se
 *   descarta enlazado al paquete.
 *
 * Mientras el paquete esté abierto o tenga archivos por leer, no hace nada.
 */
class InterpretBundleUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(bundleId: String): List<Draft> {
        val bundle = ctx.load(bundleId) ?: return emptyList()
        if (!bundle.readyToInterpret) return ctx.draftsOf(bundle)
        val ownerId = bundle.ownerId
        val directory = ctx.drafts.directory(ownerId)
        val texts = bundle.active.mapNotNull { item -> item.text?.let { BundleText(item.id, item.role, item.evidence.source, it) } }
        val payments = runCatching { ctx.interpreter.interpret(texts, directory) }.getOrDefault(emptyList())

        val existing = ctx.draftsOf(bundle)
        val keyOf = existing.associate { it.id to (it.header.paymentKey ?: BundleContext.legacyEvidenceId(it.id)) }
        val claimed = mutableSetOf<String>()

        fun draftFor(key: String): Draft? = existing.firstOrNull { it.id !in claimed && keyOf[it.id] == key }

        val now = ctx.drafts.clock.nowEpochMs()
        val device = ctx.drafts.device.deviceId()
        for (payment in payments) {
            // Un borrador de antes de los paquetes toma el primer pago que no tenga dueño.
            val draft =
                draftFor(payment.paymentKey)
                    ?: existing.firstOrNull {
                        it.id !in claimed &&
                            it.header.paymentKey == null &&
                            payments.none { p -> p.paymentKey == keyOf[it.id] }
                    }
            if (draft == null) {
                val first = bundle.active.firstOrNull { it.id in payment.evidenceIds } ?: bundle.active.first()
                val draftId = ctx.drafts.ids.newId()
                ctx.drafts.drafts.appendEvent(
                    DraftEvent.Captured(
                        ctx.drafts.ids.newId(),
                        draftId,
                        ownerId,
                        device,
                        bundle.openedAtEpochMs,
                        now,
                        first.evidence,
                        bundleId,
                        payment.paymentKey,
                    ),
                )
                ctx.drafts.drafts.appendEvent(
                    DraftEvent.Interpreted(
                        ctx.drafts.ids.newId(),
                        draftId,
                        ownerId,
                        device,
                        bundle.openedAtEpochMs,
                        now,
                        payment.proposal,
                        payment.reviewReasons,
                    ),
                )
                claimed += draftId
                continue
            }
            claimed += draft.id
            if (draft.isTerminal || revisedByPerson(draft.id)) continue
            val current = (draft as? Draft.Ready)?.proposal ?: (draft as? Draft.Incomplete)?.proposal
            if (current == payment.proposal && draft.header.reviewReasons == payment.reviewReasons) continue
            ctx.drafts.drafts.appendEvent(
                DraftEvent.Interpreted(
                    ctx.drafts.ids.newId(),
                    draft.id,
                    ownerId,
                    device,
                    now,
                    now,
                    payment.proposal,
                    payment.reviewReasons,
                ),
            )
        }
        if (payments.isEmpty() && existing.isEmpty()) {
            // Nadie entendió nada: un borrador Capturado guarda el paquete para completarlo a mano.
            val first = bundle.active.firstOrNull() ?: return emptyList()
            ctx.drafts.drafts.appendEvent(
                DraftEvent.Captured(
                    ctx.drafts.ids.newId(),
                    ctx.drafts.ids.newId(),
                    ownerId,
                    device,
                    bundle.openedAtEpochMs,
                    now,
                    first.evidence,
                    bundleId,
                    first.id,
                ),
            )
        }
        if (payments.isNotEmpty()) {
            existing
                .filter { it.id !in claimed && !it.isTerminal }
                .forEach { orphan ->
                    ctx.drafts.drafts.appendEvent(
                        DraftEvent.Discarded(ctx.drafts.ids.newId(), orphan.id, ownerId, device, now, now, bundleId),
                    )
                }
        }
        return ctx.draftsOf(bundle)
    }

    private suspend fun revisedByPerson(draftId: String): Boolean =
        ctx.drafts.drafts
            .eventsFor(draftId)
            .any { it is DraftEvent.Revised }
}

/** Caso de Uso: el último borrador pendiente de la persona ("Agregar al último"). */
class LastPendingDraftUseCase(
    private val ctx: BundleContext,
) {
    suspend operator fun invoke(ownerId: String): Draft? =
        ctx.drafts.drafts
            .draftIdsFor(ownerId)
            .mapNotNull { ctx.drafts.load(it) }
            .filter { !it.isTerminal }
            .maxByOrNull { it.header.capturedAtEpochMs }
}
