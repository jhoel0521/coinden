package com.coinden.core.domain.unit

import com.coinden.core.domain.model.interpret.TextNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextNormalizerTest {
    @Test
    fun `minusculas, sin tildes y sin signos`() {
        assertEquals("yapeaste", TextNormalizer.normalize("¡Yapeaste!"))
        assertEquals("pague", TextNormalizer.normalize("PAGUÉ"))
        assertEquals("almoco", TextNormalizer.normalize("almoço"))
        assertEquals("debito por compra", TextNormalizer.normalize("DÉBITO  POR COMPRA"))
        assertEquals("cuenta origen", TextNormalizer.normalize("Cuenta origen:"))
    }

    @Test
    fun `palabras con su texto original`() {
        val words = TextNormalizer.words("page 3 bs para trasmporte")
        assertEquals(listOf("page", "3", "bs", "para", "trasmporte"), words.map { it.normalized })
        assertEquals("trasmporte", words.last().original)
    }

    @Test
    fun `distancia de edicion de a lo sumo uno`() {
        assertTrue(TextNormalizer.withinOneEdit("page", "pague")) // falta una letra
        assertTrue(TextNormalizer.withinOneEdit("gaste", "gasto")) // una letra distinta
        assertTrue(TextNormalizer.withinOneEdit("compre", "compre"))
        assertFalse(TextNormalizer.withinOneEdit("pago", "pagar"))
    }

    @Test
    fun `tolerancia solo para palabras de cuatro letras o mas`() {
        assertTrue(TextNormalizer.matches("page", setOf("pague"), fuzzy = true))
        assertFalse(TextNormalizer.matches("pa", setOf("pay"), fuzzy = true))
        assertFalse(TextNormalizer.matches("page", setOf("pague"), fuzzy = false))
        assertTrue(TextNormalizer.matches("pague", setOf("pague"), fuzzy = false))
    }
}
