package com.coinden.app.i18n

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Ningún texto queda sin traducir: los tres idiomas tienen exactamente las mismas claves. */
class TranslationsTest {
    private fun keys(folder: String): Set<String> {
        val file = File("src/commonMain/composeResources/$folder/strings.xml")
        assertTrue(file.exists(), "no existe ${file.path}")
        return Regex("""<string name="([^"]+)">""").findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `espanol, ingles y portugues tienen las mismas claves`() {
        val es = keys("values")
        assertEquals(es, keys("values-en"))
        assertEquals(es, keys("values-pt"))
    }

    @Test
    fun `los idiomas de la app son los que tienen recursos`() {
        assertEquals(listOf("es", "en", "pt"), AppLanguage.SUPPORTED)
    }
}
