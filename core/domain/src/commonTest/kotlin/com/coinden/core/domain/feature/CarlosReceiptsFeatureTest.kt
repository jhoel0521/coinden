package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Escenario de Feature: Carlos (Bolivia) comparte sus comprobantes y dicta sus gastos hormiga.
 *
 * Los 5 comprobantes tienen la estructura real de Yape y del Banco Económico con datos inventados.
 * El intérprete real los convierte en borradores; en la noche la bandeja muestra primero lo dudoso
 * (frases sin billetera y el destino sugerido del retiro) y Carlos confirma todo.
 */
class CarlosReceiptsFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val drafts = InMemoryDraftRepository()
    private val aliases = InMemoryWalletAliasRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = DraftContext(drafts, wallets, aliases, ids, clock, device)

    private val interpreter =
        TextDraftInterpreter(
            FakeInstalledPacks(listOf(LanguagePacks.SPANISH), ReceiptTemplates.all),
            // es-BO, UTC-4
            FixedRegionalSettings(),
        )
    private val capture = CaptureDraftUseCase(ctx, interpreter)
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val inbox = GetDraftInboxUseCase(ctx)
    private val status = GetWalletStatusUseCase(wallets)

    /** Hora local de Bolivia (UTC-4) en octubre de 2026, como epoch ms. */
    private fun bolivia(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L + minute * 60L) * 1_000L

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    @Test
    fun `comprobantes y frases reales interpretados, revisados y confirmados`() =
        runTest {
            clock.nowEpochMs = bolivia(1, 8)
            val create = CreateWalletUseCase(ledger)

            suspend fun wallet(
                name: String,
                opening: String,
                type: WalletType,
            ) = assertIs<WalletOperationResult.WalletCreated>(create("carlos", name, Currency.BOB, type, bs(opening))).wallet
            val billetera = wallet("Billetera", "20", WalletType.CASH)
            val yape = wallet("Yape", "100", WalletType.DIGITAL)
            val be = wallet("BE Bs", "1000", WalletType.BANK)
            SetWalletAliasesUseCase(ctx).let {
                it("carlos", yape.id, setOf("yape"))
                it("carlos", be.id, setOf(AnonymizedReceipts.CARLOS_BE_ACCOUNT))
            }

            // En la noche del 11 comparte sus comprobantes y dicta dos gastos hormiga
            val captures =
                listOf(
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.YAPE_PAYMENT, "img-1"),
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_QR_CHICKEN, "img-2"),
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_ECOMMERCE_DEBIT, "img-3"),
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_QR_PHARMACY, "img-4"),
                    Evidence(EvidenceSource.PHOTO, AnonymizedReceipts.BE_CASH_WITHDRAWAL, "img-5"),
                    Evidence(EvidenceSource.VOICE, "page 3 bs para trasmporte"),
                    Evidence(EvidenceSource.TEXT, "gaste 2 para un semillita"),
                )
            val created =
                captures.mapIndexed { i, evidence ->
                    clock.nowEpochMs = bolivia(11, 21, i)
                    capture("carlos", evidence).single()
                }
            assertTrue(created.all { it is Draft.Ready }, "todos deberian quedar Listos: $created")

            // Confianza: los comprobantes con todo en la evidencia son ALTA; lo sugerido, BAJA
            val confidences = created.map { it.confidence }
            assertEquals(List(4) { Confidence.HIGH }, confidences.take(4))
            assertEquals(Confidence.LOW, confidences[4]) // retiro: el destino "efectivo" es sugerido
            assertEquals(listOf(Confidence.LOW, Confidence.LOW), confidences.drop(5))

            // La bandeja muestra primero lo dudoso
            val pending = inbox("carlos").pending
            assertEquals(7, pending.size)
            assertEquals(List(3) { Confidence.LOW } + List(4) { Confidence.HIGH }, pending.map { it.confidence })

            // Confirma todo en la noche
            pending.forEach { draft -> assertIs<DraftResult.Ok>(confirm(draft.id), "confirmar ${draft.header.evidence}") }
            assertTrue(inbox("carlos").pending.isEmpty())

            // Saldos exactos
            assertEquals(bs("64.00"), status(yape.id)!!.balance) // 100 - 36
            assertEquals(bs("814.59"), status(be.id)!!.balance) // 1000 - 25 - 118.71 - 31.70 - 10
            assertEquals(bs("25.00"), status(billetera.id)!!.balance) // 20 + 10 - 3 - 2

            // El gasto de Yape quedó con la hora del comprobante, no la de la captura
            val yapeExpense = wallets.eventsForWallet(yape.id).single { it.id.startsWith("draft:") }
            assertEquals(bolivia(3, 12, 54), yapeExpense.occurredAtEpochMs)
        }
}
