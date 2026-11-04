package com.coinden.app.intent

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * El modelo del sistema (Gemini Nano) como motor del intérprete: solo se consulta si el teléfono
 * lo tiene y la persona aceptó el motor de Google, y nunca rompe una captura.
 */
class GeminiNanoEngineTest {
    private class ClienteFalso(
        private val estado: SystemModelStatus = SystemModelStatus.AVAILABLE,
        private val respuesta: String? = "{}",
        private val explota: Boolean = false,
    ) : SystemModelClient {
        var consultas = 0
        var generaciones = 0
        var temperatura: Float? = null
        var sistema: String? = null

        override suspend fun status(): SystemModelStatus {
            consultas++
            if (explota) error("AICore no responde")
            return estado
        }

        override suspend fun generate(
            systemPrompt: String,
            input: String,
            temperature: Float,
        ): String? {
            generaciones++
            temperatura = temperature
            sistema = systemPrompt
            if (explota) error("AICore no responde")
            return respuesta
        }
    }

    private suspend fun GeminiNanoEngine.pide() = generate("sistema", "almuerzo 45", "esquema", 0f)

    @Test
    fun `sin aceptar el motor de Google no se consulta nada`() =
        runTest {
            val cliente = ClienteFalso()

            assertNull(GeminiNanoEngine(cliente) { false }.pide())
            assertEquals(0, cliente.consultas)
            assertEquals(0, cliente.generaciones)
        }

    @Test
    fun `si el telefono no tiene el modelo devuelve null y no genera`() =
        runTest {
            for (estado in listOf(SystemModelStatus.UNAVAILABLE, SystemModelStatus.DOWNLOADABLE, SystemModelStatus.DOWNLOADING)) {
                val cliente = ClienteFalso(estado = estado)

                assertNull(GeminiNanoEngine(cliente) { true }.pide(), "estado $estado")
                assertEquals(0, cliente.generaciones)
            }
        }

    @Test
    fun `con el modelo disponible devuelve el texto y usa temperatura 0`() =
        runTest {
            val cliente = ClienteFalso(respuesta = """{"payments":[]}""")

            assertEquals("""{"payments":[]}""", GeminiNanoEngine(cliente) { true }.generate("el prompt", "x", "g", 0.7f))
            assertEquals(0f, cliente.temperatura, "el motor fija la temperatura 0, no la que le pasen")
            assertEquals("el prompt", cliente.sistema)
        }

    @Test
    fun `quita las vallas de codigo que el modelo suele agregar`() =
        runTest {
            val cliente = ClienteFalso(respuesta = "```json\n{\"payments\":[]}\n```")

            assertEquals("""{"payments":[]}""", GeminiNanoEngine(cliente) { true }.pide())
        }

    @Test
    fun `si el sistema falla no rompe la captura`() =
        runTest {
            assertNull(GeminiNanoEngine(ClienteFalso(explota = true)) { true }.pide())
        }

    @Test
    fun `si el modelo no responde nada devuelve null`() =
        runTest {
            assertNull(GeminiNanoEngine(ClienteFalso(respuesta = null)) { true }.pide())
            assertNull(GeminiNanoEngine(ClienteFalso(respuesta = "   ")) { true }.pide())
        }

    @Test
    fun `la aceptacion se vuelve a mirar en cada captura`() =
        runTest {
            var aceptado = false
            val cliente = ClienteFalso()
            val motor = GeminiNanoEngine(cliente) { aceptado }

            assertNull(motor.pide())
            aceptado = true
            assertEquals("{}", motor.pide())
            aceptado = false
            assertNull(motor.pide())
        }
}
