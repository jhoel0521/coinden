package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.BundleInterpreter
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.usecase.bundle.AttachEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.BundleContext
import com.coinden.core.domain.usecase.bundle.CloseBundleUseCase
import com.coinden.core.domain.usecase.bundle.ExcludeEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.PendingBundlesUseCase
import com.coinden.core.domain.usecase.bundle.PerceiveBundleEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.StartBundleUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Escenario de Feature (background-processing): la cola lee las evidencias de Carlos.
 *
 * Lo que importa acá es que no se duplique nada ni se relea de más: la cola toca un paquete
 * **cerrado**, lee de a una evidencia, y recién cuando no queda nada por leer interpreta, **una
 * sola vez**. Si la app se corta a la mitad, retoma donde iba.
 */
class CarlosBundleQueueFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val packs = FakeInstalledPacks(listOf(TestPacks.spanish), TestPacks.templates)
    private val draftRepo = InMemoryDraftRepository()
    private val drafts = DraftContext(draftRepo, wallets, InMemoryWalletAliasRepository(), ids, clock, device)
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val texts = TextDraftInterpreter(packs, FixedRegionalSettings())
    private val bundleRepo = InMemoryBundleRepository()
    private val bundles = BundleContext(bundleRepo, drafts, BundleInterpreter(texts))

    private val start = StartBundleUseCase(bundles)
    private val attach = AttachEvidenceUseCase(bundles)
    private val exclude = ExcludeEvidenceUseCase(bundles)
    private val close = CloseBundleUseCase(bundles)
    private val pending = PendingBundlesUseCase(bundles)
    private val perceive = PerceiveBundleEvidenceUseCase(bundles)

    private fun photo(ref: String) = Evidence(EvidenceSource.PHOTO, attachmentRef = ref, fingerprint = "huella-$ref")

    private fun audio(ref: String) = Evidence(EvidenceSource.VOICE, attachmentRef = ref, fingerprint = "huella-$ref")

    private suspend fun bnb() {
        val id =
            assertIs<WalletOperationResult.WalletCreated>(
                CreateWalletUseCase(ledger)("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, Money.parseCanonical("500", Currency.BOB)),
            ).wallet.id
        SetWalletAliasesUseCase(drafts)("carlos", id, setOf("bnb"))
    }

    @Test
    fun `un paquete abierto no lo toca la cola`() =
        runTest {
            start("carlos", listOf(photo("qr")))

            assertEquals(emptyList<String>(), pending("carlos").map { it.id }, "mientras Carlos puede seguir agregando, no se procesa")
        }

    @Test
    fun `al cerrarlo, el paquete entra a la cola`() =
        runTest {
            val id = start("carlos", listOf(photo("qr")))
            close(id)

            assertEquals(listOf(id), pending("carlos").map { it.id })
        }

    @Test
    fun `se lee de a una evidencia y recien al final se interpreta`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(photo("qr")))
            attach(id, audio("nota-de-voz"))
            close(id)

            perceive(
                id,
                bundles
                    .load(id)!!
                    .active
                    .first { it.needsReading }
                    .id,
                "almuerzo 45 bnb",
                "ocr",
            )
            assertTrue(bundles.draftsOf(bundles.load(id)!!).isEmpty(), "todavía falta leer el audio")

            perceive(
                id,
                bundles
                    .load(id)!!
                    .active
                    .first { it.needsReading }
                    .id,
                "esto es de mi comida",
                "voz",
            )
            assertEquals(1, bundles.draftsOf(bundles.load(id)!!).size, "recién ahora interpreta, una sola vez")
        }

    @Test
    fun `cuando no queda nada por leer, el paquete sale de la cola`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(photo("qr")))
            close(id)
            perceive(
                id,
                bundles
                    .load(id)!!
                    .active
                    .first { it.needsReading }
                    .id,
                "almuerzo 45 bnb",
                "ocr",
            )

            assertEquals(emptyList<String>(), pending("carlos").map { it.id }, "ya está todo leído")
        }

    @Test
    fun `lo que ya se leyo no se vuelve a leer`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(photo("qr")))
            close(id)
            val evidenceId =
                bundles
                    .load(id)!!
                    .active
                    .first { it.needsReading }
                    .id
            perceive(id, evidenceId, "almuerzo 45 bnb", "ocr")

            assertTrue(bundles.load(id)!!.active.none { it.needsReading }, "no queda nada pendiente de leer")
        }

    @Test
    fun `una evidencia quitada no se lee`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(photo("qr")))
            attach(id, photo("de-mas"))
            val sobrante =
                bundles
                    .load(id)!!
                    .evidences
                    .last()
                    .id
            exclude(id, sobrante)
            close(id)

            val porLeer = bundles.load(id)!!.active.filter { it.needsReading }
            assertEquals(1, porLeer.size, "la quitada no entra a la cola")
        }

    @Test
    fun `una foto que no se pudo leer no deja el paquete colgado`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(photo("borrosa")))
            close(id)

            perceive(
                id,
                bundles
                    .load(id)!!
                    .active
                    .first { it.needsReading }
                    .id,
                null,
                "ocr",
            )

            assertEquals(emptyList<String>(), pending("carlos").map { it.id }, "no se puede leer, pero no se reintenta para siempre")
        }

    @Test
    fun `el corte a la mitad se retoma donde iba`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(photo("qr")))
            attach(id, audio("nota-de-voz"))
            close(id)
            perceive(
                id,
                bundles
                    .load(id)!!
                    .active
                    .first { it.needsReading }
                    .id,
                "almuerzo 45 bnb",
                "ocr",
            )

            // Acá se corta la app. Al volver, la cola sigue teniendo el paquete y lo que falta.
            assertEquals(listOf(id), pending("carlos").map { it.id })
            val queFalta = bundles.load(id)!!.active.filter { it.needsReading }
            assertEquals(1, queFalta.size, "solo falta el audio, la foto ya está leída")
        }
}
