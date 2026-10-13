package com.coinden.app

import com.coinden.core.data.memory.FixedDeviceIdentity
import com.coinden.core.data.memory.StaticRegionalSettings
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.port.output.ClockPort

/** Reloj de prueba que avanza un segundo en cada lectura (los eventos nunca empatan). */
class SteppingClock(
    private var now: Long = 1_760_000_000_000,
) : ClockPort {
    override fun nowEpochMs(): Long = now.also { now += 1_000 }
}

/** Contenedor como el de un teléfono de Carlos en Bolivia (es-BO, UTC-4), en memoria. */
fun testContainer(
    storage: AppStorage = AppStorage.inMemory(),
    ownerId: String = "carlos-${storage.hashCode()}",
): AppContainer =
    AppContainer(
        storage = storage,
        ownerId = ownerId,
        regional = StaticRegionalSettings(RegionalSettings(UserLocale("es", "BO"), utcOffsetMinutes = -240)),
        device = FixedDeviceIdentity("device-carlos"),
        clock = SteppingClock(),
    )
