package com.coinden.core.domain.port.output

import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.RegionalSettings

/**
 * Puerto de Salida (SPI) de los paquetes de idioma y plantillas instalados en el dispositivo.
 * El dominio solo los usa; cómo se descargan es asunto del adaptador.
 */
interface InstalledPacksPort {
    fun languagePacks(): List<LanguagePack>

    fun receiptTemplates(): List<ReceiptTemplate>
}

/** Puerto de Salida (SPI) del locale del usuario y la zona horaria actual del dispositivo. */
interface RegionalSettingsPort {
    fun settings(): RegionalSettings
}
