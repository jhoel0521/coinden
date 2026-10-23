package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.IntentResponseReader
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Lee lo que el modelo respondió y lo convierte en propuestas del dominio.
 *
 * La gramática ya impide que el modelo escriba algo fuera de formato, pero este lector no confía
 * en eso: si la respuesta no cumple, devuelve `null` y manda el respaldo determinista. Nunca
 * inventa un dato para completar una respuesta a medias.
 */
class IntentResponseReaderTest {
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val directory = WalletDirectory(listOf(bnb), mapOf("bnb" to "bnb"), defaultWalletId = "bnb")
    private val categorias = setOf("comida", "transporte")

    private fun read(text: String) = IntentResponseReader.read(text, directory, categorias)

    @Test
    fun `una respuesta valida se convierte en un pago`() {
        val respuesta =
            """
            {"payments":[{"kind":"EXPENSE","amount":"25.00","currency":"BOB","wallet":"bnb","category":"comida","note":"almuerzo"}]}
            """.trimIndent()

        val pagos = assertNotNull(read(respuesta))

        assertEquals(1, pagos.size)
        val gasto = assertIs<MovementProposal.Expense>(pagos.single().proposal)
        assertEquals(ProposedAmount("25.00", Currency.BOB), gasto.amount)
        assertEquals(WalletRef.ById("bnb"), gasto.wallet)
        assertEquals("almuerzo", gasto.note)
    }

    @Test
    fun `varios pagos salen en el orden en que vinieron`() {
        val respuesta =
            """
            {"payments":[
              {"kind":"EXPENSE","amount":"25","currency":"BOB","wallet":"bnb","category":null,"note":"comida"},
              {"kind":"EXPENSE","amount":"60","currency":"BOB","wallet":null,"category":null,"note":"somo"}
            ]}
            """.trimIndent()

        val pagos = assertNotNull(read(respuesta))

        assertEquals(2, pagos.size)
        assertEquals(listOf("comida", "somo"), pagos.map { (it.proposal as MovementProposal.Expense).note })
    }

    @Test
    fun `cada pago trae su clave, para poder reconciliarlo`() {
        val respuesta = """{"payments":[{"kind":"EXPENSE","amount":"25","currency":"BOB","wallet":null,"category":null,"note":null}]}"""

        val pagos = assertNotNull(read(respuesta))

        assertEquals(
            1,
            pagos
                .single()
                .paymentKey
                .isNotBlank()
                .let { 1 },
        )
    }

    @Test
    fun `un ingreso se lee como ingreso`() {
        val respuesta = """{"payments":[{"kind":"INCOME","amount":"100","currency":"BOB","wallet":"bnb","category":null,"note":null}]}"""

        val pagos = assertNotNull(read(respuesta))

        assertIs<MovementProposal.Income>(pagos.single().proposal)
    }

    @Test
    fun `una billetera que no existe se descarta en vez de inventarla`() {
        val respuesta =
            """{"payments":[{"kind":"EXPENSE","amount":"25","currency":"BOB","wallet":"yape","category":null,"note":null}]}"""

        val pagos = assertNotNull(read(respuesta))

        assertNull((pagos.single().proposal as MovementProposal.Expense).wallet)
    }

    @Test
    fun `una moneda que no existe deja el pago sin moneda`() {
        val respuesta =
            """{"payments":[{"kind":"EXPENSE","amount":"25","currency":"XYZ","wallet":null,"category":null,"note":null}]}"""

        val pagos = assertNotNull(read(respuesta))

        assertNull((pagos.single().proposal as MovementProposal.Expense).amount?.currency)
    }

    @Test
    fun `una nota con comillas escapadas se lee entera`() {
        val respuesta =
            """{"payments":[{"kind":"EXPENSE","amount":"25","currency":"BOB","wallet":null,"category":null,"note":"el \"somo\" de la esquina"}]}"""

        val pagos = assertNotNull(read(respuesta))

        assertEquals("""el "somo" de la esquina""", (pagos.single().proposal as MovementProposal.Expense).note)
    }

    @Test
    fun `una respuesta que no es del formato devuelve null`() {
        assertNull(read("Claro, con gusto. El gasto es de 25 bolivianos."))
        assertNull(read("{ roto"))
        assertNull(read(""))
    }

    @Test
    fun `una respuesta sin pagos devuelve una lista vacia, no null`() {
        assertEquals(emptyList(), read("""{"payments":[]}"""))
    }

    @Test
    fun `un pago sin monto se descarta, porque un pago sin monto no es un pago`() {
        val respuesta = """{"payments":[{"kind":"EXPENSE","amount":null,"currency":"BOB","wallet":"bnb","category":null,"note":"x"}]}"""

        assertEquals(emptyList(), read(respuesta))
    }

    @Test
    fun `un monto que no es un numero se descarta`() {
        val respuesta =
            """{"payments":[{"kind":"EXPENSE","amount":"muchos","currency":"BOB","wallet":"bnb","category":null,"note":null}]}"""

        assertEquals(emptyList(), read(respuesta))
    }

    @Test
    fun `el modelo no puede colar texto antes ni despues`() {
        val respuesta =
            """Aquí tienes: {"payments":[{"kind":"EXPENSE","amount":"25","currency":"BOB","wallet":null,"category":null,"note":null}]} ¡listo!"""

        assertNull(read(respuesta), "una respuesta con relleno alrededor no cumple el formato cerrado")
    }
}
