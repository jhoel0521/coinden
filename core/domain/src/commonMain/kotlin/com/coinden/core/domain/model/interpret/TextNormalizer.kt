package com.coinden.core.domain.model.interpret

/** Una palabra del texto: su forma normalizada para comparar y su forma original para conservar. */
data class Word(
    val original: String,
    val normalized: String,
)

/**
 * Normalización determinista para comparar palabras clave sin depender del idioma:
 * minúsculas, sin tildes ni cedillas y sin signos. El texto original nunca se modifica.
 */
object TextNormalizer {
    private val FOLD: Map<Char, Char> =
        buildMap {
            "áàâäãå".forEach { put(it, 'a') }
            "éèêë".forEach { put(it, 'e') }
            "íìîï".forEach { put(it, 'i') }
            "óòôöõ".forEach { put(it, 'o') }
            "úùûü".forEach { put(it, 'u') }
            put('ç', 'c')
            put('ñ', 'n')
        }

    fun normalize(text: String): String =
        text
            .lowercase()
            .map { FOLD[it] ?: it }
            .joinToString("")
            .replace(Regex("""[^\p{L}\p{N}$/.,\s-]"""), " ")
            .replace(Regex("""(?<![\p{N}])[.,]|[.,](?![\p{N}])"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    /** Palabras separadas por espacios, con su original (sin signos de borde) y su forma normalizada. */
    fun words(text: String): List<Word> =
        text
            .split(Regex("""\s+"""))
            .map { it.trim('¡', '!', '¿', '?', ',', ';', ':', '"', '\'', '(', ')') }
            .filter { it.isNotEmpty() }
            .map { Word(it, normalize(it)) }
            .filter { it.normalized.isNotEmpty() }

    /** ¿[a] y [b] difieren en a lo sumo una inserción, borrado o sustitución? */
    fun withinOneEdit(
        a: String,
        b: String,
    ): Boolean {
        if (a == b) return true
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        val (short, long) = if (a.length <= b.length) a to b else b to a
        var i = 0
        var j = 0
        var edits = 0
        while (i < short.length && j < long.length) {
            if (short[i] == long[j]) {
                i++
                j++
                continue
            }
            if (++edits > 1) return false
            if (short.length == long.length) i++
            j++
        }
        return edits + (long.length - j) + (short.length - i) <= 1
    }

    /**
     * ¿La palabra normalizada coincide con alguna candidata? Con [fuzzy] acepta una edición de
     * diferencia solo en palabras de 4 letras o más ("page" ~ "pague").
     */
    fun matches(
        word: String,
        candidates: Set<String>,
        fuzzy: Boolean,
    ): Boolean = word in candidates || (fuzzy && word.length >= 4 && candidates.any { it.length >= 4 && withinOneEdit(word, it) })
}
