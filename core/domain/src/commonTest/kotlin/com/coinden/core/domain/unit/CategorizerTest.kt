package com.coinden.core.domain.unit

import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.Categorizer
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.CategoryRule
import com.coinden.core.domain.model.category.CategorySuggestion
import com.coinden.core.domain.model.category.SuggestionReason
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.interpret.merged
import com.coinden.core.domain.support.TestPacks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategorizerTest {
    private val es = listOf(TestPacks.english, TestPacks.spanish).merged()
    private val en = listOf(TestPacks.english).merged()
    private val pt = listOf(TestPacks.english, TestPacks.portuguese).merged()

    private val food = CategoryRef.Base(BaseCategory.FOOD)
    private val leisure = CategoryRef.Base(BaseCategory.LEISURE)

    private fun rule(
        key: String,
        category: CategoryRef,
    ) = CategoryRule("carlos", key, category)

    @Test
    fun `palabra tipica del paquete da MEDIA`() {
        assertEquals(
            CategorySuggestion(food, Confidence.MEDIUM, SuggestionReason.PackWord("almuerzo")),
            Categorizer.suggest("almuerzo", emptyList(), es),
        )
        assertEquals(
            CategoryRef.Base(BaseCategory.SUBSCRIPTIONS),
            Categorizer.suggest("StreamingPremium650-2530000US", emptyList(), es)?.category,
        )
        assertEquals(CategoryRef.Base(BaseCategory.HEALTH), Categorizer.suggest("FARMACIA EJEMPLO S.A.", emptyList(), es)?.category)
    }

    @Test
    fun `la regla del usuario gana sobre el paquete`() {
        val suggestion = Categorizer.suggest("salteña", listOf(rule("saltena", leisure)), es)
        assertEquals(CategorySuggestion(leisure, Confidence.HIGH, SuggestionReason.UserRule("saltena")), suggestion)
    }

    @Test
    fun `descripcion desconocida no sugiere nada`() {
        assertNull(Categorizer.suggest("Mariela Quispe Rojas", emptyList(), es))
        assertNull(Categorizer.suggest(null, emptyList(), es))
        assertNull(Categorizer.suggest("   ", emptyList(), es))
    }

    @Test
    fun `ingles y portugues`() {
        assertEquals(food, Categorizer.suggest("lunch", emptyList(), en)?.category)
        assertEquals(food, Categorizer.suggest("almoço", emptyList(), pt)?.category)
        assertEquals(CategoryRef.Base(BaseCategory.TRANSPORT), Categorizer.suggest("uber", emptyList(), pt)?.category)
    }

    @Test
    fun `la regla aplica si esta contenida como palabras completas`() {
        val rules = listOf(rule("pollos del valle", food))
        assertEquals(food, Categorizer.suggest("POLLOS DEL VALLE - sucursal centro", rules, es)?.category)
        assertNull(Categorizer.suggest("POLLOS DEL VALLEGRANDE", rules, es))
    }

    @Test
    fun `gana la regla mas larga`() {
        val subscriptions = CategoryRef.Base(BaseCategory.SUBSCRIPTIONS)
        val rules = listOf(rule("pago", CategoryRef.Base(BaseCategory.BILLS)), rule("pago netflix", subscriptions))
        assertEquals(subscriptions, Categorizer.suggest("pago netflix", rules, es)?.category)
    }

    @Test
    fun `una regla hacia una categoria propia archivada se ignora`() {
        val custom = CategoryRef.Custom("semillitas")
        val rules = listOf(rule("semillita", custom))
        assertEquals(custom, Categorizer.suggest("semillita", rules, es)?.category)
        val fallback = Categorizer.suggest("semillita", rules, es, archivedCustomIds = setOf("semillitas"))
        assertEquals(food, fallback?.category) // vuelve a la palabra del paquete
        assertEquals(Confidence.MEDIUM, fallback?.confidence)
    }

    @Test
    fun `trasmporte con dos letras de diferencia no coincide`() {
        assertNull(Categorizer.suggest("trasmporte", emptyList(), es))
        assertEquals(
            CategoryRef.Base(BaseCategory.TRANSPORT),
            // con una letra de diferencia si coincide
            Categorizer.suggest("trasporte", emptyList(), es)?.category,
        )
    }
}
