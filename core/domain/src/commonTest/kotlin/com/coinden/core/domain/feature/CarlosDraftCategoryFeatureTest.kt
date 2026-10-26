package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmReadyDraftsUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.ReviseDraftUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Escenario de Feature (issue #14): Carlos elige la categoría de un borrador, lo guarda sin
 * confirmar y la categoría no se pierde: la ve al reabrirlo y el gasto se clasifica con ella al
 * confirmarlo después.
 */
class CarlosDraftCategoryFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val packs = FakeInstalledPacks(listOf(TestPacks.spanish), TestPacks.templates)
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val categories = CategorizationContext(InMemoryCategorizationRepository(), wallets, packs, ids, clock, device)
    private val ctx = DraftContext(InMemoryDraftRepository(), wallets, InMemoryWalletAliasRepository(), ids, clock, device, categories)
    private val capture = CaptureDraftUseCase(ctx, TextDraftInterpreter(packs, FixedRegionalSettings()))
    private val revise = ReviseDraftUseCase(ctx)
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val pets = CategoryRef.Custom("cat-mascotas")
    private val food = CategoryRef.Base(BaseCategory.FOOD)

    private suspend fun ready(text: String): Draft.Ready {
        val bnb =
            assertIs<WalletOperationResult.WalletCreated>(
                CreateWalletUseCase(ledger)("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, Money.parseCanonical("500", Currency.BOB)),
            ).wallet.id
        SetWalletAliasesUseCase(ctx)("carlos", bnb, setOf("bnb"))
        val id = capture("carlos", Evidence(EvidenceSource.TEXT, text)).single().id
        return assertIs<Draft.Ready>(ctx.load(id))
    }

    /** Guardar sin confirmar, como el botón Guardar del formulario. */
    private suspend fun keep(
        draft: Draft.Ready,
        category: CategoryRef?,
    ) = revise(draft.id, draft.proposal, category)

    @Test
    fun `guardar con Mascotas y reabrir muestra Mascotas`() =
        runTest {
            val vet = ready("veterinario 30 bnb")

            keep(vet, pets)

            assertEquals(pets, ctx.load(vet.id)!!.header.chosenCategory)
        }

    @Test
    fun `guardar con comida y confirmar despues clasifica como comida`() =
        runTest {
            val food40 = ready("croquetas 40 bnb")
            keep(food40, food)

            confirm(food40.id)

            assertEquals(food, categories.current(ConfirmDraftUseCase.ledgerEventIdFor(food40.id))?.category)
        }

    @Test
    fun `guardar otra categoria reemplaza la anterior`() =
        runTest {
            val vet = ready("veterinario 30 bnb")
            keep(vet, pets)

            keep(assertIs<Draft.Ready>(ctx.load(vet.id)), CategoryRef.Base(BaseCategory.HEALTH))

            assertEquals(CategoryRef.Base(BaseCategory.HEALTH), ctx.load(vet.id)!!.header.chosenCategory)
        }

    @Test
    fun `sin categoria elegida no hay categoria guardada`() =
        runTest {
            val lunch = ready("almuerzo 25 bnb")

            keep(lunch, null)

            assertNull(ctx.load(lunch.id)!!.header.chosenCategory)
        }

    @Test
    fun `una categoria elegida al confirmar gana a la guardada`() =
        runTest {
            val vet = ready("veterinario 30 bnb")
            keep(vet, pets)

            confirm(vet.id, food)

            assertEquals(food, categories.current(ConfirmDraftUseCase.ledgerEventIdFor(vet.id))?.category)
        }

    @Test
    fun `confirmar los listos de una vez respeta la categoria guardada`() =
        runTest {
            // "almuerzo" sugiere comida, pero Carlos lo guardó como salidas.
            val lunch = ready("almuerzo 25 bnb")
            keep(lunch, CategoryRef.Base(BaseCategory.LEISURE))

            ConfirmReadyDraftsUseCase(ctx, ledger)(listOf(lunch.id))

            assertEquals(
                CategoryRef.Base(BaseCategory.LEISURE),
                categories.current(ConfirmDraftUseCase.ledgerEventIdFor(lunch.id))?.category,
            )
        }
}
