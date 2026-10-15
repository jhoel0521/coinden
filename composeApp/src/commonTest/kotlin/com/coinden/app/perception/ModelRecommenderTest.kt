package com.coinden.app.perception

import kotlin.test.Test
import kotlin.test.assertEquals

/** El modelo de voz recomendado depende de la memoria, los 64 bits y el espacio libre del teléfono. */
class ModelRecommenderTest {
    private val gb = 1_000_000_000L
    private val mb = 1_000_000L

    private fun recommend(
        memory: Long,
        free: Long,
        is64Bit: Boolean = true,
    ) = ModelRecommender.recommend(DeviceProfile(totalMemoryBytes = memory, is64Bit = is64Bit, freeStorageBytes = free))

    @Test
    fun `telefono medio de Carlos recomienda el mediano`() {
        assertEquals(VoiceRecommendation.Download(VoiceModels.SMALL), recommend(memory = 6 * gb, free = 20 * gb))
    }

    @Test
    fun `telefono potente de Ana recomienda el mas preciso`() {
        assertEquals(VoiceRecommendation.Download(VoiceModels.LARGE_TURBO), recommend(memory = 12 * gb, free = 50 * gb))
    }

    @Test
    fun `telefono modesto de Facundo recomienda el pequeno`() {
        assertEquals(VoiceRecommendation.Download(VoiceModels.BASE), recommend(memory = 3 * gb, free = 20 * gb))
    }

    @Test
    fun `con poco espacio baja al modelo que cabe`() {
        assertEquals(VoiceRecommendation.Download(VoiceModels.BASE), recommend(memory = 6 * gb, free = 300 * mb))
    }

    @Test
    fun `si ningun modelo cabe recomienda el reconocimiento del sistema`() {
        assertEquals(VoiceRecommendation.SystemRecognizer, recommend(memory = 12 * gb, free = 50 * mb))
    }

    @Test
    fun `sin 64 bits recomienda el reconocimiento del sistema`() {
        assertEquals(VoiceRecommendation.SystemRecognizer, recommend(memory = 6 * gb, free = 20 * gb, is64Bit = false))
    }

    @Test
    fun `el catalogo ofrece los tres modelos de menor a mayor`() {
        assertEquals(listOf(VoiceModels.BASE, VoiceModels.SMALL, VoiceModels.LARGE_TURBO), VoiceModels.all)
    }
}
