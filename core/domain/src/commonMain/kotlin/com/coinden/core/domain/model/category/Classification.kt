package com.coinden.core.domain.model.category

/**
 * Clasificación de un gasto, guardada aparte del libro de dinero (append-only, sync-ready).
 * Cambiar la categoría agrega otro evento: el dinero nunca se toca.
 *
 * [description] es la nota o el comercio; null conserva la descripción anterior.
 * [category] null = Sin categoría.
 */
data class ClassificationEvent(
    val id: String,
    val movementId: String,
    val ownerId: String,
    val deviceId: String,
    val occurredAtEpochMs: Long,
    val recordedAtEpochMs: Long,
    val description: String?,
    val category: CategoryRef?,
    val origin: CategoryOrigin?,
)

/** Clasificación vigente de un gasto. */
data class Classification(
    val movementId: String,
    val description: String?,
    val category: CategoryRef?,
    val origin: CategoryOrigin?,
)

/**
 * Reproduce las clasificaciones de un gasto en orden (ocurrencia, registro, id): la vigente es la
 * más reciente, y una descripción null conserva la anterior.
 */
object ClassificationProjection {
    private val CHRONOLOGICAL: Comparator<ClassificationEvent> =
        compareBy<ClassificationEvent>({ it.occurredAtEpochMs }, { it.recordedAtEpochMs }, { it.id })

    fun current(events: List<ClassificationEvent>): Classification? {
        var result: Classification? = null
        events.sortedWith(CHRONOLOGICAL).forEach { e ->
            result = Classification(e.movementId, e.description ?: result?.description, e.category, e.origin)
        }
        return result
    }
}
