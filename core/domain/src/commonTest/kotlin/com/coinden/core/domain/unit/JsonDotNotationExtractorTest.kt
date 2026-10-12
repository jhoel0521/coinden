package com.coinden.core.domain.unit

import com.coinden.core.domain.extractor.JsonDotNotationExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonDotNotationExtractorTest {
    private val extractor = JsonDotNotationExtractor()

    @Test
    fun `extrae propiedad directa simple de JSON plano`() {
        val json = """{"compra": 1390.00, "venta": 1420.50, "fecha": "2026-10-07"}"""
        val rate = extractor.extract(json, "venta")

        assertEquals("1420.50", rate)
    }

    @Test
    fun `extrae propiedad anidada con un nivel de punto`() {
        val json =
            """
            {
                "base": "USD",
                "rates": {
                    "EUR": 0.92,
                    "GBP": 0.78
                }
            }
            """.trimIndent()

        val rateEur = extractor.extract(json, "rates.EUR")
        assertEquals("0.92", rateEur)

        val rateGbp = extractor.extract(json, "rates.GBP")
        assertEquals("0.78", rateGbp)
    }

    @Test
    fun `extrae propiedad profundamente anidada en multiples niveles`() {
        val json =
            """
            {
                "data": {
                    "source": "DolarBlueBolivia",
                    "blue": {
                        "buy": 11.83,
                        "sell": 11.88
                    }
                }
            }
            """.trimIndent()

        val sellRate = extractor.extract(json, "data.blue.sell")
        assertEquals("11.88", sellRate)

        val buyRate = extractor.extract(json, "data.blue.buy")
        assertEquals("11.83", buyRate)
    }

    @Test
    fun `soporta numeros formateados entre comillas dobles y con coma decimal`() {
        val json = """{"precio": "62200,50"}"""
        val rate = extractor.extract(json, "precio")

        assertEquals("62200.50", rate)
    }

    @Test
    fun `devuelve null si la propiedad no existe o el json es invalido`() {
        val json = """{"rates": {"EUR": 0.92}}"""
        assertNull(extractor.extract(json, "rates.XYZ"))
        assertNull(extractor.extract(json, "inexistente"))
        assertNull(extractor.extract("", "venta"))
    }
}
