package com.coinden.core.domain.support

import com.coinden.core.data.packs.BundledPacks
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate

/**
 * Los paquetes y plantillas reales, cargados desde sus archivos de datos.
 *
 * Las pruebas del dominio los usaban como constantes de Kotlin. Ahora vienen de donde vienen en
 * la app, así que estas pruebas comprueban de paso que los archivos dicen lo mismo que decía el
 * código. Ninguna palabra vive aquí: todas están en `core/data-packs`.
 */
object TestPacks {
    private val packs by lazy { BundledPacks.languagePacks().associateBy { it.code } }

    val english: LanguagePack get() = packs.getValue("en")
    val spanish: LanguagePack get() = packs.getValue("es")
    val portuguese: LanguagePack get() = packs.getValue("pt")

    val templates: List<ReceiptTemplate> by lazy { BundledPacks.receiptTemplates() }

    /** La plantilla con ese id, para las pruebas que prueban una sola. */
    fun template(id: String): ReceiptTemplate = templates.first { it.id == id }
}
