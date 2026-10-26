package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.perception.PerceptionService
import com.coinden.app.perception.ProcessingQueue
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Draft
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
enum class CaptureNotice { DICTATION_UNAVAILABLE, MICROPHONE_DENIED, CAMERA_DENIED, SAVED_FOR_REVIEW }

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
    /** Audios que no se pudieron transcribir porque falta un modelo de voz: se ofrece descargarlo. */
    val voiceModelNeeded: Set<String> = emptySet(),
)

data class CaptureState(
    val listening: Boolean = false,
    val notice: CaptureNotice? = null,
    val result: CaptureResult? = null,
    /** Borradores que se están leyendo en el teléfono ("Leyendo…"). */
    val reading: Set<String> = emptySet(),
    /** "Son el mismo" dejó un cambio pendiente sobre un confirmado: se abre el formulario de corrección. */
    val correction: FormMode.Correct? = null,
    /** Lo recién capturado es un paquete abierto: la pantalla abre su página de contexto. */
    val contextBundle: String? = null,
)

/**
 * Captura desde Inicio: dictado local, foto o galería y lo compartido desde otras apps; muestra el
 * borrador y permite confirmarlo en el acto. No usa Compose, así que se prueba en la JVM.
 */
class CapturePresenter(
    private val container: AppContainer,
    private val dictation: DictationPort,
    /** Cola que lee fotos, PDF y audios en segundo plano; sin ella, quedan Capturados. */
    private val queue: ProcessingQueue? = null,
    /** Para saber si un audio puede transcribirse (si no, se ofrece descargar un modelo de voz). */
    private val perception: PerceptionService? = null,
) {
    private val cards = DraftCards(container)
    private val quick = queue?.let { QuickCapture(container, it) }
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
                    ?.let { captureWithContext(Evidence(EvidenceSource.VOICE, it)) }
            DictationOutcome.NothingHeard -> Unit
            DictationOutcome.Unavailable -> mutableState.update { it.copy(notice = CaptureNotice.DICTATION_UNAVAILABLE) }
        }
    }

    /** El segundo toque corta el dictado sin crear borrador. */
    fun cancelDictation() = dictation.cancel()

    suspend fun photo(
        bytes: ByteArray,
        mimeType: String,
    ) {
        if (quick == null) return captureFile(bytes, mimeType)
        val ref = container.attachments.save(bytes, mimeType)
        captureWithContext(Evidence(EvidenceSource.PHOTO, attachmentRef = ref, fingerprint = sha256Hex(bytes)))
    }

    /** La pantalla ya abrió la página de contexto del paquete. */
    fun contextOpened() = mutableState.update { it.copy(contextBundle = null) }

    /**
     * La página de contexto se cerró (Listo, Atrás o salir de la app), así que el paquete ya está
     * cerrado. Se manda a la cola y se muestra lo que se entendió de todo junto; si todavía hay
     * algo por leer, se avisa que aparece en Revisar.
     */
    suspend fun contextClosed(bundleId: String) {
        queue?.kick()
        val bundle = container.bundles.load(bundleId)
        val drafts = bundle?.let { container.bundles.draftsOf(it) }.orEmpty()
        mutableState.update {
            if (drafts.isEmpty()) {
                it.copy(contextBundle = null, notice = CaptureNotice.SAVED_FOR_REVIEW)
            } else {
                it.copy(contextBundle = null, result = CaptureResult(cards.of(drafts)))
            }
        }
    }

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

    /** La pantalla ya abrió el formulario de corrección. */
    fun correctionOpened() = mutableState.update { it.copy(correction = null, result = null) }

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

    /**
     * Responde "¿es el mismo gasto?". Son el mismo: la tarjeta pasa a ser la del borrador que queda;
     * si el otro ya estaba confirmado y este es un comprobante, se pide abrir el formulario de
     * corrección ([CaptureState.correction]). Son distintos: la pregunta desaparece y quedan los dos.
     */
    suspend fun resolve(
        draftId: String,
        same: Boolean,
    ) {
        val result = state.value.result ?: return
        val question = result.cards.firstOrNull { it.draftId == draftId }?.question ?: return
        val updated =
            when (val outcome = container.resolveSameMovement(draftId, question.otherDraftId, same)) {
                is DraftResult.Ok -> {
                    outcome.draft.header.correctionTarget?.let { target ->
                        mutableState.update { it.copy(correction = FormMode.Correct(target, outcome.draft.id)) }
                    }
                    val fresh = cards.of(listOf(outcome.draft)).single()
                    result.copy(cards = result.cards.map { if (it.draftId == draftId) fresh else it }, problems = result.problems - draftId)
                }
                is DraftResult.Rejected -> result.copy(problems = result.problems + (draftId to problemOf(outcome.error)))
            }
        mutableState.update { it.copy(result = updated) }
    }

    private suspend fun captureFile(
        bytes: ByteArray,
        mimeType: String,
    ) {
        val draft =
            quick?.file(bytes, mimeType) ?: run {
                // Sin cola: se guarda igual y queda Capturado.
                val source = if (mimeType.startsWith("audio/")) EvidenceSource.VOICE else EvidenceSource.PHOTO
                val ref = container.attachments.save(bytes, mimeType)
                container.captureDraft(container.ownerId, Evidence(source, attachmentRef = ref, fingerprint = sha256Hex(bytes))).single()
            }
        // La captura ya está guardada: se muestra y la cola la lee cuando pueda.
        mutableState.update { it.copy(result = CaptureResult(cards.of(listOf(draft))), reading = it.reading + draft.id) }
        refresh(draft.id)
    }

    /**
     * Escucha la cola mientras la pantalla está abierta: cada borrador procesado actualiza su
     * tarjeta. La pantalla la llama una vez; termina al cerrarse.
     */
    suspend fun followProcessing() {
        queue?.processed?.collect { draftId ->
            mutableState.update { it.copy(reading = it.reading - draftId) }
            refresh(draftId)
        }
    }

    /** Vuelve a mostrar el borrador como está ahora; si es un audio sin modelo de voz, ofrece descargarlo. */
    private suspend fun refresh(draftId: String) {
        val result = state.value.result ?: return
        if (result.cards.none { it.draftId == draftId }) return
        val draft = container.drafts.load(draftId) ?: return
        val pending = draft is Draft.Captured && draft.header.perceivedText == null && draft.header.evidence.attachmentRef != null
        val needsModel = pending && draft.header.evidence.source == EvidenceSource.VOICE && perception?.canTranscribe() == false
        val fresh = cards.of(listOf(draft)).single()
        mutableState.update { state ->
            val current = state.result ?: return@update state
            state.copy(
                result =
                    current.copy(
                        cards = current.cards.map { if (it.draftId == draftId) fresh else it },
                        voiceModelNeeded = if (needsModel) current.voiceModelNeeded + draftId else current.voiceModelNeeded - draftId,
                    ),
                reading = if (pending && !needsModel) state.reading else state.reading - draftId,
            )
        }
    }

    /**
     * Lo capturado en la app pasa por la página de contexto: queda como paquete abierto y la
     * pantalla la muestra. Sin cola (pruebas viejas, plataformas sin segundo plano) se interpreta
     * suelto, como siempre.
     */
    private suspend fun captureWithContext(evidence: Evidence) {
        val bundleId = quick?.capturedBundle(evidence) ?: return capture(evidence)
        mutableState.update { it.copy(contextBundle = bundleId, result = null) }
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
