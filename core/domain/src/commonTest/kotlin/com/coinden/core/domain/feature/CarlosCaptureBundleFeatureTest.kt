package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.bundle.BundleProjection
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.BundleInterpreter
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.usecase.bundle.AttachEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.AttachResult
import com.coinden.core.domain.usecase.bundle.BundleContext
import com.coinden.core.domain.usecase.bundle.CloseBundleUseCase
import com.coinden.core.domain.usecase.bundle.ExcludeEvidenceUseCase
import com.coinden.core.domain.usecase.bundle.ReopenBundleUseCase
import com.coinden.core.domain.usecase.bundle.StartBundleUseCase
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.ReviseDraftUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Escenario de Feature (capture-bundle): Carlos manda junto lo que explica sus pagos.
 *
 * Comparte comprobantes, les suma audios y notas, quita lo que no era y sale. El paquete guarda
 * todo cifrado al llegar, se interpreta al cerrarse y produce un borrador por pago real; reabrirlo
 * vuelve a interpretar sin pisar lo que Carlos corrigió ni lo que ya confirmó.
 */
class CarlosCaptureBundleFeatureTest {
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
    private val reopen = ReopenBundleUseCase(bundles)

    private fun receipt(text: String) = Evidence(EvidenceSource.TEXT, text)

    private fun photo(ref: String) = Evidence(EvidenceSource.PHOTO, attachmentRef = ref, fingerprint = "huella-$ref")

    private fun note(text: String) = Evidence(EvidenceSource.MANUAL, text)

    private suspend fun bnb(): String {
        val id =
            assertIs<WalletOperationResult.WalletCreated>(
                CreateWalletUseCase(ledger)("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, Money.parseCanonical("500", Currency.BOB)),
            ).wallet.id
        SetWalletAliasesUseCase(drafts)("carlos", id, setOf("bnb", AnonymizedReceipts.CARLOS_BE_ACCOUNT))
        return id
    }

    private fun amountOf(draft: Draft): ProposedAmount? =
        when (val p = (draft as? Draft.Ready)?.proposal ?: (draft as? Draft.Incomplete)?.proposal) {
            is MovementProposal.Expense -> p.amount
            is MovementProposal.Income -> p.amount
            else -> null
        }

    @Test
    fun `un comprobante compartido es un paquete abierto con esa sola evidencia`() =
        runTest {
            val id = start("carlos", listOf(receipt(AnonymizedReceipts.BE_QR_CHICKEN)))

            val bundle = bundles.load(id)!!
            assertTrue(bundle.open)
            assertEquals(listOf(EvidenceRole.RECEIPT), bundle.evidences.map { it.role })
            assertTrue(bundles.draftsOf(bundle).isEmpty(), "abierto no produce borradores")
        }

    @Test
    fun `varias evidencias quedan en el paquete en el orden en que llegaron`() =
        runTest {
            val id = start("carlos", listOf(photo("foto-1"), photo("foto-2")))
            attach(id, Evidence(EvidenceSource.VOICE, attachmentRef = "audio-1", fingerprint = "huella-audio-1"))
            attach(id, note("almuerzo con el equipo"))

            val bundle = bundles.load(id)!!
            assertEquals(listOf("foto-1", "foto-2", "audio-1", null), bundle.evidences.map { it.evidence.attachmentRef })
            assertEquals(
                listOf(
                    EvidenceRole.RECEIPT,
                    EvidenceRole.RECEIPT,
                    EvidenceRole.CONTEXT,
                    EvidenceRole.CONTEXT,
                ),
                bundle.evidences.map {
                    it.role
                },
            )
        }

    @Test
    fun `si la app se cierra con el paquete abierto las evidencias siguen guardadas`() =
        runTest {
            val id = start("carlos", listOf(photo("foto-1")))
            attach(id, Evidence(EvidenceSource.VOICE, attachmentRef = "audio-1"))

            val reopenedApp =
                BundleContext(
                    bundleRepo,
                    DraftContext(draftRepo, wallets, InMemoryWalletAliasRepository(), ids, clock, device),
                    BundleInterpreter(texts),
                )
            assertEquals(2, reopenedApp.load(id)!!.evidences.size)
        }

    @Test
    fun `el paquete se ve igual en otro dispositivo`() =
        runTest {
            val id = start("carlos", listOf(photo("foto-1")))
            attach(id, note("farmacia"))

            val pc = BundleProjection.project(bundleRepo.allEvents.filter { it.bundleId == id }.toList())
            assertEquals(bundles.load(id), pc)
        }

    @Test
    fun `compartir y salir cierra el paquete y produce el borrador del comprobante`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(receipt(AnonymizedReceipts.BE_QR_CHICKEN)))

            val produced = close(id)

            assertFalse(bundles.load(id)!!.open)
            val draft = produced.single()
            assertEquals(ProposedAmount("25.00", Currency.BOB), amountOf(draft))
            assertEquals(id, draft.header.bundleId)
        }

    @Test
    fun `quitar no borra la evidencia pero deja de interpretarse`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(receipt(AnonymizedReceipts.BE_QR_CHICKEN), receipt(AnonymizedReceipts.BE_QR_PHARMACY)))
            val pharmacy = bundles.load(id)!!.evidences[1].id

            exclude(id, pharmacy)
            val produced = close(id)

            val bundle = bundles.load(id)!!
            assertEquals(2, bundle.evidences.size)
            assertTrue(bundle.evidences.single { it.id == pharmacy }.excluded)
            assertEquals(listOf(ProposedAmount("25.00", Currency.BOB)), produced.map(::amountOf))
        }

    @Test
    fun `el mismo archivo guardado y compartido otra vez no se agrega dos veces`() =
        runTest {
            val id = start("carlos", listOf(photo("whatsapp-1")))

            val again = attach(id, photo("whatsapp-1"))

            assertEquals(AttachResult.AlreadyInBundle, again)
            assertEquals(1, bundles.load(id)!!.evidences.size)
        }

    @Test
    fun `reabrir el paquete y agregar una nota vuelve a interpretar el mismo borrador`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(receipt(AnonymizedReceipts.BE_QR_CHICKEN)))
            val first = close(id).single()

            assertTrue(reopen(id))
            attach(id, note("almuerzo con el equipo"))
            val after = close(id)

            assertEquals(listOf(first.id), after.map { it.id })
            assertEquals(2, bundles.load(id)!!.active.size)
        }

    @Test
    fun `un paquete con todo confirmado no se reabre`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(receipt(AnonymizedReceipts.BE_QR_CHICKEN)))
            val draft = close(id).single()
            val ready = assertIs<Draft.Ready>(draft)
            assertIs<DraftResult.Ok>(ConfirmDraftUseCase(drafts, ledger)(ready.id))

            assertFalse(reopen(id))
        }

    @Test
    fun `un borrador de antes de los paquetes se lee como paquete cerrado sin escribir nada`() =
        runTest {
            val old = CaptureDraftUseCase(drafts, texts)("carlos", Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb")).single()
            val before = draftRepo.allEvents.size

            val bundle = bundles.load(old.id)!!

            assertFalse(bundle.open)
            assertEquals(listOf("almuerzo 45 bnb"), bundle.evidences.map { it.evidence.text })
            assertEquals(listOf(old.id), bundles.draftsOf(bundle).map { it.id })
            assertTrue(bundleRepo.allEvents.isEmpty())
            assertEquals(before, draftRepo.allEvents.size)
        }

    @Test
    fun `reabrir un borrador de antes lo convierte en paquete y conserva el mismo borrador`() =
        runTest {
            bnb()
            val old = CaptureDraftUseCase(drafts, texts)("carlos", Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb")).single()

            assertTrue(reopen(old.id))
            attach(old.id, note("con el equipo"))
            val after = close(old.id)

            assertEquals(listOf(old.id), after.filterNot { it.isTerminal }.map { it.id })
        }

    @Test
    fun `reinterpretar no pisa lo que Carlos corrigio a mano`() =
        runTest {
            val bnbId = bnb()
            val id = start("carlos", listOf(receipt(AnonymizedReceipts.BE_QR_CHICKEN)))
            val draft = close(id).single()
            val gift =
                MovementProposal.Expense(ProposedAmount("25.00", Currency.BOB), WalletRef.ById(bnbId), note = "regalo")
            assertIs<DraftResult.Ok>(ReviseDraftUseCase(drafts)(draft.id, gift))

            reopen(id)
            attach(id, note("almuerzo"))
            val after = close(id).single()

            assertEquals("regalo", ((after as Draft.Ready).proposal as MovementProposal.Expense).note)
        }

    @Test
    fun `reinterpretar con un borrador confirmado no lo cambia ni lo duplica`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(receipt(AnonymizedReceipts.BE_QR_CHICKEN), receipt(AnonymizedReceipts.BE_QR_PHARMACY)))
            val produced = close(id)
            assertEquals(2, produced.size)
            val chicken = produced.single { amountOf(it) == ProposedAmount("25.00", Currency.BOB) }
            assertIs<DraftResult.Ok>(ConfirmDraftUseCase(drafts, ledger)(chicken.id))

            reopen(id)
            attach(id, note("farmacia de mamá"))
            val after = close(id)

            assertEquals(2, after.size)
            assertIs<Draft.Confirmed>(after.single { it.id == chicken.id })
        }

    @Test
    fun `quitar un comprobante descarta su borrador enlazado al paquete`() =
        runTest {
            bnb()
            val id = start("carlos", listOf(receipt(AnonymizedReceipts.BE_QR_CHICKEN), receipt(AnonymizedReceipts.BE_QR_PHARMACY)))
            val produced = close(id)
            val pharmacyDraft = produced.single { amountOf(it) == ProposedAmount("31.70", Currency.BOB) }
            val pharmacyEvidence = bundles.load(id)!!.evidences[1].id

            exclude(id, pharmacyEvidence)

            val discarded = assertIs<Draft.Discarded>(drafts.load(pharmacyDraft.id))
            assertEquals(id, discarded.mergedInto)
        }
}
