package com.coinden.core.domain.feature

import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.port.output.InstalledPacksPort
import com.coinden.core.domain.port.output.RegionalSettingsPort

/** Paquetes y plantillas "instalados" para las pruebas. */
class FakeInstalledPacks(
    var packs: List<LanguagePack> = emptyList(),
    var templates: List<ReceiptTemplate> = emptyList(),
) : InstalledPacksPort {
    override fun languagePacks(): List<LanguagePack> = packs

    override fun receiptTemplates(): List<ReceiptTemplate> = templates
}

/** Locale y zona horaria fijos para las pruebas (por defecto Bolivia, UTC-4). */
class FixedRegionalSettings(
    var locale: UserLocale = UserLocale("es", "BO"),
    var utcOffsetMinutes: Int = -240,
) : RegionalSettingsPort {
    override fun settings(): RegionalSettings = RegionalSettings(locale, utcOffsetMinutes)
}
