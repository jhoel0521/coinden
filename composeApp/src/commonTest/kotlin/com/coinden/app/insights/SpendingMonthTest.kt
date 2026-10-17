package com.coinden.app.insights

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SpendingMonthTest {
    @Test
    fun `Carlos y Sophie usan meses civiles con el desfase local`() {
        val instant = Instant.parse("2026-11-01T01:00:00Z").toEpochMilli()
        assertEquals(SpendingMonth(2026, 10), SpendingMonth.at(instant, -240))
        assertEquals(SpendingMonth(2026, 11), SpendingMonth.at(instant, 120))
        val october = SpendingMonth(2026, 10).bounds(-240)
        assertEquals(Instant.parse("2026-10-01T04:00:00Z").toEpochMilli(), october.first)
        assertEquals(Instant.parse("2026-11-01T04:00:00Z").toEpochMilli(), october.second)
        assertEquals(SpendingMonth(2025, 12), SpendingMonth(2026, 1).shift(-1))
        assertEquals(SpendingMonth(2026, 1), SpendingMonth(2025, 12).shift(1))
        val leap = SpendingMonth(2024, 2).bounds(60)
        assertEquals(29 * 86_400_000L, leap.second - leap.first)
    }
}
