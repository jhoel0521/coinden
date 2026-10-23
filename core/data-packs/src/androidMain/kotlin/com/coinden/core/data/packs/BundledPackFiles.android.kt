package com.coinden.core.data.packs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Los archivos que viajan con la app son recursos del classpath. El classpath no se puede listar,
 * así que primero se lee el índice y después cada archivo que nombra.
 */
internal actual fun bundledPackTexts(): List<String> = BundledPackFiles.texts()

private object BundledPackFiles {
    fun texts(): List<String> = names().mapNotNull { read("$PACKS_DIR/$it") }

    private fun names(): List<String> {
        val index = read("$PACKS_DIR/$INDEX") ?: return emptyList()
        return runCatching {
            Json
                .parseToJsonElement(index)
                .jsonObject["files"]!!
                .jsonArray
                .map { it.jsonPrimitive.content }
        }.getOrDefault(emptyList())
    }

    private fun read(path: String): String? =
        runCatching {
            BundledPackFiles::class.java.classLoader
                ?.getResourceAsStream(path)
                ?.use { it.readBytes().decodeToString() }
        }.getOrNull()
}
