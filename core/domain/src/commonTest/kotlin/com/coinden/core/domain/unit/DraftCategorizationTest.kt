package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.FakeInstalledPacks
import com.coinden.core.domain.feature.FixedClock
import com.coinden.core.domain.feature.FixedDevice
import com.coinden.core.domain.feature.InMemoryCategorizationRepository
import com.coinden.core.domain.feature.InMemoryDraftRepository
import com.coinden.core.domain.feature.InMemoryWalletAliasRepository
import com.coinden.core.domain.feature.InMemoryWalletRepository
import com.coinden.core.domain.feature.ScriptedInterpreter
import com.coinden.core.domain.feature.SequentialIdGenerator
import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryOrigin
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.CategorySuggestion
import com.coinden.core.domain.model.category.Classification
import com.coinden.core.domain.model.category.SuggestionReason
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DraftCategorizationTest {
    private val wallets = InMemoryWalletRepository()
    private val categorization = InMemoryCategorizationRepository()
    private val ids = SequentialIdGenerator()
    private val clock = FixedClock(at(12))
    private val device = FixedDevice("celular")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val categories =
        CategorizationContext(categorization, wallets, FakeInstalledPacks(listOf(LanguagePacks.SPANISH)), ids, clock, device)
    private val ctx = DraftContext(InMemoryDraftRepository(), wallets, InMemoryWalletAliasRepository(), ids, clock, device, categories)

    private val food = CategoryRef.Base(BaseCategory.FOOD)

    private suspend fun captureExpense(note: String): String {
        val wallet =
            wallets.listWallets("carlos").firstOrNull()?.id
                ?: assertIs<WalletOperationResult.WalletCreated>(
                    CreateWalletUseCase(ledger)(
                        "carlos",
                        "Billetera",
                        Currency.BOB,
                        WalletType.CASH,
                        Money.parseCanonical("500", Currency.BOB),
                        occurredAtEpochMs = at(7),
                    ),
                ).wallet.id
        val interpreter =
            ScriptedInterpreter(mapOf(note to listOf(MovementProposal.Expense(ProposedAmount("25"), WalletRef.ById(wallet), note = note))))
        return CaptureDraftUseCase(ctx, interpreter)("carlos", Evidence(EvidenceSource.TEXT, note)).single().id
    }

    @Test
    fun `la bandeja sugiere la categoria con su motivo`() =
        runTest {
            val draft = captureExpense("almuerzo")
            val suggestion = GetDraftInboxUseCase(ctx)("carlos").suggestions[draft]
            assertEquals(CategorySuggestion(food, Confidence.MEDIUM, SuggestionReason.PackWord("almuerzo")), suggestion)
        }

    @Test
    fun `confirmar con categoria clasifica con la nota y aprende`() =
        runTest {
            val draft = captureExpense("POLLOS DEL VALLE")
            assertEquals(null, GetDraftInboxUseCase(ctx)("carlos").suggestions[draft])

            assertIs<DraftResult.Ok>(ConfirmDraftUseCase(ctx, ledger)(draft, food))
            val movementId = ConfirmDraftUseCase.ledgerEventIdFor(draft)
            assertEquals(Classification(movementId, "POLLOS DEL VALLE", food, CategoryOrigin.USER), categories.current(movementId))

            val next = captureExpense("POLLOS DEL VALLE")
            val learned = GetDraftInboxUseCase(ctx)("carlos").suggestions[next]
            assertEquals(Confidence.HIGH, learned?.confidence)
            assertEquals(food, learned?.category)
        }

    @Test
    fun `confirmar sin categoria registra el gasto como Sin categoria`() =
        runTest {
            val draft = captureExpense("Mariela Quispe Rojas")
            assertIs<DraftResult.Ok>(ConfirmDraftUseCase(ctx, ledger)(draft))
            val movementId = ConfirmDraftUseCase.ledgerEventIdFor(draft)
            assertIs<LedgerEvent.Expense>(wallets.findEvent(movementId))
            assertEquals(Classification(movementId, "Mariela Quispe Rojas", null, null), categories.current(movementId))
            assertEquals(emptyList(), categorization.rulesOf("carlos"))
        }

    @Test
    fun `la confirmacion idempotente no duplica la clasificacion`() =
        runTest {
            val draft = captureExpense("almuerzo")
            val movementId = ConfirmDraftUseCase.ledgerEventIdFor(draft)
            // Otro dispositivo ya confirmó y clasificó este gasto
            wallets.appendEvent(
                LedgerEvent.Expense(
                    movementId,
                    "pc",
                    at(12),
                    at(21),
                    wallets.listWallets("carlos").single().id,
                    Money.parseCanonical("25", Currency.BOB),
                ),
            )
            categories.classify("carlos", movementId, "almuerzo", food, CategoryOrigin.USER)

            assertIs<DraftResult.Ok>(ConfirmDraftUseCase(ctx, ledger)(draft, CategoryRef.Base(BaseCategory.LEISURE)))
            assertEquals(1, categorization.classificationsFor(movementId).size)
            assertEquals(food, categories.current(movementId)?.category)
        }
}
