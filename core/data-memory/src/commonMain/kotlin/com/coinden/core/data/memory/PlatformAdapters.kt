package com.coinden.core.data.memory

import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.DeviceIdentityPort
import com.coinden.core.domain.port.output.IdGeneratorPort
import com.coinden.core.domain.port.output.InstalledPacksPort
import com.coinden.core.domain.port.output.RegionalSettingsPort
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Ids globales únicos generados en el dispositivo (sync-ready). */
class UuidIdGenerator : IdGeneratorPort {
    @OptIn(ExperimentalUuidApi::class)
    override fun newId(): String = Uuid.random().toString()
}

/** Hora real del sistema en milisegundos epoch. */
class SystemClock : ClockPort {
    override fun nowEpochMs(): Long = currentEpochMs()
}

internal expect fun currentEpochMs(): Long

/** Identidad fija de este dispositivo (persistirla llega con local-persistence). */
class FixedDeviceIdentity(
    private val id: String,
) : DeviceIdentityPort {
    override fun deviceId(): String = id
}

/**
 * Paquetes de idioma y plantillas "instalados". Mientras no haya descargas, se usa el catálogo
 * que viene con la app: español y portugués (el inglés siempre está activo) y las plantillas iniciales.
 */
class StaticInstalledPacks(
    private val packs: List<LanguagePack> = listOf(LanguagePacks.SPANISH, LanguagePacks.PORTUGUESE),
    private val templates: List<ReceiptTemplate> = ReceiptTemplates.all,
) : InstalledPacksPort {
    override fun languagePacks(): List<LanguagePack> = packs

    override fun receiptTemplates(): List<ReceiptTemplate> = templates
}

/** Configuración regional fija, leída de la plataforma al iniciar la app. */
class StaticRegionalSettings(
    private val settings: RegionalSettings,
) : RegionalSettingsPort {
    override fun settings(): RegionalSettings = settings
}
