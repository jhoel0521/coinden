package com.coinden.app.insights

/** Calendario de presentación: usa el desfase actual del teléfono, también al consultar el pasado. Sin dependencias de JVM. */
data class SpendingMonth(
    val year: Int,
    val month: Int,
) : Comparable<SpendingMonth> {
    init {
        require(month in 1..12) { "Mes fuera de rango: $month" }
    }

    fun shift(months: Long): SpendingMonth {
        val total = year * 12L + (month - 1L) + months
        return SpendingMonth(total.floorDiv(12L).toInt(), (total.mod(12L) + 1L).toInt())
    }

    /** Intervalo semiabierto [inicio, mes siguiente). */
    fun bounds(utcOffsetMinutes: Int): Pair<Long, Long> {
        val next = shift(1)
        val offsetMs = utcOffsetMinutes * 60_000L
        return (daysFromCivil(year, month, 1) * DAY_MS - offsetMs) to
            (daysFromCivil(next.year, next.month, 1) * DAY_MS - offsetMs)
    }

    override fun compareTo(other: SpendingMonth): Int = compareValuesBy(this, other, SpendingMonth::year, SpendingMonth::month)

    companion object {
        private const val DAY_MS = 86_400_000L

        fun at(
            epochMs: Long,
            utcOffsetMinutes: Int,
        ): SpendingMonth {
            val days = (epochMs + utcOffsetMinutes * 60_000L).floorDiv(DAY_MS)
            val z = days + 719_468L
            val era = z.floorDiv(146_097L)
            val doe = z - era * 146_097L
            val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
            val y = yoe + era * 400L
            val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
            val mp = (5L * doy + 2L) / 153L
            val month = (if (mp < 10L) mp + 3L else mp - 9L).toInt()
            val year = (y + if (month <= 2) 1L else 0L).toInt()
            return SpendingMonth(year, month)
        }

        /** Días desde 1970-01-01 del calendario gregoriano (algoritmo civil de Howard Hinnant). */
        private fun daysFromCivil(
            year: Int,
            month: Int,
            day: Int,
        ): Long {
            val y = if (month <= 2) year - 1L else year.toLong()
            val era = y.floorDiv(400L)
            val yoe = y - era * 400L
            val mp = (month + 9) % 12
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
            return era * 146_097L + doe - 719_468L
        }
    }
}
