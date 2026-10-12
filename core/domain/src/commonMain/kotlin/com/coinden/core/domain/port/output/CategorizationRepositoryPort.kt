package com.coinden.core.domain.port.output

import com.coinden.core.domain.model.category.CategoryRule
import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.category.CustomCategory

/**
 * Puerto de Salida (SPI) de la categorización. Las clasificaciones son append-only; las reglas y
 * las categorías propias son metadatos que se guardan como estado actual.
 */
interface CategorizationRepositoryPort {
    suspend fun appendClassification(event: ClassificationEvent)

    suspend fun classificationsFor(movementId: String): List<ClassificationEvent>

    suspend fun classificationsOf(ownerId: String): List<ClassificationEvent>

    /** Guarda la regla, reemplazando la de la misma clave del mismo dueño. */
    suspend fun saveRule(rule: CategoryRule)

    suspend fun rulesOf(ownerId: String): List<CategoryRule>

    suspend fun saveCustomCategory(category: CustomCategory)

    suspend fun customCategoriesOf(ownerId: String): List<CustomCategory>
}
