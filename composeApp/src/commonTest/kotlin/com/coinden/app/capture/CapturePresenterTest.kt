package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.WalletSuggestion
import com.coinden.core.domain.model.draft.WalletSuggestionReason
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Dictado falso: devuelve lo que la prueba diga que se escuchó. */
private class FakeDictation(
    private val outcome: DictationOutcome,
) : DictationPort {
    var language: String? = null

    override suspend fun listen(languageCode: String): DictationOutcome {
        language = languageCode
        return outcome
    }

    override fun cancel() = Unit
}

/** Carlos (Bolivia) captura desde Inicio: dicta, fotografía, comparte y confirma. */
class CapturePresenterTest {
    private suspend fun carlos(
        dictation: DictationPort = FakeDictation(DictationOutcome.NothingHeard),
    ): Pair<AppContainer, CapturePresenter> {
        val container = testContainer()
        seedCarlos(container)
        return container to CapturePresenter(container, dictation)
    }

    private fun CapturePresenter.card() = assertNotNull(state.value.result).cards.single()

    private suspend fun AppContainer.balanceOf(name: String): Money {
        val wallet = drafts.directory(ownerId).wallets.single { it.name == name }
        return walletBalances(ownerId).single { it.walletId == wallet.id }.balance
    }

    private fun bs(value: String) = Money.parseCanonical(value, Currency.BOB)

    @Test
    fun `dictar almuerzo 45 bnb guarda un borrador de voz interpretado en el idioma de la app`() =
        runTest {
            val dictation = FakeDictation(DictationOutcome.Heard("almuerzo 45 bnb"))
            val (_, presenter) = carlos(dictation)

            presenter.dictate()

            val card = presenter.card()
            assertEquals("es", dictation.language)
            assertEquals(EvidenceSource.VOICE, card.evidenceSource)
            assertEquals("almuerzo 45 bnb", card.evidenceText)
            assertEquals(MovementKindView.EXPENSE, card.kind)
            assertEquals(bs("45"), card.amount)
            assertEquals("BNB Bs", card.walletName)
            assertEquals(false, presenter.state.value.listening)
        }

    @Test
    fun `sin reconocimiento de voz local se avisa y no se crea borrador`() =
        runTest {
            val (container, presenter) = carlos(FakeDictation(DictationOutcome.Unavailable))

            presenter.dictate()

            assertEquals(CaptureNotice.DICTATION_UNAVAILABLE, presenter.state.value.notice)
            assertNull(presenter.state.value.result)
            assertTrue(container.draftInbox(container.ownerId).pending.isEmpty())
        }

    @Test
    fun `una foto queda capturada con su archivo guardado`() =
        runTest {
            val (container, presenter) = carlos()
            val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)

            presenter.photo(jpeg, "image/jpeg")

            val card = presenter.card()
            assertEquals(EvidenceSource.PHOTO, card.evidenceSource)
            assertEquals(DraftStatusView.CAPTURED, card.status)
            val saved = assertNotNull(container.attachments.read(assertNotNull(card.attachmentRef)))
            assertContentEquals(jpeg, saved.bytes)
            assertEquals("image/jpeg", saved.mimeType)
        }

    @Test
    fun `el texto compartido de un comprobante se lee al instante`() =
        runTest {
            val (_, presenter) = carlos()
            // Estructura de un pago QR del Banco Económico con datos inventados
            val receipt =
                """
                Banco ECONOMICO
                Pago QR otros bancos
                Procesada
                Beneficiario
                POLLOS DEL VALLE
                Importe
                Bs25.00
                Nota: POLLOS DEL VALLE
                Fecha y hora: 10/Oct/2026 14:22:50
                """.trimIndent()

            presenter.shared(SharedContent.Text(receipt))

            val card = presenter.card()
            assertEquals(EvidenceSource.TEXT, card.evidenceSource)
            assertEquals(MovementKindView.EXPENSE, card.kind)
            assertEquals(bs("25"), card.amount)
        }

    @Test
    fun `una imagen, un pdf y una nota de voz compartidos quedan capturados con su archivo`() =
        runTest {
            val (container, presenter) = carlos()
            val files =
                listOf(
                    "image/png" to EvidenceSource.PHOTO,
                    "application/pdf" to EvidenceSource.PHOTO,
                    "audio/ogg" to EvidenceSource.VOICE,
                )
            for ((mime, source) in files) {
                val bytes = mime.encodeToByteArray()

                presenter.shared(SharedContent.File(bytes, mime))

                val card = presenter.card()
                assertEquals(source, card.evidenceSource, mime)
                assertEquals(DraftStatusView.CAPTURED, card.status, mime)
                val saved = assertNotNull(container.attachments.read(assertNotNull(card.attachmentRef)))
                assertContentEquals(bytes, saved.bytes)
                assertEquals(mime, saved.mimeType)
            }
            assertEquals(3, container.draftInbox(container.ownerId).pending.size)
        }

    @Test
    fun `la tarjeta trae el motivo de la billetera sugerida`() =
        runTest {
            val (_, presenter) = carlos()

            presenter.shared(SharedContent.Text("almuerzo 45 qr"))

            assertEquals(WalletSuggestion(WalletSuggestionReason.ONLY_CANDIDATE, candidates = 1), presenter.card().walletSuggestion)
        }

    @Test
    fun `confirmar almuerzo 45 bnb registra el gasto en BNB Bs clasificado como comida`() =
        runTest {
            val (container, presenter) = carlos(FakeDictation(DictationOutcome.Heard("almuerzo 45 bnb")))
            presenter.dictate()
            val draftId = presenter.card().draftId

            presenter.confirm(draftId)

            val result = assertNotNull(presenter.state.value.result)
            assertEquals(setOf(draftId), result.confirmed)
            assertEquals(bs("455"), container.balanceOf("BNB Bs"))
            val classification = container.categorization.current(ConfirmDraftUseCase.ledgerEventIdFor(draftId))
            assertEquals(CategoryRef.Base(BaseCategory.FOOD), classification?.category)
            assertTrue(container.draftInbox(container.ownerId).pending.isEmpty())
        }

    @Test
    fun `confirmar sin saldo suficiente deja el borrador pendiente con el faltante`() =
        runTest {
            val (container, presenter) = carlos(FakeDictation(DictationOutcome.Heard("page 300 bs para trasmporte")))
            presenter.dictate()
            val draftId = presenter.card().draftId

            presenter.confirm(draftId)

            val result = assertNotNull(presenter.state.value.result)
            assertEquals(emptySet(), result.confirmed)
            assertEquals(ConfirmProblem.InsufficientFunds(bs("200")), result.problems[draftId])
            assertEquals(bs("100"), container.balanceOf("Billetera"))
            assertEquals(1, container.draftInbox(container.ownerId).pending.size)
        }

    @Test
    fun `lo guardado en el formulario se confirma desde el resultado con la categoria elegida`() =
        runTest {
            val (container, presenter) = carlos()
            val form = DraftFormPresenter(container).also { it.load() }
            form.onAmountChange("30")
            form.onWalletChange(
                form.state.value.wallets
                    .single { it.name == "BNB Bs" }
                    .id,
            )
            form.onCategoryChange(CategoryRef.Base(BaseCategory.GROCERIES))

            presenter.showResult(form.save())
            val draftId = presenter.card().draftId
            presenter.confirm(draftId)

            assertEquals(bs("470"), container.balanceOf("BNB Bs"))
            val classification = container.categorization.current(ConfirmDraftUseCase.ledgerEventIdFor(draftId))
            assertEquals(CategoryRef.Base(BaseCategory.GROCERIES), classification?.category)
        }

    @Test
    fun `cerrar el resultado lo quita y el aviso se puede descartar`() =
        runTest {
            val (_, presenter) = carlos(FakeDictation(DictationOutcome.Unavailable))
            presenter.dictate()
            presenter.dismissNotice()
            presenter.shared(SharedContent.Text("almuerzo 45 bnb"))

            presenter.dismissResult()

            assertNull(presenter.state.value.notice)
            assertNull(presenter.state.value.result)
        }
}
