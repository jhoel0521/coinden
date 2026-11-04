package com.coinden.app.i18n

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** CoinDen es una libreta de gastos: ningún texto visible usa palabras de servicio financiero. */
class VisibleVocabularyTest {
    private fun resources(folder: String): Map<String, String> =
        Regex("""<string name="([^"]+)">([^<]*)</string>""")
            .findAll(File("src/commonMain/composeResources/$folder/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private val retired =
        Regex(
            listOf(
                """billeteras?""",
                """patrimonio""",
                """saldos?""",
                """tasas?""",
                """cotizaci(?:ón|ones)""",
                """pagos?""",
                """guardas tu dinero""",
                """wallets?""",
                """net worth""",
                """balances?""",
                """rates?""",
                """quotes?""",
                """payments?""",
                """carteiras?""",
                """patrimônio""",
                """taxas?""",
                """cota(?:ção|ções)""",
                """pagamentos?""",
                """bnb""",
                """yape""",
                """meru""",
                """nubank""",
                """chase""",
                """paypal""",
                """pix""",
            ).joinToString("|") { """\b(?:$it)\b""" },
            RegexOption.IGNORE_CASE,
        )

    /** Claves que pueden nombrar algo retirado y por qué; hoy ninguna. */
    private val allowed = emptySet<String>()

    @Test
    fun `ningun texto visible usa palabras retiradas en los tres idiomas`() {
        val found =
            listOf("values", "values-en", "values-pt").flatMap { folder ->
                resources(folder)
                    .filterKeys { it !in allowed }
                    .filterValues { retired.containsMatchIn(it) }
                    .map { "$folder/${it.key}: ${it.value}" }
            }
        assertTrue(found.isEmpty(), "Palabras retiradas en ${found.size} textos:\n" + found.joinToString("\n"))
    }

    @Test
    fun `el medio Transferencia o QR se mantiene`() {
        assertTrue(resources("values").getValue("method_transfer").contains("Transferencia"))
    }
}
