package com.coinden.core.data.local

import com.coinden.core.data.local.json.EventCodec
import com.coinden.core.domain.model.bundle.BundleEvent
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Los eventos del paquete de captura, guardados y leídos sin perder nada.
 *
 * Van en la misma tabla `events` con tipos nuevos: el esquema no cambia, así que una base de la
 * 0.1.0 se sigue leyendo igual y actualizar nunca borra datos.
 */
class BundleCodecTest {
    private val base =
        BundleEvent.Opened(
            id = "ev1",
            bundleId = "b1",
            ownerId = "carlos",
            deviceId = "device-carlos",
            occurredAtEpochMs = 1_700_000_000_000,
            recordedAtEpochMs = 1_700_000_000_100,
        )

    private fun ida_y_vuelta(event: BundleEvent): BundleEvent? {
        val guardado = EventCodec.encodeBundle(event)
        return EventCodec.decodeBundle(guardado.type, guardado.version, guardado.payload, event.bundleId)
    }

    @Test
    fun `abrir un paquete se guarda y se lee igual`() {
        assertEquals(base, ida_y_vuelta(base))
    }

    @Test
    fun `una evidencia adjunta conserva su archivo, su huella y su papel`() {
        val event =
            BundleEvent.EvidenceAttached(
                base.id,
                base.bundleId,
                base.ownerId,
                base.deviceId,
                base.occurredAtEpochMs,
                base.recordedAtEpochMs,
                evidenceId = "e1",
                evidence = Evidence(EvidenceSource.PHOTO, text = null, attachmentRef = "att-1", fingerprint = "huella-1"),
                role = EvidenceRole.RECEIPT,
            )

        assertEquals(event, ida_y_vuelta(event))
    }

    @Test
    fun `quitar una evidencia se guarda y se lee igual`() {
        val event =
            BundleEvent.EvidenceExcluded(
                base.id,
                base.bundleId,
                base.ownerId,
                base.deviceId,
                base.occurredAtEpochMs,
                base.recordedAtEpochMs,
                evidenceId = "e1",
            )

        assertEquals(event, ida_y_vuelta(event))
    }

    @Test
    fun `lo leido de una evidencia se guarda, incluso cuando no se pudo leer`() {
        val leido =
            BundleEvent.EvidencePerceived(
                base.id,
                base.bundleId,
                base.ownerId,
                base.deviceId,
                base.occurredAtEpochMs,
                base.recordedAtEpochMs,
                evidenceId = "e1",
                text = "Importe Bs25.00",
                engine = "tesseract",
            )
        val ilegible = leido.copy(text = null)

        assertEquals(leido, ida_y_vuelta(leido))
        assertEquals(ilegible, ida_y_vuelta(ilegible), "una foto que no se pudo leer también se registra")
    }

    @Test
    fun `cerrar y reabrir se guardan y se leen igual`() {
        val cerrado =
            BundleEvent.Closed(
                base.id,
                base.bundleId,
                base.ownerId,
                base.deviceId,
                base.occurredAtEpochMs,
                base.recordedAtEpochMs,
            )
        val reabierto =
            BundleEvent.Reopened(
                base.id,
                base.bundleId,
                base.ownerId,
                base.deviceId,
                base.occurredAtEpochMs,
                base.recordedAtEpochMs,
            )

        assertEquals(cerrado, ida_y_vuelta(cerrado))
        assertEquals(reabierto, ida_y_vuelta(reabierto))
    }

    @Test
    fun `separar una evidencia conserva a que paquete se fue`() {
        val event =
            BundleEvent.EvidenceSplit(
                base.id,
                base.bundleId,
                base.ownerId,
                base.deviceId,
                base.occurredAtEpochMs,
                base.recordedAtEpochMs,
                evidenceId = "e1",
                newBundleId = "b2",
            )

        assertEquals(event, ida_y_vuelta(event))
    }

    @Test
    fun `un tipo que esta app no conoce se omite al leer, pero sigue en disco`() {
        assertNull(EventCodec.decodeBundle("bundle.inventado", 1L, "{}", "b1"))
    }

    @Test
    fun `una version mas nueva que la que entiende esta app se omite al leer`() {
        val guardado = EventCodec.encodeBundle(base)

        assertNull(EventCodec.decodeBundle(guardado.type, guardado.version + 1, guardado.payload, "b1"))
    }

    @Test
    fun `los tipos del paquete no pisan los que ya existian`() {
        val delPaquete = EventCodec.encodeBundle(base).type

        assertEquals(true, delPaquete.startsWith("bundle."), "los tipos nuevos van en su propio espacio")
    }
}
