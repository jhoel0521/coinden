package com.coinden.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.bar_dictate
import coinden.composeapp.generated.resources.bar_listening
import coinden.composeapp.generated.resources.bar_photo
import coinden.composeapp.generated.resources.bar_write
import coinden.composeapp.generated.resources.capture_add_context
import coinden.composeapp.generated.resources.capture_add_to_last
import coinden.composeapp.generated.resources.notice_camera_denied
import coinden.composeapp.generated.resources.notice_dictation_unavailable
import coinden.composeapp.generated.resources.notice_dismiss
import coinden.composeapp.generated.resources.notice_microphone_denied
import coinden.composeapp.generated.resources.notice_saved_for_review
import coinden.composeapp.generated.resources.result_confirm
import coinden.composeapp.generated.resources.result_confirmed
import coinden.composeapp.generated.resources.result_done
import coinden.composeapp.generated.resources.result_not_ready
import coinden.composeapp.generated.resources.result_reading
import coinden.composeapp.generated.resources.result_rejected
import coinden.composeapp.generated.resources.result_saved
import coinden.composeapp.generated.resources.result_shortfall
import coinden.composeapp.generated.resources.result_voice_model
import coinden.composeapp.generated.resources.result_voice_model_button
import coinden.composeapp.generated.resources.same_already_confirmed
import coinden.composeapp.generated.resources.same_no
import coinden.composeapp.generated.resources.same_question
import coinden.composeapp.generated.resources.same_question_note
import coinden.composeapp.generated.resources.same_repeated_file
import coinden.composeapp.generated.resources.same_repeated_receipt
import coinden.composeapp.generated.resources.same_wallet
import coinden.composeapp.generated.resources.same_yes
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.format.TimeFormatter
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.CardTone
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdNotice
import com.coinden.app.ui.components.CdSheet
import com.coinden.app.ui.components.CdToolbar
import com.coinden.app.ui.components.NoticeKind
import com.coinden.core.domain.model.draft.SameMovementReason
import com.coinden.core.domain.model.interpret.UserLocale
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Los tres botones de captura, siempre visibles al pie de Inicio, al alcance del pulgar. */
@Composable
fun CaptureBar(
    listening: Boolean,
    onDictate: () -> Unit,
    onPhoto: () -> Unit,
    onWrite: () -> Unit,
    onAddToLast: (() -> Unit)? = null,
) {
    val actions =
        if (listening) {
            listOf(CdActionSpec(stringResource(Res.string.bar_listening), onDictate, ActionRole.Primary, icon = CoinDenIcon.STOP.vector))
        } else {
            listOf(
                CdActionSpec(stringResource(Res.string.bar_dictate), onDictate, ActionRole.Primary, icon = CoinDenIcon.MIC.vector),
                CdActionSpec(stringResource(Res.string.bar_photo), onPhoto, ActionRole.Primary, icon = CoinDenIcon.CAMERA.vector),
                CdActionSpec(stringResource(Res.string.bar_write), onWrite, ActionRole.Secondary, icon = CoinDenIcon.PENCIL.vector),
            ) + listOfNotNull(onAddToLast?.let { CdActionSpec(stringResource(Res.string.capture_add_to_last), it, ActionRole.Tertiary) })
        }
    CdToolbar(actions, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
}

/** Aviso de captura (dictado no disponible, permiso negado) con el porqué. */
@Composable
fun CaptureNoticeCard(
    notice: CaptureNotice,
    onDismiss: () -> Unit,
) {
    CdNotice(
        NoticeKind.Info,
        stringResource(notice.label()),
        Modifier.padding(horizontal = 16.dp),
        action = CdActionSpec(stringResource(Res.string.notice_dismiss), onDismiss, ActionRole.Tertiary),
    )
}

/**
 * Después de capturar: "Guardado como borrador", la tarjeta y la opción de confirmarlo ya.
 * Cerrar la hoja lo deja pendiente para la revisión.
 */
@Composable
fun CaptureResultSheet(
    result: CaptureResult,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    onConfirm: (draftId: String) -> Unit,
    onResolve: (draftId: String, same: Boolean) -> Unit,
    onDone: () -> Unit,
    reading: Set<String> = emptySet(),
    onVoiceModels: () -> Unit = {},
    onAddContext: ((draftId: String) -> Unit)? = null,
) {
    CdSheet(onDismiss = onDone) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(CoinDenIcon.CHECK_CIRCLE.vector, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(Res.string.result_saved), style = MaterialTheme.typography.titleLarge)
        }
        result.cards.forEach { card ->
            DraftCardView(card, locale)
            if (card.draftId in reading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.result_reading), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (card.draftId in result.voiceModelNeeded) {
                CdNotice(
                    NoticeKind.Info,
                    stringResource(Res.string.result_voice_model),
                    action = CdActionSpec(stringResource(Res.string.result_voice_model_button), onVoiceModels),
                )
            }
            card.question?.let { SameMovementPrompt(it, locale, utcOffsetMinutes) { same -> onResolve(card.draftId, same) } }
            val confirmed = card.draftId in result.confirmed
            when {
                confirmed -> CdNotice(NoticeKind.Success, stringResource(Res.string.result_confirmed))
                else -> result.problems[card.draftId]?.let { CdNotice(NoticeKind.Error, problemText(it, locale)) }
            }
            if (!confirmed) {
                CdActions(
                    listOfNotNull(
                        onAddContext?.let { CdActionSpec(stringResource(Res.string.capture_add_context), { it(card.draftId) }) },
                        CdActionSpec(stringResource(Res.string.result_confirm), { onConfirm(card.draftId) }, ActionRole.Primary)
                            .takeIf { card.status == DraftStatusView.READY },
                    ),
                )
            }
        }
        CdActions(listOf(CdActionSpec(stringResource(Res.string.result_done), onDone)), Modifier.padding(bottom = 8.dp))
    }
}

/** "¿Es el mismo gasto?" con la otra captura y las dos respuestas. Nunca decide sola. */
@Composable
fun SameMovementPrompt(
    question: SameMovementView,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    onAnswer: (same: Boolean) -> Unit,
) {
    CdCard(tone = CardTone.Warning) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(CoinDenIcon.ALERT.vector, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(questionText(question, locale, utcOffsetMinutes), style = MaterialTheme.typography.titleSmall)
        }
        question.otherWalletName?.let { Text(stringResource(Res.string.same_wallet, it), style = MaterialTheme.typography.bodySmall) }
        if (question.otherConfirmed) {
            Text(stringResource(Res.string.same_already_confirmed), style = MaterialTheme.typography.bodySmall)
        }
        CdActions(
            listOf(
                CdActionSpec(stringResource(Res.string.same_yes), { onAnswer(true) }, ActionRole.Primary),
                CdActionSpec(stringResource(Res.string.same_no), { onAnswer(false) }),
            ),
        )
    }
}

@Composable
private fun questionText(
    question: SameMovementView,
    locale: UserLocale,
    utcOffsetMinutes: Int,
): String =
    when (question.reason) {
        SameMovementReason.SAME_FILE -> stringResource(Res.string.same_repeated_file)
        SameMovementReason.SAME_REFERENCE, SameMovementReason.SAME_TEXT -> stringResource(Res.string.same_repeated_receipt)
        SameMovementReason.SAME_DAY_AMOUNT -> {
            val amount = question.otherAmount?.let { MoneyFormatter.format(it, locale) }.orEmpty()
            val time = TimeFormatter.hourMinute(question.otherCapturedAtEpochMs, utcOffsetMinutes)
            question.otherNote?.let { stringResource(Res.string.same_question_note, it, amount, time) }
                ?: stringResource(Res.string.same_question, amount, time)
        }
    }

@Composable
fun problemText(
    problem: ConfirmProblem,
    locale: UserLocale,
): String =
    when (problem) {
        is ConfirmProblem.InsufficientFunds -> stringResource(Res.string.result_shortfall, MoneyFormatter.format(problem.shortfall, locale))
        ConfirmProblem.NotReady -> stringResource(Res.string.result_not_ready)
        ConfirmProblem.Rejected -> stringResource(Res.string.result_rejected)
    }

private fun CaptureNotice.label(): StringResource =
    when (this) {
        CaptureNotice.DICTATION_UNAVAILABLE -> Res.string.notice_dictation_unavailable
        CaptureNotice.MICROPHONE_DENIED -> Res.string.notice_microphone_denied
        CaptureNotice.CAMERA_DENIED -> Res.string.notice_camera_denied
        CaptureNotice.SAVED_FOR_REVIEW -> Res.string.notice_saved_for_review
    }
