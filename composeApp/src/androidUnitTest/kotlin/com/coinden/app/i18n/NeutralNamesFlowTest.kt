package com.coinden.app.i18n

import com.coinden.app.AppContainer
import com.coinden.app.capture.MovementKindView
import com.coinden.app.capture.label
import com.coinden.app.history.HistoryPresenter
import com.coinden.app.onboarding.label
import com.coinden.app.seedCarlos
import com.coinden.app.settings.WalletSettingsPresenter
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Cambiar los nombres visibles no cambia el tipo, los saldos ni los movimientos guardados. */
class NeutralNamesFlowTest {
    private val es =
        Regex("""<string name="([^"]+)">([^<]*)</string>""")
            .findAll(File("src/commonMain/composeResources/values/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private fun bs(value: String) = Money.parseCanonical(value, Currency.BOB)

    private fun usd(value: String) = Money.parseCanonical(value, Currency.USD)

    private suspend fun AppContainer.walletId(name: String) =
        drafts
            .directory(ownerId)
            .wallets
            .single { it.name == name }
            .id

    private suspend fun AppContainer.balanceOf(name: String): Money {
        val id = walletId(name)
        return walletBalances(ownerId).single { it.walletId == id }.balance
    }

    @Test
    fun `Carlos ve su billetera de tipo banco como Cuenta con el mismo saldo`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val p = WalletSettingsPresenter(c).also { it.load() }
            val bnb =
                p.state.value.wallets
                    .single { it.wallet.name == "BNB Bs" }
            assertEquals(WalletType.BANK, bnb.wallet.type)
            assertEquals(
                "Cuenta",
                es.getValue(
                    bnb.wallet.type
                        .label()
                        .key,
                ),
            )
            assertEquals(bs("500"), bnb.balance)
        }

    @Test
    fun `Facundo agrega una Tarjeta con deuda inicial de -25,00 ARS`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val p = WalletSettingsPresenter(c).also { it.load() }
            assertTrue(p.addCurrency("ARS"))
            assertTrue(p.addWallet("Visa Galicia", WalletType.CREDIT, "ARS", "-25.00"))
            val visa =
                p.state.value.wallets
                    .single { it.wallet.name == "Visa Galicia" }
            assertEquals(
                "Tarjeta",
                es.getValue(
                    visa.wallet.type
                        .label()
                        .key,
                ),
            )
            assertEquals(Money.parseCanonical("-25.00", Currency.ARS), visa.balance)
        }

    @Test
    fun `Carlos ve el Traspaso de 50,00 USD a 594,00 Bs en el historial con los mismos saldos`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            assertTrue(
                WalletSettingsPresenter(c).also { it.load() }.run {
                    addCurrency("USD") &&
                        addWallet("Meru", WalletType.DIGITAL, "USD", "100")
                },
            )
            val captured = c.captureManual(c.ownerId, Evidence(EvidenceSource.MANUAL)).single()
            c.reviseDraft(
                captured.id,
                MovementProposal.Transfer(
                    ProposedAmount("50.00", Currency.USD),
                    WalletRef.ById(c.walletId("Meru")),
                    into = ProposedAmount("594.00", Currency.BOB),
                    to = WalletRef.ById(c.walletId("BNB Bs")),
                ),
            )
            assertIs<DraftResult.Ok>(c.confirmDraft(captured.id))

            val history = HistoryPresenter(c).also { it.load() }
            val move =
                history.state.value.items
                    .single()
            assertEquals(MovementKindView.TRANSFER, move.kind)
            assertEquals("Traspaso", es.getValue(move.kind.label().key))
            assertEquals(usd("50.00"), move.amount)
            assertEquals(usd("50.00"), c.balanceOf("Meru"))
            assertEquals(bs("1094.00"), c.balanceOf("BNB Bs"))
        }
}
