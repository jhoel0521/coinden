package com.coinden.core.domain.usecase.category

import com.coinden.core.domain.model.category.CategorizationError
import com.coinden.core.domain.model.category.Categorizer
import com.coinden.core.domain.model.category.CategoryOrigin
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.CategoryRule
import com.coinden.core.domain.model.category.CategorySuggestion
import com.coinden.core.domain.model.category.Classification
import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.category.ClassificationProjection
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.merged
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.port.output.CategorizationRepositoryPort
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.IdGeneratorPort
import com.coinden.core.domain.port.output.InstalledPacksPort
import com.coinden.core.domain.port.output.WalletRepositoryPort

/** Resultado tipado de la categorización. */
sealed interface CategorizationResult<out T> {
    data class Ok<T>(
        val value: T,
    ) : CategorizationResult<T>

    data class Rejected(
        val error: CategorizationError,
    ) : CategorizationResult<Nothing>
}

/** Dependencias comunes de la categorización. */
class CategorizationContext(
    val repository: CategorizationRepositoryPort,
    val wallets: WalletRepositoryPort,
    val packs: InstalledPacksPort,
    val ids: IdGeneratorPort,
    val clock: ClockPort,
    val device: DeviceIdentityPort,
) {
    /** Sugerencia para una descripción con las reglas del usuario y los paquetes instalados. */
    suspend fun suggest(
        ownerId: String,
        description: String?,
    ): CategorySuggestion? {
        val pack = (listOf(LanguagePacks.ENGLISH) + packs.languagePacks().filter { it.code != LanguagePacks.ENGLISH.code }).merged()
        val archived = repository.customCategoriesOf(ownerId).filter { it.archived }.map { it.id }.toSet()
        return Categorizer.suggest(description, repository.rulesOf(ownerId), pack, archived)
    }

    suspend fun current(movementId: String): Classification? = ClassificationProjection.current(repository.classificationsFor(movementId))

    /**
     * Registra una clasificación y, si el usuario eligió una categoría para un gasto con
     * descripción, aprende la regla "descripción -> categoría".
     */
    suspend fun classify(
        ownerId: String,
        movementId: String,
        description: String?,
        category: CategoryRef?,
        origin: CategoryOrigin?,
    ): Classification {
        val now = clock.nowEpochMs()
        repository.appendClassification(
            ClassificationEvent(ids.newId(), movementId, ownerId, device.deviceId(), now, now, description, category, origin),
        )
        val classification = current(movementId)!!
        val key = classification.description?.let { Categorizer.ruleKey(it) }
        if (origin == CategoryOrigin.USER && category != null && key != null) {
            repository.saveRule(CategoryRule(ownerId, key, category))
        }
        return classification
    }

    suspend fun isArchived(
        ownerId: String,
        category: CategoryRef?,
    ): Boolean = category is CategoryRef.Custom && repository.customCategoriesOf(ownerId).any { it.id == category.id && it.archived }
}

/** Caso de Uso: crear una categoría propia con el nombre que escribe el usuario. */
class CreateCustomCategoryUseCase(
    private val ctx: CategorizationContext,
) {
    suspend operator fun invoke(
        ownerId: String,
        name: String,
    ): CategorizationResult<CustomCategory> {
        if (name.isBlank()) return CategorizationResult.Rejected(CategorizationError.InvalidName)
        val category = CustomCategory(ctx.ids.newId(), ownerId, name.trim())
        ctx.repository.saveCustomCategory(category)
        return CategorizationResult.Ok(category)
    }
}

/** Caso de Uso: archivar una categoría propia. No se sugiere más, pero los gastos que la tienen la conservan. */
class ArchiveCustomCategoryUseCase(
    private val ctx: CategorizationContext,
) {
    suspend operator fun invoke(
        ownerId: String,
        categoryId: String,
    ): CategorizationResult<CustomCategory> {
        val category =
            ctx.repository.customCategoriesOf(ownerId).firstOrNull { it.id == categoryId }
                ?: return CategorizationResult.Rejected(CategorizationError.NotFound)
        val archived = category.copy(archived = true)
        ctx.repository.saveCustomCategory(archived)
        return CategorizationResult.Ok(archived)
    }
}

/**
 * Caso de Uso: clasificar o reclasificar un gasto ya registrado (con descripción opcional para los
 * gastos registrados sin borrador). Nunca toca el libro de dinero; aprende la regla.
 */
class ClassifyExpenseUseCase(
    private val ctx: CategorizationContext,
) {
    suspend operator fun invoke(
        ownerId: String,
        movementId: String,
        category: CategoryRef?,
        description: String? = null,
    ): CategorizationResult<Classification> {
        val event = ctx.wallets.findEvent(movementId) ?: return CategorizationResult.Rejected(CategorizationError.NotFound)
        if (event !is LedgerEvent.Expense) return CategorizationResult.Rejected(CategorizationError.NotAnExpense)
        if (ctx.wallets.getWallet(event.walletId)?.ownerId != ownerId) return CategorizationResult.Rejected(CategorizationError.NotFound)
        if (ctx.isArchived(ownerId, category)) return CategorizationResult.Rejected(CategorizationError.CategoryArchived)
        return CategorizationResult.Ok(
            ctx.classify(ownerId, movementId, description?.trim()?.ifEmpty { null }, category, CategoryOrigin.USER),
        )
    }
}
