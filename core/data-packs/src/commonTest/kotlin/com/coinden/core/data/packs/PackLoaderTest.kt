package com.coinden.core.data.packs

import com.coinden.core.domain.model.interpret.merged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Junta los paquetes que vienen con la app con los que la persona descargó. Un archivo que no se
 * entiende se descarta sin dejar a nadie sin capturar, y ante la misma palabra en dos idiomas
 * manda el idioma de la app.
 */
class PackLoaderTest {
    private val ingles = """{ "formatVersion": 1, "code": "en", "expenseWords": ["paid"], "currencyWords": { "pesos": "MXN" } }"""
    private val espanol = """{ "formatVersion": 1, "code": "es", "expenseWords": ["pague"], "currencyWords": { "pesos": "BOB" } }"""
    private val portugues = """{ "formatVersion": 1, "code": "pt", "expenseWords": ["paguei"] }"""

    @Test
    fun `junta los que vienen con la app con los descargados`() {
        val packs = PackLoader.languagePacks(bundled = listOf(ingles), downloaded = listOf(espanol, portugues), appLanguage = "es")

        assertEquals(setOf("en", "es", "pt"), packs.map { it.code }.toSet())
    }

    @Test
    fun `ante la misma palabra gana el paquete del idioma de la app`() {
        val packs = PackLoader.languagePacks(bundled = listOf(ingles), downloaded = listOf(espanol), appLanguage = "es")

        // merged() deja ganar al último, así que el idioma de la app va al final.
        assertEquals("BOB", packs.merged().currencyWords["pesos"])
        assertEquals("es", packs.last().code)
    }

    @Test
    fun `sin el idioma de la app el orden sigue siendo estable`() {
        val packs = PackLoader.languagePacks(bundled = listOf(ingles), downloaded = listOf(portugues), appLanguage = "fr")

        assertEquals(listOf("en", "pt"), packs.map { it.code })
    }

    @Test
    fun `un archivo danado no impide cargar los demas`() {
        val packs = PackLoader.languagePacks(bundled = listOf(ingles, "{ roto"), downloaded = listOf(espanol), appLanguage = "es")

        assertEquals(setOf("en", "es"), packs.map { it.code }.toSet())
    }

    @Test
    fun `un paquete descargado reemplaza al que viene con la app`() {
        val inglesCorregido = """{ "formatVersion": 1, "code": "en", "expenseWords": ["paid", "spent"] }"""

        val packs = PackLoader.languagePacks(bundled = listOf(ingles), downloaded = listOf(inglesCorregido), appLanguage = "en")

        assertEquals(1, packs.size)
        assertEquals(setOf("paid", "spent"), packs.single().expenseWords)
    }

    @Test
    fun `sin ningun archivo no hay paquetes y nada falla`() {
        assertEquals(emptyList(), PackLoader.languagePacks(bundled = emptyList(), downloaded = emptyList(), appLanguage = "es"))
    }

    @Test
    fun `las plantillas se juntan y las invalidas se descartan`() {
        val buena = """{ "formatVersion": 1, "id": "bo-yape-payment", "markers": ["yapeaste"], "kind": "EXPENSE" }"""
        val mala = """{ "formatVersion": 1, "id": "x", "markers": [], "kind": "EXPENSE" }"""

        val plantillas = PackLoader.receiptTemplates(bundled = listOf(buena, mala), downloaded = listOf("{ roto"))

        assertEquals(listOf("bo-yape-payment"), plantillas.map { it.id })
    }

    @Test
    fun `una plantilla descargada reemplaza a la que viene con la app`() {
        val base = """{ "formatVersion": 1, "id": "bo-yape-payment", "markers": ["yapeaste"], "kind": "EXPENSE" }"""
        val corregida =
            """{ "formatVersion": 1, "id": "bo-yape-payment", "markers": ["yapeaste"], "kind": "EXPENSE", "noteLabel": "glosa" }"""

        val plantillas = PackLoader.receiptTemplates(bundled = listOf(base), downloaded = listOf(corregida))

        assertEquals(1, plantillas.size)
        assertEquals("glosa", plantillas.single().noteLabel)
    }

    @Test
    fun `informa que archivos no pudo leer`() {
        val resultado = PackLoader.load(bundled = listOf(ingles, "{ roto"), downloaded = listOf(espanol), appLanguage = "es")

        assertEquals(2, resultado.languagePacks.size)
        assertEquals(1, resultado.descartados)
        assertTrue(resultado.languagePacks.any { it.code == "es" })
    }
}
