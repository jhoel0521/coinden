package com.coinden.app.history

import com.coinden.app.AppContainer
import com.coinden.app.capture.DraftCards
import com.coinden.app.capture.DraftFormPresenter
import com.coinden.app.capture.FormMode
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldChange
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.category.ArchiveCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.CategorizationResult
import com.coinden.core.domain.usecase.category.CreateCustomCategoryUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El historial de Carlos: lo vigente primero lo más reciente, y Corregir abre el formulario. */
class HistoryPresenterTest {
    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun carlos(): Pair<AppContainer, HistoryPresenter> {
        val container = testContainer()
        seedCarlos(container)
        return container to HistoryPresenter(container)
    }

    private suspend fun AppContainer.confirmed(phrase: String): String {
        val draft = captureDraft(ownerId, Evidence(EvidenceSource.VOICE, phrase)).single()
        assertIs<DraftResult.Ok>(confirmDraft(draft.id))
        return draft.id
    }

    @Test
    fun `lo mas reciente primero, y corregir a mano deja vigente solo lo corregido`() =
        runTest {
            val (container, history) = carlos()
            val lunch = container.confirmed("pagué 35 de almuerzo bnb")
            val taxi = container.confirmed("pagué 10 de taxi bnb")
            history.load()
            assertEquals(
                listOf(taxi, lunch),
                history.state.value.items
                    .map { it.draftId },
            )

            val mode =
                history.correct(
                    history.state.value.items
                        .single { it.draftId == lunch },
                )
            assertEquals(FormMode.Correct(lunch), mode)
            val form = DraftFormPresenter(container, mode!!).also { it.load() }
            form.onAmountChange("32")
            assertTrue(form.confirm())
            history.load()

            val fixed =
                history.state.value.items
                    .single { it.draftId == lunch }
            assertEquals(bs("32"), fixed.amount)
            assertTrue(fixed.corrected)
            assertEquals(2, history.state.value.items.size)
            assertFalse(
                history.state.value.items
                    .single { it.draftId == taxi }
                    .corrected,
            )
        }

    @Test
    fun `Carlos John y Sophie ven y conservan el nombre de sus categorias propias en tarjetas historial y formulario`() =
        runTest {
            val (carlos, history) = carlos()
            val pets =
                assertIs<CategorizationResult.Ok<CustomCategory>>(
                    CreateCustomCategoryUseCase(carlos.categorization)(carlos.ownerId, "Mascotas"),
                ).value
            val vet = carlos.captureDraft(carlos.ownerId, Evidence(EvidenceSource.VOICE, "pagué 80 de veterinaria bnb")).single()
            assertIs<DraftResult.Ok>(carlos.confirmDraft(vet.id, CategoryRef.Custom(pets.id)))

            val pending = carlos.captureDraft(carlos.ownerId, Evidence(EvidenceSource.VOICE, "pagué 45 de veterinaria bnb")).single()
            val card = DraftCards(carlos).of(listOf(pending)).single()
            assertEquals(CategoryRef.Custom(pets.id), card.category?.ref)
            assertEquals("Mascotas", card.category?.customName)

            history.load()
            val item =
                history.state.value.items
                    .single()
            assertEquals(CategoryRef.Custom(pets.id), item.category)
            assertEquals("Mascotas", item.customCategoryName)

            val form = DraftFormPresenter(carlos, FormMode.Correct(vet.id)).also { it.load() }
            assertEquals(CategoryRef.Custom(pets.id), form.state.value.category)
            assertEquals("Mascotas", form.state.value.customCategoryName(pets.id))
            form.onAmountChange("75")
            assertTrue(form.confirm())

            history.load()
            assertEquals(
                CategoryRef.Custom(pets.id),
                history.state.value.items
                    .single()
                    .category,
            )
            assertEquals(
                "Mascotas",
                history.state.value.items
                    .single()
                    .customCategoryName,
            )

            val compare = DraftFormPresenter(carlos, FormMode.Correct(vet.id)).also { it.load() }
            compare.onCategoryChange(CategoryRef.Base(BaseCategory.FOOD))
            assertTrue(
                FieldChange.Category(CategoryRef.Custom(pets.id), CategoryRef.Base(BaseCategory.FOOD)) in compare.state.value.changes,
            )
            assertEquals("Mascotas", compare.state.value.customCategoryName(pets.id))

            val john = testContainer(ownerId = "john", country = "US", language = "en")
            john.storage.profiles.saveProfile(UserCurrencyProfile(userId = john.ownerId, localCurrency = Currency.USD))
            assertIs<WalletOperationResult.WalletCreated>(
                john.createWallet(john.ownerId, "Cash", Currency.USD, WalletType.CASH, Money.parseCanonical("100", Currency.USD)),
            )
            val mom =
                assertIs<CategorizationResult.Ok<CustomCategory>>(
                    CreateCustomCategoryUseCase(john.categorization)(john.ownerId, "Casa de mamá"),
                ).value
            val gift = john.captureDraft(john.ownerId, Evidence(EvidenceSource.TEXT, "paid 20 cash")).single()
            assertIs<DraftResult.Ok>(john.confirmDraft(gift.id, CategoryRef.Custom(mom.id)))
            john.chooseLanguage("en")
            val johnHistory = HistoryPresenter(john).also { it.load() }
            assertEquals(
                "Casa de mamá",
                johnHistory.state.value.items
                    .single()
                    .customCategoryName,
            )

            val sophie = testContainer(ownerId = "sophie", country = "FR", language = "es")
            sophie.storage.profiles.saveProfile(UserCurrencyProfile(userId = sophie.ownerId, localCurrency = Currency.EUR))
            assertIs<WalletOperationResult.WalletCreated>(
                sophie.createWallet(sophie.ownerId, "Carte", Currency.EUR, WalletType.BANK, Money.parseCanonical("200", Currency.EUR)),
            )
            val archivedCat =
                assertIs<CategorizationResult.Ok<CustomCategory>>(
                    CreateCustomCategoryUseCase(sophie.categorization)(sophie.ownerId, "Atelier"),
                ).value
            val firstDraft = sophie.captureDraft(sophie.ownerId, Evidence(EvidenceSource.TEXT, "pagué 15 carte")).single()
            assertIs<DraftResult.Ok>(sophie.confirmDraft(firstDraft.id, CategoryRef.Custom(archivedCat.id)))
            ArchiveCustomCategoryUseCase(sophie.categorization)(sophie.ownerId, archivedCat.id)
            val secondDraft = sophie.captureDraft(sophie.ownerId, Evidence(EvidenceSource.TEXT, "pagué 10 carte")).single()
            assertIs<DraftResult.Ok>(sophie.confirmDraft(secondDraft.id, CategoryRef.Custom("missing-uuid")))

            val sophieHistory = HistoryPresenter(sophie).also { it.load() }
            val byId =
                sophieHistory.state.value.items
                    .associateBy { it.draftId }
            assertEquals("Atelier", byId.getValue(firstDraft.id).customCategoryName)
            assertNull(byId.getValue(secondDraft.id).customCategoryName)
            val unknownForm = DraftFormPresenter(sophie, FormMode.Correct(secondDraft.id)).also { it.load() }
            assertNull(unknownForm.state.value.customCategoryName("missing-uuid"))
        }
}
