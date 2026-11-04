package com.coinden.app.perception

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** El modelo de lenguaje se ofrece solo si el teléfono puede correrlo; si no, la app sigue con el respaldo. */
class LanguageModelRecommenderTest {
    private val gb = 1_000_000_000L
    private val mb = 1_000_000L

    private fun offer(
        memory: Long,
        free: Long = 20 * gb,
        is64Bit: Boolean = true,
    ) = LanguageModelRecommender.recommend(
        DeviceProfile(totalMemoryBytes = memory, is64Bit = is64Bit, freeStorageBytes = free),
    )

    @Test
    fun `telefono de 4 GB y 64 bits ofrece el pequeno`() {
        assertEquals(LanguageModelOffer.Download(LanguageModels.SMALL), offer(memory = 4 * gb))
    }

    @Test
    fun `telefono de 6 GB o mas ofrece el mediano`() {
        assertEquals(LanguageModelOffer.Download(LanguageModels.MEDIUM), offer(memory = 6 * gb))
        assertEquals(LanguageModelOffer.Download(LanguageModels.MEDIUM), offer(memory = 12 * gb))
    }

    @Test
    fun `telefono de 2 GB no ofrece nada`() {
        assertEquals(LanguageModelOffer.None(LanguageModelOffer.Reason.LOW_MEMORY), offer(memory = 2 * gb))
    }

    @Test
    fun `telefono de 32 bits no ofrece nada`() {
        assertEquals(
            LanguageModelOffer.None(LanguageModelOffer.Reason.UNSUPPORTED_CPU),
            offer(memory = 8 * gb, is64Bit = false),
        )
    }

    @Test
    fun `sin espacio para el archivo dos veces no ofrece nada`() {
        assertEquals(
            LanguageModelOffer.None(LanguageModelOffer.Reason.LOW_STORAGE),
            offer(memory = 4 * gb, free = 900 * mb),
        )
    }

    @Test
    fun `con poco espacio baja al modelo que cabe`() {
        assertEquals(
            LanguageModelOffer.Download(LanguageModels.SMALL),
            offer(memory = 8 * gb, free = LanguageModels.SMALL.sizeBytes * 2 + mb),
        )
    }

    @Test
    fun `el catalogo es de menor a mayor y cada modelo trae su huella y una direccion publica`() {
        assertEquals(listOf(LanguageModels.SMALL, LanguageModels.MEDIUM), LanguageModels.all)
        LanguageModels.all.forEach {
            assertEquals(64, it.sha256.length)
            assertTrue(it.url.startsWith("https://huggingface.co/litert-community/"))
            assertTrue(it.url.endsWith(it.remoteFileName))
            assertEquals(LanguageModels.LOCAL_FILE_NAME, it.fileName)
        }
    }
}
