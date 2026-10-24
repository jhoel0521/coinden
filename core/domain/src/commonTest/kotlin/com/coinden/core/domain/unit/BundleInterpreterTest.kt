package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.FakeInstalledPacks
import com.coinden.core.domain.feature.FixedRegionalSettings
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.BundleInterpreter
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.support.TestPacks
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * El respaldo determinista del paquete: lo que corre cuando no hay modelo de lenguaje.
 *
 * Empareja solo por lo que el dominio puede comparar sin listas de palabras —monto, moneda y
 * medio de pago— y completa con lo que el comprobante no trae, sin pisar nunca lo que sí trae.
 */
class BundleInterpreterTest {
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val billetera = Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
    private val directory =
        WalletDirectory(listOf(bnb, billetera), mapOf("bnb" to "bnb"), defaultWalletId = "billetera")

    private val interpreter =
        BundleInterpreter(TextDraftInterpreter(FakeInstalledPacks(listOf(TestPacks.spanish), TestPacks.templates), FixedRegionalSettings()))

    private fun receipt(
        id: String,
        text: String,
    ) = BundleText(id, EvidenceRole.RECEIPT, EvidenceSource.TEXT, text)

    private fun said(
        id: String,
        text: String,
    ) = BundleText(id, EvidenceRole.CONTEXT, EvidenceSource.VOICE, text)

    private fun walletOf(proposal: MovementProposal) = (proposal as? MovementProposal.Expense)?.wallet

    @Test
    fun `una frase sin monto que nombra una billetera la aporta al pago`() =
        runTest {
            // El comprobante no identifica de dónde salió el dinero; el audio sí.
            val pagos =
                interpreter.interpret(
                    listOf(receipt("e1", "Pago QR Importe Bs25.00"), said("e2", "lo pagué con bnb")),
                    directory,
                )

            val pago = pagos.single()
            assertEquals(WalletRef.ByHint("bnb"), walletOf(pago.proposal), "la billetera que dijo Carlos no se pierde")
        }

    @Test
    fun `la frase sin monto tambien deja su nota`() =
        runTest {
            val pagos =
                interpreter.interpret(
                    listOf(receipt("e1", "Pago QR Importe Bs25.00"), said("e2", "esto es de mi comida")),
                    directory,
                )

            val gasto = assertIs<MovementProposal.Expense>(pagos.single().proposal)
            assertNotNull(gasto.note)
        }

    @Test
    fun `una billetera dicha no pisa la que identifica el comprobante`() =
        runTest {
            // El comprobante de Yape identifica su billetera; lo dicho no puede cambiarla.
            val pagos =
                interpreter.interpret(
                    listOf(receipt("e1", "¡Yapeaste! Bs 36 Nro. de transaccion: 900000001"), said("e2", "lo pagué con bnb")),
                    directory,
                )

            val wallet = walletOf(pagos.single().proposal)
            assertEquals(WalletRef.ByHint("yape"), wallet, "el hecho del comprobante manda sobre lo dicho")
        }

    @Test
    fun `el monto del comprobante nunca cambia por lo que se diga`() =
        runTest {
            val pagos =
                interpreter.interpret(
                    listOf(receipt("e1", "Pago QR Importe Bs25.00"), said("e2", "lo pagué con bnb")),
                    directory,
                )

            val gasto = assertIs<MovementProposal.Expense>(pagos.single().proposal)
            assertEquals("25.00", gasto.amount?.value)
            assertEquals(Currency.BOB, gasto.amount?.currency)
        }
}
