package com.coinden.core.domain.packs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El dominio no tiene palabras de ningún idioma: están en archivos de datos que se pueden
 * descargar (ver `core/data-packs`). Es una regla del Manifiesto, y antes el código la rompía con
 * cientos de palabras de es, en y pt.
 *
 * Esta prueba es el guardia que impide que vuelvan a entrar sin que nadie se dé cuenta.
 */
class NoWordsInDomainTest {
    private val fuentes = File("src/commonMain/kotlin")

    /** Una colección con varias palabras seguidas es vocabulario, no configuración. */
    private val coleccionDePalabras =
        Regex("""(setOf|listOf|mapOf)\s*\(([^()]{0,4000}?)\)""", RegexOption.DOT_MATCHES_ALL)
    private val literal = Regex(""""([^"\\]{2,40})"""")

    /**
     * Una palabra de idioma va en minúsculas y normalizada ("almuerzo", "nro de transaccion").
     * Los códigos ISO van en mayúsculas ("USD", "BOB") y el Manifiesto sí los permite en el
     * dominio: son códigos, no lenguaje.
     */
    private val esPalabra = Regex("""^[\p{Ll}][\p{Ll} ]*$""")

    private val maximoPalabras = 4

    @Test
    fun `hay fuentes del dominio para revisar`() {
        assertTrue(archivos().size > 20, "se esperaban los archivos de core/domain en ${fuentes.path}")
    }

    @Test
    fun `la regla detecta una lista de palabras, para que el guardia sirva de algo`() {
        val recaida =
            """
            val SPANISH = LanguagePack(
                expenseWords = setOf("pague", "gaste", "compre", "envie", "transferi"),
            )
            """.trimIndent()

        assertTrue(palabrasEn(recaida).isNotEmpty(), "el guardia no detectó una recaída evidente")
    }

    @Test
    fun `la regla no se queja de codigos ni de listas cortas`() {
        assertTrue(palabrasEn("""val iso = setOf("USD", "EUR", "BOB", "BRL", "ARS", "CLP")""").isEmpty())
        assertTrue(palabrasEn("""val pocas = setOf("uno", "dos")""").isEmpty())
    }

    @Test
    fun `ningun archivo del dominio declara una lista de palabras de idioma`() {
        val encontrados = mutableListOf<String>()

        archivos().forEach { archivo ->
            palabrasEn(archivo.readText()).forEach { palabras ->
                encontrados += "${archivo.name}: ${palabras.take(6)}${if (palabras.size > 6) "..." else ""}"
            }
        }

        assertEquals(
            emptyList(),
            encontrados,
            "el dominio volvió a tener palabras de idioma; van en un archivo de datos de core/data-packs",
        )
    }

    /** Cada colección del texto que parezca vocabulario, con las palabras que trae. */
    private fun palabrasEn(texto: String): List<List<String>> =
        coleccionDePalabras
            .findAll(texto)
            .map { match ->
                literal
                    .findAll(match.groupValues[2])
                    .map { it.groupValues[1] }
                    .filter { esPalabra.matches(it) }
                    .toList()
            }.filter { it.size > maximoPalabras }
            .toList()

    private fun archivos(): List<File> = fuentes.walkTopDown().filter { it.extension == "kt" }.toList()
}
