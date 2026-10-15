package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.DraftError
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.wallet.WalletError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Lo que otra app compartió con CoinDen. */
sealed interface SharedContent {
    class Text(
        val text: String,
    ) : SharedContent

    /** Imagen, PDF o audio, copiado entero: el enlace de la otra app deja de valer después. */
    class File(
        val bytes: ByteArray,
        val mimeType: String,
    ) : SharedContent
}

/** Avisos de la captura (la pantalla los traduce). */
enum class CaptureNotice { DICTATION_UNAVAILABLE, MICROPHONE_DENIED, CAMERA_DENIED }

/** Por qué no se pudo confirmar un borrador. */
sealed interface ConfirmProblem {
    /** Quedaría en negativo: [shortfall] es lo que falta en la moneda de la billetera. */
    data class InsufficientFunds(
        val shortfall: Money,
    ) : ConfirmProblem

    /** Le faltan datos o no es un movimiento: se completa en la bandeja. */
    data object NotReady : ConfirmProblem

    data object Rejected : ConfirmProblem
}

/** Resultado de la última captura: sus borradores y lo que pasó al confirmarlos. */
data class CaptureResult(
    val cards: List<DraftCard>,
    val confirmed: Set<String> = emptySet(),
    val problems: Map<String, ConfirmProblem> = emptyMap(),
)

data class CaptureState(
    val listening: Boolean = false,
    val notice: CaptureNotice? = null,
    val result: CaptureResult? = null,
)

/**
 * Captura desde Inicio: dictado local, foto o galería y lo compartido desde otras apps; muestra el
 * borrador y permite confirmarlo en el acto. No usa Compose, así que se prueba en la JVM.
 */
class CapturePresenter(
    private val container: AppContainer,
    private val dictation: DictationPort,
) {
    private val cards = DraftCards(container)
    private val mutableState = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = mutableState.asStateFlow()

    /** Un toque empieza a escuchar; el reconocedor termina solo con el silencio. */
    suspend fun dictate() {
        if (state.value.listening) return
        mutableState.update { it.copy(listening = true, notice = null) }
        val outcome =
            try {
                dictation.listen(container.languageCode)
            } finally {
                mutableState.update { it.copy(listening = false) }
            }
        when (outcome) {
            is DictationOutcome.Heard ->
                outcome.text
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { capture(Evidence(EvidenceSource.VOICE, it)) }
            DictationOutcome.NothingHeard -> Unit
            DictationOutcome.Unavailable -> mutableState.update { it.copy(notice = CaptureNotice.DICTATION_UNAVAILABLE) }
        }
    }

    /** El segundo toque corta el dictado sin crear borrador. */
    fun cancelDictation() = dictation.cancel()

    suspend fun photo(
        bytes: ByteArray,
        mimeType: String,
    ) = captureFile(bytes, mimeType)

    suspend fun shared(content: SharedContent) {
        when (content) {
            is SharedContent.Text ->
                content.text
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { capture(Evidence(EvidenceSource.TEXT, it)) }
            is SharedContent.File -> captureFile(content.bytes, content.mimeType)
        }
    }

    fun permissionDenied(notice: CaptureNotice) = mutableState.update { it.copy(notice = notice) }

    fun dismissNotice() = mutableState.update { it.copy(notice = null) }

    fun dismissResult() = mutableState.update { it.copy(result = null) }

    /** Muestra el resultado de una captura hecha en otra pantalla (el formulario). */
    fun showResult(result: CaptureResult) = mutableState.update { it.copy(result = result) }

    /** Confirma con la categoría sugerida; si el libro lo rechaza, el borrador sigue pendiente. */
    suspend fun confirm(draftId: String) {
        val result = state.value.result ?: return
        val card = result.cards.firstOrNull { it.draftId == draftId } ?: return
        val updated =
            when (val outcome = container.confirmDraft(draftId, card.category?.ref)) {
                is DraftResult.Ok -> result.copy(confirmed = result.confirmed + draftId, problems = result.problems - draftId)
                is DraftResult.Rejected -> result.copy(problems = result.problems + (draftId to problemOf(outcome.error)))
            }
        mutableState.update { it.copy(result = updated) }
    }

    private suspend fun captureFile(
        bytes: ByteArray,
        mimeType: String,
    ) {
        val source = if (mimeType.startsWith("audio/")) EvidenceSource.VOICE else EvidenceSource.PHOTO
        val ref = container.attachments.save(bytes, mimeType)
        capture(Evidence(source, attachmentRef = ref))
    }

    private suspend fun capture(evidence: Evidence) {
        val drafts = container.captureDraft(container.ownerId, evidence)
        mutableState.update { it.copy(result = CaptureResult(cards.of(drafts))) }
    }

    private fun problemOf(error: DraftError): ConfirmProblem =
        when (error) {
            is DraftError.NotReady -> ConfirmProblem.NotReady
            is DraftError.Wallet ->
                (error.error as? WalletError.InsufficientFunds)?.let { ConfirmProblem.InsufficientFunds(it.shortfall) }
                    ?: ConfirmProblem.Rejected
            else -> ConfirmProblem.Rejected
        }
}
