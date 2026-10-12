package com.coinden.core.domain.model.category

import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.TextNormalizer

/**
 * Recomienda la categoría de un gasto a partir de su descripción, de forma pura y explicable:
 * 1. la regla aprendida del usuario que coincide (la más larga) -> ALTA;
 * 2. la primera palabra típica de los paquetes de idioma activos -> MEDIA;
 * 3. nada -> null (Sin categoría).
 */
object Categorizer {
    fun suggest(
        description: String?,
        rules: List<CategoryRule>,
        pack: LanguagePack,
        archivedCustomIds: Set<String> = emptySet(),
    ): CategorySuggestion? {
        val normalized = description?.let { TextNormalizer.normalize(it) }?.takeIf { it.isNotBlank() } ?: return null

        fun usable(category: CategoryRef) = category !is CategoryRef.Custom || category.id !in archivedCustomIds

        rules
            .filter { usable(it.category) && matchesRule(normalized, it.key) }
            .maxByOrNull { it.key.length }
            ?.let { return CategorySuggestion(it.category, Confidence.HIGH, SuggestionReason.UserRule(it.key)) }

        TextNormalizer.words(description).forEach { word ->
            val category =
                pack.categoryWords[word.normalized]
                    ?: pack.categoryWords.entries
                        .firstOrNull { (candidate, _) -> TextNormalizer.matches(word.normalized, setOf(candidate), fuzzy = true) }
                        ?.value
            if (category != null) {
                return CategorySuggestion(CategoryRef.Base(category), Confidence.MEDIUM, SuggestionReason.PackWord(word.normalized))
            }
        }
        // Palabras pegadas a marcas ("StreamingPremium650"): se busca la palabra típica al inicio del token.
        TextNormalizer.words(description).forEach { word ->
            pack.categoryWords.entries
                .filter { (candidate, _) -> candidate.length >= 5 && word.normalized.startsWith(candidate) }
                .maxByOrNull { it.key.length }
                ?.let { return CategorySuggestion(CategoryRef.Base(it.value), Confidence.MEDIUM, SuggestionReason.PackWord(it.key)) }
        }
        return null
    }

    /** Regla igual a la descripción o contenida en ella como palabras completas. */
    fun matchesRule(
        normalizedDescription: String,
        key: String,
    ): Boolean = normalizedDescription == key || " $normalizedDescription ".contains(" $key ")

    /** Clave de una regla aprendida a partir de una descripción. */
    fun ruleKey(description: String): String? = TextNormalizer.normalize(description).takeIf { it.isNotBlank() }
}
