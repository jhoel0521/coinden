package com.coinden.core.data.packs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El catálogo dice qué paquetes y plantillas hay para descargar, con su tamaño y su huella.
 * El tamaño se muestra antes de bajar nada; la huella es lo que se verifica después.
 */
class PackCatalogTest {
    private val catalogo =
        """
        {
          "formatVersion": 1,
          "entries": [
            {
              "id": "fr",
              "kind": "LANGUAGE",
              "url": "https://jhoel0521.github.io/coinden/packs/fr.json",
              "sha256": "aa11bb22cc33dd44ee55ff6600112233445566778899aabbccddeeff00112233",
              "sizeBytes": 4821
            },
            {
              "id": "template-pe-yape",
              "kind": "TEMPLATE",
              "url": "https://jhoel0521.github.io/coinden/packs/template-pe-yape.json",
              "sha256": "bb11bb22cc33dd44ee55ff6600112233445566778899aabbccddeeff00112233",
              "sizeBytes": 312
            }
          ]
        }
        """.trimIndent()

    @Test
    fun `un catalogo valido se lee entero`() {
        val entradas = PackCatalog.parse(catalogo)

        assertEquals(2, entradas.size)
        val frances = entradas.first()
        assertEquals("fr", frances.id)
        assertEquals(PackKind.LANGUAGE, frances.kind)
        assertEquals(4821, frances.sizeBytes)
        assertTrue(frances.url.startsWith("https://"))
    }

    @Test
    fun `el tamano esta disponible antes de descargar nada`() {
        assertEquals(listOf(4821L, 312L), PackCatalog.parse(catalogo).map { it.sizeBytes })
    }

    @Test
    fun `un catalogo danado o vacio no rompe nada`() {
        assertEquals(emptyList(), PackCatalog.parse("{ roto"))
        assertEquals(emptyList(), PackCatalog.parse(""))
    }

    @Test
    fun `un catalogo de una version mas nueva se ignora`() {
        assertEquals(emptyList(), PackCatalog.parse("""{ "formatVersion": 99, "entries": [] }"""))
    }

    @Test
    fun `una entrada sin huella se descarta, porque no se podria verificar`() {
        val sinHuella =
            """
            { "formatVersion": 1, "entries": [
              { "id": "fr", "kind": "LANGUAGE", "url": "https://x/fr.json", "sizeBytes": 10 }
            ] }
            """.trimIndent()

        assertEquals(emptyList(), PackCatalog.parse(sinHuella))
    }

    @Test
    fun `una entrada que no va por https se descarta`() {
        val inseguro =
            """
            { "formatVersion": 1, "entries": [
              { "id": "fr", "kind": "LANGUAGE", "url": "http://x/fr.json",
                "sha256": "aa11bb22cc33dd44ee55ff6600112233445566778899aabbccddeeff00112233", "sizeBytes": 10 }
            ] }
            """.trimIndent()

        assertEquals(emptyList(), PackCatalog.parse(inseguro))
    }

    @Test
    fun `una entrada con huella que no parece una huella se descarta`() {
        val rara =
            """
            { "formatVersion": 1, "entries": [
              { "id": "fr", "kind": "LANGUAGE", "url": "https://x/fr.json", "sha256": "nope", "sizeBytes": 10 }
            ] }
            """.trimIndent()

        assertEquals(emptyList(), PackCatalog.parse(rara))
    }

    @Test
    fun `una entrada mala no se lleva puestas a las buenas`() {
        val mezcla =
            """
            { "formatVersion": 1, "entries": [
              { "id": "malo", "kind": "LANGUAGE", "url": "http://x/malo.json", "sha256": "nope", "sizeBytes": 1 },
              { "id": "fr", "kind": "LANGUAGE", "url": "https://x/fr.json",
                "sha256": "aa11bb22cc33dd44ee55ff6600112233445566778899aabbccddeeff00112233", "sizeBytes": 10 }
            ] }
            """.trimIndent()

        assertEquals(listOf("fr"), PackCatalog.parse(mezcla).map { it.id })
    }

    @Test
    fun `un tipo desconocido se descarta`() {
        val raro =
            """
            { "formatVersion": 1, "entries": [
              { "id": "x", "kind": "MODELO", "url": "https://x/x.json",
                "sha256": "aa11bb22cc33dd44ee55ff6600112233445566778899aabbccddeeff00112233", "sizeBytes": 10 }
            ] }
            """.trimIndent()

        assertEquals(emptyList(), PackCatalog.parse(raro))
    }

    @Test
    fun `el nombre del archivo sale del id y del tipo`() {
        val entradas = PackCatalog.parse(catalogo)

        assertEquals("fr.json", entradas.first { it.id == "fr" }.fileName)
        assertEquals("template-pe-yape.json", entradas.first { it.id == "template-pe-yape" }.fileName)
    }

    @Test
    fun `lo que ya viaja con la app no se ofrece para descargar`() {
        val conRepetido =
            """
            { "formatVersion": 1, "entries": [
              { "id": "es", "kind": "LANGUAGE", "url": "https://x/es.json",
                "sha256": "aa11bb22cc33dd44ee55ff6600112233445566778899aabbccddeeff00112233", "sizeBytes": 10 },
              { "id": "fr", "kind": "LANGUAGE", "url": "https://x/fr.json",
                "sha256": "bb11bb22cc33dd44ee55ff6600112233445566778899aabbccddeeff00112233", "sizeBytes": 10 }
            ] }
            """.trimIndent()

        val nuevos = PackCatalog.parse(conRepetido).filterNot { it.id in setOf("en", "es", "pt") }

        assertEquals(listOf("fr"), nuevos.map { it.id })
    }

    @Test
    fun `una entrada se puede buscar por su id`() {
        val entradas = PackCatalog.parse(catalogo)

        assertNotNull(entradas.firstOrNull { it.id == "fr" })
        assertNull(entradas.firstOrNull { it.id == "de" })
    }
}
