package com.coinden.app.insights

import kotlin.test.Test
import kotlin.test.assertEquals

class SpendingMonthTest {
    @Test
    fun `Carlos y Sophie usan meses civiles con el desfase local`() {
        // 2026-11-01T01:00:00Z
        val instant = SpendingMonth(2026, 11).bounds(0).first + 3_600_000L
        assertEquals(SpendingMonth(2026, 10), SpendingMonth.at(instant, -240))
        assertEquals(SpendingMonth(2026, 11), SpendingMonth.at(instant, 120))
        val october = SpendingMonth(2026, 10).bounds(-240)
        assertEquals(1_790_827_200_000L, october.first)
        assertEquals(1_793_505_600_000L, october.second)
        assertEquals(SpendingMonth(2025, 12), SpendingMonth(2026, 1).shift(-1))
        assertEquals(SpendingMonth(2026, 1), SpendingMonth(2025, 12).shift(1))
        val leap = SpendingMonth(2024, 2).bounds(60)
        assertEquals(29 * 86_400_000L, leap.second - leap.first)
    }
}
