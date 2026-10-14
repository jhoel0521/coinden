package com.coinden.core.data.local

import com.coinden.core.domain.port.output.CurrencyCatalogPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contrato del catálogo de monedas: lo debe cumplir cada adaptador de plataforma (hoy JVM/Android;
 * mañana iOS con Foundation), con los datos oficiales ISO 4217.
 */
abstract class CurrencyCatalogContract {
    abstract fun catalog(): CurrencyCatalogPort

    @Test
    fun `decimales oficiales`() {
        val catalog = catalog()
        assertEquals(2, catalog.find("BOB")?.decimals)
        assertEquals(0, catalog.find("CLP")?.decimals)
        assertEquals(3, catalog.find("BHD")?.decimals)
        assertEquals(0, catalog.find("JPY")?.decimals)
    }

    @Test
    fun `moneda de cada pais`() {
        val catalog = catalog()
        assertEquals("BOB", catalog.forRegion("BO")?.code)
        assertEquals("EUR", catalog.forRegion("FR")?.code)
        assertEquals("CNY", catalog.forRegion("CN")?.code)
        assertEquals("DOP", catalog.forRegion("DO")?.code)
        assertNull(catalog.forRegion("ZZ"))
    }

    @Test
    fun `el catalogo es mundial y sin codigos invalidos`() {
        val all = catalog().all()
        assertTrue(all.size >= 140, "solo ${all.size} monedas")
        assertTrue(all.none { it.isCustom })
        assertNull(catalog().find("XYZ"))
    }
}

class PlatformCurrencyCatalogContractTest : CurrencyCatalogContract() {
    override fun catalog() = PlatformCurrencyCatalog()
}
