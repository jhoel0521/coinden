package com.coinden.core.data.packs

import com.coinden.core.domain.model.interpret.TextNormalizer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El intérprete compara contra palabras normalizadas (minúsculas, sin tildes). Una palabra con
 * tilde en un archivo no fallaría: simplemente nunca coincidiría con nada, en silencio.
 *
 * Esta prueba es la que convierte ese silencio en un error, y vale para cualquier paquete que se
 * agregue después, no solo para los tres de hoy.
 */
class PackFilesNormalizedTest {
    private val carpeta = File("src/commonMain/resources/packs")

    @Test
    fun `hay archivos de paquetes para revisar`() {
        assertTrue(archivos().isNotEmpty(), "no se encontró ningún archivo en ${carpeta.path}")
    }

    @Test
    fun `todas las palabras de todos los paquetes estan normalizadas`() {
        val malas = mutableListOf<String>()

        archivos().filterNot { it.name.startsWith("template-") }.forEach { archivo ->
            val pack = PackParser.languagePack(archivo.readText()) ?: return@forEach
            val palabras =
                pack.amountLabels + pack.counterpartyLabels + pack.accountLabels + pack.noteLabels +
                    pack.referenceLabels + pack.expenseWords + pack.incomeWords + pack.withdrawalWords +
                    pack.exchangeWords + pack.notePrepositions + pack.conjunctions + pack.rateMarkers +
                    pack.receivedMarkers + pack.genericWalletWords + pack.months.keys +
                    pack.currencyWords.keys + pack.categoryWords.keys + pack.paymentMethodWords.keys

            palabras.forEach { palabra ->
                if (palabra != TextNormalizer.normalize(palabra)) malas += "${archivo.name}: \"$palabra\""
            }
        }

        assertEquals(emptyList(), malas, "estas palabras no están normalizadas y nunca coincidirían")
    }

    @Test
    fun `las etiquetas de todas las plantillas estan normalizadas`() {
        val malas = mutableListOf<String>()

        archivos().filter { it.name.startsWith("template-") }.forEach { archivo ->
            val plantilla = PackParser.receiptTemplate(archivo.readText()) ?: return@forEach
            val etiquetas =
                plantilla.markers +
                    listOfNotNull(
                        plantilla.amountLabel,
                        plantilla.counterpartyLabel,
                        plantilla.noteLabel,
                        plantilla.accountLabel,
                        plantilla.referenceLabel,
                        plantilla.issuerWalletHint,
                    )

            etiquetas.forEach { etiqueta ->
                if (etiqueta != TextNormalizer.normalize(etiqueta)) malas += "${archivo.name}: \"$etiqueta\""
            }
        }

        assertEquals(emptyList(), malas, "estas etiquetas no están normalizadas y nunca coincidirían")
    }

    private fun archivos(): List<File> =
        carpeta
            .listFiles()
            ?.filter { it.extension == "json" }
            .orEmpty()
            .sortedBy { it.name }
}
