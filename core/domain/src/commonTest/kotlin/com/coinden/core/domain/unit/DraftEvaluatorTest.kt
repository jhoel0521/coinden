package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftEvaluator
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.DraftHeader
import com.coinden.core.domain.model.draft.DraftIssue
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.NonMovementIntent
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DraftEvaluatorTest {
    private val billetera = Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val colchon = Wallet("colchon", "carlos", "Dólares colchón", Currency.USD, WalletType.CASH)
    private val viejo = Wallet("viejo", "carlos", "Cuenta vieja", Currency.BOB, WalletType.BANK).archived()

    private val directory =
        WalletDirectory(
            wallets = listOf(billetera, bnb, colchon, viejo),
            aliases = mapOf("bnb" to "bnb", "billetera" to "billetera"),
        )

    private val header = DraftHeader("d-1", "carlos", Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb"), at(12, 58))

    private fun evaluate(proposal: MovementProposal) = DraftEvaluator.evaluate(header, proposal, directory)

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private fun usd(canonical: String) = Money.parseCanonical(canonical, Currency.USD)

    @Test
    fun `gasto completo queda Listo con la hora de captura`() {
        val draft = evaluate(MovementProposal.Expense(ProposedAmount("45", Currency.BOB), WalletRef.ById("bnb"), note = "almuerzo"))
        val ready = assertIs<Draft.Ready>(draft)
        assertEquals(ReadyMovement.Expense(bs("45.00"), "bnb", at(12, 58), "almuerzo"), ready.movement)
    }

    @Test
    fun `monto sin moneda toma la de la billetera`() {
        val ready = assertIs<Draft.Ready>(evaluate(MovementProposal.Expense(ProposedAmount("45"), WalletRef.ById("billetera"))))
        assertEquals(bs("45.00"), (ready.movement as ReadyMovement.Expense).amount)
    }

    @Test
    fun `alias en mayusculas se resuelve`() {
        val ready = assertIs<Draft.Ready>(evaluate(MovementProposal.Expense(ProposedAmount("45"), WalletRef.ByHint(" BNB "))))
        assertEquals("bnb", (ready.movement as ReadyMovement.Expense).walletId)
    }

    @Test
    fun `hora de ocurrencia propuesta tiene prioridad`() {
        val ready = assertIs<Draft.Ready>(evaluate(MovementProposal.Income(ProposedAmount("3500"), WalletRef.ById("bnb"), at(9))))
        assertEquals(at(9), (ready.movement as ReadyMovement.Income).occurredAtEpochMs)
    }

    @Test
    fun `monto no canonico o no positivo es invalido`() {
        listOf("45,50", "Bs 45", "0", "-5").forEach { raw ->
            val draft = assertIs<Draft.Incomplete>(evaluate(MovementProposal.Expense(ProposedAmount(raw), WalletRef.ById("billetera"))))
            assertEquals(setOf(DraftIssue.INVALID_AMOUNT), draft.issues, "monto '$raw'")
        }
    }

    @Test
    fun `moneda que no coincide con la billetera`() {
        val draft =
            assertIs<Draft.Incomplete>(evaluate(MovementProposal.Expense(ProposedAmount("12", Currency.USD), WalletRef.ById("bnb"))))
        assertEquals(setOf(DraftIssue.CURRENCY_MISMATCH), draft.issues)
    }

    @Test
    fun `pista desconocida, id desconocido y faltantes`() {
        val hint = assertIs<Draft.Incomplete>(evaluate(MovementProposal.Expense(ProposedAmount("5"), WalletRef.ByHint("tigo"))))
        assertEquals(setOf(DraftField.WALLET), hint.missing)
        assertEquals(setOf(DraftIssue.UNKNOWN_WALLET_HINT), hint.issues)

        val id = assertIs<Draft.Incomplete>(evaluate(MovementProposal.Expense(ProposedAmount("5"), WalletRef.ById("nada"))))
        assertEquals(setOf(DraftIssue.UNKNOWN_WALLET), id.issues)

        val empty = assertIs<Draft.Incomplete>(evaluate(MovementProposal.Expense()))
        assertEquals(setOf(DraftField.AMOUNT, DraftField.WALLET), empty.missing)
    }

    @Test
    fun `billetera archivada impide estar Listo`() {
        val draft = assertIs<Draft.Incomplete>(evaluate(MovementProposal.Expense(ProposedAmount("5"), WalletRef.ById("viejo"))))
        assertEquals(setOf(DraftIssue.WALLET_ARCHIVED), draft.issues)
    }

    @Test
    fun `cambio de moneda Listo con tasa 11_88`() {
        val proposal =
            MovementProposal.Transfer(
                out = ProposedAmount("50", Currency.USD),
                from = WalletRef.ById("colchon"),
                into = ProposedAmount("594"),
                to = WalletRef.ByHint("billetera"),
            )
        val ready = assertIs<Draft.Ready>(evaluate(proposal))
        val transfer = assertIs<ReadyMovement.Transfer>(ready.movement)
        assertEquals(bs("594.00"), transfer.into)
        assertEquals(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.88"), transfer.rate)
    }

    @Test
    fun `retiro de cajero sin monto de llegada usa el mismo monto en la misma moneda`() {
        val proposal =
            MovementProposal.Transfer(
                out = ProposedAmount("200"),
                from = WalletRef.ById("bnb"),
                to = WalletRef.ById("billetera"),
            )
        val transfer = assertIs<ReadyMovement.Transfer>(assertIs<Draft.Ready>(evaluate(proposal)).movement)
        assertEquals(bs("200.00"), transfer.out)
        assertEquals(bs("200.00"), transfer.into)
        assertEquals(ExchangeRate.identity(Currency.BOB), transfer.rate)
    }

    @Test
    fun `transferencias invalidas`() {
        val same =
            assertIs<Draft.Incomplete>(
                evaluate(
                    MovementProposal.Transfer(ProposedAmount("10"), WalletRef.ById("bnb"), ProposedAmount("10"), WalletRef.ByHint("bnb")),
                ),
            )
        assertEquals(setOf(DraftIssue.SAME_WALLET), same.issues)

        val inconsistent =
            assertIs<Draft.Incomplete>(
                evaluate(
                    MovementProposal.Transfer(
                        ProposedAmount("200"),
                        WalletRef.ById("bnb"),
                        ProposedAmount("195"),
                        WalletRef.ById("billetera"),
                    ),
                ),
            )
        assertEquals(setOf(DraftIssue.INCONSISTENT_AMOUNTS), inconsistent.issues)

        val missingInto =
            assertIs<Draft.Incomplete>(
                evaluate(
                    MovementProposal.Transfer(
                        ProposedAmount("50", Currency.USD),
                        WalletRef.ById("colchon"),
                        null,
                        WalletRef.ById("billetera"),
                    ),
                ),
            )
        assertEquals(setOf(DraftField.TO_AMOUNT), missingInto.missing)
    }

    @Test
    fun `ajuste a un saldo objetivo`() {
        val ready = assertIs<Draft.Ready>(evaluate(MovementProposal.AdjustTo(ProposedAmount("140"), WalletRef.ById("billetera"))))
        assertEquals(ReadyMovement.AdjustTo(bs("140.00"), "billetera", at(12, 58), null), ready.movement)
    }

    @Test
    fun `lo que no es un movimiento queda No soportado`() {
        val draft =
            assertIs<Draft.Unsupported>(evaluate(MovementProposal.NotAMovement(NonMovementIntent.RECURRING_PAYMENT, "netflix 12 usd")))
        assertEquals(NonMovementIntent.RECURRING_PAYMENT, draft.intent)
        assertEquals("netflix 12 usd", draft.note)
    }

    @Test
    fun `lo que no se entendio queda Incompleto sin tipo`() {
        val draft = assertIs<Draft.Incomplete>(evaluate(MovementProposal.Unknown("HIPERMAXI")))
        assertEquals(setOf(DraftField.KIND), draft.missing)
    }

    @Test
    fun `el cambio de moneda usa el dinero exacto`() {
        val ready =
            assertIs<Draft.Ready>(
                evaluate(
                    MovementProposal.Transfer(
                        ProposedAmount("50.00", Currency.USD),
                        WalletRef.ById("colchon"),
                        ProposedAmount("594.00"),
                        WalletRef.ById("billetera"),
                    ),
                ),
            )
        assertEquals(usd("50.00"), (ready.movement as ReadyMovement.Transfer).out)
    }
}
