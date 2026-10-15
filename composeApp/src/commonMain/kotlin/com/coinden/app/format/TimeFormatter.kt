package com.coinden.app.format

/** Hora local "HH:mm" de un instante, con el desfase del teléfono. */
object TimeFormatter {
    private const val DAY_MS = 86_400_000L

    fun hourMinute(
        epochMs: Long,
        utcOffsetMinutes: Int,
    ): String {
        val minutes = Math.floorMod(epochMs + utcOffsetMinutes * 60_000L, DAY_MS) / 60_000L
        return "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
    }
}
