package com.coinden.core.data.packs

import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.interpret.TemplateKind
import com.coinden.core.domain.model.wallet.PaymentMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El formato de los archivos de datos: un paquete de idioma y una plantilla de comprobante.
 * Un archivo que no se entiende se descarta sin tumbar a los demás ni a la captura.
 */
class PackParserTest {
    private val paqueteValido =
        """
        {
          "formatVersion": 1,
          "code": "es",
          "amountLabels": ["importe", "monto"],
          "expenseWords": ["pague", "gaste"],
          "notePrepositions": ["para", "de"],
          "months": { "ene": 1, "enero": 1 },
          "currencyWords": { "dolares": "USD", "pesos": null },
          "categoryWords": { "almuerzo": "FOOD" },
          "paymentMethodWords": { "efectivo": "CASH", "qr": "TRANSFER" }
        }
        """.trimIndent()

    @Test
    fun `un paquete valido se lee entero`() {
        val pack = assertNotNull(PackParser.languagePack(paqueteValido))

        assertEquals("es", pack.code)
        assertEquals(setOf("importe", "monto"), pack.amountLabels)
        assertEquals(setOf("pague", "gaste"), pack.expenseWords)
        assertEquals(mapOf("ene" to 1, "enero" to 1), pack.months)
        assertEquals(BaseCategory.FOOD, pack.categoryWords["almuerzo"])
        assertEquals(PaymentMethod.CASH, pack.paymentMethodWords["efectivo"])
    }

    @Test
    fun `una moneda nula significa la del locale`() {
        val pack = assertNotNull(PackParser.languagePack(paqueteValido))

        assertEquals("USD", pack.currencyWords["dolares"])
        assertTrue(pack.currencyWords.containsKey("pesos"))
        assertNull(pack.currencyWords["pesos"])
    }

    @Test
    fun `las listas que el archivo no trae quedan vacias`() {
        val minimo = """{ "formatVersion": 1, "code": "zz" }"""

        val pack = assertNotNull(PackParser.languagePack(minimo))

        assertEquals("zz", pack.code)
        assertEquals(emptySet(), pack.expenseWords)
        assertEquals(emptyMap(), pack.categoryWords)
    }

    @Test
    fun `un paquete sin formatVersion se descarta`() {
        assertNull(PackParser.languagePack("""{ "code": "es", "expenseWords": ["pague"] }"""))
    }

    @Test
    fun `un paquete con formatVersion mayor se ignora sin fallar`() {
        val futuro = """{ "formatVersion": 99, "code": "es", "expenseWords": ["pague"] }"""

        assertNull(PackParser.languagePack(futuro))
    }

    @Test
    fun `un paquete danado se descarta`() {
        assertNull(PackParser.languagePack("{ esto no es json"))
        assertNull(PackParser.languagePack(""))
    }

    @Test
    fun `una categoria o un medio de pago desconocidos no tumban el paquete`() {
        val raro =
            """
            {
              "formatVersion": 1,
              "code": "es",
              "categoryWords": { "almuerzo": "FOOD", "cohete": "ESPACIO" },
              "paymentMethodWords": { "efectivo": "CASH", "trueque": "INVENTADO" }
            }
            """.trimIndent()

        val pack = assertNotNull(PackParser.languagePack(raro))

        assertEquals(mapOf("almuerzo" to BaseCategory.FOOD), pack.categoryWords)
        assertEquals(mapOf("efectivo" to PaymentMethod.CASH), pack.paymentMethodWords)
    }

    @Test
    fun `una plantilla valida se lee entera`() {
        val plantilla =
            """
            {
              "formatVersion": 1,
              "id": "bo-yape-payment",
              "markers": ["yapeaste"],
              "kind": "EXPENSE",
              "issuerWalletHint": "yape",
              "counterpartyAfterAmount": true,
              "referenceLabel": "nro de transaccion"
            }
            """.trimIndent()

        val leida = assertNotNull(PackParser.receiptTemplate(plantilla))

        assertEquals("bo-yape-payment", leida.id)
        assertEquals(listOf("yapeaste"), leida.markers)
        assertEquals(TemplateKind.EXPENSE, leida.kind)
        assertEquals("yape", leida.issuerWalletHint)
        assertTrue(leida.counterpartyAfterAmount)
        assertEquals("nro de transaccion", leida.referenceLabel)
    }

    @Test
    fun `una plantilla sin marcas se descarta`() {
        val sinMarcas = """{ "formatVersion": 1, "id": "x", "markers": [], "kind": "EXPENSE" }"""

        assertNull(PackParser.receiptTemplate(sinMarcas))
    }

    @Test
    fun `una plantilla con un tipo desconocido se descarta`() {
        val raro = """{ "formatVersion": 1, "id": "x", "markers": ["m"], "kind": "REGALO" }"""

        assertNull(PackParser.receiptTemplate(raro))
    }

    @Test
    fun `un archivo danado no impide cargar los demas`() {
        val archivos = listOf(paqueteValido, "{ roto", """{ "formatVersion": 1, "code": "en" }""")

        val cargados = archivos.mapNotNull { PackParser.languagePack(it) }

        assertEquals(listOf("es", "en"), cargados.map { it.code })
    }
}
