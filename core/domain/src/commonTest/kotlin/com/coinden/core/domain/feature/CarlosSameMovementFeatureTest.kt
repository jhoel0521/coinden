package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.SameMovementQuestion
import com.coinden.core.domain.model.draft.SameMovementReason
import com.coinden.core.domain.model.interpret.DateTimeReader
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DiscardDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.FindSameMovementQuestionsUseCase
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.ResolveSameMovementUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetActiveWalletBalancesUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: el mismo pago de Carlos (Bolivia) llega por varios caminos (dictado,
 * comprobante, archivo repetido). Cada captura se guarda y la app pregunta si son el mismo gasto.
 */
class CarlosSameMovementFeatureTest {
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
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val discard = DiscardDraftUseCase(ctx)
    private val findQuestions = FindSameMovementQuestionsUseCase(ctx)

    private val account = AnonymizedReceipts.CARLOS_BE_ACCOUNT

    /** Hora local de Bolivia (UTC-4) en octubre de 2026, como epoch ms. */
    private fun bolivia(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = (DateTimeReader.daysFromCivil(2026, 10, day) * 86_400L + (hour + 4) * 3_600L + minute * 60L) * 1_000L

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    /** Carlos: "Billetera" en efectivo (por defecto) y "BNB Bs", cuyo alias es su número de cuenta. */
    private suspend fun carlos() {
        fun created(result: WalletOperationResult) = assertIs<WalletOperationResult.WalletCreated>(result).wallet
        created(
            CreateWalletUseCase(ledger)("carlos", "Billetera", Currency.BOB, WalletType.CASH, bs("100"), occurredAtEpochMs = bolivia(1, 7)),
        )
        val bnb =
            created(
                CreateWalletUseCase(
                    ledger,
                )("carlos", "BNB Bs", Currency.BOB, WalletType.BANK, bs("500"), occurredAtEpochMs = bolivia(1, 7)),
            )
        SetWalletAliasesUseCase(ctx)("carlos", bnb.id, setOf(account))
    }

    /** Pago QR con la estructura del Banco Económico y datos inventados. */
    private fun qrReceipt(
        amount: String,
        time: String,
        reference: String,
        beneficiary: String = "TIENDA EJEMPLO",
    ) = """
        Banco ECONOMICO
        Pago QR otros bancos
        Procesada
        Beneficiario
        $beneficiary
        Importe
        Bs$amount
        Remitente
        Cuenta origen: CA: $account (Bs)
        Fecha y hora: $time
        Nro. de transacción: $reference
        """.trimIndent()

    private suspend fun say(
        phrase: String,
        at: Long,
        source: EvidenceSource = EvidenceSource.VOICE,
    ): Draft {
        clock.nowEpochMs = at
        return capture("carlos", Evidence(source, phrase), at).single()
    }

    private suspend fun share(
        text: String,
        at: Long,
    ): Draft = say(text, at, EvidenceSource.TEXT)

    private suspend fun questions(): List<SameMovementQuestion> = findQuestions("carlos", utcOffsetMinutes = -240)

    @Test
    fun `taxi y power del mismo precio preguntan si son el mismo gasto`() =
        runTest {
            carlos()
            val taxi = say("gasté 10 bs por qr", bolivia(13, 8))
            val power = say("gasté 10 bs por qr", bolivia(13, 9, 30))

            // Una frase corta repetida no es un comprobante repetido: es la pregunta de mismo día y monto
            assertEquals(listOf(SameMovementQuestion(power.id, taxi.id, SameMovementReason.SAME_DAY_AMOUNT)), questions())
        }

    @Test
    fun `el comprobante del power pregunta por el dictado del mismo pago`() =
        runTest {
            carlos()
            val audio = say("gasté 10 en power", bolivia(14, 10, 40))
            val receipt = share(qrReceipt("10.00", "14/Oct/2026 10:42:10", "400000123"), bolivia(14, 11, 30))

            assertEquals(listOf(SameMovementQuestion(receipt.id, audio.id, SameMovementReason.SAME_DAY_AMOUNT)), questions())
        }

    @Test
    fun `el comprobante de un gasto ya confirmado tambien pregunta`() =
        runTest {
            carlos()
            val taxi = say("pagué 40 de taxi por qr", bolivia(15, 8))
            assertIs<DraftResult.Ok>(confirm(taxi.id))
            val receipt = share(qrReceipt("40.00", "15/Oct/2026 08:01:00", "400000777"), bolivia(15, 15))

            assertEquals(listOf(SameMovementQuestion(receipt.id, taxi.id, SameMovementReason.SAME_DAY_AMOUNT)), questions())
        }

    @Test
    fun `otro dia u otro monto no pregunta nada`() =
        runTest {
            carlos()
            say("gasté 10 bs por qr", bolivia(13, 8))
            say("gasté 10 bs por qr", bolivia(14, 8))
            say("gasté 12 bs por qr", bolivia(14, 9))

            assertEquals(emptyList(), questions())
        }

    @Test
    fun `el mismo comprobante compartido dos veces avisa que esta repetido`() =
        runTest {
            carlos()
            val text = qrReceipt("25.00", "10/Oct/2026 14:22:50", "400000001", "POLLOS DEL VALLE")
            val first = share(text, bolivia(10, 14, 30))
            val second = share(text, bolivia(10, 21))

            assertEquals(listOf(SameMovementQuestion(second.id, first.id, SameMovementReason.SAME_REFERENCE)), questions())
        }

    @Test
    fun `la misma referencia con otro monto no es el mismo comprobante`() =
        runTest {
            carlos()
            share(qrReceipt("25.00", "10/Oct/2026 14:22:50", "123456"), bolivia(10, 14, 30))
            share(qrReceipt("80.00", "11/Oct/2026 09:00:00", "123456"), bolivia(11, 9, 30))

            assertEquals(emptyList(), questions())
        }

    @Test
    fun `la misma nota de voz compartida dos veces avisa que es el mismo archivo`() =
        runTest {
            carlos()
            clock.nowEpochMs = bolivia(12, 9)
            val note = Evidence(EvidenceSource.VOICE, attachmentRef = "audio-1", fingerprint = "huella-del-audio")
            val first = capture("carlos", note, bolivia(12, 9)).single()
            clock.nowEpochMs = bolivia(13, 20)
            val second = capture("carlos", note.copy(attachmentRef = "audio-2"), bolivia(13, 20)).single()

            // Capturados sin monto y en días distintos: igual son el mismo archivo
            assertEquals(listOf(SameMovementQuestion(second.id, first.id, SameMovementReason.SAME_FILE)), questions())
        }

    @Test
    fun `un borrador descartado ya no se compara`() =
        runTest {
            carlos()
            val taxi = say("gasté 10 bs por qr", bolivia(13, 8))
            say("gasté 10 bs por qr", bolivia(13, 9, 30))
            assertIs<DraftResult.Ok>(discard(taxi.id))

            assertEquals(emptyList(), questions())
        }

    // --- Respuestas ---

    private val resolve = ResolveSameMovementUseCase(ctx)
    private val inbox = GetDraftInboxUseCase(ctx)

    private suspend fun balanceOf(name: String): Money {
        val wallet = ctx.directory("carlos").wallets.single { it.name == name }
        return GetActiveWalletBalancesUseCase(wallets)("carlos").single { it.walletId == wallet.id }.balance
    }

    private suspend fun pending() = inbox("carlos").pending.map { it.id }

    @Test
    fun `son distintos quedan los dos, no vuelve a preguntar y el saldo baja 20`() =
        runTest {
            carlos()
            val taxi = say("gasté 10 bs por qr", bolivia(13, 8))
            val power = say("gasté 10 bs por qr", bolivia(13, 9, 30))

            assertIs<DraftResult.Ok>(resolve(power.id, taxi.id, same = false))

            assertEquals(emptyList(), questions())
            assertIs<DraftResult.Ok>(confirm(taxi.id))
            assertIs<DraftResult.Ok>(confirm(power.id))
            assertEquals(bs("480"), balanceOf("BNB Bs"))
        }

    @Test
    fun `son el mismo con el comprobante repetido baja el saldo una sola vez`() =
        runTest {
            carlos()
            val text = qrReceipt("25.00", "10/Oct/2026 14:22:50", "400000001", "POLLOS DEL VALLE")
            val first = share(text, bolivia(10, 14, 30))
            val second = share(text, bolivia(10, 21))

            assertIs<DraftResult.Ok>(resolve(second.id, first.id, same = true))

            assertEquals(listOf(first.id), pending())
            assertEquals(emptyList(), questions())
            assertIs<DraftResult.Ok>(confirm(first.id))
            assertEquals(bs("475"), balanceOf("BNB Bs"))
        }

    @Test
    fun `power con su comprobante queda con la nota dicha y los datos del comprobante`() =
        runTest {
            carlos()
            val audio = say("gasté 10 en power", bolivia(14, 10, 40))
            val receipt = share(qrReceipt("10.00", "14/Oct/2026 10:42:10", "400000123"), bolivia(14, 11, 30))

            val kept = assertIs<Draft.Ready>(assertIs<DraftResult.Ok>(resolve(receipt.id, audio.id, same = true)).draft)

            assertEquals(audio.id, kept.id)
            val movement = assertIs<ReadyMovement.Expense>(kept.movement)
            assertEquals("power", movement.note)
            assertEquals(bs("10"), movement.amount)
            assertEquals(
                ctx
                    .directory("carlos")
                    .wallets
                    .single { it.name == "BNB Bs" }
                    .id,
                movement.walletId,
            )
            assertEquals(bolivia(14, 10, 42) + 10_000, movement.occurredAtEpochMs)
            assertEquals("400000123", movement.reference)
        }

    @Test
    fun `al unir la evidencia no se pierde y el otro queda descartado enlazado`() =
        runTest {
            carlos()
            val audio = say("gasté 10 en power", bolivia(14, 10, 40))
            val receipt = share(qrReceipt("10.00", "14/Oct/2026 10:42:10", "400000123"), bolivia(14, 11, 30))

            val kept = assertIs<DraftResult.Ok>(resolve(receipt.id, audio.id, same = true)).draft

            assertEquals(listOf(audio.header.evidence), listOf(kept.header.evidence))
            assertEquals(listOf(receipt.header.evidence), kept.header.extraEvidence)
            val absorbed = assertIs<Draft.Discarded>(ctx.load(receipt.id))
            assertEquals(audio.id, absorbed.mergedInto)
            assertEquals(listOf(audio.id), pending())
        }

    @Test
    fun `unir con el taxi confirmado le agrega la referencia sin cambiar el saldo`() =
        runTest {
            carlos()
            val taxi = say("pagué 40 de taxi por qr", bolivia(15, 8))
            assertIs<DraftResult.Ok>(confirm(taxi.id))
            val receipt = share(qrReceipt("40.00", "15/Oct/2026 08:01:00", "400000777"), bolivia(15, 15))

            val kept = assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(resolve(receipt.id, taxi.id, same = true)).draft)

            assertEquals("400000777", (kept.movement as ReadyMovement.Expense).reference)
            assertEquals(listOf(receipt.header.evidence), kept.header.extraEvidence)
            assertEquals(bs("460"), balanceOf("BNB Bs"))
            assertEquals(emptyList(), pending())
        }

    @Test
    fun `si la billetera del confirmado no coincide no se unen`() =
        runTest {
            carlos()
            // Dictado sin medio: quedó en la billetera de efectivo por defecto
            val taxi = say("pagué 40 de taxi", bolivia(15, 8))
            assertIs<DraftResult.Ok>(confirm(taxi.id))
            val receipt = share(qrReceipt("40.00", "15/Oct/2026 08:01:00", "400000777"), bolivia(15, 15))

            val result = resolve(receipt.id, taxi.id, same = true)

            assertEquals(DraftError.WalletMismatch, assertIs<DraftResult.Rejected>(result).error)
            assertEquals(listOf(receipt.id), pending())
            assertEquals(bs("60"), balanceOf("Billetera"))
            assertEquals(bs("500"), balanceOf("BNB Bs"))
        }
}
