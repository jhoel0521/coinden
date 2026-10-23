package com.coinden.core.domain.feature

import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.port.output.InstalledPacksPort
import com.coinden.core.domain.port.output.RegionalSettingsPort
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.support.testLocale

/** Paquetes y plantillas "instalados" para las pruebas. */
class FakeInstalledPacks(
    var packs: List<LanguagePack> = emptyList(),
    var templates: List<ReceiptTemplate> = emptyList(),
) : InstalledPacksPort {
    /**
     * Cumple lo que el puerto promete: el inglés siempre está, porque en la app viaja con ella, y
     * va primero para que el paquete del idioma de la persona gane al unirlos. Una prueba que
     * instala solo el español sigue entendiendo las palabras en inglés, como el teléfono real.
     */
    override fun languagePacks(): List<LanguagePack> =
        if (packs.any { it.code == TestPacks.english.code }) packs else listOf(TestPacks.english) + packs

    override fun receiptTemplates(): List<ReceiptTemplate> = templates
}

/** Locale y zona horaria fijos para las pruebas (por defecto Bolivia, UTC-4). */
class FixedRegionalSettings(
    var locale: UserLocale = testLocale("es", "BO"),
    var utcOffsetMinutes: Int = -240,
) : RegionalSettingsPort {
    override fun settings(): RegionalSettings = RegionalSettings(locale, utcOffsetMinutes)
}
