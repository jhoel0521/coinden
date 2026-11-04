package com.coinden.app.perception

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeLanguageModels(
    private val profile: DeviceProfile,
    private val corrupt: Boolean = false,
) : LanguageModelManager {
    var installedModel: LanguageModel? = null
    var loadFailure = false
    var gate: CompletableDeferred<Unit>? = null
    var downloads = 0

    override fun deviceProfile() = profile

    override fun isInstalled(): Boolean = installedModel != null

    override suspend fun download(
        model: LanguageModel,
        onProgress: (Long, Long) -> Unit,
    ): Boolean {
        downloads++
        onProgress(model.sizeBytes / 2, model.sizeBytes)
        gate?.await()
        if (corrupt) return false
        onProgress(model.sizeBytes, model.sizeBytes)
        installedModel = model
        return true
    }

    override suspend fun delete() {
        installedModel = null
    }

    override fun consumeLoadFailure(): Boolean = loadFailure.also { loadFailure = false }
}

/** Carlos ve qué modelo de lenguaje le ofrece la app y lo descarga o lo borra cuando quiere. */
class LanguageModelPresenterTest {
    private val gb = 1_000_000_000L
    private val phone4gb = DeviceProfile(totalMemoryBytes = 4 * gb, is64Bit = true, freeStorageBytes = 20 * gb)
    private val phone2gb = DeviceProfile(totalMemoryBytes = 2 * gb, is64Bit = true, freeStorageBytes = 20 * gb)

    @Test
    fun `telefono de 4 GB ve el modelo pequeno sin instalar`() {
        val p = LanguageModelPresenter(FakeLanguageModels(phone4gb)).also { it.load() }

        assertEquals(LanguageModelOffer.Download(LanguageModels.SMALL), p.state.value.offer)
        assertFalse(p.state.value.installed)
    }

    @Test
    fun `telefono de 2 GB no ve ninguna oferta`() {
        val p = LanguageModelPresenter(FakeLanguageModels(phone2gb)).also { it.load() }

        assertEquals(LanguageModelOffer.None(LanguageModelOffer.Reason.LOW_MEMORY), p.state.value.offer)
    }

    @Test
    fun `descargar deja el modelo instalado y muestra el avance`() =
        runTest {
            val models = FakeLanguageModels(phone4gb)
            val p = LanguageModelPresenter(models).also { it.load() }

            p.download(LanguageModels.SMALL)

            assertTrue(p.state.value.installed)
            assertNull(p.state.value.downloading)
            assertFalse(p.state.value.failed)
        }

    @Test
    fun `una descarga corrupta no se usa y avisa que fallo`() =
        runTest {
            val p = LanguageModelPresenter(FakeLanguageModels(phone4gb, corrupt = true)).also { it.load() }

            p.download(LanguageModels.SMALL)

            assertFalse(p.state.value.installed)
            assertTrue(p.state.value.failed)
        }

    @Test
    fun `mientras descarga no empieza otra descarga`() =
        runTest {
            val models = FakeLanguageModels(phone4gb).also { it.gate = CompletableDeferred() }
            val p = LanguageModelPresenter(models).also { it.load() }

            val first = launch { p.download(LanguageModels.SMALL) }
            runCurrent()
            assertEquals(LanguageModels.SMALL, p.state.value.downloading)
            p.download(LanguageModels.SMALL)
            models.gate!!.complete(Unit)
            first.join()

            assertEquals(1, models.downloads)
        }

    @Test
    fun `borrar el modelo lo quita`() =
        runTest {
            val models = FakeLanguageModels(phone4gb).also { it.installedModel = LanguageModels.SMALL }
            val p = LanguageModelPresenter(models).also { it.load() }
            assertTrue(p.state.value.installed)

            p.delete()

            assertFalse(p.state.value.installed)
        }

    @Test
    fun `si el modelo instalado no carga se avisa una sola vez`() {
        val models =
            FakeLanguageModels(phone4gb).also {
                it.installedModel = LanguageModels.SMALL
                it.loadFailure = true
            }
        val p = LanguageModelPresenter(models)

        p.load()
        assertTrue(p.state.value.loadFailed)

        p.load()
        assertFalse(p.state.value.loadFailed)
    }
}
