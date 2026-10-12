package com.coinden.core.domain.model

/**
 * Value Object inmutable que representa una divisa monetaria o criptoactivo.
 *
 * En Arquitectura Hexagonal y DDD:
 * - Es 100% inmutable ('val').
 * - Autovalida sus invariantes en 'init':
 *   - Código de 3 a 6 caracteres alfanuméricos en mayúsculas (ej. "BOB", "USD", "VALE", "BTC").
 *   - Precisión decimal estricta entre 0 y 8 posiciones (0 para CLP/JPY/VALE, 8 para Satoshis de Bitcoin).
 * - Distingue si es una divisa estándar del catálogo o personalizada ([isCustom]).
 * - Sin idioma: las monedas del catálogo solo tienen código y decimales. [symbol] y [name]
 *   existen únicamente en monedas personalizadas, tal como los escribió el usuario; el nombre
 *   y el símbolo de una moneda ISO los resuelve la presentación según el locale.
 */
data class Currency(
    val code: String,
    val symbol: String? = null,
    val decimals: Int = 2,
    val name: String? = null,
    val isCustom: Boolean = false,
) {
    init {
        val trimmed = code.trim()
        require(trimmed.isNotBlank() && trimmed.matches(CODE_REGEX)) {
            "El código de divisa debe tener entre 3 y 6 caracteres alfanuméricos en mayúsculas. Recibido: '$code'"
        }
        require(decimals in 0..8) {
            "La cantidad de decimales debe estar entre 0 y 8 (máximo para precisión Satoshi en Bitcoin). Recibido: $decimals"
        }
    }

    fun isSameAs(other: Currency): Boolean = code.equals(other.code, ignoreCase = true)

    companion object {
        private val CODE_REGEX = Regex("^[A-Z0-9]{3,6}$")

        // Divisas Ancla y Globales Principales
        val USD = Currency(code = "USD", decimals = 2)
        val EUR = Currency(code = "EUR", decimals = 2)
        val CNY = Currency(code = "CNY", decimals = 2)
        val BTC = Currency(code = "BTC", decimals = 8)

        // Divisas Frecuentes de la Región
        val BOB = Currency(code = "BOB", decimals = 2)
        val ARS = Currency(code = "ARS", decimals = 2)
        val CLP = Currency(code = "CLP", decimals = 0)
        val COP = Currency(code = "COP", decimals = 0)
        val PEN = Currency(code = "PEN", decimals = 2)
        val BRL = Currency(code = "BRL", decimals = 2)

        private val PREDEFINED_MAP: Map<String, Currency> =
            listOf(
                USD, EUR, CNY, BTC, BOB, ARS, CLP, COP, PEN, BRL,
            ).associateBy { it.code }

        fun of(
            code: String,
            decimals: Int? = null,
            symbol: String? = null,
            name: String? = null,
        ): Currency {
            val normalized = code.trim().uppercase()
            val existing = PREDEFINED_MAP[normalized]
            if (existing != null && (decimals == null || existing.decimals == decimals)) {
                return existing
            }
            return Currency(
                code = normalized,
                symbol = symbol ?: existing?.symbol,
                decimals = decimals ?: existing?.decimals ?: 2,
                name = name ?: existing?.name,
            )
        }

        /**
         * Resuelve una divisa por su código ISO / ticker (insensible a mayúsculas/minúsculas).
         * Si no está en las predefinidas, crea una instancia genérica estándar con 2 decimales.
         */
        fun fromCode(code: String): Currency = of(code)

        /**
         * Crea explícitamente una divisa personalizada definida por el usuario (ej. tokens locales, fondos propios).
         */
        fun custom(
            code: String,
            symbol: String,
            decimals: Int,
            name: String,
        ): Currency =
            Currency(
                code = code.trim().uppercase(),
                symbol = symbol.trim(),
                decimals = decimals,
                name = name.trim(),
                isCustom = true,
            )
    }
}
