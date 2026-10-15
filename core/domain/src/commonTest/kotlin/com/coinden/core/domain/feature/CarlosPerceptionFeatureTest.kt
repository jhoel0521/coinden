package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.SameMovementQuestion
import com.coinden.core.domain.model.draft.SameMovementReason
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.FindSameMovementQuestionsUseCase
import com.coinden.core.domain.usecase.draft.InterpretPerceivedUseCase
import com.coinden.core.domain.usecase.draft.ReviseDraftUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Escenario de Feature: las fotos, PDF y audios de Carlos (Bolivia) se guardan primero y se leen
 * después en el teléfono; el texto leído pasa por el mismo intérprete sin perder el archivo.
 */
class CarlosPerceptionFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val drafts = InMemoryDraftRepository()
    private val aliases = InMemoryWalletAliasRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock()
    private val device = FixedDevice("celular-carlos")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = DraftContext(drafts, wallets, aliases, ids, clock, device)

    private val interpreter =
        TextDraftInterpreter(
            FakeInstalledPacks(listOf(LanguagePacks.SPANISH), ReceiptTemplates.all),
            // es-BO, UTC-4
            FixedRegionalSettings(),
        )
    private val capture = CaptureDraftUseCase(ctx, interpreter)
    private val perceived = InterpretPerceivedUseCase(ctx, interpreter)
    private val revise = ReviseDraftUseCase(ctx)

    private fun bolivia(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L + minute * 60L) * 1_000L

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private suspend fun carlos() {
        fun created(result: WalletOperationResult) = assertIs<WalletOperationResult.WalletCreated>(result)
        created(
            CreateWalletUseCase(ledger)("carlos", "Billetera", Currency.BOB, WalletType.CASH, bs("100"), occurredAtEpochMs = bolivia(1, 7)),
        )
        created(
            CreateWalletUseCase(ledger)("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, bs("500"), occurredAtEpochMs = bolivia(1, 7)),
        )
    }

    /** Texto que un lector sacaría de la foto de un pago QR (estructura del Banco Económico, datos inventados). */
    private val qrPhotoText =
        """
        Banco ECONOMICO
        Pago QR otros bancos
        Beneficiario
        POLLOS DEL VALLE
        Importe
        Bs25.00
        Nro. de transacción: 400000001
        """.trimIndent()

    private val photo = Evidence(EvidenceSource.PHOTO, attachmentRef = "foto-1", fingerprint = "huella-foto-1")

    private suspend fun capturedPhoto(evidence: Evidence = photo): Draft {
        clock.nowEpochMs = bolivia(10, 14)
        return assertIs<Draft.Captured>(capture("carlos", evidence, bolivia(10, 14)).single())
    }

    @Test
    fun `una foto leida despues propone el gasto y conserva la foto`() =
        runTest {
            carlos()
            val draft = capturedPhoto()

            clock.nowEpochMs = bolivia(10, 14) + 1_000
            val result = perceived(draft.id, qrPhotoText, engine = "tesseract")

            val read = result.single()
            assertEquals(draft.id, read.id)
            assertEquals(photo, read.header.evidence)
            assertEquals(qrPhotoText, read.header.perceivedText)
            val proposal = assertIs<MovementProposal.Expense>((read as Draft.Incomplete).proposal)
            assertEquals(ProposedAmount("25.00", Currency.BOB), proposal.amount)
            assertEquals("400000001", proposal.reference)
            val event = drafts.eventsFor(draft.id).filterIsInstance<DraftEvent.Perceived>().single()
            assertEquals("tesseract", event.engine)
        }

    @Test
    fun `un audio con dos gastos crea otro borrador con el mismo audio`() =
        runTest {
            carlos()
            val audio = Evidence(EvidenceSource.VOICE, attachmentRef = "audio-1", fingerprint = "huella-audio-1")
            val draft = capturedPhoto(audio)

            val result = perceived(draft.id, "almuerzo 45 bnb y micro 3 bnb", engine = "whisper")

            assertEquals(2, result.size)
            assertEquals(draft.id, result[0].id)
            val second = result[1]
            assertEquals(audio, second.header.evidence)
            assertEquals("almuerzo 45 bnb y micro 3 bnb", second.header.perceivedText)
            assertEquals(bs("3"), (assertIs<Draft.Ready>(second).movement as ReadyMovement.Expense).amount)
        }

    @Test
    fun `un borrador ya completado a mano no cambia`() =
        runTest {
            carlos()
            val draft = capturedPhoto()
            val bnb = ctx.directory("carlos").wallets.single { it.name == "BNB Bs" }
            val manual = MovementProposal.Expense(ProposedAmount("30", Currency.BOB), WalletRef.ById(bnb.id), note = "farmacia")
            assertIs<DraftResult.Ok>(revise(draft.id, manual))
            val before = drafts.eventsFor(draft.id)

            val result = perceived(draft.id, qrPhotoText, engine = "tesseract")

            assertEquals(before, drafts.eventsFor(draft.id))
            assertEquals("farmacia", ((result.single() as Draft.Ready).movement as ReadyMovement.Expense).note)
        }

    @Test
    fun `una lectura sin texto deja el borrador capturado`() =
        runTest {
            carlos()
            val draft = capturedPhoto()

            val result = perceived(draft.id, "   ", engine = "whisper")

            assertIs<Draft.Captured>(result.single())
            assertNull(result.single().header.perceivedText)
        }

    @Test
    fun `una lectura ya hecha no se repite, tampoco en otro dispositivo`() =
        runTest {
            carlos()
            val draft = capturedPhoto()
            // Texto leído sin propuesta: el borrador sigue Capturado, pero ya fue leído.
            perceived(draft.id, "texto sin montos", engine = "tesseract")
            val after = drafts.eventsFor(draft.id)

            perceived(draft.id, qrPhotoText, engine = "mlkit")

            assertEquals(after, drafts.eventsFor(draft.id))
            assertEquals("texto sin montos", ctx.load(draft.id)!!.header.perceivedText)
        }

    @Test
    fun `dos fotos distintas del mismo comprobante se reconocen por el texto leido`() =
        runTest {
            carlos()
            val first = capturedPhoto()
            val second = capturedPhoto(Evidence(EvidenceSource.PHOTO, attachmentRef = "foto-2", fingerprint = "huella-foto-2"))
            perceived(first.id, qrPhotoText, engine = "tesseract")
            perceived(second.id, qrPhotoText, engine = "tesseract")

            val questions = FindSameMovementQuestionsUseCase(ctx)("carlos", utcOffsetMinutes = -240)

            assertEquals(listOf(SameMovementQuestion(second.id, first.id, SameMovementReason.SAME_REFERENCE)), questions)
        }
}
