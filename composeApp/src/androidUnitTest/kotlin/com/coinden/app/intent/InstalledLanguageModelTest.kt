package com.coinden.app.intent

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El modelo de lenguaje se reconoce **sin reiniciar la app**: aparece al descargarlo, desaparece
 * al borrarlo, y si no carga se sigue con el respaldo y se avisa una sola vez.
 */
class InstalledLanguageModelTest {
    private val dir =
        File.createTempFile("modelo", "").apply {
            delete()
            mkdirs()
        }
    private val file = File(dir, "coinden-intent.litertlm")

    @AfterTest
    fun limpiar() {
        dir.deleteRecursively()
    }

    private class MotorFalso(
        private val carga: Boolean = true,
        private val respuesta: String? = "{}",
    ) : LoadableEngine {
        var cargas = 0
        var generaciones = 0
        var liberado = false

        override suspend fun load(): Boolean {
            cargas++
            return carga
        }

        override suspend fun generate(
            systemPrompt: String,
            input: String,
            grammar: String,
            temperature: Float,
        ): String? {
            generaciones++
            return respuesta
        }

        override fun release() {
            liberado = true
        }
    }

    @Test
    fun `sin el archivo devuelve null y no crea ningun motor`() =
        runTest {
            var creados = 0
            val modelo =
                InstalledLanguageModel(file) {
                    creados++
                    MotorFalso()
                }

            assertNull(modelo.complete("almuerzo 45", "g"))
            assertEquals(0, creados)
            assertFalse(modelo.consumeLoadFailure())
        }

    @Test
    fun `un modelo descargado despues se usa sin reiniciar la app`() =
        runTest {
            val motor = MotorFalso(respuesta = "{\"ok\":1}")
            val modelo = InstalledLanguageModel(file) { motor }
            assertNull(modelo.complete("antes", "g"))

            file.writeText("modelo")

            assertEquals("{\"ok\":1}", modelo.complete("despues", "g"))
            assertEquals(1, motor.generaciones)
        }

    @Test
    fun `el motor se crea una sola vez y se reutiliza`() =
        runTest {
            file.writeText("modelo")
            var creados = 0
            val modelo =
                InstalledLanguageModel(file) {
                    creados++
                    MotorFalso()
                }

            modelo.complete("uno", "g")
            modelo.complete("dos", "g")

            assertEquals(1, creados)
        }

    @Test
    fun `al borrar el archivo se libera el motor y se vuelve al respaldo`() =
        runTest {
            file.writeText("modelo")
            val motor = MotorFalso()
            val modelo = InstalledLanguageModel(file) { motor }
            modelo.complete("uno", "g")

            file.delete()

            assertNull(modelo.complete("dos", "g"))
            assertTrue(motor.liberado)
        }

    @Test
    fun `si el modelo no carga sigue el respaldo y se avisa una sola vez`() =
        runTest {
            file.writeText("modelo roto")
            val modelo = InstalledLanguageModel(file) { MotorFalso(carga = false) }

            assertNull(modelo.complete("uno", "g"))
            assertTrue(modelo.consumeLoadFailure())
            assertNull(modelo.complete("dos", "g"))
            assertFalse(modelo.consumeLoadFailure())
        }
}
