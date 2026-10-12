package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.AnonymizedReceipts
import com.coinden.core.domain.feature.FakeInstalledPacks
import com.coinden.core.domain.feature.FixedRegionalSettings
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.ReceiptTemplates
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TextDraftInterpreterTest {
    private val directory =
        WalletDirectory(
            listOf(
                Wallet("billetera", "carlos", "Billetera", Currency.BOB, WalletType.CASH),
                Wallet("yape", "carlos", "Yape", Currency.BOB, WalletType.DIGITAL),
            ),
            aliases = mapOf("yape" to "yape"),
            defaultWalletId = "billetera",
        )
    private val packs = FakeInstalledPacks(listOf(LanguagePacks.SPANISH), ReceiptTemplates.all)
    private val interpreter = TextDraftInterpreter(packs, FixedRegionalSettings())

    private suspend fun interpret(
        text: String?,
        source: EvidenceSource = EvidenceSource.TEXT,
    ) = interpreter.interpret(Evidence(source, text), directory)

    @Test
    fun `texto vacio no propone nada`() =
        runTest {
            assertEquals(emptyList(), interpret(null, EvidenceSource.PHOTO))
            assertEquals(emptyList(), interpret("   ", EvidenceSource.PHOTO))
        }

    @Test
    fun `una plantilla instalada tiene prioridad`() =
        runTest {
            val proposal = assertIs<MovementProposal.Expense>(interpret(AnonymizedReceipts.YAPE_PAYMENT, EvidenceSource.PHOTO).single())
            assertEquals(WalletRef.ByHint("yape"), proposal.wallet)
            assertEquals(FieldOrigin.EVIDENCE, proposal.origins[DraftField.KIND])
        }

    @Test
    fun `sin la plantilla el mismo comprobante va al lector generico`() =
        runTest {
            packs.templates = emptyList()
            val proposal = assertIs<MovementProposal.Expense>(interpret(AnonymizedReceipts.YAPE_PAYMENT, EvidenceSource.PHOTO).single())
            assertEquals(FieldOrigin.INFERRED, proposal.origins[DraftField.KIND])
        }

    @Test
    fun `una frase va al lector de frases`() =
        runTest {
            val proposal = assertIs<MovementProposal.Expense>(interpret("page 3 bs para trasmporte").single())
            assertEquals("trasmporte", proposal.note)
            assertEquals(FieldOrigin.SUGGESTED, proposal.origins[DraftField.WALLET])
        }

    @Test
    fun `el ingles siempre esta activo aunque no se instale`() =
        runTest {
            packs.packs = emptyList()
            val proposal = assertIs<MovementProposal.Expense>(interpret("paid 12 usd for lunch").single())
            assertEquals("lunch", proposal.note)
        }

    @Test
    fun `misma entrada, misma salida`() =
        runTest {
            listOf(
                "page 3 bs para trasmporte",
                AnonymizedReceipts.BE_QR_CHICKEN,
                AnonymizedReceipts.AR_UNKNOWN_TRANSFER,
            ).forEach { text ->
                assertEquals(interpret(text, EvidenceSource.PHOTO), interpret(text, EvidenceSource.PHOTO))
            }
        }
}
