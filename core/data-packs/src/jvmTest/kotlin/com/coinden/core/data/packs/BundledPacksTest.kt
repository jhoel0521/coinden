package com.coinden.core.data.packs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Lo que viaja dentro de la app: tiene que cargar sin red, sin nada descargado y dando
 * exactamente lo mismo que daban las constantes de Kotlin.
 */
class BundledPacksTest {
    @Test
    fun `los paquetes que viajan con la app cargan`() {
        val packs = BundledPacks.languagePacks()

        assertEquals(setOf("en", "es", "pt"), packs.map { it.code }.toSet())
    }

    @Test
    fun `el ingles esta siempre, que es lo que la app garantiza`() {
        assertNotNull(BundledPacks.languagePacks().firstOrNull { it.code == "en" })
    }

    @Test
    fun `el paquete del idioma de la app va al final para ganar al unirlos`() {
        assertEquals("es", BundledPacks.languagePacks(appLanguage = "es").last().code)
        assertEquals("pt", BundledPacks.languagePacks(appLanguage = "pt").last().code)
    }

    @Test
    fun `un paquete descargado se suma a los que viajan con la app`() {
        val frances = """{ "formatVersion": 1, "code": "fr", "expenseWords": ["paye"] }"""

        val packs = BundledPacks.languagePacks(downloaded = listOf(frances), appLanguage = "fr")

        assertTrue(packs.any { it.code == "fr" })
        assertEquals("fr", packs.last().code)
        assertEquals(4, packs.size)
    }
}
