package com.coinden.core.domain.unit

import com.coinden.core.domain.extractor.HtmlRegexExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HtmlRegexExtractorTest {
    private val extractor = HtmlRegexExtractor()

    // Payload HTML real inspirado en el sitio dolarbluebolivia.click provisto en el requerimiento
    private val realDolarBlueBoliviaHtml =
        """
        <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
        <script>
        (function(){
            const API_BASE = "https://api.dolarbluebolivia.click";
            const RATES_SSR = {"/v1/officialRate":{"data":{"source":"DolarBlueBolivia","pair":"BOB/USDT","blue":{"buy":11.83,"sell":11.88},"official":{"buy":11.97,"sell":11.97}}}};
        })();
        </script>
        </head>
        <body>
            <div class="card">
                <h2>Dólar Blue Bolivia</h2>
                <div id="usdRate">Bs 11.88</div>
                <span class="subtext">Compra: 11.83 | Venta: 11.88</span>
            </div>
        </body>
        </html>
        """.trimIndent()

    @Test
    fun `extrae tasa exacta de 11_88 usando regex sobre el selector visual del DOM de dolarbluebolivia`() {
        val selector = """id="usdRate">[^<]*?(\d+(?:\.\d+)?)"""
        val rate = extractor.extract(realDolarBlueBoliviaHtml, selector)

        assertEquals(11.88, rate)
    }

    @Test
    fun `extrae tasa exacta de 11_88 buscando dentro del script SSR incrustado`() {
        val selector = """"blue":\{"buy":\d+(?:\.\d+)?,"sell":(\d+(?:\.\d+)?)\}"""
        val rate = extractor.extract(realDolarBlueBoliviaHtml, selector)

        assertEquals(11.88, rate)
    }

    @Test
    fun `soporta separador de coma decimal en sitios web en espanol`() {
        val html = """<span class="cotizacion">Cotización Venta: 11,88 Bs</span>"""
        val selector = """Cotización Venta:\s*([0-9]+,[0-9]+)"""
        val rate = extractor.extract(html, selector)

        assertEquals(11.88, rate)
    }

    @Test
    fun `extrae correctamente cuando no se especifica grupo de captura pero el fragmento contiene un numero`() {
        val html = """<div class="dolar-hoy">11.88</div>"""
        val selector = """class="dolar-hoy">"""
        val rate = extractor.extract(html, selector)

        assertEquals(11.88, rate)
    }

    @Test
    fun `devuelve null de forma segura ante un redisenio de la pagina web sin arrojar excepciones`() {
        // Simular que el sitio web cambió la clase 'usdRate' por 'modernRate_v2'
        val selector = """id="usdRate">[^<]*?(\d+(?:\.\d+)?)"""
        val redesignedHtml = """<div><h1>Dólar Blue</h1><span id="modernRate_v2">12.00</span></div>"""

        val rate = extractor.extract(redesignedHtml, selector)
        assertNull(rate)
    }
}
