package com.coinden.app.format

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.interpret.UserLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormatterTest {
    // 12 de octubre de 2026, 16:40 UTC
    private val lunch = 1_791_763_200_000L + 16 * 3_600_000L + 40 * 60_000L

    @Test
    fun `dia y mes en el orden del pais con la hora local`() {
        assertEquals("12/10 12:40", TimeFormatter.dayMonthTime(lunch, -240, UserLocale("es", "BO", Currency.BOB, ',')))
        assertEquals("10/12 12:40", TimeFormatter.dayMonthTime(lunch, -240, UserLocale("en", "US", Currency.USD, '.')))
    }

    @Test
    fun `el desfase puede cambiar el dia`() {
        assertEquals("13/10 00:40", TimeFormatter.dayMonthTime(lunch, 8 * 60, UserLocale("zh", "CN", Currency.CNY, '.')))
    }
}
