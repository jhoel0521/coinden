package com.coinden.app.perception

/**
 * Un modelo de voz descargable (Whisper en formato ggml). [sha256] es la huella oficial del archivo:
 * una descarga que no coincide se borra. Son datos públicos; nada del usuario se envía al bajarlo.
 */
data class VoiceModel(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    /** Memoria total desde la que se recomienda en un teléfono. */
    val recommendedFromMemoryBytes: Long,
) {
    val url: String get() = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$fileName"
}

/** Catálogo fijo de modelos de voz, de menor a mayor. Los umbrales se ajustan con mediciones reales. */
object VoiceModels {
    private const val GB = 1_000_000_000L

    val BASE =
        VoiceModel(
            id = "base",
            fileName = "ggml-base-q5_1.bin",
            sizeBytes = 59_707_625,
            sha256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
            recommendedFromMemoryBytes = 0,
        )
    val SMALL =
        VoiceModel(
            id = "small",
            fileName = "ggml-small-q5_1.bin",
            sizeBytes = 190_085_487,
            sha256 = "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
            recommendedFromMemoryBytes = 4 * GB,
        )
    val LARGE_TURBO =
        VoiceModel(
            id = "large-v3-turbo",
            fileName = "ggml-large-v3-turbo-q5_0.bin",
            sizeBytes = 574_041_195,
            sha256 = "394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2",
            recommendedFromMemoryBytes = 8 * GB,
        )

    val all: List<VoiceModel> = listOf(BASE, SMALL, LARGE_TURBO)

    fun byId(id: String): VoiceModel? = all.firstOrNull { it.id == id }
}

/** Lo que la plataforma dice del teléfono para recomendar un modelo. */
data class DeviceProfile(
    val totalMemoryBytes: Long,
    val is64Bit: Boolean,
    val freeStorageBytes: Long,
)

sealed interface VoiceRecommendation {
    data class Download(
        val model: VoiceModel,
    ) : VoiceRecommendation

    /** Ningún modelo cabe o el teléfono no puede correrlo: el reconocimiento local del sistema. */
    data object SystemRecognizer : VoiceRecommendation
}

/**
 * Recomienda el modelo de voz para un teléfono, con reglas fijas: el mayor que su memoria
 * permite y que cabe dos veces en el espacio libre (archivo temporal y final); si ninguno cabe
 * o el teléfono no es de 64 bits, el reconocimiento del sistema.
 */
object ModelRecommender {
    fun recommend(device: DeviceProfile): VoiceRecommendation {
        if (!device.is64Bit) return VoiceRecommendation.SystemRecognizer
        return VoiceModels.all
            .filter { device.totalMemoryBytes >= it.recommendedFromMemoryBytes }
            .lastOrNull { device.freeStorageBytes >= it.sizeBytes * 2 }
            ?.let { VoiceRecommendation.Download(it) }
            ?: VoiceRecommendation.SystemRecognizer
    }
}
