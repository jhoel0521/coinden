package com.coinden.app.perception

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.coinden.app.AppContainer
import com.coinden.app.AppStorage
import com.coinden.app.PlatformCurrencyNames
import com.coinden.app.capture.InMemoryAttachmentStore
import com.coinden.app.capture.QuickCapture
import com.coinden.app.capture.SharedContent
import com.coinden.core.data.local.PlatformCurrencyCatalog
import com.coinden.core.data.memory.FixedDeviceIdentity
import com.coinden.core.data.memory.StaticRegionalSettings
import com.coinden.core.data.memory.SystemClock
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Se ejecuta en el emulador: el trabajador de WorkManager lee una foto guardada y libera los modelos. */
@RunWith(AndroidJUnit4::class)
class PerceptionWorkerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Carlos (Bolivia) en memoria, con "Billetera" en efectivo. */
    private fun carlos(): AppContainer =
        AppContainer(
            storage = AppStorage.inMemory(),
            ownerId = "carlos",
            regional = StaticRegionalSettings(RegionalSettings(UserLocale("es", "BO", Currency.BOB, ','), utcOffsetMinutes = -240)),
            device = FixedDeviceIdentity("emulador"),
            clock = SystemClock(),
            catalog = PlatformCurrencyCatalog(),
            currencyNames = PlatformCurrencyNames,
            initialLanguage = "es",
            attachments = InMemoryAttachmentStore(),
        ).also { container ->
            runBlocking {
                container.storage.profiles.saveProfile(UserCurrencyProfile(userId = "carlos", localCurrency = Currency.BOB))
                container.createWallet("carlos", "Billetera", Currency.BOB, WalletType.CASH, Money.parseCanonical("100", Currency.BOB))
            }
        }

    private class NoQueue : ProcessingQueue {
        override suspend fun kick() = Unit

        override val processed = MutableSharedFlow<String>()
    }

    @Test
    fun el_trabajador_lee_la_foto_pendiente_y_libera_los_modelos() =
        runBlocking {
            val container = carlos()
            var released = 0
            val base = AppPerception(context, container).readers
            val readers =
                PerceptionReaders(
                    base.freeImageReader,
                    base.googleImageReader,
                    base.pdfReader,
                    base.audioDecoder,
                    base.transcribers,
                    release = { released++ },
                )
            val runner = QueueRunner(PerceptionService(container, readers), readers, InMemoryFailureLog())
            // La foto se guarda al instante; el trabajo pesado queda para la cola.
            val draft = QuickCapture(container, NoQueue()).shared(SharedContent.File(ReceiptImages.jpeg(), "image/jpeg")).single()
            assertTrue(draft is Draft.Captured)

            val worker =
                TestListenableWorkerBuilder<PerceptionWorker>(context)
                    .setWorkerFactory(
                        object : WorkerFactory() {
                            override fun createWorker(
                                appContext: Context,
                                workerClassName: String,
                                workerParameters: WorkerParameters,
                            ): ListenableWorker = PerceptionWorker(appContext, workerParameters, runner)
                        },
                    ).build()

            assertEquals(ListenableWorker.Result.success(), worker.doWork())
            val read = container.drafts.load(draft.id)!!
            assertTrue(
                read.header.perceivedText.orEmpty(),
                read.header.perceivedText
                    .orEmpty()
                    .contains("400000001"),
            )
            assertEquals(1, released)
        }

    @Test
    fun el_registro_de_fallas_sobrevive_en_archivo() =
        runBlocking {
            val file = context.filesDir.resolve("prueba-fallas.tsv").apply { delete() }
            val log = FileFailureLog(file)
            assertEquals(1, log.fail("d1"))
            assertEquals(2, FileFailureLog(file).fail("d1"))
            log.settle("d2")
            assertTrue(FileFailureLog(file).isSettled("d2"))
            FileFailureLog(file).clear("d2")
            assertEquals(false, log.isSettled("d2"))
            file.delete()
            Unit
        }
}
