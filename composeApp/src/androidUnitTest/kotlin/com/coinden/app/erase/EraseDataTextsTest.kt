package com.coinden.app.erase

import com.coinden.app.perception.PrivacyPoints
import com.coinden.app.profile.ProfileOption
import com.coinden.app.profile.label
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Lo que se ve de "Borrar mis datos" en Perfil, en la confirmación y en la pantalla de privacidad. */
class EraseDataTextsTest {
    private fun resources(folder: String): Map<String, String> =
        Regex("""<string name="([^"]+)">([^<]*)</string>""")
            .findAll(File("src/commonMain/composeResources/$folder/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private val es = resources("values")

    @Test
    fun `Carlos ve en Perfil Borrar mis datos despues de Modelos de voz y antes de Acerca de`() {
        assertEquals(
            listOf("Fuentes y equivalencias", "Categorías propias", "Privacidad", "Modelos de voz", "Borrar mis datos"),
            ProfileOption.entries.map { es.getValue(it.label().key) },
        )
    }

    @Test
    fun `la confirmacion explica que se borra, que no se deshace y que solo afecta este telefono`() {
        val confirmation = EraseTexts.confirmation.map { es.getValue(it.key) }
        assertTrue(confirmation.any { "fuentes, movimientos, borradores, fotos, audios y preferencias" in it })
        assertTrue(confirmation.any { "No se puede deshacer" in it })
        assertTrue(confirmation.any { "Solo se borra este teléfono" in it && "No se envía nada a ningún lado" in it })
        assertEquals("Borrar todo", es.getValue("erase_confirm"))
        assertEquals("Cancelar", es.getValue("dialog_cancel"))
    }

    @Test
    fun `Carlos lee en la pantalla de privacidad como borrar todo y que solo afecta este telefono`() {
        val points = PrivacyPoints.all.map { es.getValue(it.second.key) }
        assertTrue(points.any { "borrar todos tus datos" in it && "Perfil" in it && "este teléfono" in it })
    }

    @Test
    fun `la confirmacion y la linea de privacidad existen en ingles y portugues`() {
        for (folder in listOf("values-en", "values-pt")) {
            val strings = resources(folder)
            for (key in EraseTexts.confirmation.map { it.key } + "erase_confirm" + "erase_title" + "privacy_erase") {
                assertTrue(strings[key].orEmpty().isNotBlank(), "$folder/$key")
            }
        }
    }
}
