package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.CorrectionOrigin
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldChange
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.TimelineEntry
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.CorrectMovementUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetMovementHistoryUseCase
import com.coinden.core.domain.usecase.draft.GetMovementTimelineUseCase
import com.coinden.core.domain.usecase.draft.ResolveSameMovementUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: el historial de Carlos (Bolivia). Muestra lo vigente, del más reciente al
 * más antiguo, y cada movimiento cuenta qué le pasó: qué leyó el comprobante y qué aprobó Carlos.
 */
class CarlosHistoryFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val packs = FakeInstalledPacks(listOf(TestPacks.spanish), TestPacks.templates)
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val categories = CategorizationContext(InMemoryCategorizationRepository(), wallets, packs, ids, clock, device)
    private val ctx = DraftContext(InMemoryDraftRepository(), wallets, InMemoryWalletAliasRepository(), ids, clock, device, categories)
    private val capture = CaptureDraftUseCase(ctx, TextDraftInterpreter(packs, FixedRegionalSettings()))
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val correct = CorrectMovementUseCase(ctx, ledger)
    private val history = GetMovementHistoryUseCase(ctx)
    private val timeline = GetMovementTimelineUseCase(ctx)

    private val account = AnonymizedReceipts.CARLOS_BE_ACCOUNT
    private lateinit var bnb: String

    private fun bolivia(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L + minute * 60L) * 1_000L

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun carlos() {
        clock.nowEpochMs = bolivia(1, 7)
        bnb =
            assertIs<WalletOperationResult.WalletCreated>(
                CreateWalletUseCase(ledger)("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, bs("500")),
            ).wallet.id
        SetWalletAliasesUseCase(ctx)("carlos", bnb, setOf("bnb", account))
    }

    private suspend fun confirmed(
        phrase: String,
        at: Long,
    ): Draft.Confirmed {
        clock.nowEpochMs = at
        val draft = capture("carlos", Evidence(EvidenceSource.VOICE, phrase), at).single()
        return assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(confirm(draft.id, CategoryRef.Base(BaseCategory.FOOD))).draft)
    }

    /** Pago QR con la estructura del Banco Económico y datos inventados. */
    private fun qrReceipt(
        amount: String,
        time: String,
        reference: String,
    ) = """
        Banco ECONOMICO
        Pago QR otros bancos
        Procesada
        Beneficiario
        RESTAURANTE EJEMPLO
        Importe
        Bs$amount
        Remitente
        Cuenta origen: CA: $account (Bs)
        Fecha y hora: $time
        Nro. de transacción: $reference
        """.trimIndent()

    @Test
    fun `el historial muestra lo vigente del mas reciente al mas antiguo`() =
        runTest {
            carlos()
            val lunch = confirmed("pagué 35 de almuerzo bnb", bolivia(12, 12, 40))
            val taxi = confirmed("pagué 10 de taxi bnb", bolivia(13, 8))
            clock.nowEpochMs = bolivia(17, 21)
            correct(
                lunch.id,
                MovementProposal.Expense(ProposedAmount("32", Currency.BOB), WalletRef.ById(bnb), bolivia(12, 12, 40), "almuerzo"),
                null,
            )

            val entries = history("carlos")

            assertEquals(listOf(taxi.id, lunch.id), entries.map { it.draft.id })
            assertEquals(bs("32"), (entries[1].draft.movement as ReadyMovement.Expense).amount)
            assertEquals(CategoryRef.Base(BaseCategory.FOOD), entries[0].classification?.category)
        }

    @Test
    fun `la linea de tiempo cuenta que el comprobante leyo 320 y Carlos aprobo 32`() =
        runTest {
            carlos()
            val lunch = confirmed("pagué 35 de almuerzo bnb", bolivia(12, 12, 40))
            clock.nowEpochMs = bolivia(17, 20)
            // El OCR perdió la coma: leyó 320.00
            val receipt =
                capture(
                    "carlos",
                    Evidence(EvidenceSource.TEXT, qrReceipt("320.00", "12/Oct/2026 12:41:00", "400000001")),
                ).single()
            ResolveSameMovementUseCase(ctx)(receipt.id, lunch.id, same = true)
            clock.nowEpochMs = bolivia(17, 21)
            val approved =
                MovementProposal.Expense(
                    ProposedAmount("32", Currency.BOB),
                    WalletRef.ById(bnb),
                    bolivia(12, 12, 41),
                    "almuerzo",
                    reference = "400000001",
                )
            assertIs<DraftResult.Ok>(
                correct(lunch.id, approved, CategoryRef.Base(BaseCategory.FOOD), CorrectionOrigin.EVIDENCE, receipt.id),
            )

            val entries = timeline(lunch.id)

            assertIs<TimelineEntry.Confirmed>(entries[0])
            val fix = assertIs<TimelineEntry.Corrected>(entries[1])
            assertEquals(CorrectionOrigin.EVIDENCE, fix.origin)
            assertEquals(bs("320"), (fix.read as ReadyMovement.Expense).amount)
            assertEquals(FieldChange.Amount(bs("35"), bs("32"), large = false), fix.changes.first())
            assertEquals(FieldChange.Reference(null, "400000001"), fix.changes.last())
            assertEquals(receipt.header.evidence, fix.evidence)
        }
}
