package com.coinden.core.data.packs

import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate

/** Nombre del índice que dice qué archivos trae la app: el classpath no se puede listar. */
internal const val INDEX = "index.json"

/** Carpeta de los archivos de datos dentro de los recursos. */
internal const val PACKS_DIR = "packs"

/**
 * Los paquetes y plantillas que viajan dentro de la app. Están siempre, desde el primer arranque
 * y sin red: son los que hacen que la app funcione completa sin descargar nada.
 *
 * Leer los recursos depende de la plataforma; lo que se hace con ellos, no.
 */
internal expect fun bundledPackTexts(): List<String>

/**
 * Punto de entrada para quien solo quiere los paquetes listos: junta lo que viaja con la app con
 * lo que la persona haya descargado.
 */
object BundledPacks {
    fun languagePacks(
        downloaded: List<String> = emptyList(),
        appLanguage: String = "en",
    ): List<LanguagePack> = PackLoader.languagePacks(bundledPackTexts(), downloaded, appLanguage)

    fun receiptTemplates(downloaded: List<String> = emptyList()): List<ReceiptTemplate> =
        PackLoader.receiptTemplates(bundledPackTexts(), downloaded)
}
