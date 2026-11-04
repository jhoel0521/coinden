package com.coinden.app.perception

/**
 * Un modelo de lenguaje descargable (formato `.litertlm`, el que lee LiteRT-LM). [sha256] es la
 * huella del archivo: una descarga que no coincide se borra. Son datos públicos; nada de la
 * persona se envía al bajarlo.
 *
 * [fileName] es el nombre con el que se guarda en el teléfono (siempre el mismo, porque solo hay
 * un modelo de lenguaje instalado a la vez); [remoteFileName] es el del repositorio.
 */
data class LanguageModel(
    val id: String,
    val remoteFileName: String,
    val repository: String,
    val sizeBytes: Long,
    val sha256: String,
    /** Memoria total mínima del teléfono para ofrecerlo. */
    val minMemoryBytes: Long,
) {
    val fileName: String get() = LanguageModels.LOCAL_FILE_NAME
    val url: String get() = "https://huggingface.co/litert-community/$repository/resolve/main/$remoteFileName"
}

/**
 * Catálogo fijo de modelos de lenguaje, de menor a mayor. Los dos son Qwen 3 con licencia
 * Apache 2.0, publicados por `litert-community`. Las huellas son las del repositorio.
 *
 * El mediano todavía no se midió en un teléfono: se ofrece desde 6 GB, pero la primera prueba real
 * fue con el pequeño.
 */
object LanguageModels {
    private const val GB = 1_000_000_000L

    /** Nombre del archivo en `filesDir/models/`. Lo lee la app al arrancar para saber si hay modelo. */
    const val LOCAL_FILE_NAME = "coinden-intent.litertlm"

    val SMALL =
        LanguageModel(
            id = "qwen3-0.6b",
            remoteFileName = "Qwen3-0.6B.litertlm",
            repository = "Qwen3-0.6B",
            sizeBytes = 613_722_264,
            sha256 = "c5f39b4191926786c3e6668b62af4d6eb15248dfc7a5348252a9b406f33f2108",
            minMemoryBytes = 4 * GB,
        )
    val MEDIUM =
        LanguageModel(
            id = "qwen3-1.7b",
            remoteFileName = "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
            repository = "Qwen3-1.7B",
            sizeBytes = 977_184_032,
            sha256 = "2a72330b504a6596d53a2c00923c57173b10f1b83e6a541213ffec08d1fc5de8",
            minMemoryBytes = 6 * GB,
        )

    val all: List<LanguageModel> = listOf(SMALL, MEDIUM)

    fun byId(id: String): LanguageModel? = all.firstOrNull { it.id == id }
}

sealed interface LanguageModelOffer {
    data class Download(
        val model: LanguageModel,
    ) : LanguageModelOffer

    /** Ningún modelo se ofrece: la app sigue completa con el respaldo de reglas. */
    data class None(
        val reason: Reason,
    ) : LanguageModelOffer

    enum class Reason { UNSUPPORTED_CPU, LOW_MEMORY, LOW_STORAGE }
}

/**
 * Qué modelo de lenguaje ofrecer a un teléfono, con reglas fijas: solo en 64 bits, el mayor que su
 * memoria permite y que cabe dos veces en el espacio libre (archivo temporal y final).
 */
object LanguageModelRecommender {
    fun recommend(device: DeviceProfile): LanguageModelOffer {
        if (!device.is64Bit) return LanguageModelOffer.None(LanguageModelOffer.Reason.UNSUPPORTED_CPU)
        val byMemory = LanguageModels.all.filter { device.totalMemoryBytes >= it.minMemoryBytes }
        if (byMemory.isEmpty()) return LanguageModelOffer.None(LanguageModelOffer.Reason.LOW_MEMORY)
        val fits = byMemory.lastOrNull { device.freeStorageBytes >= it.sizeBytes * 2 }
        return fits?.let { LanguageModelOffer.Download(it) }
            ?: LanguageModelOffer.None(LanguageModelOffer.Reason.LOW_STORAGE)
    }
}
