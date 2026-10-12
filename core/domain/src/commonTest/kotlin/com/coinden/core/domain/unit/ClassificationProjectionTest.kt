package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.InMemoryCategorizationRepository
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryOrigin
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.Classification
import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.category.ClassificationProjection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ClassificationProjectionTest {
    private val food = CategoryRef.Base(BaseCategory.FOOD)
    private val leisure = CategoryRef.Base(BaseCategory.LEISURE)

    private fun event(
        id: String,
        time: Long,
        description: String?,
        category: CategoryRef?,
        origin: CategoryOrigin? = CategoryOrigin.USER,
    ) = ClassificationEvent(id, "draft:d-1", "carlos", "celular", time, time, description, category, origin)

    @Test
    fun `sin eventos no hay clasificacion`() {
        assertNull(ClassificationProjection.current(emptyList()))
    }

    @Test
    fun `la vigente es la mas reciente sin importar el orden de llegada`() {
        val first = event("c-1", 100, "salteña", food)
        val second = event("c-2", 200, "salteña", leisure)
        val expected = Classification("draft:d-1", "salteña", leisure, CategoryOrigin.USER)
        assertEquals(expected, ClassificationProjection.current(listOf(first, second)))
        assertEquals(expected, ClassificationProjection.current(listOf(second, first)))
    }

    @Test
    fun `una descripcion nula conserva la anterior`() {
        val events = listOf(event("c-1", 100, "POLLOS DEL VALLE", null, null), event("c-2", 200, null, food))
        assertEquals(Classification("draft:d-1", "POLLOS DEL VALLE", food, CategoryOrigin.USER), ClassificationProjection.current(events))
    }

    @Test
    fun `las clasificaciones son append-only`() =
        runTest {
            val repo = InMemoryCategorizationRepository()
            val e = event("c-1", 100, "x", food)
            repo.appendClassification(e)
            assertFailsWith<IllegalStateException> { repo.appendClassification(e.copy(category = leisure)) }
            assertEquals(listOf(e), repo.allClassifications)
        }
}
