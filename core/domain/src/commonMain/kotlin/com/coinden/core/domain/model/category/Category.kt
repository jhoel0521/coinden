package com.coinden.core.domain.model.category

import com.coinden.core.domain.model.draft.Confidence

/**
 * Categorías base, identificadas sin idioma: la presentación las traduce ("Comida", "Food"...).
 * "Sin categoría" no es una categoría: es la ausencia de [CategoryRef].
 */
enum class BaseCategory {
    FOOD,
    GROCERIES,
    TRANSPORT,
    HEALTH,
    SUBSCRIPTIONS,
    BILLS,
    HOUSING,
    LEISURE,
    SHOPPING,
    EDUCATION,
    PEOPLE,
    FEES,
}

/** Referencia a una categoría: base o propia del usuario. */
sealed interface CategoryRef {
    data class Base(
        val category: BaseCategory,
    ) : CategoryRef

    data class Custom(
        val id: String,
    ) : CategoryRef
}

/** Categoría creada por el usuario; su nombre se guarda tal como lo escribió. Nunca se borra. */
data class CustomCategory(
    val id: String,
    val ownerId: String,
    val name: String,
    val archived: Boolean = false,
)

/** De dónde salió la categoría de un gasto. */
enum class CategoryOrigin {
    /** Una regla aprendida del usuario. */
    RULE,

    /** Una palabra típica del paquete de idioma. */
    PACK,

    /** La eligió el usuario. */
    USER,
}

/** Regla aprendida: una descripción normalizada recomienda una categoría. */
data class CategoryRule(
    val ownerId: String,
    val key: String,
    val category: CategoryRef,
)

/** Por qué se recomendó una categoría, para poder explicarlo. */
sealed interface SuggestionReason {
    data class UserRule(
        val key: String,
    ) : SuggestionReason

    data class PackWord(
        val word: String,
    ) : SuggestionReason
}

data class CategorySuggestion(
    val category: CategoryRef,
    val confidence: Confidence,
    val reason: SuggestionReason,
)

/** Motivos de rechazo de la categorización. Sin textos. */
sealed interface CategorizationError {
    data object NotFound : CategorizationError

    data object NotAnExpense : CategorizationError

    data object CategoryArchived : CategorizationError

    data object InvalidName : CategorizationError
}
