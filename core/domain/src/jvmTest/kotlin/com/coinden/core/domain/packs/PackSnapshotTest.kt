package com.coinden.core.domain.packs

import com.coinden.core.data.packs.BundledPacks
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.support.TestPacks
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Red de seguridad de la mudanza de las palabras a archivos de datos.
 *
 * Congela, palabra por palabra, lo que los paquetes y las plantillas tienen hoy. Mientras las
 * palabras pasan de Kotlin a JSON, esta prueba es la que avisa si se perdió o se cambió alguna:
 * la huella se calcula de lo que haya cargado, venga de donde venga.
 *
 * Si falla y el cambio es a propósito (agregar una palabra), se borra el archivo de referencia,
 * se vuelve a correr y se revisa el diff en git antes de darlo por bueno.
 */
class PackSnapshotTest {
    private val snapshot = File("src/jvmTest/resources/packs-snapshot.txt")

    @Test
    fun `los paquetes y las plantillas conservan exactamente sus palabras`() {
        val actual = canonical(BundledPacks.languagePacks(), BundledPacks.receiptTemplates())

        if (!snapshot.exists()) {
            snapshot.parentFile.mkdirs()
            snapshot.writeText(actual)
            fail(
                "No había referencia: se acaba de escribir ${snapshot.path} con el contenido de hoy. " +
                    "Revisa el archivo, confirma que es lo que esperas y vuelve a correr la prueba.",
            )
        }

        assertEquals(snapshot.readText().normalizeLineEndings(), actual.normalizeLineEndings())
    }

    @Test
    fun `la huella no depende del orden en que se escribieron las palabras`() {
        val uno = canonical(listOf(TestPacks.english, TestPacks.spanish), emptyList())
        val otro = canonical(listOf(TestPacks.spanish, TestPacks.english), emptyList())

        assertEquals(uno, otro, "la huella debe ordenar por código de idioma, no por el orden de la lista")
    }

    private fun String.normalizeLineEndings() = replace("\r\n", "\n").trim()

    /** Texto estable y ordenado: el mismo contenido da siempre el mismo texto. */
    private fun canonical(
        packs: List<LanguagePack>,
        templates: List<ReceiptTemplate>,
    ): String =
        buildString {
            packs.sortedBy { it.code }.forEach { pack ->
                appendLine("# paquete ${pack.code}")
                words("amountLabels", pack.amountLabels)
                words("counterpartyLabels", pack.counterpartyLabels)
                words("accountLabels", pack.accountLabels)
                words("noteLabels", pack.noteLabels)
                words("referenceLabels", pack.referenceLabels)
                words("expenseWords", pack.expenseWords)
                words("incomeWords", pack.incomeWords)
                words("withdrawalWords", pack.withdrawalWords)
                words("exchangeWords", pack.exchangeWords)
                words("notePrepositions", pack.notePrepositions)
                words("conjunctions", pack.conjunctions)
                words("rateMarkers", pack.rateMarkers)
                words("receivedMarkers", pack.receivedMarkers)
                words("genericWalletWords", pack.genericWalletWords)
                entries("months", pack.months.mapValues { it.value.toString() })
                entries("currencyWords", pack.currencyWords.mapValues { it.value ?: "(locale)" })
                entries("categoryWords", pack.categoryWords.mapValues { it.value.name })
                entries("paymentMethodWords", pack.paymentMethodWords.mapValues { it.value.name })
            }
            templates.sortedBy { it.id }.forEach { plantilla ->
                appendLine("# plantilla ${plantilla.id}")
                appendLine("  kind: ${plantilla.kind}")
                // Los marcadores van en orden: en una plantilla el orden es parte del dato.
                appendLine("  markers (${plantilla.markers.size})")
                plantilla.markers.forEach { appendLine("    $it") }
                appendLine("  amountLabel: ${plantilla.amountLabel ?: "-"}")
                appendLine("  counterpartyLabel: ${plantilla.counterpartyLabel ?: "-"}")
                appendLine("  noteLabel: ${plantilla.noteLabel ?: "-"}")
                appendLine("  accountLabel: ${plantilla.accountLabel ?: "-"}")
                appendLine("  referenceLabel: ${plantilla.referenceLabel ?: "-"}")
                appendLine("  issuerWalletHint: ${plantilla.issuerWalletHint ?: "-"}")
                appendLine("  counterpartyAfterAmount: ${plantilla.counterpartyAfterAmount}")
            }
        }

    private fun StringBuilder.words(
        name: String,
        values: Set<String>,
    ) {
        appendLine("  $name (${values.size})")
        values.sorted().forEach { appendLine("    $it") }
    }

    private fun StringBuilder.entries(
        name: String,
        values: Map<String, String>,
    ) {
        appendLine("  $name (${values.size})")
        values.entries.sortedBy { it.key }.forEach { appendLine("    ${it.key} -> ${it.value}") }
    }
}
