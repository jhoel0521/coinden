package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.FakeInstalledPacks
import com.coinden.core.domain.feature.FixedClock
import com.coinden.core.domain.feature.FixedDevice
import com.coinden.core.domain.feature.InMemoryCategorizationRepository
import com.coinden.core.domain.feature.InMemoryWalletRepository
import com.coinden.core.domain.feature.SequentialIdGenerator
import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategorizationError
import com.coinden.core.domain.model.category.CategoryOrigin
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.Classification
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.category.ArchiveCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.category.CategorizationResult
import com.coinden.core.domain.usecase.category.ClassifyExpenseUseCase
import com.coinden.core.domain.usecase.category.CreateCustomCategoryUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.MovementKind
import com.coinden.core.domain.usecase.wallet.RegisterMovementUseCase
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CategoryUseCasesTest {
    private val wallets = InMemoryWalletRepository()
    private val categorization = InMemoryCategorizationRepository()
    private val ids = SequentialIdGenerator()
    private val clock = FixedClock(at(20))
    private val device = FixedDevice("celular")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = CategorizationContext(categorization, wallets, FakeInstalledPacks(listOf(LanguagePacks.SPANISH)), ids, clock, device)
    private val classify = ClassifyExpenseUseCase(ctx)

    private val food = CategoryRef.Base(BaseCategory.FOOD)
    private val leisure = CategoryRef.Base(BaseCategory.LEISURE)

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun cashWallet(): String =
        assertIs<WalletOperationResult.WalletCreated>(
            CreateWalletUseCase(ledger)("carlos", "Billetera", Currency.BOB, WalletType.CASH, bs("100"), occurredAtEpochMs = at(7)),
        ).wallet.id

    private suspend fun expense(walletId: String): String =
        assertIs<WalletOperationResult.EventRecorded>(
            RegisterMovementUseCase(ledger)(walletId, MovementKind.EXPENSE, bs("8"), at(12)),
        ).event.id

    @Test
    fun `categoria propia con su nombre y archivada sin borrar`() =
        runTest {
            val created =
                assertIs<CategorizationResult.Ok<CustomCategory>>(
                    CreateCustomCategoryUseCase(ctx)("carlos", "  Semillitas "),
                ).value
            assertEquals("Semillitas", created.name)
            assertEquals(
                CategorizationError.InvalidName,
                assertIs<CategorizationResult.Rejected>(CreateCustomCategoryUseCase(ctx)("carlos", " ")).error,
            )

            val custom = CategoryRef.Custom(created.id)
            val wallet = cashWallet()
            val spent = expense(wallet)
            assertIs<CategorizationResult.Ok<Classification>>(classify("carlos", spent, custom, "semillita"))
            assertEquals(Confidence.HIGH, ctx.suggest("carlos", "semillita")?.confidence)

            ArchiveCustomCategoryUseCase(ctx)("carlos", created.id)
            // Archivada: no se sugiere ni se ofrece para gastos nuevos...
            assertEquals(food, ctx.suggest("carlos", "semillita")?.category)
            assertEquals(
                CategorizationError.CategoryArchived,
                assertIs<CategorizationResult.Rejected>(classify("carlos", expense(wallet), custom)).error,
            )
            // ...pero el gasto que ya la tenía la conserva
            assertEquals(custom, ctx.current(spent)?.category)
        }

    @Test
    fun `reclasificar aprende la regla y no toca el libro`() =
        runTest {
            val wallet = cashWallet()
            val spent = expense(wallet)
            val ledgerBefore = wallets.eventsForWallet(wallet)

            classify("carlos", spent, food, "salteña")
            assertEquals(food, ctx.suggest("carlos", "salteña")?.category)

            clock.nowEpochMs = at(21)
            val result = assertIs<CategorizationResult.Ok<Classification>>(classify("carlos", spent, leisure))
            assertEquals(Classification(spent, "salteña", leisure, CategoryOrigin.USER), result.value)
            assertEquals(leisure, ctx.suggest("carlos", "salteña")?.category)
            assertEquals(2, categorization.classificationsFor(spent).size)
            assertEquals(ledgerBefore, wallets.eventsForWallet(wallet))
        }

    @Test
    fun `solo se categorizan gastos existentes del mismo dueno`() =
        runTest {
            val cash = cashWallet()
            val bank =
                assertIs<WalletOperationResult.WalletCreated>(
                    CreateWalletUseCase(ledger)("carlos", "BNB", Currency.BOB, WalletType.BANK, bs("500"), occurredAtEpochMs = at(7)),
                ).wallet.id
            val transfer =
                assertIs<WalletOperationResult.EventRecorded>(
                    RegisterTransferUseCase(ledger)(bank, cash, bs("200"), bs("200"), at(11)),
                ).event.id

            assertEquals(
                CategorizationError.NotAnExpense,
                assertIs<CategorizationResult.Rejected>(classify("carlos", transfer, food)).error,
            )
            assertEquals(CategorizationError.NotFound, assertIs<CategorizationResult.Rejected>(classify("carlos", "no-existe", food)).error)
            assertEquals(CategorizationError.NotFound, assertIs<CategorizationResult.Rejected>(classify("otro", expense(cash), food)).error)
        }
}
