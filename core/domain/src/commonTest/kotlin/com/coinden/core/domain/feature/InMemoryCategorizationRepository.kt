package com.coinden.core.domain.feature

import com.coinden.core.domain.model.category.CategoryRule
import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.port.output.CategorizationRepositoryPort

/** Categorización en memoria: clasificaciones append-only, reglas y categorías como estado. */
class InMemoryCategorizationRepository : CategorizationRepositoryPort {
    private val classifications = mutableListOf<ClassificationEvent>()
    private val rules = mutableMapOf<Pair<String, String>, CategoryRule>()
    private val custom = mutableMapOf<String, CustomCategory>()

    override suspend fun appendClassification(event: ClassificationEvent) {
        check(classifications.none { it.id == event.id }) { "Las clasificaciones son append-only: ${event.id} ya existe" }
        classifications += event
    }

    override suspend fun classificationsFor(movementId: String): List<ClassificationEvent> =
        classifications.filter {
            it.movementId == movementId
        }

    override suspend fun classificationsOf(ownerId: String): List<ClassificationEvent> = classifications.filter { it.ownerId == ownerId }

    override suspend fun saveRule(rule: CategoryRule) {
        rules[rule.ownerId to rule.key] = rule
    }

    override suspend fun rulesOf(ownerId: String): List<CategoryRule> = rules.values.filter { it.ownerId == ownerId }

    override suspend fun saveCustomCategory(category: CustomCategory) {
        custom[category.id] = category
    }

    override suspend fun customCategoriesOf(ownerId: String): List<CustomCategory> = custom.values.filter { it.ownerId == ownerId }

    val allClassifications: List<ClassificationEvent> get() = classifications.toList()
}
