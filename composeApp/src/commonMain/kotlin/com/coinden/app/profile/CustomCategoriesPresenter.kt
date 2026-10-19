package com.coinden.app.profile

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.category.CategorizationError
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.usecase.category.ArchiveCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.CategorizationResult
import com.coinden.core.domain.usecase.category.CreateCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.ReactivateCustomCategoryUseCase
import com.coinden.core.domain.usecase.category.RenameCustomCategoryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CategoriesError { EMPTY_NAME, NOT_FOUND, STORAGE }

data class CustomCategoriesState(
    val loaded: Boolean = false,
    val busy: Boolean = false,
    /** Activas primero (por nombre) y archivadas al final. */
    val categories: List<CustomCategory> = emptyList(),
    val error: CategoriesError? = null,
)

/** Categorías propias de la persona: crear, renombrar, archivar y reactivar. Las reglas viven en el dominio. */
class CustomCategoriesPresenter(
    private val container: AppContainer,
) {
    private val mutableState = MutableStateFlow(CustomCategoriesState())
    val state = mutableState.asStateFlow()

    private val create = CreateCustomCategoryUseCase(container.categorization)
    private val rename = RenameCustomCategoryUseCase(container.categorization)
    private val archive = ArchiveCustomCategoryUseCase(container.categorization)
    private val reactivate = ReactivateCustomCategoryUseCase(container.categorization)

    suspend fun load() {
        mutableState.update { it.copy(busy = true, error = null) }
        try {
            val all = container.storage.categorization.customCategoriesOf(container.ownerId)
            mutableState.update { it.copy(loaded = true, busy = false, categories = ordered(all)) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableState.update { it.copy(busy = false, error = CategoriesError.STORAGE) }
        }
    }

    suspend fun create(name: String): Boolean = apply { create(container.ownerId, name) }

    suspend fun rename(
        id: String,
        name: String,
    ): Boolean = apply { rename(container.ownerId, id, name) }

    suspend fun archive(id: String): Boolean = apply { archive(container.ownerId, id) }

    suspend fun reactivate(id: String): Boolean = apply { reactivate(container.ownerId, id) }

    /** Ejecuta un caso de uso y recarga; un rechazo queda como aviso traducido en la pantalla. */
    private suspend fun apply(action: suspend () -> CategorizationResult<CustomCategory>): Boolean {
        mutableState.update { it.copy(busy = true, error = null) }
        val result =
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(busy = false, error = CategoriesError.STORAGE) }
                return false
            }
        return when (result) {
            is CategorizationResult.Ok -> {
                load()
                true
            }
            is CategorizationResult.Rejected -> {
                val error = if (result.error == CategorizationError.InvalidName) CategoriesError.EMPTY_NAME else CategoriesError.NOT_FOUND
                mutableState.update { it.copy(busy = false, error = error) }
                false
            }
        }
    }

    private fun ordered(all: List<CustomCategory>): List<CustomCategory> =
        all.sortedWith(compareBy({ it.archived }, { it.name.lowercase() }))
}
