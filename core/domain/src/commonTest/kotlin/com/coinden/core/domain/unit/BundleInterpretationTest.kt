package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.bundle.ReviewReason
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.BundleInterpretation
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.BundleInterpreterPort
import com.coinden.core.domain.port.output.BundlePayment
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.port.output.IntentInterpreterPort
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Lo que pasa cuando hablan los dos: el modelo entiende y las reglas verifican.
 *
 * Es el punto donde se cumple el principio 4 del Manifiesto. Si coinciden, gana la lectura del
 * modelo, que es más rica. Si no, **no se elige en silencio**: el borrador va a revisar con las
 * dos versiones. Y sin modelo, la app sigue con el respaldo.
 */
class BundleInterpretationTest {
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val directory = WalletDirectory(listOf(bnb), mapOf("bnb" to "bnb"), defaultWalletId = "bnb")
    private val categorias = setOf("comida")

    private val comprobante =
        BundleText("e1", EvidenceRole.RECEIPT, EvidenceSource.PHOTO, "Pago QR Importe Bs25.00")
    private val textos = listOf(comprobante)

    private fun gasto(
        monto: String,
        nota: String? = null,
        moneda: Currency? = Currency.BOB,
    ) = MovementProposal.Expense(ProposedAmount(monto, moneda), WalletRef.ById("bnb"), note = nota)

    private fun pago(
        clave: String,
        proposal: MovementProposal,
    ) = BundlePayment(clave, proposal, setOf("e1"))

    private class Reglas(
        private val salida: List<BundlePayment>,
    ) : BundleInterpreterPort {
        override suspend fun interpret(
            texts: List<BundleText>,
            wallets: WalletDirectory,
        ) = salida
    }

    private class Modelo(
        private val salida: List<BundlePayment>?,
    ) : IntentInterpreterPort {
        override suspend fun interpret(
            texts: List<BundleText>,
            wallets: WalletDirectory,
            categories: Set<String>,
        ) = salida
    }

    private suspend fun interpretar(
        delModelo: List<BundlePayment>?,
        deLasReglas: List<BundlePayment>,
        conModelo: Boolean = true,
    ) = BundleInterpretation(
        model = if (conModelo) Modelo(delModelo) else null,
        rules = Reglas(deLasReglas),
    ).interpret(textos, directory, categorias)

    @Test
    fun `si coinciden en los hechos gana la lectura del modelo`() =
        runTest {
            val pagos =
                interpretar(
                    delModelo = listOf(pago("p1", gasto("25", nota = "comida"))),
                    deLasReglas = listOf(pago("p1", gasto("25"))),
                )

            val gasto = assertIs<MovementProposal.Expense>(pagos.single().proposal)
            assertEquals("comida", gasto.note, "la nota del modelo explica mejor el pago")
            assertEquals(emptySet(), pagos.single().reviewReasons, "si coinciden, no hay nada que revisar")
        }

    @Test
    fun `si no coinciden en el monto va a revisar con las dos lecturas`() =
        runTest {
            // Los dos montos están dichos: el comprobante trae 25 y el audio menciona 60. No es que
            // el modelo invente (eso lo frena el validador): es que entendieron cosas distintas.
            val dicho = BundleText("e2", EvidenceRole.CONTEXT, EvidenceSource.VOICE, "gasté 60 en la comida")
            val pagos =
                BundleInterpretation(
                    model = Modelo(listOf(pago("p1", gasto("60", nota = "comida")))),
                    rules = Reglas(listOf(pago("p1", gasto("25")))),
                ).interpret(listOf(comprobante, dicho), directory, categorias)

            val desacuerdo =
                pagos
                    .single()
                    .reviewReasons
                    .filterIsInstance<ReviewReason.ModelDisagrees>()
                    .single()
            assertEquals("60", desacuerdo.fromModel)
            assertEquals("25", desacuerdo.fromRules)
        }

    @Test
    fun `ante el desacuerdo se conserva lo que leyeron las reglas del comprobante`() =
        runTest {
            val pagos =
                interpretar(
                    delModelo = listOf(pago("p1", gasto("60"))),
                    deLasReglas = listOf(pago("p1", gasto("25"))),
                )

            val gasto = assertIs<MovementProposal.Expense>(pagos.single().proposal)
            assertEquals("25", gasto.amount?.value, "el hecho del comprobante manda sobre lo que entendió el modelo")
        }

    @Test
    fun `sin modelo queda la lectura de las reglas`() =
        runTest {
            val pagos = interpretar(delModelo = null, deLasReglas = listOf(pago("p1", gasto("25"))), conModelo = false)

            assertEquals("25", (pagos.single().proposal as MovementProposal.Expense).amount?.value)
            assertEquals(emptySet(), pagos.single().reviewReasons)
        }

    @Test
    fun `si el modelo no contesta queda la lectura de las reglas`() =
        runTest {
            val pagos = interpretar(delModelo = null, deLasReglas = listOf(pago("p1", gasto("25"))))

            assertEquals("25", (pagos.single().proposal as MovementProposal.Expense).amount?.value)
        }

    @Test
    fun `una propuesta del modelo que no se puede verificar se descarta`() =
        runTest {
            // 45 no está en ningún texto del paquete: el validador no la respalda.
            val pagos =
                interpretar(
                    delModelo = listOf(pago("p1", gasto("45", nota = "comida"))),
                    deLasReglas = listOf(pago("p1", gasto("25"))),
                )

            val gasto = assertIs<MovementProposal.Expense>(pagos.single().proposal)
            assertEquals("25", gasto.amount?.value)
            assertTrue(pagos.single().reviewReasons.contains(ReviewReason.UnverifiedProposal))
        }

    @Test
    fun `un pago que solo vio el modelo se conserva, para revisar`() =
        runTest {
            val dicho = listOf(BundleText("e2", EvidenceRole.CONTEXT, EvidenceSource.VOICE, "y le di 25 al taxi"))
            val pagos =
                BundleInterpretation(
                    model = Modelo(listOf(pago("p1", gasto("25")), pago("p2", gasto("25", nota = "taxi")))),
                    rules = Reglas(listOf(pago("p1", gasto("25")))),
                ).interpret(textos + dicho, directory, categorias)

            assertEquals(2, pagos.size, "un pago que las reglas no vieron no se pierde")
            assertTrue(pagos.first { it.paymentKey == "p2" }.reviewReasons.isNotEmpty(), "pero queda para revisar")
        }

    @Test
    fun `un pago que solo vieron las reglas no se pierde`() =
        runTest {
            val pagos =
                interpretar(
                    delModelo = listOf(pago("p1", gasto("25"))),
                    deLasReglas = listOf(pago("p1", gasto("25")), pago("p2", gasto("25"))),
                )

            assertEquals(2, pagos.size)
        }

    @Test
    fun `sin nada que interpretar no hay pagos`() =
        runTest {
            assertEquals(emptyList(), interpretar(delModelo = emptyList(), deLasReglas = emptyList()))
        }
}
