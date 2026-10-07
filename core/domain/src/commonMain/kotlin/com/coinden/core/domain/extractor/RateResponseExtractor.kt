package com.coinden.core.domain.extractor

/**
 * Contrato de extracción de tipos de cambio numéricos a partir de payloads remotos (JSON o HTML).
 */
interface RateResponseExtractor {
    fun extract(
        payload: String,
        selector: String,
    ): Double?
}

/**
 * Extractor para respuestas JSON que soporta notación de puntos (dot-notation).
 *
 * Ejemplos:
 * - "venta" sobre {"venta": 1420.50} -> 1420.50
 * - "rates.EUR" sobre {"rates": {"EUR": 0.92}} -> 0.92
 * - "data.blue.sell" sobre {"data": {"blue": {"sell": 11.88}}} -> 11.88
 * - Soporta tanto números numéricos puros (11.88) como cadenas ("11.88").
 */
class JsonDotNotationExtractor : RateResponseExtractor {
    override fun extract(
        payload: String,
        selector: String,
    ): Double? {
        if (payload.isBlank() || selector.isBlank()) return null

        val pathSegments = selector.trim().split(".").map { it.trim() }.filter { it.isNotEmpty() }
        if (pathSegments.isEmpty()) return null

        var currentScope = payload

        // Navegar por los segmentos intermedios (objetos anidados)
        for (i in 0 until pathSegments.size - 1) {
            val segment = pathSegments[i]
            val objectRegex = Regex("""["']?${Regex.escape(segment)}["']?\s*:\s*\{""", RegexOption.IGNORE_CASE)
            val match = objectRegex.find(currentScope) ?: return null

            // Extraer el bloque del objeto balanceando llaves
            val startIndex = match.range.last
            val block = extractJsonBlock(currentScope, startIndex, '{', '}') ?: return null
            currentScope = block
        }

        // Extraer el valor hoja (último segmento)
        val leafKey = pathSegments.last()
        val valueRegex =
            Regex(
                """["']?${Regex.escape(leafKey)}["']?\s*:\s*["']?([0-9]+(?:[.,][0-9]+)?)["']?""",
                RegexOption.IGNORE_CASE,
            )

        val leafMatch = valueRegex.find(currentScope) ?: return null
        val rawValue = leafMatch.groupValues[1].replace(',', '.')
        return rawValue.toDoubleOrNull()
    }

    private fun extractJsonBlock(
        text: String,
        openBraceIndex: Int,
        openChar: Char,
        closeChar: Char,
    ): String? {
        var depth = 1
        var inQuotes = false
        var escape = false

        for (i in (openBraceIndex + 1) until text.length) {
            val char = text[i]
            if (escape) {
                escape = false
                continue
            }
            if (char == '\\') {
                escape = true
                continue
            }
            if (char == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (char == openChar) {
                    depth++
                } else if (char == closeChar) {
                    depth--
                    if (depth == 0) {
                        return text.substring(openBraceIndex, i + 1)
                    }
                }
            }
        }
        return null
    }
}

/**
 * Extractor para páginas HTML y respuestas web no estructuradas mediante expresiones regulares.
 *
 * Diseñado para casos reales como dolarbluebolivia.click:
 * - Soporta expresiones regulares con grupo de captura: `id="usdRate">[^<]*?(\d+(?:\.\d+)?)`
 * - Si no hay grupo de captura explícito, busca el primer valor numérico contenido en la coincidencia.
 * - Soporta separadores decimales universales (. y ,).
 */
class HtmlRegexExtractor : RateResponseExtractor {
    override fun extract(
        payload: String,
        selector: String,
    ): Double? {
        if (payload.isBlank() || selector.isBlank()) return null

        try {
            val regex = Regex(selector, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val match = regex.find(payload) ?: return null

            val rawNumeric =
                if (match.groupValues.size > 1 && match.groupValues[1].isNotBlank()) {
                    // Hay grupo de captura explícito
                    match.groupValues[1]
                } else {
                    // Si no hay grupo de captura, buscar el primer número dentro de la coincidencia o en la ventana adyacente
                    val windowEnd = (match.range.last + 100).coerceAtMost(payload.length)
                    val searchScope = payload.substring(match.range.first, windowEnd)
                    val numberRegex = Regex("""([0-9]+(?:[.,][0-9]+)?)""")
                    val numMatch = numberRegex.find(searchScope) ?: return null
                    numMatch.groupValues[1]
                }

            val normalized = rawNumeric.trim().replace(',', '.')
            return normalized.toDoubleOrNull()
        } catch (_: Exception) {
            return null
        }
    }
}
