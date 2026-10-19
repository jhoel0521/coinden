package com.coinden.core.domain.feature

import com.coinden.core.domain.model.category.CategorizationError
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.usecase.category.ArchiveCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.category.CategorizationResult
import com.coinden.core.domain.usecase.category.CreateCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.ReactivateCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.RenameCustomCategoryUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: Carlos (Bolivia) arma sus propias categorías. Crea "Mascota", se da
 * cuenta de que prefiere "Mascotas" y la renombra (los gastos guardan la categoría por su id, así
 * que todos muestran el nombre nuevo), la archiva cuando ya no la usa y la reactiva cuando vuelve.
 */
class CarlosCustomCategoriesFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val packs = FakeInstalledPacks(listOf(LanguagePacks.SPANISH), ReceiptTemplates.all)
    private val repository = InMemoryCategorizationRepository()
    private val categories = CategorizationContext(repository, wallets, packs, ids, clock, device)
    private val create = CreateCustomCategoryUseCase(categories)
    private val rename = RenameCustomCategoryUseCase(categories)
    private val archive = ArchiveCustomCategoryUseCase(categories)
    private val reactivate = ReactivateCustomCategoryUseCase(categories)

    private suspend fun mine(): List<CustomCategory> = repository.customCategoriesOf("carlos")

    @Test
    fun `Carlos renombra Mascota a Mascotas y la categoria conserva su id`() =
        runTest {
            val pets = assertIs<CategorizationResult.Ok<CustomCategory>>(create("carlos", "Mascota")).value

            val renamed = assertIs<CategorizationResult.Ok<CustomCategory>>(rename("carlos", pets.id, "  Mascotas ")).value

            assertEquals(pets.id, renamed.id)
            assertEquals(listOf("Mascotas"), mine().map { it.name })
        }

    @Test
    fun `archivar y reactivar devuelve la categoria a las activas`() =
        runTest {
            val seeds = assertIs<CategorizationResult.Ok<CustomCategory>>(create("carlos", "Semillitas")).value
            archive("carlos", seeds.id)
            assertEquals(true, mine().single().archived)

            assertIs<CategorizationResult.Ok<CustomCategory>>(reactivate("carlos", seeds.id))

            assertEquals(false, mine().single().archived)
        }

    @Test
    fun `un nombre vacio no se guarda y una categoria ajena no se encuentra`() =
        runTest {
            val pets = assertIs<CategorizationResult.Ok<CustomCategory>>(create("carlos", "Mascotas")).value

            assertEquals(CategorizationError.InvalidName, assertIs<CategorizationResult.Rejected>(rename("carlos", pets.id, "   ")).error)
            assertEquals(CategorizationError.NotFound, assertIs<CategorizationResult.Rejected>(rename("john", pets.id, "Pets")).error)
            assertEquals(CategorizationError.NotFound, assertIs<CategorizationResult.Rejected>(reactivate("carlos", "nada")).error)
            assertEquals("Mascotas", mine().single().name)
        }
}
