package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.interpret.ModelIntentInterpreter
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.port.output.LocalModelPort
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El intérprete de intenciones: le pasa el paquete al modelo local y devuelve propuestas del
 * dominio. Con el modelo detrás de un puerto, todo esto se prueba sin motor ni modelo real.
 */
class ModelIntentInterpreterTest {
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val directory = WalletDirectory(listOf(bnb), mapOf("bnb" to "bnb"), defaultWalletId = "bnb")
    private val categorias = setOf("comida")

    private val comprobante =
        BundleText("e1", EvidenceRole.RECEIPT, EvidenceSource.PHOTO, "Pago QR Importe Bs25.00")
    private val audio =
        BundleText("e2", EvidenceRole.CONTEXT, EvidenceSource.VOICE, "esto es de mi comida")

    /** Un motor de mentira: guarda lo que le pidieron y devuelve lo que se le indique. */
    private class FakeModel(
        private val respuesta: String?,
    ) : LocalModelPort {
        var entrada: String? = null
        var gramatica: String? = null
        var llamadas = 0

        override suspend fun complete(
            input: String,
            grammar: String,
        ): String? {
            entrada = input
            gramatica = grammar
            llamadas++
            return respuesta
        }
    }

    private val respuestaBuena =
        """{"payments":[{"kind":"EXPENSE","amount":"25.00","currency":"BOB","wallet":"bnb","category":"comida","note":"comida"}]}"""

    private suspend fun interpretarCon(
        respuesta: String?,
        textos: List<BundleText> = listOf(comprobante, audio),
    ): Pair<FakeModel, List<com.coinden.core.domain.port.output.BundlePayment>?> {
        val modelo = FakeModel(respuesta)
        return modelo to ModelIntentInterpreter(modelo).interpret(textos, directory, categorias)
    }

    @Test
    fun `con una respuesta valida devuelve el pago`() =
        runTest {
            val (_, pagos) = interpretarCon(respuestaBuena)

            val gasto = assertIs<MovementProposal.Expense>(assertNotNull(pagos).single().proposal)
            assertEquals("comida", gasto.note)
        }

    @Test
    fun `sin modelo devuelve null, para que mande el respaldo`() =
        runTest {
            val (_, pagos) = interpretarCon(respuesta = null)

            assertNull(pagos, "sin modelo no hay propuesta: decide el respaldo determinista")
        }

    @Test
    fun `una respuesta que no cumple el formato devuelve null`() =
        runTest {
            val (_, pagos) = interpretarCon("Claro, fueron 25 bolivianos de comida.")

            assertNull(pagos)
        }

    @Test
    fun `le pasa al modelo el texto de cada evidencia con su papel`() =
        runTest {
            val (modelo, _) = interpretarCon(respuestaBuena)

            val entrada = assertNotNull(modelo.entrada)
            assertTrue(entrada.contains("Importe Bs25.00"), "falta el texto del comprobante")
            assertTrue(entrada.contains("esto es de mi comida"), "falta el texto del audio")
            assertTrue(entrada.contains(EvidenceRole.RECEIPT.name), "el modelo tiene que saber qué es comprobante")
            assertTrue(entrada.contains(EvidenceRole.CONTEXT.name), "y qué es contexto")
        }

    @Test
    fun `le pasa al modelo el formato con sus billeteras y categorias`() =
        runTest {
            val (modelo, _) = interpretarCon(respuestaBuena)

            val gramatica = assertNotNull(modelo.gramatica)
            assertTrue(gramatica.contains(""""bnb""""), "el formato tiene que traer sus billeteras")
            assertTrue(gramatica.contains(""""comida""""), "y sus categorías")
        }

    @Test
    fun `sin evidencias no molesta al modelo`() =
        runTest {
            val (modelo, pagos) = interpretarCon(respuestaBuena, textos = emptyList())

            assertNull(pagos)
            assertEquals(0, modelo.llamadas, "no tiene sentido despertar el modelo sin nada que interpretar")
        }

    @Test
    fun `una evidencia sin texto no se le manda`() =
        runTest {
            val sinTexto = BundleText("e3", EvidenceRole.CONTEXT, EvidenceSource.VOICE, "   ")

            val (modelo, _) = interpretarCon(respuestaBuena, textos = listOf(comprobante, sinTexto))

            assertTrue(!assertNotNull(modelo.entrada).contains("e3"), "una evidencia sin texto no aporta nada")
        }

    @Test
    fun `se le pide una sola vez por paquete`() =
        runTest {
            val (modelo, _) = interpretarCon(respuestaBuena)

            assertEquals(1, modelo.llamadas, "el paquete se interpreta de una sola pasada")
        }
}
