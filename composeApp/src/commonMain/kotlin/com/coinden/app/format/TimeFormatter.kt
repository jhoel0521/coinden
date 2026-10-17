package com.coinden.app.format

import com.coinden.core.domain.model.interpret.UserLocale

/** Fechas y horas locales de un instante, con el desfase del teléfono. */
object TimeFormatter {
    private const val DAY_MS = 86_400_000L

    /** Países que escriben el mes antes que el día ("10/12"). */
    private val MONTH_FIRST = setOf("US", "PH", "BZ", "FM", "MH", "PW")

    /** "HH:mm". */
    fun hourMinute(
        epochMs: Long,
        utcOffsetMinutes: Int,
    ): String {
        val minutes = (epochMs + utcOffsetMinutes * 60_000L).mod(DAY_MS) / 60_000L
        return "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
    }

    /** Día y mes en el orden del país, y la hora: "12/10 12:40" (en EE. UU., "10/12 12:40"). */
    fun dayMonthTime(
        epochMs: Long,
        utcOffsetMinutes: Int,
        locale: UserLocale,
    ): String {
        val days = (epochMs + utcOffsetMinutes * 60_000L).floorDiv(DAY_MS)
        val (month, day) = monthDayOf(days)
        val dd = day.toString().padStart(2, '0')
        val mm = month.toString().padStart(2, '0')
        val date = if (locale.country.uppercase() in MONTH_FIRST) "$mm/$dd" else "$dd/$mm"
        return "$date ${hourMinute(epochMs, utcOffsetMinutes)}"
    }

    /** Mes y día de un día civil contado desde 1970-01-01 (algoritmo de Howard Hinnant). */
    private fun monthDayOf(daysSinceEpoch: Long): Pair<Int, Int> {
        val z = daysSinceEpoch + 719_468
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        return month to day
    }
}
