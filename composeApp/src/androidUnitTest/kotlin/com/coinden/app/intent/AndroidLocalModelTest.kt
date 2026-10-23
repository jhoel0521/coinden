package com.coinden.app.intent

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El adaptador del modelo: fija el prompt y la temperatura, y sobre todo **no falla** cuando no
 * hay modelo. Quedarse sin modelo es el caso normal de media Bolivia, no un error.
 */
class AndroidLocalModelTest {
    private class MotorFalso(
        private val respuesta: String?,
        private val explota: Boolean = false,
    ) : ModelEngine {
        var systemPrompt: String? = null
        var temperatura: Float? = null
        var gramatica: String? = null

        override suspend fun generate(
            systemPrompt: String,
            input: String,
            grammar: String,
            temperature: Float,
        ): String? {
            this.systemPrompt = systemPrompt
            this.temperatura = temperature
            this.gramatica = grammar
            if (explota) error("el motor se cayó")
            return respuesta
        }
    }

    @Test
    fun `sin motor devuelve null, y eso no es un error`() =
        runTest {
            assertNull(AndroidLocalModel(engine = null).complete("lo que sea", "gramatica"))
        }

    @Test
    fun `si el motor se cae, se sigue con el respaldo en vez de romper la captura`() =
        runTest {
            val resultado = AndroidLocalModel(MotorFalso(respuesta = null, explota = true)).complete("algo", "g")

            assertNull(resultado)
        }

    @Test
    fun `usa temperatura 0, para que la misma captura de siempre lo mismo`() =
        runTest {
            val motor = MotorFalso("{}")

            AndroidLocalModel(motor).complete("algo", "g")

            assertEquals(0f, motor.temperatura)
        }

    @Test
    fun `le pasa al motor la gramatica que recibio`() =
        runTest {
            val motor = MotorFalso("{}")

            AndroidLocalModel(motor).complete("algo", "la gramatica")

            assertEquals("la gramatica", motor.gramatica)
        }

    @Test
    fun `el prompt le prohibe inventar y le prohibe pisar el comprobante`() =
        runTest {
            val motor = MotorFalso("{}")

            AndroidLocalModel(motor).complete("algo", "g")

            val prompt = motor.systemPrompt.orEmpty()
            assertTrue(prompt.contains("never invent an amount"), "el prompt tiene que prohibir inventar montos")
            assertTrue(prompt.contains("never change an amount"), "y prohibir cambiar lo que dice el comprobante")
            assertTrue(prompt.contains("RECEIPT") && prompt.contains("CONTEXT"), "tiene que explicar los dos papeles")
        }

    @Test
    fun `el prompt es siempre el mismo, no se arma con datos de la persona`() =
        runTest {
            val uno = MotorFalso("{}")
            val otro = MotorFalso("{}")

            AndroidLocalModel(uno).complete("gasté 25 en comida", "g")
            AndroidLocalModel(otro).complete("pagué 900 el alquiler", "g")

            assertEquals(uno.systemPrompt, otro.systemPrompt)
            assertEquals(AndroidLocalModel.SYSTEM_PROMPT, uno.systemPrompt)
        }

    @Test
    fun `devuelve tal cual lo que contesto el motor, sin tocarlo`() =
        runTest {
            val respuesta = """{"payments":[]}"""

            assertEquals(respuesta, AndroidLocalModel(MotorFalso(respuesta)).complete("algo", "g"))
        }
}
