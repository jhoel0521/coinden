package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.bundle.ReviewReason
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.interpret.BundleInterpreter
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.BundlePayment
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.support.TestPacks
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Escenario de Feature (text-interpreter): los casos de la spec, **sin modelo de lenguaje**.
 *
 * Esta prueba marca la línea entre lo que el respaldo determinista resuelve solo y lo que espera
 * al modelo local. Lo que no alcanza a entender no se registra mal: queda para revisar.
 *
 * Los casos que piden entender lenguaje ("un ambulante", "el somosero") no están acá a propósito:
 * son del modelo, y se prueban cuando el motor esté integrado.
 */
class CarlosBundleScenariosFeatureTest {
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val billetera = Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
    private val directory = WalletDirectory(listOf(bnb, billetera), mapOf("bnb" to "bnb"), defaultWalletId = "billetera")

    private val interpreter =
        BundleInterpreter(
            TextDraftInterpreter(FakeInstalledPacks(listOf(TestPacks.spanish), TestPacks.templates), FixedRegionalSettings()),
        )

    private fun receipt(
        id: String,
        text: String,
    ) = BundleText(id, EvidenceRole.RECEIPT, EvidenceSource.TEXT, text)

    private fun said(
        id: String,
        text: String,
    ) = BundleText(id, EvidenceRole.CONTEXT, EvidenceSource.VOICE, text)

    private fun amountOf(p: BundlePayment) =
        when (val proposal = p.proposal) {
            is MovementProposal.Expense -> proposal.amount?.value
            is MovementProposal.Income -> proposal.amount?.value
            else -> null
        }

    private fun noteOf(p: BundlePayment) = (p.proposal as? MovementProposal.Expense)?.note

    @Test
    fun `comprobante sin glosa y su audio dan un solo pago explicado`() =
        runTest {
            val pagos =
                interpreter.interpret(
                    listOf(receipt("e1", "Pago QR Importe Bs25.00"), said("e2", "esto es de mi comida")),
                    directory,
                )

            assertEquals(1, pagos.size, "un comprobante más lo que lo explica es un pago, no dos")
            assertEquals("25.00", amountOf(pagos.single()))
            assertTrue(noteOf(pagos.single())?.contains("comida") == true)
        }

    @Test
    fun `un paquete sin comprobante se entiende como la frase que es`() =
        runTest {
            val pagos = interpreter.interpret(listOf(said("e1", "gasté 45 en almuerzo con bnb")), directory)

            assertEquals("45", amountOf(pagos.single()))
        }

    @Test
    fun `la frase explica el unico pago, sin motivo de revision`() =
        runTest {
            val pagos =
                interpreter.interpret(
                    listOf(receipt("e1", "Pago QR Importe Bs25.00"), said("e2", "útiles del colegio")),
                    directory,
                )

            assertEquals(emptySet(), pagos.single().reviewReasons, "no hay nada dudoso que revisar")
            assertTrue(noteOf(pagos.single())?.contains("colegio") == true)
        }

    @Test
    fun `dos pagos y una frase dejan la nota en los dos, para revisar`() =
        runTest {
            val pagos =
                interpreter.interpret(
                    listOf(
                        receipt("e1", "Pago QR Importe Bs40.00"),
                        receipt("e2", "Pago QR Importe Bs78.00"),
                        said("e3", "estaba comiendo"),
                    ),
                    directory,
                )

            assertEquals(2, pagos.size)
            assertTrue(
                pagos.all { ReviewReason.ContextForSeveral in it.reviewReasons },
                "lo dicho puede ser de cualquiera de los dos: decide la persona",
            )
        }

    @Test
    fun `un comprobante equivocado queda para revisar con las dos versiones`() =
        runTest {
            val pagos =
                interpreter.interpret(
                    listOf(receipt("e1", "Pago QR Importe Bs25.00"), said("e2", "gasté 1500")),
                    directory,
                )

            assertTrue(
                pagos.any { it.reviewReasons.isNotEmpty() },
                "lo dicho y el comprobante no coinciden: no se elige en silencio",
            )
        }

    @Test
    fun `otro pago entre varios comprobantes no se pierde`() =
        runTest {
            val pagos =
                interpreter.interpret(
                    listOf(
                        receipt("e1", "Pago QR Importe Bs25.00"),
                        receipt("e2", "Pago QR Importe Bs60.00"),
                        said("e3", "y le pagué 15 al taxi"),
                    ),
                    directory,
                )

            assertEquals(3, pagos.size, "los dos comprobantes más el pago que solo se dijo")
            val taxi = pagos.first { amountOf(it) == "15" }
            assertTrue(taxi.reviewReasons.isNotEmpty(), "un pago sin comprobante va a revisar")
        }

    @Test
    fun `una foto que no se pudo leer no impide usar lo que se dijo`() =
        runTest {
            // La foto no dejó texto: solo queda lo que Carlos escribió.
            val pagos = interpreter.interpret(listOf(said("e1", "farmacia 33 bnb")), directory)

            assertEquals("33", amountOf(pagos.single()))
        }

    @Test
    fun `el mismo pago dicho dos veces sigue siendo un pago`() =
        runTest {
            val pagos =
                interpreter.interpret(
                    listOf(receipt("e1", "Pago QR Importe Bs60.00"), said("e2", "60 de mi comida")),
                    directory,
                )

            assertEquals(1, pagos.size, "lo dicho coincide con el comprobante: lo explica, no lo duplica")
        }
}
