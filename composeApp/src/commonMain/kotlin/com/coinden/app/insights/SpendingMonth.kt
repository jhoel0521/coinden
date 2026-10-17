package com.coinden.app.insights

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

/** Calendario de presentación: usa el desfase actual del teléfono, también al consultar el pasado. */
data class SpendingMonth(
    val year: Int,
    val month: Int,
) : Comparable<SpendingMonth> {
    init {
        YearMonth.of(year, month)
    }

    fun shift(months: Long): SpendingMonth = YearMonth.of(year, month).plusMonths(months).let { SpendingMonth(it.year, it.monthValue) }

    /** Intervalo semiabierto [inicio, mes siguiente). */
    fun bounds(utcOffsetMinutes: Int): Pair<Long, Long> {
        val offset = ZoneOffset.ofTotalSeconds(utcOffsetMinutes * 60)
        val start = YearMonth.of(year, month)
        return start
            .atDay(1)
            .atStartOfDay()
            .toInstant(offset)
            .toEpochMilli() to
            start
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay()
                .toInstant(offset)
                .toEpochMilli()
    }

    override fun compareTo(other: SpendingMonth): Int = compareValuesBy(this, other, SpendingMonth::year, SpendingMonth::month)

    companion object {
        fun at(
            epochMs: Long,
            utcOffsetMinutes: Int,
        ): SpendingMonth {
            val date = Instant.ofEpochMilli(epochMs).atOffset(ZoneOffset.ofTotalSeconds(utcOffsetMinutes * 60))
            return SpendingMonth(date.year, date.monthValue)
        }
    }
}
