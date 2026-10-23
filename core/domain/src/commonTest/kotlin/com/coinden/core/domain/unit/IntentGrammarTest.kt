package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.interpret.IntentGrammar
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La gramática con la que el modelo responde.
 *
 * No es una sugerencia: el motor enmascara en cada paso los tokens que la violarían, así que el
 * modelo **no puede** devolver algo fuera de este formato. Es lo que hace cumplible el principio 4
 * del Manifiesto ("responde solo con un formato cerrado") sin confiar en su buena voluntad.
 */
class IntentGrammarTest {
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val colchon = Wallet("colchon", "carlos", "Dólares colchón", Currency.USD, WalletType.CASH)
    private val directory = WalletDirectory(listOf(bnb, colchon), mapOf("bnb" to "bnb"), defaultWalletId = "bnb")

    private fun grammar(categories: Set<String> = setOf("comida", "transporte")) = IntentGrammar.of(directory, categories)

    @Test
    fun `solo ofrece las billeteras de la persona`() {
        val g = grammar()

        assertTrue(g.contains("\\\"bnb\\\""), "falta la billetera bnb")
        assertTrue(g.contains("\\\"colchon\\\""), "falta la billetera colchon")
        assertFalse(g.contains("\\\"yape\\\""), "no puede ofrecer una billetera que la persona no tiene")
    }

    @Test
    fun `solo ofrece las categorias de la persona`() {
        val g = grammar(setOf("comida"))

        assertTrue(g.contains("\\\"comida\\\""))
        assertFalse(g.contains("\\\"transporte\\\""), "no puede ofrecer una categoría que la persona no tiene")
    }

    @Test
    fun `solo ofrece las monedas de sus billeteras`() {
        val g = grammar()

        assertTrue(g.contains("\\\"BOB\\\""))
        assertTrue(g.contains("\\\"USD\\\""))
        assertFalse(g.contains("\\\"EUR\\\""), "no puede ofrecer una moneda que no usa")
    }

    @Test
    fun `el monto es una cadena de digitos, nunca un numero suelto`() {
        val g = grammar()

        // Dinero exacto: un float en la gramática seria la puerta a 0.1 + 0.2 = 0.30000000000000004.
        assertTrue(g.contains("amount ::="), "falta la regla del monto")
        val regla = g.lineSequence().first { it.startsWith("amount ::=") }
        assertTrue(regla.contains("\"\\\"\""), "el monto tiene que ir entre comillas, como cadena")
        assertFalse(regla.contains("e"), "nada de notación exponencial en un monto")
    }

    @Test
    fun `siempre hay una regla raiz`() {
        assertTrue(grammar().lineSequence().any { it.startsWith("root ::=") })
    }

    @Test
    fun `una persona sin categorias igual obtiene una gramatica usable`() {
        val g = IntentGrammar.of(directory, emptySet())

        assertTrue(g.lineSequence().any { it.startsWith("root ::=") })
        assertTrue(g.contains("null"), "sin categorías, la categoría solo puede ser null")
    }

    @Test
    fun `una persona sin billeteras igual obtiene una gramatica usable`() {
        val vacio = WalletDirectory(emptyList(), emptyMap(), defaultWalletId = null)

        val g = IntentGrammar.of(vacio, emptySet())

        assertTrue(g.lineSequence().any { it.startsWith("root ::=") })
    }

    @Test
    fun `los ids con comillas no rompen la gramatica`() {
        val raro = Wallet("""w"1""", "carlos", "Rara", Currency.BOB, WalletType.CASH)
        val conRaro = WalletDirectory(listOf(raro), emptyMap(), defaultWalletId = null)

        val g = IntentGrammar.of(conRaro, setOf("""ca"tegoria"""))

        assertFalse(g.contains("""w"1"""), "un id con comillas tiene que ir escapado")
        assertTrue(g.contains("""w\"1"""), "se esperaba el id escapado")
    }

    @Test
    fun `la misma entrada da siempre la misma gramatica`() {
        assertEquals(grammar(), grammar(), "la gramática tiene que ser determinista")
    }

    @Test
    fun `las billeteras salen ordenadas, para que no dependa del orden del directorio`() {
        val alReves = WalletDirectory(listOf(colchon, bnb), mapOf("bnb" to "bnb"), defaultWalletId = "bnb")

        assertEquals(IntentGrammar.of(directory, emptySet()), IntentGrammar.of(alReves, emptySet()))
    }
}
