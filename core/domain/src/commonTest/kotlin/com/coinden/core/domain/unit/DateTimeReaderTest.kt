package com.coinden.core.domain.unit

import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.ReadDateTime
import com.coinden.core.domain.model.interpret.UserLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateTimeReaderTest {
    private val months =
        mapOf(
            "oct" to 10, "octubre" to 10, "out" to 10, "outubro" to 10, "october" to 10,
            "ene" to 1, "jan" to 1, "dic" to 12, "dez" to 12, "dec" to 12,
        )
    private val bo = UserLocale("es", "BO")
    private val us = UserLocale("en", "US")
    private val utcMinus4 = -240

    /** Epoch ms UTC de una fecha y hora UTC (referencia independiente del lector). */
    private fun utc(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): Long {
        // 2026-10-01T00:00:00Z = 1790812800000 ms; los casos usan octubre de 2026.
        require(year == 2026 && month == 10)
        return 1_790_812_800_000L + ((day - 1) * 86_400L + hour * 3_600L + minute * 60L + second) * 1_000L
    }

    private fun read(
        text: String,
        locale: UserLocale = bo,
    ) = DateTimeReader.read(text, months, locale, utcMinus4)

    @Test
    fun `mes abreviado con punto y p_m (Yape)`() {
        val read = read("03 oct. 2026 | 12:54 p. m.")
        assertEquals(ReadDateTime(utc(2026, 10, 3, 16, 54), ambiguous = false), read)
    }

    @Test
    fun `mes abreviado entre barras con segundos (Banco Economico)`() {
        assertEquals(ReadDateTime(utc(2026, 10, 10, 18, 22, 50), false), read("Fecha y hora: 10/Oct/2026 14:22:50"))
        assertEquals(ReadDateTime(utc(2026, 10, 9, 22, 49, 22), false), read("Fecha y Hora: 09/Oct/2026 18:49:22"))
    }

    @Test
    fun `fecha numerica sin ambiguedad`() {
        assertEquals(ReadDateTime(utc(2026, 10, 25, 13, 15), false), read("25/10/2026 09:15"))
    }

    @Test
    fun `fecha numerica ambigua sigue el locale`() {
        // en-US: mes primero -> 11 de octubre; es-BO: dia primero -> 10 de noviembre
        val american = read("10/11/2026 2:22 PM", us)!!
        assertEquals(utc(2026, 10, 11, 18, 22), american.epochMs)
        assertEquals(true, american.ambiguous)
    }

    @Test
    fun `formato estadounidense con mes en texto`() {
        assertEquals(ReadDateTime(utc(2026, 10, 10, 18, 22), false), read("Oct 10, 2026 2:22 PM", us))
    }

    @Test
    fun `medianoche y mediodia con am pm`() {
        assertEquals(utc(2026, 10, 3, 4, 5), read("03 oct 2026 12:05 a. m.")!!.epochMs)
        assertEquals(utc(2026, 10, 3, 16, 5), read("03 oct 2026 12:05 PM")!!.epochMs)
    }

    @Test
    fun `sin hora no propone`() {
        assertNull(read("03 oct. 2026"))
        assertNull(read("almuerzo 45"))
    }
}
