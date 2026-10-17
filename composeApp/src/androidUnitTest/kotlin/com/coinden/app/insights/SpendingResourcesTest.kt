package com.coinden.app.insights

import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.spending_month_1
import coinden.composeapp.generated.resources.spending_month_10
import coinden.composeapp.generated.resources.spending_month_12
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpendingResourcesTest {
    private fun resources(folder: String): Map<String, String> =
        Regex("""<string name="([^"]+)">([^<]*)</string>""")
            .findAll(File("src/commonMain/composeResources/$folder/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `John en ingles y Carlos en portugues tienen meses avisos y controles traducidos`() {
        val es = resources("values")
        val en = resources("values-en")
        val pt = resources("values-pt")
        assertEquals("Gastos del mes", es["spending_title"])
        assertEquals("October", en["spending_month_10"])
        assertEquals("Outubro", pt["spending_month_10"])
        assertEquals(Res.string.spending_month_1, SpendingMonth(2026, 1).monthResource())
        assertEquals(Res.string.spending_month_10, SpendingMonth(2026, 10).monthResource())
        assertEquals(Res.string.spending_month_12, SpendingMonth(2026, 12).monthResource())
        for (strings in listOf(es, en, pt)) {
            assertTrue((1..12).all { !strings["spending_month_$it"].isNullOrBlank() })
            assertTrue(
                listOf(
                    "home_total",
                    "home_partial",
                    "home_unconverted",
                    "spending_partial",
                    "spending_estimate",
                    "spending_missing_rate",
                    "spending_previous",
                    "spending_next",
                    "spending_retry",
                    "spending_error",
                    "spending_no_conversion",
                ).all {
                    !strings[it].isNullOrBlank()
                },
            )
        }
    }
}
