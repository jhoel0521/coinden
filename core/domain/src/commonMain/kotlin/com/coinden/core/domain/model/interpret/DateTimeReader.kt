package com.coinden.core.domain.model.interpret

/** Instante leído; [ambiguous] si el orden día/mes se eligió por el locale. */
data class ReadDateTime(
    val epochMs: Long,
    val ambiguous: Boolean,
)

/**
 * Lee fecha y hora de comprobantes de cualquier país y las convierte a un instante con el
 * desplazamiento horario del dispositivo, sin librerías de zonas horarias.
 *
 * Fechas: "03 oct. 2026", "10/Oct/2026", "25/10/2026", "10-11-2026", "Oct 10, 2026".
 * Horas: "14:22:50", "12:54 p. m.", "2:22 PM". Sin hora no se propone nada.
 */
object DateTimeReader {
    private val NUMERIC_DATE = Regex("""(?<!\d)(\d{1,2})[/-](\d{1,2})[/-](\d{4})(?!\d)""")
    private val DAY_MONTH_NAME = Regex("""(?<!\d)(\d{1,2})[\s/.-]*([\p{L}]{3,10})\.?[\s/.-]*(\d{4})(?!\d)""")
    private val MONTH_NAME_DAY = Regex("""([\p{L}]{3,10})\.?\s+(\d{1,2}),?\s+(\d{4})(?!\d)""")
    private val TIME =
        Regex(
            """(?<!\d)(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(a\.?\s?m\.?|p\.?\s?m\.?)?(?![\p{L}\d])""",
            RegexOption.IGNORE_CASE,
        )

    fun read(
        text: String,
        months: Map<String, Int>,
        locale: UserLocale,
        utcOffsetMinutes: Int,
    ): ReadDateTime? {
        val date = readDate(text, months, locale) ?: return null
        val time = TIME.find(text) ?: return null

        var hour = time.groupValues[1].toInt()
        val minute = time.groupValues[2].toInt()
        val second = time.groupValues[3].ifEmpty { "0" }.toInt()
        val meridiem = time.groupValues[4].lowercase().filter { it.isLetter() }
        if (meridiem == "pm" && hour < 12) hour += 12
        if (meridiem == "am" && hour == 12) hour = 0
        if (hour > 23 || minute > 59 || second > 59) return null

        val localSeconds = daysFromCivil(date.year, date.month, date.day) * 86_400L + hour * 3_600L + minute * 60L + second
        val utcMs = (localSeconds - utcOffsetMinutes * 60L) * 1_000L
        return ReadDateTime(utcMs, date.ambiguous)
    }

    private data class CivilDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val ambiguous: Boolean,
    )

    private fun readDate(
        text: String,
        months: Map<String, Int>,
        locale: UserLocale,
    ): CivilDate? {
        DAY_MONTH_NAME.findAll(text).forEach { m ->
            val month = months[TextNormalizer.normalize(m.groupValues[2])] ?: return@forEach
            return valid(m.groupValues[3].toInt(), month, m.groupValues[1].toInt(), false)
        }
        MONTH_NAME_DAY.findAll(text).forEach { m ->
            val month = months[TextNormalizer.normalize(m.groupValues[1])] ?: return@forEach
            return valid(m.groupValues[3].toInt(), month, m.groupValues[2].toInt(), false)
        }
        val numeric = NUMERIC_DATE.find(text) ?: return null
        val a = numeric.groupValues[1].toInt()
        val b = numeric.groupValues[2].toInt()
        val year = numeric.groupValues[3].toInt()
        val monthFirst = locale.country.uppercase() == "US"
        return when {
            a > 12 -> valid(year, b, a, false)
            b > 12 -> valid(year, a, b, false)
            a == b -> valid(year, a, b, false)
            monthFirst -> valid(year, a, b, true)
            else -> valid(year, b, a, true)
        }
    }

    private fun valid(
        year: Int,
        month: Int,
        day: Int,
        ambiguous: Boolean,
    ): CivilDate? = if (month in 1..12 && day in 1..31) CivilDate(year, month, day, ambiguous) else null

    /** Días desde 1970-01-01 del calendario gregoriano (algoritmo civil de Howard Hinnant). */
    internal fun daysFromCivil(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097L + doe - 719_468L
    }
}
