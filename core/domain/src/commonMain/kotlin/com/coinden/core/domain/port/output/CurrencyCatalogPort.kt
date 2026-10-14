package com.coinden.core.domain.port.output

import com.coinden.core.domain.model.Currency

/**
 * SPI: catálogo mundial de monedas oficiales (ISO 4217) con sus decimales, a partir de los datos
 * de la plataforma. Sin nombres ni símbolos: el dominio no tiene idioma; la presentación los
 * resuelve con la misma plataforma.
 */
interface CurrencyCatalogPort {
    /** Todas las monedas oficiales vigentes. */
    fun all(): List<Currency>

    /** La moneda con ese código ISO, o null si la plataforma no la conoce. */
    fun find(code: String): Currency?

    /** La moneda de un país (código ISO 3166 de dos letras), o null si no se conoce. */
    fun forRegion(countryCode: String): Currency?
}
