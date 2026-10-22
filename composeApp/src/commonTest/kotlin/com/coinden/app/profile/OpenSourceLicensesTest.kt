package com.coinden.app.profile

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenSourceLicensesTest {
    private val folder = File("src/commonMain/composeResources/files/licenses")

    @Test
    fun `cada libreria tiene el texto completo de su licencia en la app`() {
        OpenSourceLicenses.all.filter { it.textFile != null }.forEach { library ->
            val file = File(folder, library.textFile!!)
            assertTrue(file.isFile && file.readText().isNotBlank(), "${library.name}: falta ${library.textFile}")
        }
    }

    @Test
    fun `estan las librerias nativas y las fuentes que van dentro de la app`() {
        val names = OpenSourceLicenses.all.map { it.name }
        listOf("whisper.cpp", "Tesseract", "Leptonica", "SQLCipher", "Manrope", "Bricolage Grotesque", "ML Kit").forEach {
            assertTrue(names.any { name -> name.startsWith(it) }, "falta $it")
        }
    }

    @Test
    fun `lo que no es software libre lo dice y enlaza sus terminos`() {
        val mlKit = OpenSourceLicenses.all.single { it.name.startsWith("ML Kit") }
        assertTrue(mlKit.textFile == null && mlKit.url.startsWith("https://"))
    }

    @Test
    fun `el texto se lee como parrafos sin los cortes de linea del archivo`() {
        val text = "MIT License\n\nPermission is hereby granted,\nfree of charge.\r\n\r\nTHE SOFTWARE\nIS PROVIDED.\n"
        assertEquals(
            "MIT License\n\nPermission is hereby granted, free of charge.\n\nTHE SOFTWARE IS PROVIDED.",
            OpenSourceLicenses.paragraphs(text),
        )
    }
}
