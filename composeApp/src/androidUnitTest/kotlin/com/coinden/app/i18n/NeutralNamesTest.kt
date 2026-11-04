package com.coinden.app.i18n

import com.coinden.app.capture.label
import com.coinden.app.onboarding.label
import com.coinden.core.domain.model.draft.MovementType
import com.coinden.core.domain.model.wallet.WalletType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Los tipos de billetera y de movimiento se ven con nombres de registro, no de producto financiero. */
class NeutralNamesTest {
    private fun resources(folder: String): Map<String, String> =
        Regex("""<string name="([^"]+)">([^<]*)</string>""")
            .findAll(File("src/commonMain/composeResources/$folder/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private val retired = listOf("banco", "bank", "cripto", "crypto", "crédito", "credit", "transfer")

    @Test
    fun `ningun tipo de billetera ni de movimiento nombra un producto financiero en los tres idiomas`() {
        for (folder in listOf("values", "values-en", "values-pt")) {
            val strings = resources(folder)
            val visible =
                WalletType.entries.map { it.label().key } + MovementType.entries.map { it.label().key }
            for (key in visible) {
                val text = strings.getValue(key)
                assertFalse(retired.any { text.lowercase().contains(it) }, "$folder/$key: $text")
            }
        }
    }

    @Test
    fun `agregar otra moneda no menciona cripto en los tres idiomas`() {
        for (folder in listOf("values", "values-en", "values-pt")) {
            val text = resources(folder).getValue("currencies_add_custom")
            assertFalse(listOf("cripto", "crypto").any { text.lowercase().contains(it) }, "$folder: $text")
        }
    }

    @Test
    fun `Carlos, John y Ana ven los nombres neutros de la tabla`() {
        val expected =
            mapOf(
                "values" to listOf("Efectivo", "Cuenta", "Digital", "Personalizada", "Tarjeta", "Traspaso"),
                "values-en" to listOf("Cash", "Account", "Digital", "Custom", "Card", "Move"),
                "values-pt" to listOf("Dinheiro", "Conta", "Digital", "Personalizada", "Cartão", "Repasse"),
            )
        for ((folder, names) in expected) {
            val strings = resources(folder)
            assertEquals(names.dropLast(1), WalletType.entries.map { strings.getValue(it.label().key) }, folder)
            assertEquals(names.last(), strings.getValue(MovementType.TRANSFER.label().key), folder)
        }
    }

    @Test
    fun `el medio de pago Transferencia o QR no cambia`() {
        assertEquals("Transferencia o QR", resources("values").getValue("method_transfer"))
    }
}
