package com.coinden.app.profile

import com.coinden.app.testContainer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomCategoriesPresenterTest {
    @Test
    fun `Carlos crea Mascota, la renombra y la ve en la lista`() =
        runTest {
            val presenter = CustomCategoriesPresenter(testContainer())
            presenter.load()

            assertTrue(presenter.create("Mascota"))
            val id =
                presenter.state.value.categories
                    .single()
                    .id
            assertTrue(presenter.rename(id, "Mascotas"))

            assertEquals(
                listOf("Mascotas"),
                presenter.state.value.categories
                    .map { it.name },
            )
        }

    @Test
    fun `las activas van primero y las archivadas al final`() =
        runTest {
            val presenter = CustomCategoriesPresenter(testContainer())
            presenter.load()
            presenter.create("Semillitas")
            presenter.create("Mascotas")
            val seeds =
                presenter.state.value.categories
                    .first { it.name == "Semillitas" }
                    .id

            presenter.archive(seeds)
            assertEquals(
                listOf("Mascotas", "Semillitas"),
                presenter.state.value.categories
                    .map { it.name },
            )
            assertTrue(
                presenter.state.value.categories
                    .last()
                    .archived,
            )

            presenter.reactivate(seeds)
            assertFalse(
                presenter.state.value.categories
                    .any { it.archived },
            )
        }

    @Test
    fun `un nombre vacio no se guarda y avisa`() =
        runTest {
            val presenter = CustomCategoriesPresenter(testContainer())
            presenter.load()

            assertFalse(presenter.create("   "))

            assertEquals(CategoriesError.EMPTY_NAME, presenter.state.value.error)
            assertTrue(
                presenter.state.value.categories
                    .isEmpty(),
            )
        }
}
